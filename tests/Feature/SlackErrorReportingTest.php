<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\SlackErrorReporter;
use Illuminate\Http\Client\Factory;
use Illuminate\Log\Events\MessageLogged;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Route;
use PHPUnit\Framework\Attributes\Test;
use RuntimeException;
use Tests\TestCase;

final class SlackErrorReportingTest extends TestCase
{
    private const string WEBHOOK = 'https://hooks.slack.com/services/TTEST/BTEST/testsecret';

    protected function setUp(): void
    {
        parent::setUp();
        config(['services.slack.errors.enabled' => true, 'services.slack.errors.webhook_url' => self::WEBHOOK,
            'services.slack.errors.cache_store' => 'array', 'services.slack.errors.release' => 'test-build',
            'logging.default' => 'null']);
        Cache::store('array')->flush();
        Http::preventStrayRequests();
        Http::fake([self::WEBHOOK => Http::response('ok')]);
    }

    #[Test]
    public function actual_log_events_group_repeats_and_a_resolved_issue_alerts_on_its_return(): void
    {
        Log::error('push.fcm.rejected', ['status' => 503]);
        Log::error('push.fcm.rejected', ['status' => 503]);
        Http::assertSentCount(1);
        $request = Http::recorded()[0][0];
        preg_match('/Issue ID: ([a-f0-9]{64})/', $request['blocks'][1]['text']['text'], $match);
        $fingerprint = $match[1];
        $this->assertSame(2, Cache::store('array')->get('slack-errors:issue:'.$fingerprint)['count']);
        $this->assertSame(0, Artisan::call('errors:resolve', ['fingerprint' => $fingerprint]));
        Log::error('push.fcm.rejected');
        Log::error('push.fcm.rejected');
        Http::assertSentCount(2);
        $details = Http::recorded()[1][0]['blocks'][1]['text']['text'];
        $this->assertStringContainsString('REGRESSED', $details);
        $this->assertStringContainsString('Occurrences recorded: 3', $details);
        $this->assertSame(1, Artisan::call('errors:resolve', ['fingerprint' => 'unknown']));
    }

    #[Test]
    public function exception_handler_reports_real_server_errors_with_safe_mobile_context(): void
    {
        Route::get('/api/v1/monitoring-test/{id}', fn () => throw $this->serverError());
        $this->withHeaders(['X-App-Platform' => 'Android', 'X-App-Version' => '1.0', 'X-App-Build' => '2',
            'X-Device-Model' => 'Google Pixel 8', 'X-OS-Version' => 'Android 15', 'X-App-Mode' => 'CUSTOMER',
            'Authorization' => 'Bearer secret-token'])
            ->getJson('/api/v1/monitoring-test/123?password=hidden-query')->assertStatus(500);
        Http::assertSentCount(1);
        $payload = json_encode(Http::recorded()[0][0]->data(), JSON_THROW_ON_ERROR);
        foreach (['Android', 'Google Pixel 8', 'RuntimeException', 'test-build'] as $value) {
            $this->assertStringContainsString($value, $payload);
        }
        foreach (['secret-token', 'hidden-query', 'customer@example.test', 'private-password', 'SELECT *', '123?'] as $value) {
            $this->assertStringNotContainsString($value, $payload);
        }
    }

    #[Test]
    public function informational_events_disabled_monitoring_and_expected_business_errors_do_not_alert(): void
    {
        Log::warning('normal.warning');
        Log::info('normal.info');
        report(new BusinessRuleViolationException('Expected user validation'));
        config(['services.slack.errors.enabled' => false]);
        Log::critical('real.failure');
        Http::assertNothingSent();
        $this->assertSame(1, Artisan::call('errors:slack-test'));
    }

    #[Test]
    public function failed_delivery_can_be_retried_by_the_next_occurrence_without_hiding_the_application_error(): void
    {
        Http::swap(new Factory);
        Http::preventStrayRequests();
        Http::fake([self::WEBHOOK => Http::sequence()->push('unavailable', 503)->push('ok')]);
        Log::error('integration.failed');
        Log::error('integration.failed');
        Log::error('integration.failed');
        Http::assertSentCount(2);
    }

    #[Test]
    public function connection_failure_and_invalid_webhook_do_not_throw_or_leak_secrets(): void
    {
        Http::swap(new Factory);
        Http::preventStrayRequests();
        Http::fake([self::WEBHOOK => Http::failedConnection()]);
        $this->assertFalse(app(SlackErrorReporter::class)->testConnection());
        config(['services.slack.errors.webhook_url' => 'https://untrusted.test/services/test']);
        $this->assertFalse(app(SlackErrorReporter::class)->testConnection());
        Http::assertSentCount(1);
    }

    #[Test]
    public function lost_cache_does_not_replace_the_original_error_with_a_monitoring_failure(): void
    {
        Cache::shouldReceive('store')->once()->andThrow(new RuntimeException('private-cache-secret'));
        $this->assertNull(app(SlackErrorReporter::class)->report(new MessageLogged('error', 'test.failure')));
        Http::assertNothingSent();
    }

    #[Test]
    public function context_is_allowlisted_and_extra_payload_is_never_sent(): void
    {
        request()->headers->set('X-App-Platform', 'Android <!channel>');
        request()->headers->set('X-Device-Model', '<!channel>');
        Log::error('safe.failure', ['password' => 'hidden-value', 'payload' => ['email' => 'customer@example.test']]);
        $details = Http::recorded()[0][0]['blocks'][1]['text']['text'];
        $this->assertStringContainsString('Client platform: Web / unknown', $details);
        $this->assertStringContainsString('Device: unknown', $details);
        $this->assertStringNotContainsString('hidden-value', $details);
        $this->assertStringNotContainsString('customer@example.test', $details);
        $this->assertStringNotContainsString('<!channel>', $details);
    }

    #[Test]
    public function command_sends_a_labelled_test_without_creating_an_issue(): void
    {
        $this->assertSame(0, Artisan::call('errors:slack-test'));
        Http::assertSentCount(1);
        $request = Http::recorded()[0][0];
        $this->assertSame('Bremo | Laravel | TEST', $request['text']);
        $this->assertStringNotContainsString(self::WEBHOOK, Artisan::output());
    }

    private function serverError(): RuntimeException
    {
        return new RuntimeException('SELECT * customer@example.test private-password');
    }
}
