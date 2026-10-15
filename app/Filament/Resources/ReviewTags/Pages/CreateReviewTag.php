<?php

declare(strict_types=1);

namespace App\Filament\Resources\ReviewTags\Pages;

use App\Filament\Resources\ReviewTags\ReviewTagResource;
use Filament\Resources\Pages\CreateRecord;

final class CreateReviewTag extends CreateRecord
{
    protected static string $resource = ReviewTagResource::class;
}
