<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Models;

use App\Support\Localization;
use Database\Factories\Modules\Reviews\Models\ReviewTagFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

final class ReviewTag extends Model
{
    /** @use HasFactory<ReviewTagFactory> */
    use HasFactory;

    protected $fillable = ['code', 'label', 'label_en', 'sort', 'is_active'];

    protected function casts(): array
    {
        return ['sort' => 'integer', 'is_active' => 'boolean'];
    }

    protected static function newFactory(): ReviewTagFactory
    {
        return ReviewTagFactory::new();
    }

    /** @return list<array{code: string, label: string}> */
    public static function options(): array
    {
        return self::query()->where('is_active', true)->orderBy('sort')->orderBy('id')->get()
            ->map(fn (self $tag): array => ['code' => $tag->code, 'label' => Localization::field($tag, 'label')])->all();
    }

    /** @return list<array{code: string, label: string, count: int}> */
    public static function statisticsForProvider(int $providerId): array
    {
        $counts = [];
        foreach (Review::query()->visible()->where('provider_profile_id', $providerId)->select(['id', 'tags'])->lazyById(500) as $review) {
            foreach (array_unique($review->tags ?? []) as $code) {
                $counts[$code] = ($counts[$code] ?? 0) + 1;
            }
        }

        return self::query()->orderBy('sort')->orderBy('id')->get()->filter(fn (self $tag): bool => ($counts[$tag->code] ?? 0) > 0)
            ->map(fn (self $tag): array => ['code' => $tag->code, 'label' => Localization::field($tag, 'label'), 'count' => $counts[$tag->code]])->values()->all();
    }
}
