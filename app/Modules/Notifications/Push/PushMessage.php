<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Push;

/** حمولة FCM HTTP v1 — 17 §حمولة Push. */
final readonly class PushMessage
{
    public function __construct(
        public string $notificationId,
        public string $code,
        public string $title,
        public string $body,
        public ?string $deepLink,
        public string $appMode,
        public string $channel,
        public ?string $group,
    ) {}

    /** @return array<string, mixed> */
    public function toFcm(string $token): array
    {
        $android = ['priority' => 'HIGH', 'notification' => ['channel_id' => $this->channel]];
        $aps = ['sound' => 'default'];

        if ($this->group !== null) {
            $android['notification']['tag'] = $this->group;
            $aps['thread-id'] = $this->group;
        }

        return [
            'message' => [
                'token' => $token,
                'notification' => ['title' => $this->title, 'body' => $this->body],
                'data' => array_filter([
                    'notification_id' => $this->notificationId,
                    'code' => $this->code,
                    'deep_link' => $this->deepLink,
                    'app_mode' => $this->appMode,
                ], static fn (?string $value): bool => $value !== null),
                'android' => $android,
                'apns' => ['payload' => ['aps' => $aps]],
            ],
        ];
    }
}
