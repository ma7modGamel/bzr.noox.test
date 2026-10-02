<?php

declare(strict_types=1);

namespace App\Modules\Identity\Models;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Factories\UserFactory;
use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

/**
 * هوية واحدة بوضعين (DEC-001): كل مستخدم عميل افتراضيًا، ويصبح فنيًا بملف معتمد.
 */
final class User extends Authenticatable implements MustVerifyEmail
{
    use HasApiTokens;
    use HasFactory;
    use Notifiable;
    use SoftDeletes;

    protected $fillable = ['name', 'email', 'password', 'phone', 'avatar_path', 'status', 'rating_reminders_enabled'];

    protected $hidden = ['password', 'remember_token'];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
            'accepted_terms_version' => 'integer',
            'terms_accepted_at' => 'immutable_datetime',
            'email_verified_at' => 'immutable_datetime',
            'status' => UserStatus::class,
            'customer_rating_avg' => 'decimal:2',
            'rating_reminders_enabled' => 'bool',
        ];
    }

    public function providerProfile(): HasOne
    {
        return $this->hasOne(ProviderProfile::class);
    }

    public function addresses(): HasMany
    {
        return $this->hasMany(CustomerAddress::class);
    }

    public function orders(): HasMany
    {
        return $this->hasMany(Order::class, 'customer_id');
    }

    public function isBlocked(): bool
    {
        return $this->status === UserStatus::Blocked;
    }

    /** BR-005 — شارة "موثّق" للعميل تعني بريدًا موثقًا فقط. */
    public function isVerified(): bool
    {
        return $this->email_verified_at !== null;
    }

    /** BR-093 — يظهر اسم المُقيِّم مختصرًا ("سارة م."). */
    public function shortName(): string
    {
        $parts = preg_split('/\s+/u', trim($this->name)) ?: [];
        $first = $parts[0] ?? $this->name;
        $initial = isset($parts[1]) ? mb_substr($parts[1], 0, 1).'.' : '';

        return trim($first.' '.$initial);
    }

    protected static function newFactory(): UserFactory
    {
        return UserFactory::new();
    }
}
