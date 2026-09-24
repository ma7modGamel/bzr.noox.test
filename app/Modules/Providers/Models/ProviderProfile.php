<?php

declare(strict_types=1);

namespace App\Modules\Providers\Models;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Geography\Models\Area;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Reviews\Models\Review;
use Database\Factories\ProviderProfileFactory;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * ملف مقدم الخدمة — 15، BR-130..BR-132.
 * `employment_type` صفة تعاقدية لا حالة (39)؛ الأهلية في BR-022.
 */
final class ProviderProfile extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id', 'status', 'employment_type', 'bio', 'experience_years', 'available_now',
        'payout_method', 'payout_details',
    ];

    protected $hidden = ['payout_details'];

    protected function casts(): array
    {
        return [
            'status' => ProviderStatus::class,
            'employment_type' => EmploymentType::class,
            'available_now' => 'bool',
            'phone_verified_at' => 'immutable_datetime',
            'submitted_at' => 'immutable_datetime',
            'reviewed_at' => 'immutable_datetime',
            'dues_blocked_at' => 'immutable_datetime',
            'payout_details' => 'encrypted',
            'rating_avg' => 'decimal:2',
            'rating_quality_avg' => 'decimal:2',
            'rating_punctuality_avg' => 'decimal:2',
            'rating_conduct_avg' => 'decimal:2',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function categories(): BelongsToMany
    {
        return $this->belongsToMany(Category::class, 'provider_categories');
    }

    public function specialties(): BelongsToMany
    {
        return $this->belongsToMany(ProblemType::class, 'provider_specialties');
    }

    public function areas(): BelongsToMany
    {
        return $this->belongsToMany(Area::class, 'provider_areas');
    }

    public function documents(): HasMany
    {
        return $this->hasMany(ProviderDocument::class);
    }

    public function portfolioItems(): HasMany
    {
        return $this->hasMany(PortfolioItem::class)->orderBy('sort');
    }

    public function offers(): HasMany
    {
        return $this->hasMany(Offer::class);
    }

    public function reviews(): HasMany
    {
        return $this->hasMany(Review::class);
    }

    public function orders(): HasMany
    {
        return $this->hasMany(Order::class);
    }

    /** BR-005 — الفني الموثّق: هوية مراجَعة + هاتف موثق يدويًا (DEC-033). */
    public function isVerified(): bool
    {
        return $this->status === ProviderStatus::Active && $this->phone_verified_at !== null;
    }

    /** BR-063 — لا يُطبق في وضع الموظفين (BR-065). */
    public function isBlockedByDues(): bool
    {
        return $this->dues_blocked_at !== null;
    }

    /** الحِمل الحالي: الطلبات غير النهائية — يُستخدم في ترتيب لوحة التعيين (08، SCR-A15). */
    public function currentLoad(): int
    {
        return $this->orders()
            ->whereNotIn('status', [
                OrderStatus::Closed->value,
                OrderStatus::Cancelled->value,
                OrderStatus::Expired->value,
            ])
            ->count();
    }

    public function scopeActive(Builder $query): void
    {
        $query->where('status', ProviderStatus::Active->value);
    }

    public function scopeEmployees(Builder $query): void
    {
        $query->where('employment_type', EmploymentType::Employee->value);
    }

    protected static function newFactory(): ProviderProfileFactory
    {
        return ProviderProfileFactory::new();
    }
}
