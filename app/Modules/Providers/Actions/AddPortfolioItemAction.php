<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\PortfolioItem;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

final class AddPortfolioItemAction
{
    public function execute(ProviderProfile $profile, int $mediaId, ?string $caption): PortfolioItem
    {
        return DB::transaction(function () use ($profile, $mediaId, $caption): PortfolioItem {
            $locked = ProviderProfile::query()->lockForUpdate()->findOrFail($profile->getKey());
            if ($locked->portfolioItems()->count() >= 20) {
                throw ValidationException::withMessages(['media_id' => 'معرض الأعمال مكتمل (20 صورة).']);
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
                throw ValidationException::withMessages(['media_id' => 'الصورة غير صالحة أو غير مملوكة للحساب.']);
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
