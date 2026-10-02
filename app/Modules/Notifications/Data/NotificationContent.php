<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Data;

use App\Modules\Notifications\Enums\NotificationCode;

/**
 * محتوى إشعار واحد لمستلم واحد. الرابط من قائمة 17 §الروابط العميقة، والخادم وحده يحدده.
 */
final readonly class NotificationContent
{
    public const SCHEME = 'bremo://';

    public const CUSTOMER = 'CUSTOMER';

    public const PROVIDER = 'PROVIDER';

    public function __construct(
        public NotificationCode $code,
        public string $title,
        public string $body,
        public ?string $path,
        public string $appMode,
        public ?int $orderId = null,
    ) {}

    public function deepLink(): ?string
    {
        return $this->path === null ? null : self::SCHEME.$this->path;
    }

    /** رابط البريد الذي يفتح التطبيق عبر App Links/Universal Links (DEC-058). */
    public function webLink(): ?string
    {
        return $this->path === null ? null : rtrim((string) config('app.url'), '/').'/app/'.$this->path;
    }

    /** @return array<string, mixed> */
    public function toArray(): array
    {
        return [
            'code' => $this->code->value,
            'title' => $this->title,
            'body' => $this->body,
            'deep_link' => $this->deepLink(),
            'app_mode' => $this->appMode,
            'order_id' => $this->orderId,
        ];
    }
}
