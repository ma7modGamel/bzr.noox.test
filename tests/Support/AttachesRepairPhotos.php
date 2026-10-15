<?php

declare(strict_types=1);

namespace Tests\Support;

use App\Modules\Orders\Actions\AttachWorkPhotoAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\MediaUploadService;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;

trait AttachesRepairPhotos
{
    private function attachRepairPhotos(Order $order, ProviderProfile $provider): void
    {
        Storage::fake('local');
        foreach (['REPAIR_BEFORE', 'REPAIR_AFTER'] as $purpose) {
            $media = app(MediaUploadService::class)->store($provider->user, UploadedFile::fake()->image('repair.jpg'));
            app(AttachWorkPhotoAction::class)->execute($order, $provider, $media->id, $purpose);
        }
        $order->refresh();
    }
}
