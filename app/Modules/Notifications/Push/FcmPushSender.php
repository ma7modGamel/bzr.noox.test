<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Push;

use App\Modules\Notifications\Contracts\PushSender;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * FCM HTTP v1 للمنصتين (DEC-058). iOS يصل عبر APNs من خلال Firebase.
 * تصنيف الأخطاء: https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode
 */
final class FcmPushSender implements PushSender
{
    public function __construct(private readonly GoogleAccessToken $accessToken) {}

    public function send(string $token, PushMessage $message): PushResult
    {
        $projectId = (string) config('services.fcm.project_id');

        try {
            if ($projectId === '') {
                throw new RuntimeException('FCM_PROJECT_ID is missing (DEP-PUSH-01).');
            }

            $response = Http::withToken($this->accessToken->get())
                ->acceptJson()
                ->timeout((int) config('services.fcm.timeout', 10))
                ->post("https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send", $message->toFcm($token));
        } catch (ConnectionException) {
            return PushResult::Retry;
        } catch (RuntimeException $exception) {
            Log::error('push.fcm.configuration', ['code' => $message->code, 'error' => $exception->getMessage()]);

            return PushResult::Fatal;
        }

        if ($response->successful()) {
            return PushResult::Sent;
        }

        $status = $response->status();
        $errorCode = $this->errorCode((array) $response->json());

        $result = match (true) {
            $status === 404, $errorCode === 'UNREGISTERED' => PushResult::InvalidToken,
            // INVALID_ARGUMENT يُحذف فيه الرمز فقط إذا كان الخطأ في الرمز نفسه، لا في الحمولة.
            $status === 400 && $errorCode === 'INVALID_ARGUMENT'
                && str_contains(strtolower((string) $response->json('error.message')), 'registration token') => PushResult::InvalidToken,
            $status === 429, $status >= 500 => PushResult::Retry,
            default => PushResult::Fatal,
        };

        if ($result === PushResult::Fatal) {
            Log::error('push.fcm.rejected', ['code' => $message->code, 'status' => $status, 'error' => $errorCode]);
        }

        return $result;
    }

    /** @param array<string, mixed> $body */
    private function errorCode(array $body): ?string
    {
        foreach ((array) data_get($body, 'error.details', []) as $detail) {
            if (is_array($detail) && isset($detail['errorCode'])) {
                return (string) $detail['errorCode'];
            }
        }

        $status = data_get($body, 'error.status');

        return is_string($status) ? $status : null;
    }
}
