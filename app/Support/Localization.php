<?php

declare(strict_types=1);

namespace App\Support;

use App\Support\Exceptions\MissingLocalizationException;
use BackedEnum;
use Illuminate\Database\Eloquent\Model;
use RuntimeException;

final class Localization
{
    public static function locale(): string
    {
        return request()->attributes->get('locale')
            ?? (request()->is('api/*') ? (request()->getPreferredLanguage(['ar', 'en']) ?? 'ar') : 'ar');
    }

    /** @param array<string, string|int|float> $parameters */
    public static function text(string $key, array $parameters = [], ?string $locale = null): string
    {
        $locale ??= self::locale();
        $message = config('localization.'.$locale.'.'.$key);

        if (! is_string($message) || trim($message) === '') {
            throw new RuntimeException("Missing {$locale} translation: {$key}");
        }

        return preg_replace_callback('/:[a-z_]+/', static function (array $match) use ($parameters, $key): string {
            $name = substr($match[0], 1);
            if (! array_key_exists($name, $parameters)) {
                throw new RuntimeException("Missing parameter {$name}: {$key}");
            }

            return (string) $parameters[$name];
        }, $message);
    }

    public static function enum(BackedEnum $value, string $field = 'label'): string
    {
        return self::text('enums.'.class_basename($value).'.'.$value->value.'.'.$field);
    }

    public static function design(string $key): string
    {
        static $catalogs = [];
        $locale = self::locale();
        $catalogs[$locale] ??= json_decode((string) file_get_contents(base_path('design/strings.'.$locale.'.json')), true, flags: JSON_THROW_ON_ERROR);
        $value = $catalogs[$locale][$key] ?? null;
        if (! is_string($value) || trim($value) === '') {
            throw new RuntimeException("Missing {$locale} design translation: {$key}");
        }

        return $value;
    }

    public static function field(?Model $model, string $field = 'name'): ?string
    {
        if ($model === null) {
            return null;
        }

        if (self::locale() === 'ar') {
            return $model->getAttribute($field);
        }

        $translation = $model->getAttribute($field.'_en');
        if ($translation === null && in_array($model->getTable(), ['cities', 'areas'], true)) {
            return (string) $model->getAttribute($field);
        }
        $translation ??= config('localization.content.'.hash('sha256', (string) $model->getAttribute($field)));

        if (! is_string($translation) || trim($translation) === '') {
            throw new MissingLocalizationException($model->getTable().'.'.$model->getKey().'.'.$field);
        }

        return $translation;
    }
}
