<?php

declare(strict_types=1);

namespace App\Filament\Resources\ReviewTags\Pages;

use App\Filament\Resources\ReviewTags\ReviewTagResource;
use Filament\Resources\Pages\EditRecord;

final class EditReviewTag extends EditRecord
{
    protected static string $resource = ReviewTagResource::class;
}
