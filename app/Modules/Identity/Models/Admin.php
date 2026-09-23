<?php

declare(strict_types=1);

namespace App\Modules\Identity\Models;

use Database\Factories\AdminFactory;
use App\Modules\Identity\Enums\AdminRole;
use Filament\Models\Contracts\FilamentUser;
use Filament\Panel;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;

/**
 * حساب إدارة — منفصل تمامًا عن `users` (03، DEC-021).
 * الصلاحيات التفصيلية: 23-PERMISSIONS-MATRIX.
 */
final class Admin extends Authenticatable implements FilamentUser
{
    use HasFactory;
    use Notifiable;

    protected $fillable = ['name', 'email', 'password', 'role', 'is_active'];

    protected $hidden = ['password', 'remember_token', 'two_factor_secret'];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
            'role' => AdminRole::class,
            'is_active' => 'bool',
            'two_factor_confirmed_at' => 'immutable_datetime',
        ];
    }

    public function canAccessPanel(Panel $panel): bool
    {
        return $this->is_active;
    }

    public function isSuper(): bool
    {
        return $this->role->isSuper();
    }

    protected static function newFactory(): AdminFactory
    {
        return AdminFactory::new();
    }
}
