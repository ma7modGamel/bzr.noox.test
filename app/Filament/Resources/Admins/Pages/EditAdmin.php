<?php

declare(strict_types=1);

namespace App\Filament\Resources\Admins\Pages;

use App\Filament\Resources\Admins\AdminResource;
use App\Modules\Identity\Enums\AdminRole;
use App\Modules\Identity\Models\Admin;
use Filament\Resources\Pages\EditRecord;
use Illuminate\Validation\ValidationException;

/** لا يعطّل المدير نفسه، ولا يُترك النظام بلا مدير عام نشط (DEC-060). */
final class EditAdmin extends EditRecord
{
    protected static string $resource = AdminResource::class;

    protected function mutateFormDataBeforeSave(array $data): array
    {
        /** @var Admin $record */
        $record = $this->getRecord();
        $active = (bool) ($data['is_active'] ?? $record->is_active);
        $role = AdminRole::from((string) ($data['role'] ?? $record->role->value));

        if ($record->is($this->currentAdmin()) && (! $active || $role !== $record->role)) {
            throw ValidationException::withMessages(['data.is_active' => 'لا يمكنك تعطيل حسابك أو تغيير دورك بنفسك.']);
        }

        $losesSuper = $record->isSuper() && $record->is_active && (! $active || $role !== AdminRole::Super);
        $otherSupers = Admin::query()->whereKeyNot($record->getKey())
            ->where('role', AdminRole::Super->value)->where('is_active', true)->exists();
        if ($losesSuper && ! $otherSupers) {
            throw ValidationException::withMessages(['data.role' => 'يجب أن يبقى مدير عام نشط واحد على الأقل.']);
        }

        return $data;
    }

    private function currentAdmin(): ?Admin
    {
        $admin = auth('admin')->user();

        return $admin instanceof Admin ? $admin : null;
    }
}
