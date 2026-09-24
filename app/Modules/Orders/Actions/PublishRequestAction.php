<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Content\Services\TermsService;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Orders\Services\OrderDeadlines;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Orders\Services\SchedulingCalendar;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\AccountBlockedException;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\EmailNotVerifiedException;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

/**
 * T-01 — نشر الطلب (07).
 *
 * إنشاء لا انتقال، فلا يمر بآلة الحالات: الطلب يولد في `OPEN` ويُسجَّل EVT-001.
 * الوضع يُثبَّت هنا مرة واحدة ولا يتغير بعدها مهما تبدّل CFG-090 (BR-009).
 */
final readonly class PublishRequestAction
{
    public function __construct(
        private FeatureGate $features,
        private SettingsRepository $settings,
        private SchedulingCalendar $calendar,
        private OrderDeadlines $deadlines,
        private ProviderEligibility $eligibility,
        private TermsService $terms,
    ) {}

    public function execute(User $customer, PublishRequestData $data): Order
    {
        $this->assertCustomerMayPublish($customer);

        $address = $this->resolveAddress($customer, $data);
        $category = $this->resolveCategory($address, $data);
        $problemType = $this->resolveProblemType($category, $data);

        $this->assertDescription($problemType, $data);
        $this->assertTerms($customer, $data);

        $slot = $this->resolveSlot($address, $data);
        $mode = $this->features->mode();
        $publishedAt = CarbonImmutable::now();

        $deadlines = $this->deadlines->for($mode, $data->timingType, $slot['start'] ?? null, $publishedAt);

        $order = DB::transaction(function () use ($customer, $data, $address, $category, $problemType, $slot, $mode, $deadlines, $publishedAt): Order {
            $order = Order::query()->create([
                'number' => DB::table('order_numbers')->insertGetId([]),
                'operating_mode' => $mode,
                'customer_id' => $customer->getKey(),
                'city_id' => $address->city_id,
                'area_id' => $address->area_id,
                'category_id' => $category->getKey(),
                'problem_type_id' => $problemType->getKey(),
                'description' => $data->description,
                'customer_address_id' => $address->getKey(),
                'address_text' => $address->address_text,
                'building' => $address->building,
                'floor' => $address->floor,
                'apartment' => $address->apartment,
                'landmark' => $address->landmark,
                'lat' => $address->lat,
                'lng' => $address->lng,
                'timing_type' => $data->timingType,
                'slot_start' => $slot['start'] ?? null,
                'slot_end' => $slot['end'] ?? null,
                'pricing_mode' => $this->pricingMode($data),
                'budget_amount' => $this->budget($data),
                'materials_responsibility' => $data->materialsResponsibility,
                'status' => OrderStatus::Open,
                'offers_close_at' => $deadlines['offers_close_at'],
                'selection_deadline_at' => $deadlines['selection_deadline_at'],
                'terms_version' => $this->terms->currentVersion(),
                'terms_accepted_at' => $publishedAt,
            ]);

            if ($data->termsAccepted) {
                $this->terms->recordAcceptance($customer, $this->terms->currentVersion());
            }

            $this->attachMedia($order, $customer, $data);

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::Published,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'to_status' => OrderStatus::Open,
                'meta' => [
                    'transition' => 'T-01',
                    'operating_mode' => $mode->value,
                    // وضع الموظفين لا يُشعر أحدًا: الطلب يظهر في لوحة التعيين (08)
                    'notified_providers' => $this->features->offersEnabled()
                        ? $this->eligibility->query($order)->count()
                        : 0,
                ],
                'created_at' => $publishedAt,
            ]);

            return $order;
        });

        return $order->refresh();
    }

    /**
     * BR-020 — تحذير غير مانع عند تكرار الفئة ونفس العنوان لطلب `OPEN` قائم.
     * يُستدعى قبل النشر لعرض التأكيد، ولا يمنع الإنشاء.
     */
    public function duplicateWarning(User $customer, PublishRequestData $data): bool
    {
        return Order::query()
            ->where('customer_id', $customer->getKey())
            ->where('status', OrderStatus::Open->value)
            ->where('category_id', $data->categoryId)
            ->where('customer_address_id', $data->customerAddressId)
            ->exists();
    }

    private function assertCustomerMayPublish(User $customer): void
    {
        // BR-001 — رمز مستقل في 31 ليفتح التطبيق شاشة التفعيل مباشرة (AC-ACC-01)
        if (! $customer->isVerified()) {
            throw EmailNotVerifiedException::make();
        }

        // BR-003 — المحظور لا ينشر
        if ($customer->isBlocked()) {
            throw AccountBlockedException::make();
        }

        // BR-019 — حد الطلبات المفتوحة
        $limit = $this->settings->int(Cfg::MaxOpenOrdersPerCustomer);
        $open = Order::query()
            ->where('customer_id', $customer->getKey())
            ->where('status', OrderStatus::Open->value)
            ->count();

        if ($open >= $limit) {
            throw BusinessRuleViolationException::rule(
                'BR-019',
                "لا يمكن تجاوز {$limit} طلبات مفتوحة في نفس الوقت.",
            );
        }
    }

    private function resolveAddress(User $customer, PublishRequestData $data): CustomerAddress
    {
        /** @var CustomerAddress|null $address */
        $address = CustomerAddress::query()
            ->with(['city', 'area'])
            ->where('user_id', $customer->getKey())
            ->find($data->customerAddressId);

        if ($address === null) {
            throw BusinessRuleViolationException::rule('BR-010', 'العنوان غير موجود.');
        }

        // BR-010 — منطقة مفعّلة في مدينة مفعّلة
        if (! $address->city->is_active || ! $address->area->is_active) {
            throw BusinessRuleViolationException::rule('BR-010', 'الخدمة غير متاحة في هذه المنطقة حاليًا.');
        }

        return $address;
    }

    private function resolveCategory(CustomerAddress $address, PublishRequestData $data): Category
    {
        /** @var Category|null $category */
        $category = Category::query()->where('is_active', true)->find($data->categoryId);

        // BR-011 — الفئة مفعّلة في مدينة الطلب
        $servesCity = $category !== null && $category->cities()
            ->where('cities.id', $address->city_id)
            ->wherePivot('is_active', true)
            ->exists();

        if (! $servesCity) {
            throw BusinessRuleViolationException::rule('BR-011', 'الفئة غير متاحة في هذه المدينة.');
        }

        return $category;
    }

    private function resolveProblemType(Category $category, PublishRequestData $data): ProblemType
    {
        /** @var ProblemType|null $problemType */
        $problemType = ProblemType::query()
            ->where('category_id', $category->getKey())
            ->where('is_active', true)
            ->find($data->problemTypeId);

        if ($problemType === null) {
            throw BusinessRuleViolationException::rule('BR-011', 'نوع المشكلة غير متاح.');
        }

        return $problemType;
    }

    /** BR-013 — الوصف إلزامي (10 أحرف على الأقل) في "مشكلة أخرى"، وحده الأقصى 1000. */
    private function assertDescription(ProblemType $problemType, PublishRequestData $data): void
    {
        $description = trim((string) $data->description);

        if ($description !== '' && mb_strlen($description) > 1000) {
            throw BusinessRuleViolationException::rule('BR-013', 'الوصف لا يتجاوز 1000 حرف.');
        }

        if ($problemType->is_other && mb_strlen($description) < 10) {
            throw BusinessRuleViolationException::rule(
                'BR-013',
                'وصف المشكلة إلزامي (10 أحرف على الأقل) عند اختيار "مشكلة أخرى".',
            );
        }
    }

    /** BR-018 — الموافقة إلزامية، ويُحفظ رقم النسخة ووقت الموافقة. */
    /** BR-018 — الموافقة مطلوبة عند أول طلب بعد سريان نسخة شروط لم يوافق عليها العميل. */
    private function assertTerms(User $customer, PublishRequestData $data): void
    {
        if (! $data->termsAccepted && $this->terms->acceptanceRequired($customer)) {
            throw BusinessRuleViolationException::rule('BR-018', 'الموافقة على الشروط وسياسة الإلغاء إلزامية.');
        }
    }

    /** @return array{start: CarbonImmutable, end: CarbonImmutable}|array{} */
    private function resolveSlot(CustomerAddress $address, PublishRequestData $data): array
    {
        if ($data->timingType === TimingType::Now) {
            $this->calendar->assertNowIsAllowed($address->city);

            return [];
        }

        if ($data->slotStart === null) {
            throw BusinessRuleViolationException::rule('BR-017', 'اختيار الفترة إلزامي للطلب المجدول.');
        }

        return $this->calendar->assertValidSlot($address->city, $data->slotStart);
    }

    /** BR-008 — وضع الموظفين: كل الطلبات معاينة، والحقل لا يظهر للعميل أصلًا. */
    private function pricingMode(PublishRequestData $data): PricingMode
    {
        if (! $this->features->offersEnabled()) {
            return PricingMode::Inspection;
        }

        return $data->pricingMode ?? PricingMode::Execution;
    }

    /** BR-015 معلوماتية، ولا تظهر في وضع الموظفين (BR-008). */
    private function budget(PublishRequestData $data): ?string
    {
        if (! $this->features->offersEnabled() || $data->budgetAmount === null) {
            return null;
        }

        $min = (float) $this->settings->decimal(Cfg::MinOfferAmount);

        if ((float) $data->budgetAmount < $min) {
            throw BusinessRuleViolationException::rule('BR-015', "الميزانية المتوقعة لا تقل عن {$min} جنيه.");
        }

        return $data->budgetAmount;
    }

    /** BR-014 — الوسائط المرفوعة مؤقتًا تُربط بالطلب وتفقد تاريخ الانتهاء. */
    private function attachMedia(Order $order, User $customer, PublishRequestData $data): void
    {
        if ($data->mediaIds === []) {
            return;
        }

        $media = OrderMedia::query()
            ->whereIn('id', $data->mediaIds)
            ->where('uploaded_by', $customer->getKey())
            ->whereNull('order_id')
            ->where(fn ($query) => $query->whereNull('expires_at')->orWhere('expires_at', '>', now()))
            ->lockForUpdate()
            ->get();

        if ($media->count() !== count(array_unique($data->mediaIds))) {
            throw BusinessRuleViolationException::rule('BR-014', 'إحدى الوسائط غير متاحة أو لا تخص هذا الحساب.');
        }

        $counts = $media->countBy('type');

        if (($counts['IMAGE'] ?? 0) > 5 || ($counts['VIDEO'] ?? 0) > 1 || ($counts['AUDIO'] ?? 0) > 1) {
            throw BusinessRuleViolationException::rule('BR-014', 'تجاوزت الوسائط العدد المسموح.');
        }

        OrderMedia::query()->whereKey($media->modelKeys())->update([
            'order_id' => $order->getKey(),
            'expires_at' => null,
        ]);
    }
}
