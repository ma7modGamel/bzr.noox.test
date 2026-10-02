<?php

declare(strict_types=1);

namespace App\Filament\Resources\Admins\Pages;

use App\Filament\Resources\Admins\AdminResource;
use Filament\Resources\Pages\CreateRecord;

final class CreateAdmin extends CreateRecord
{
    protected static string $resource = AdminResource::class;
}
