<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Push;

use Illuminate\Contracts\Cache\Repository as Cache;
use Illuminate\Support\Facades\Http;
use RuntimeException;

/**
 * رمز OAuth2 لحساب خدمة Firebase (JWT RS256 موقّع محليًا)، مخزّن مؤقتًا حتى قبل انتهائه بـ5 دقائق.
 * الملف يُقرأ من `FCM_CREDENTIALS_PATH` خارج git (DEP-PUSH-01).
 */
final class GoogleAccessToken
{
    private const SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';

    public function __construct(private readonly Cache $cache) {}

    public function get(): string
    {
        $credentials = $this->credentials();

        return $this->cache->remember(
            'fcm.access_token.'.sha1($credentials['client_email']),
            now()->addMinutes(55),
            fn (): string => $this->fetch($credentials),
        );
    }

    /** @param array{client_email: string, private_key: string, token_uri: string} $credentials */
    private function fetch(array $credentials): string
    {
        $now = time();
        $segments = [
            $this->encode(['alg' => 'RS256', 'typ' => 'JWT']),
            $this->encode([
                'iss' => $credentials['client_email'],
                'scope' => self::SCOPE,
                'aud' => $credentials['token_uri'],
                'iat' => $now,
                'exp' => $now + 3600,
            ]),
        ];

        if (! openssl_sign(implode('.', $segments), $signature, $credentials['private_key'], OPENSSL_ALGO_SHA256)) {
            throw new RuntimeException('Unable to sign the FCM service account assertion.');
        }

        $segments[] = $this->base64Url($signature);

        $response = Http::asForm()
            ->timeout((int) config('services.fcm.timeout', 10))
            ->post($credentials['token_uri'], [
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion' => implode('.', $segments),
            ]);

        $token = $response->json('access_token');

        if (! $response->successful() || ! is_string($token)) {
            throw new RuntimeException('FCM OAuth token request failed with HTTP '.$response->status().'.');
        }

        return $token;
    }

    /** @return array{client_email: string, private_key: string, token_uri: string} */
    private function credentials(): array
    {
        $path = (string) config('services.fcm.credentials');

        if ($path === '' || ! is_readable($path)) {
            throw new RuntimeException('FCM_CREDENTIALS_PATH is missing or unreadable (DEP-PUSH-01).');
        }

        $data = json_decode((string) file_get_contents($path), true);

        if (! is_array($data) || ! isset($data['client_email'], $data['private_key'])) {
            throw new RuntimeException('FCM service account file is not a valid Google service account key.');
        }

        return [
            'client_email' => (string) $data['client_email'],
            'private_key' => (string) $data['private_key'],
            'token_uri' => (string) ($data['token_uri'] ?? 'https://oauth2.googleapis.com/token'),
        ];
    }

    /** @param array<string, mixed> $value */
    private function encode(array $value): string
    {
        return $this->base64Url(json_encode($value, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES));
    }

    private function base64Url(string $value): string
    {
        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }
}
