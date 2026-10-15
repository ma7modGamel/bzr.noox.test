<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Data;

use App\Support\Localization;
use BackedEnum;
use Illuminate\Database\Eloquent\Model;

final readonly class LocalizedMessage
{
    /** @param array<string, string|int|float|array{ar: string, en: ?string}> $parameters */
    public function __construct(public string $key, public array $parameters = []) {}

    public function render(string $locale): string
    {
        $parameters = [];
        foreach ($this->parameters as $name => $value) {
            $parameters[$name] = is_array($value) ? ($value[$locale] ?? throw new \RuntimeException("Missing {$locale} notification parameter: {$name}")) : $value;
        }

        if ($this->key === 'notifications.offers_count') {
            $count = (int) $parameters['count'];
            $key = match (true) {
                $count === 0 => 'offers_zero',
                $count === 1 => 'offers_one',
                $count === 2 => 'offers_two',
                $locale === 'ar' && $count % 100 >= 3 && $count % 100 <= 10 => 'offers_few',
                $locale === 'ar' && ($count % 100 < 11 || $count % 100 > 99) => 'offers_other',
                default => 'offers_count',
            };

            return Localization::text('notifications.'.$key, $parameters, $locale);
        }

        return Localization::text($this->key, $parameters, $locale);
    }

    /** @return array{ar: string, en: ?string} */
    public static function field(?Model $model): array
    {
        $arabic = (string) $model?->getAttribute('name');

        return ['ar' => $arabic, 'en' => $model?->getAttribute('name_en') ?? config('localization.content.'.hash('sha256', $arabic))];
    }

    /** @return array{ar: string, en: string} */
    public static function enum(BackedEnum $value): array
    {
        $key = 'enums.'.class_basename($value).'.'.$value->value.'.label';

        return ['ar' => Localization::text($key, locale: 'ar'), 'en' => Localization::text($key, locale: 'en')];
    }
}
