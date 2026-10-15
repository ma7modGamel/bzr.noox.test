<?php

declare(strict_types=1);

namespace App\Support;

use App\Support\Exceptions\DomainException;
use Illuminate\Contracts\Cache\Repository;
use Illuminate\Log\Events\MessageLogged;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Throwable;

final class SlackErrorReporter
{
    private bool $sending = false;

    public function report(MessageLogged $event): ?string
    {
        if ($this->sending || ! config('services.slack.errors.enabled')
            || ! in_array($event->level, ['error', 'critical', 'alert', 'emergency'], true)) {
            return null;
        }
        $exception = ($event->context['exception'] ?? null) instanceof Throwable ? $event->context['exception'] : null;
        if ($exception instanceof DomainException && $exception->httpStatus() < 500) {
            return null;
        }
        $this->sending = true;
        try {
            $name = is_string($event->message) && preg_match('/\A[a-z][a-z0-9_.-]{0,99}\z/', $event->message)
                ? $event->message : 'exception';
            $location = $exception === null ? 'application log' : $this->relativeFile($exception->getFile()).':'.$exception->getLine();
            $origin = $location;
            foreach ($exception?->getTrace() ?? [] as $frame) {
                if (isset($frame['file']) && str_starts_with($frame['file'], app_path().'/')) {
                    $origin = $this->relativeFile($frame['file']).':'.($frame['line'] ?? 0);
                    break;
                }
            }
            $logGroup = $name === 'exception' && $exception === null
                ? hash('sha256', preg_replace('/\d+/', '#', (string) $event->message) ?? '') : $name;
            $fingerprint = hash('sha256', implode('|', [config('app.url'), app()->environment(), $exception ? $exception::class : $logGroup, $origin, (string) ($exception?->getCode() ?? 0)]));
            $cache = $this->cache();
            $cache->lock('slack-errors:lock:'.$fingerprint, 10)->block(1, function () use ($cache, $fingerprint, $event, $exception, $name, $location): void {
                $key = 'slack-errors:issue:'.$fingerprint;
                $state = $cache->get($key, ['count' => 0, 'notified' => false, 'resolved' => false]);
                $state['count']++;
                $state['last_seen_at'] = now()->toIso8601String();
                $state['release'] = config('services.slack.errors.release');
                if (! $state['notified'] || $state['resolved']) {
                    $request = app()->bound('request') ? request() : null;
                    $context = [
                        'Source' => 'Laravel backend',
                        'Issue' => $state['resolved'] ? 'REGRESSED / عاد بعد تسجيل إصلاحه' : 'NEW / خطأ جديد',
                        'Environment' => app()->environment(),
                        'Release' => (string) config('services.slack.errors.release'),
                        'Severity' => $event->level,
                        'Exception / event' => $exception ? $exception::class : $name,
                        'Location' => $location,
                        'Occurrences recorded' => (string) $state['count'],
                        'Issue ID' => $fingerprint,
                        'UTC time' => now()->utc()->toIso8601String(),
                        'PHP' => PHP_VERSION,
                        'Request' => app()->runningInConsole() ? 'CLI / queue / scheduler' : ($request?->method().' '.($request?->route()?->uri() ?? '[unmatched route]')),
                        'Client platform' => $this->header('X-App-Platform', '/\A(?:Android|iOS)\z/') ?? 'Web / unknown',
                        'Client version' => $this->header('X-App-Version', '/\A[0-9][0-9A-Za-z.+_-]{0,39}\z/') ?? 'unknown',
                        'Client build' => $this->header('X-App-Build', '/\A[0-9]{1,12}\z/') ?? 'unknown',
                        'Device' => $this->header('X-Device-Model', '/\A[A-Za-z0-9][A-Za-z0-9 .,()_-]{0,79}\z/') ?? 'unknown',
                        'OS' => $this->header('X-OS-Version', '/\A[A-Za-z0-9][A-Za-z0-9 .()_-]{0,59}\z/') ?? 'unknown',
                        'Mode' => $this->header('X-App-Mode', '/\A(?:CUSTOMER|PROVIDER)\z/') ?? 'unknown',
                    ];
                    $details = implode("\n", array_map(fn (string $key, string $value): string => $key.': '.$value, array_keys($context), $context));
                    $trace = $exception === null ? '' : $this->trace($exception);
                    if ($this->send('Bremo | Laravel | '.$context['Issue'], $details."\n\n".$trace
                        ."\nRaw messages, SQL, request bodies, tokens and customer details omitted. Original error: private server log.")) {
                        $state['notified'] = true;
                        $state['resolved'] = false;
                    }
                }
                $cache->forever($key, $state);
            });

            return $fingerprint;
        } catch (Throwable) {
            error_log('Bremo Slack reporting failed; the original error remains in its normal log.');

            return null;
        } finally {
            $this->sending = false;
        }
    }

