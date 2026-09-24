<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
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
use App\Modules\Orders\Services\SchedulingCalendar;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

final readonly class UpdateRequestAction
{
    public function __construct(
        private SchedulingCalendar $calendar,
        private OrderDeadlines $deadlines,
        private SettingsRepository $settings,
    ) {}

    public function execute(Order $order, User $customer, PublishRequestData $data): Order
    {
        $this->assertEditable($order, $customer);
        $address = $this->address($customer, $data->customerAddressId);
        $category = $this->category($address, $data->categoryId);
        $problemType = $this->problemType($category, $data->problemTypeId);
        $this->assertDescription($problemType, $data->description);
        $slot = $this->slot($address, $data);
        $now = now()->toImmutable();
        $deadlines = $this->deadlines->for($order->operating_mode, $data->timingType, $slot['start'] ?? null, $now);

        return DB::transaction(function () use ($order, $customer, $data, $address, $category, $problemType, $slot, $deadlines, $now): Order {
            $order->forceFill([
                'city_id' => $address->city_id,
                'area_id' => $address->area_id,
                'customer_address_id' => $address->getKey(),
                'category_id' => $category->getKey(),
                'problem_type_id' => $problemType->getKey(),
                'description' => $data->description,
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
                'pricing_mode' => $order->operating_mode === OperatingMode::Employee
                    ? PricingMode::Inspection
                    : ($data->pricingMode ?? PricingMode::Execution),
                'budget_amount' => $this->budget($order, $data->budgetAmount),
                'materials_responsibility' => $data->materialsResponsibility,
                'offers_close_at' => $deadlines['offers_close_at'],
                'selection_deadline_at' => $deadlines['selection_deadline_at'],
            ])->save();

            $this->attachMedia($order, $customer, $data->mediaIds);

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::Updated,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'from_status' => OrderStatus::Open,
                'to_status' => OrderStatus::Open,
                'created_at' => $now,
            ]);

            return $order->refresh();
        });
    }

    private function assertEditable(Order $order, User $customer): void
    {
        if ($order->customer_id !== $customer->getKey()
            || $order->status !== OrderStatus::Open
            || $order->offers()->exists()) {
            throw BusinessRuleViolationException::rule('BR-021', 'لا يمكن تعديل الطلب بعد وصول أول عرض.');
        }
    }

    private function address(User $customer, int $id): CustomerAddress
    {
        $address = CustomerAddress::query()->with(['city', 'area'])
            ->where('user_id', $customer->getKey())->find($id);
        if ($address === null || ! $address->city->is_active || ! $address->area->is_active) {
            throw BusinessRuleViolationException::rule('BR-010', 'العنوان غير متاح.');
        }

        return $address;
    }

    private function category(CustomerAddress $address, int $id): Category
    {
        $category = Category::query()->where('is_active', true)->find($id);
        if ($category === null || ! $category->cities()->where('cities.id', $address->city_id)->wherePivot('is_active', true)->exists()) {
            throw BusinessRuleViolationException::rule('BR-011', 'الفئة غير متاحة في هذه المدينة.');
        }

        return $category;
    }

    private function problemType(Category $category, int $id): ProblemType
    {
        $problemType = ProblemType::query()->where('category_id', $category->getKey())
            ->where('is_active', true)->find($id);
        if ($problemType === null) {
            throw BusinessRuleViolationException::rule('BR-011', 'نوع المشكلة غير متاح.');
        }

        return $problemType;
    }

    private function assertDescription(ProblemType $problemType, ?string $description): void
    {
        $length = mb_strlen(trim((string) $description));
        if ($length > 1000 || ($problemType->is_other && $length < 10)) {
            throw BusinessRuleViolationException::rule('BR-013', 'وصف المشكلة غير صالح.');
        }
    }

    /** @return array{start: CarbonImmutable, end: CarbonImmutable}|array{} */
    private function slot(CustomerAddress $address, PublishRequestData $data): array
    {
        if ($data->timingType === TimingType::Now) {
            $this->calendar->assertNowIsAllowed($address->city);

            return [];
        }
        if ($data->slotStart === null) {
            throw BusinessRuleViolationException::rule('BR-017', 'اختيار الفترة إلزامي.');
        }

        return $this->calendar->assertValidSlot($address->city, $data->slotStart);
    }

    private function budget(Order $order, ?string $budget): ?string
    {
        if ($order->operating_mode === OperatingMode::Employee || $budget === null) {
            return null;
        }
        if ((float) $budget < (float) $this->settings->decimal(Cfg::MinOfferAmount)) {
            throw BusinessRuleViolationException::rule('BR-015', 'الميزانية أقل من الحد الأدنى.');
        }

        return $budget;
    }

    /** @param list<int> $mediaIds */
    private function attachMedia(Order $order, User $customer, array $mediaIds): void
    {
        if ($mediaIds === []) {
            return;
        }
        $media = OrderMedia::query()->whereIn('id', $mediaIds)->where('uploaded_by', $customer->getKey())
            ->whereNull('order_id')->lockForUpdate()->get();
        if ($media->count() !== count(array_unique($mediaIds))) {
            throw BusinessRuleViolationException::rule('BR-014', 'إحدى الوسائط غير متاحة.');
        }
        OrderMedia::query()->whereKey($media->modelKeys())->update(['order_id' => $order->getKey(), 'expires_at' => null]);
    }
}
