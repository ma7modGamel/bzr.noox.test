<?php

declare(strict_types=1);

namespace App\Modules\Identity\Models;

use App\Modules\Identity\Enums\AdminRole;
use Database\Factories\AdminFactory;
use Filament\Auth\MultiFactor\App\Contracts\HasAppAuthentication;
use Filament\Auth\MultiFactor\App\Contracts\HasAppAuthenticationRecovery;
use Filament\Models\Contracts\FilamentUser;
use Filament\Panel;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;

/**
 * حساب إدارة — منفصل تمامًا عن `users` (03، DEC-021).
 * الصلاحيات التفصيلية: 23-PERMISSIONS-MATRIX.
 */
final class Admin extends Authenticatable implements FilamentUser, HasAppAuthentication, HasAppAuthenticationRecovery
{
    use HasFactory;
    use Notifiable;

    protected $fillable = ['name', 'email', 'password', 'role', 'is_active'];

    protected $hidden = ['password', 'remember_token', 'two_factor_secret', 'two_factor_recovery_codes'];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
            'role' => AdminRole::class,
            'is_active' => 'bool',
            'two_factor_confirmed_at' => 'immutable_datetime',
            'two_factor_secret' => 'encrypted',
            'two_factor_recovery_codes' => 'encrypted:array',
        ];
    }

    public function canAccessPanel(Panel $panel): bool
    {
        return $this->is_active;
    }

    /** ASM-14، DEC-060 — TOTP إلزامي لكل حساب إدارة (Filament MFA). */
    public function getAppAuthenticationSecret(): ?string
    {
        return $this->two_factor_secret;
    }

    public function saveAppAuthenticationSecret(#[\SensitiveParameter] ?string $secret): void
    {
        $this->forceFill([
            'two_factor_secret' => $secret,
            'two_factor_confirmed_at' => $secret === null ? null : now(),
        ])->save();
    }

    public function getAppAuthenticationHolderName(): string
    {
        return $this->email;
    }

    /** @return array<string>|null */
    public function getAppAuthenticationRecoveryCodes(): ?array
    {
        return $this->two_factor_recovery_codes;
    }

    /** @param array<string>|null $codes */
    public function saveAppAuthenticationRecoveryCodes(#[\SensitiveParameter] ?array $codes): void
    {
        $this->forceFill(['two_factor_recovery_codes' => $codes])->save();
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