    public function resolve(string $fingerprint): bool
    {
        if (! preg_match('/\A[a-f0-9]{64}\z/', $fingerprint)) {
            return false;
        }
        $cache = $this->cache();

        return (bool) $cache->lock('slack-errors:lock:'.$fingerprint, 10)->block(1, function () use ($cache, $fingerprint): bool {
            $key = 'slack-errors:issue:'.$fingerprint;
            $state = $cache->get($key);
            if (! is_array($state)) {
                return false;
            }
            $state['resolved'] = true;
            $state['resolved_at'] = now()->toIso8601String();
            $cache->forever($key, $state);

            return true;
        });
    }

    public function testConnection(): bool
    {
        return config('services.slack.errors.enabled') && $this->send('Bremo | Laravel | TEST',
            "Connection test only; no application crash occurred.\nEnvironment: ".app()->environment()
            ."\nRelease: ".config('services.slack.errors.release')."\nPHP: ".PHP_VERSION);
    }

    private function cache(): Repository
    {
        return Cache::store((string) config('services.slack.errors.cache_store', 'file'));
    }

    private function header(string $name, string $pattern): ?string
    {
        $value = app()->bound('request') ? request()->header($name) : null;

        return is_string($value) && preg_match($pattern, $value) ? $value : null;
    }

    private function relativeFile(string $file): string
    {
        return str_starts_with($file, base_path().'/') ? substr($file, strlen(base_path()) + 1) : basename($file);
    }

    private function trace(Throwable $exception): string
    {
        $lines = [];
        foreach (array_slice($exception->getTrace(), 0, 8) as $frame) {
            $lines[] = ($frame['class'] ?? '').($frame['type'] ?? '').($frame['function'] ?? '')
                .' at '.$this->relativeFile($frame['file'] ?? '[internal]').':'.($frame['line'] ?? 0);
        }

        return "Stack (arguments omitted):\n".implode("\n", $lines);
    }

    private function send(string $title, string $details): bool
    {
        $url = (string) config('services.slack.errors.webhook_url');
        if (! preg_match('~\Ahttps://hooks\.slack\.com/services/[A-Za-z0-9]+/[A-Za-z0-9]+/[A-Za-z0-9]+\z~', $url)) {
            return false;
        }
        try {
            $response = Http::connectTimeout(2)->timeout(4)->withoutRedirecting()->post($url, [
                'text' => $title,
                'blocks' => [
                    ['type' => 'header', 'text' => ['type' => 'plain_text', 'text' => $title]],
                    ['type' => 'section', 'text' => ['type' => 'plain_text', 'text' => mb_substr($details, 0, 2900)]],
                ],
            ]);
            if ($response->successful() && trim($response->body()) === 'ok') {
                return true;
            }
            error_log('Bremo Slack delivery failed (HTTP '.$response->status().').');
        } catch (Throwable) {
            error_log('Bremo Slack delivery failed (connection error).');
        }

        return false;
    }
}
