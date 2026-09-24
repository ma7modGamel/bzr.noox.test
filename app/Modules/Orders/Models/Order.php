<?php

declare(strict_types=1);

namespace App\Modules\Orders\Models;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Models\Payment;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\CustomerRating;
use App\Modules\Reviews\Models\Review;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Support\Models\Dispute;
use Database\Factories\OrderFactory;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

/**
 * الطلب — كيان واحد من النشر حتى الإغلاق (DEC-027).
 *
 * الحالة لا تتغير إلا عبر OrderStateMachine؛ ممنوع `$order->status = ...` في أي مكان آخر (30).
 */
final class Order extends Model
{
    use HasFactory;

    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'operating_mode' => OperatingMode::class,
            'status' => OrderStatus::class,
            'disputed_from_status' => OrderStatus::class,
            'timing_type' => TimingType::class,
            'pricing_mode' => PricingMode::class,
            'materials_responsibility' => MaterialsResponsibility::class,
            'payment_method' => PaymentMethod::class,
            'payment_status' => OrderPaymentStatus::class,
            'cancel_reason_code' => CancelReason::class,
            'slot_start' => 'immutable_datetime',
            'slot_end' => 'immutable_datetime',
            'offers_close_at' => 'immutable_datetime',
            'selection_deadline_at' => 'immutable_datetime',
            'assigned_at' => 'immutable_datetime',
            'confirmed_at' => 'immutable_datetime',
            'trip_started_at' => 'immutable_datetime',
            'arrived_at' => 'immutable_datetime',
            'work_started_at' => 'immutable_datetime',
            'completed_at' => 'immutable_datetime',
            'closed_at' => 'immutable_datetime',
            'cancelled_at' => 'immutable_datetime',
            'expired_at' => 'immutable_datetime',
            'settlement_eligible_at' => 'immutable_datetime',
            'terms_accepted_at' => 'immutable_datetime',
            'lat' => 'decimal:7',
            'lng' => 'decimal:7',
            'arrived_lat' => 'decimal:7',
            'arrived_lng' => 'decimal:7',
            'eta_approximate' => 'bool',
            'eta_calculated_at' => 'immutable_datetime',
            'eta_origin_lat' => 'decimal:7',
            'eta_origin_lng' => 'decimal:7',
            'budget_amount' => 'decimal:2',
            'commission_rate' => 'decimal:4',
            'labor_total' => 'decimal:2',
            'materials_total' => 'decimal:2',
            'final_amount' => 'decimal:2',
            'commission_amount' => 'decimal:2',
            'labor_refunded' => 'decimal:2',
            'refunded_total' => 'decimal:2',
        ];
    }

    // ── العلاقات ──────────────────────────────────────────────────────

    public function customer(): BelongsTo
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function assignedByAdmin(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'assigned_by_admin_id');
    }

    public function acceptedOffer(): BelongsTo
    {
        return $this->belongsTo(Offer::class, 'accepted_offer_id');
    }

    public function city(): BelongsTo
    {
        return $this->belongsTo(City::class);
    }

    public function area(): BelongsTo
    {
        return $this->belongsTo(Area::class);
    }

    public function category(): BelongsTo
    {
        return $this->belongsTo(Category::class);
    }

    public function problemType(): BelongsTo
    {
        return $this->belongsTo(ProblemType::class);
    }

    public function media(): HasMany
    {
        return $this->hasMany(OrderMedia::class);
    }

    /** العروض الحقيقية فقط — صف التعيين الإداري مستبعد (BR-007). */
    public function offers(): HasMany
    {
        return $this->hasMany(Offer::class)->where('source', '!=', 'ADMIN_ASSIGNMENT');
    }

    public function allOfferRows(): HasMany
    {
        return $this->hasMany(Offer::class);
    }

    public function proposals(): HasMany
    {
        return $this->hasMany(PriceProposal::class);
    }

    public function events(): HasMany
    {
        return $this->hasMany(OrderEvent::class)->orderBy('id');
    }

    public function payments(): HasMany
    {
        return $this->hasMany(Payment::class);
    }

    public function conversations(): HasMany
    {
        return $this->hasMany(Conversation::class);
    }

    public function disputes(): HasMany
    {
        return $this->hasMany(Dispute::class);
    }

    public function trackingPoints(): HasMany
    {
        return $this->hasMany(TrackingPoint::class);
    }

    public function shareLink(): HasOne
    {
        return $this->hasOne(ShareLink::class)->latestOfMany();
    }

    public function pendingProposalRecord(): HasOne
    {
        return $this->hasOne(PriceProposal::class)
            ->where('status', ProposalStatus::Pending->value)
            ->latestOfMany();
    }

    public function latestSuccessfulPayment(): HasOne
    {
        return $this->hasOne(Payment::class)
            ->where('status', 'SUCCEEDED')
            ->latestOfMany('paid_at');
    }

    public function latestPaymentAttempt(): HasOne
    {
        return $this->hasOne(Payment::class)->latestOfMany();
    }

    public function latestTrackingPoint(): HasOne
    {
        return $this->hasOne(TrackingPoint::class)->latestOfMany('recorded_at');
    }

    public function review(): HasOne
    {
        return $this->hasOne(Review::class);
    }

    public function customerRating(): HasOne
    {
        return $this->hasOne(CustomerRating::class);
    }

    // ── أسئلة الحالة ─────────────────────────────────────────────────

    public function isEmployeeMode(): bool
    {
        return $this->operating_mode === OperatingMode::Employee;
    }

    /** BR-008 — المعاينة مجانية عندما لا يوجد عرض برسوم. */
    public function inspectionIsFree(): bool
    {
        return $this->pricing_mode === PricingMode::Inspection && $this->inspectionFee() === '0.00';
    }

    /** رسوم المعاينة = سعر العرض المقبول في طلب المعاينة؛ صفر في وضع الموظفين. */
    public function inspectionFee(): string
    {
        if ($this->pricing_mode !== PricingMode::Inspection) {
            return '0.00';
        }

        return (string) ($this->acceptedOffer?->price ?? '0.00');
    }

    public function statusLabel(): string
    {
        return $this->status->labelFor($this->operating_mode);
    }

    public function pendingProposal(): ?PriceProposal
    {
        return $this->proposals()
            ->where('status', ProposalStatus::Pending->value)
            ->first();
    }

    /** BR-045 — لا إنهاء مع مقترح معلّق. */
    public function hasPendingProposal(): bool
    {
        return $this->proposals()->where('status', ProposalStatus::Pending->value)->exists();
    }

    public function hasOpenDispute(): bool
    {
        return $this->disputes()->where('status', 'OPEN')->exists();
    }

    /** BR-042 — عرض التنفيذ مرة واحدة فقط مهما كانت نتيجته. */
    public function hasExecutionQuote(): bool
    {
        return $this->proposals()->where('type', 'EXECUTION_QUOTE')->exists();
    }

    // ── نطاقات ────────────────────────────────────────────────────────

    /** طلبات بانتظار التعيين — لوحة التوزيع SCR-A15. */
    public function scopeAwaitingAssignment(Builder $query): void
    {
        $query->where('operating_mode', OperatingMode::Employee->value)
            ->where('status', OrderStatus::Open->value);
    }

    public function scopeActive(Builder $query): void
    {
        $query->whereNotIn('status', [
            OrderStatus::Closed->value,
            OrderStatus::Cancelled->value,
            OrderStatus::Expired->value,
        ]);
    }

    /** العروض غير المختارة تُغلق عند الوصول (O-06). */
    public function otherSubmittedOffers(int $exceptOfferId): Builder
    {
        return $this->offers()->getQuery()
            ->where('id', '!=', $exceptOfferId)
            ->whereIn('status', [OfferStatus::Submitted->value, OfferStatus::NotSelected->value]);
    }

    protected static function newFactory(): OrderFactory
    {
        return OrderFactory::new();
    }
}
