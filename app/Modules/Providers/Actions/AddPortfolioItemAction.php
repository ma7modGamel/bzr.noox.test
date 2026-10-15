<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\PortfolioItem;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

final class AddPortfolioItemAction
{
    public function execute(ProviderProfile $profile, int $mediaId, ?string $caption): PortfolioItem
    {
        return DB::transaction(function () use ($profile, $mediaId, $caption): PortfolioItem {
            $locked = ProviderProfile::query()->lockForUpdate()->findOrFail($profile->getKey());
            if ($locked->portfolioItems()->count() >= 20) {
                throw ValidationException::withMessages(['media_id' => Localization::text('copy.the_portfolio_is_full_20_photos')]);
            }

            $media = OrderMedia::query()
                ->whereKey($mediaId)
                ->where('uploaded_by', $locked->user_id)
                ->whereNull('order_id')
                ->where('type', 'IMAGE')
                ->where('expires_at', '>', now())
                ->lockForUpdate()
                ->first();

            if ($media === null) {
                throw ValidationException::withMessages(['media_id' => Localization::text('copy.the_photo_is_invalid_or_does_not_belong_to')]);
            }

            $item = $locked->portfolioItems()->create([
                'image_path' => $media->path,
                'caption' => $caption,
                'sort' => ((int) $locked->portfolioItems()->max('sort')) + 1,
            ]);
            $media->delete();

            return $item;
        }, attempts: 3);
    }
}
