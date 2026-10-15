<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Data;

use App\Modules\Notifications\Enums\NotificationCode;
use App\Support\Localization;

/**
 * محتوى إشعار واحد لمستلم واحد. الرابط من قائمة 17 §الروابط العميقة، والخادم وحده يحدده.
 */
final readonly class NotificationContent
{
    public const SCHEME = 'bremo://';

    public const CUSTOMER = 'CUSTOMER';

    public const PROVIDER = 'PROVIDER';

    public string $title;

    public string $body;

    public function __construct(
        public NotificationCode $code,
        public string|LocalizedMessage $titleTemplate,
        public string|LocalizedMessage $bodyTemplate,
        public ?string $path,
        public string $appMode,
        public ?int $orderId = null,
    ) {
        $this->title = $titleTemplate instanceof LocalizedMessage ? $titleTemplate->render('ar') : $titleTemplate;
        $this->body = $bodyTemplate instanceof LocalizedMessage ? $bodyTemplate->render('ar') : $bodyTemplate;
    }

    /** @param array<string, mixed> $data
     * @return array<string, mixed>
     */
    public static function localized(array $data, string $locale): array
    {
        foreach (['title', 'body'] as $field) {
            $template = $data[$field.'_template'] ?? null;
            if (is_array($template) && isset($template['key'])) {
                $data[$field] = (new LocalizedMessage($template['key'], $template['parameters'] ?? []))->render($locale);
            } elseif (($data['locale'] ?? 'ar') !== $locale) {
                $data[$field] = Localization::text('messages.legacy_notification_'.$field, locale: $locale);
                $data['legacy_content_unavailable'] = true;
            }
        }
        $data['locale'] = $locale;

        return $data;
    }

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
            'title_template' => $this->titleTemplate instanceof LocalizedMessage ? ['key' => $this->titleTemplate->key, 'parameters' => $this->titleTemplate->parameters] : null,
            'body' => $this->body,
            'body_template' => $this->bodyTemplate instanceof LocalizedMessage ? ['key' => $this->bodyTemplate->key, 'parameters' => $this->bodyTemplate->parameters] : null,
            'locale' => 'ar',
            'deep_link' => $this->deepLink(),
            'app_mode' => $this->appMode,
            'order_id' => $this->orderId,
        ];
    }
}
