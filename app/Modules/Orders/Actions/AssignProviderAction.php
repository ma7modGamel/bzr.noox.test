<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Offers\Enums\OfferSource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-27 — تعيين مقدم خدمة لطلب في وضع الموظفين (BR-007).
 *
 * لا كيان جديد: يُنشأ صف عرض بحالة ACCEPTED وسعر 0.00 ومصدر ADMIN_ASSIGNMENT
 * فيبقى `orders.accepted_offer_id` وكل ما بُني عليه واحدًا في الوضعين.
 */
final readonly class AssignProviderAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private FeatureGate $features,
        private ProviderEligibility $eligibility,
    ) {}

    public function execute(Order $order, ProviderProfile $provider, Admin $admin): Order
    {
        $this->features->requireEmployeeMode('assignProvider');
        $this->eligibility->assertAssignable($order, $provider);

        return $this->stateMachine->apply(
            order: $order,
            action: 'assignProvider',
            actorType: ActorType::Admin,
            actorId: $admin->getKey(),
            meta: [
                'provider_profile_id' => $provider->getKey(),
                'provider_name' => $provider->user->name,
                'operating_mode' => $order->operating_mode->value,
            ],
            mutate: function (Order $fresh) use ($provider, $admin): void {
                $assignment = Offer::query()->create([
                    'order_id' => $fresh->getKey(),
                    'provider_profile_id' => $provider->getKey(),
                    'source' => OfferSource::AdminAssignment,
                    'price' => '0.00',
                    'status' => OfferStatus::Accepted,
                    'submitted_at' => now(),
                    'decided_at' => now(),
                ]);

                $fresh->provider_profile_id = $provider->getKey();
                $fresh->accepted_offer_id = $assignment->getKey();
                $fresh->assigned_by_admin_id = $admin->getKey();
                $fresh->assigned_at = now();
                $fresh->confirmed_at = now();

                // BR-065 — الموظف بأجر: كامل المصنعية للمنصة، فالنسبة المثبتة 100%.
                $fresh->commission_rate = '1.0000';
            },
        );
    }

    /**
     * T-29 — إعادة التعيين قبل الوصول: اعتذار + تعيين بديل في معاملة واحدة.
     * يُسجَّل الحدثان (EVT-012 ثم EVT-011) كما في 10.
     */
    public function reassign(Order $order, ProviderProfile $provider, Admin $admin, string $reason): Order
    {
        $this->features->requireEmployeeMode('reassignProvider');

        if ($order->provider_profile_id === $provider->getKey()) {
            throw BusinessRuleViolationException::rule(
                'BR-007',
                'إعادة التعيين تتطلب اختيار مقدم خدمة مختلف عن الحالي.',
            );
        }

        $this->eligibility->assertAssignable($order, $provider);

        return $this->stateMachine->apply(
            order: $order,
            action: 'reassignProvider',
            actorType: ActorType::Admin,
            actorId: $admin->getKey(),
            meta: [
                'provider_profile_id' => $provider->getKey(),
                'provider_name' => $provider->user->name,
                'previous_provider_profile_id' => $order->provider_profile_id,
                'reason' => $reason,
            ],
            mutate: function (Order $fresh) use ($provider, $admin): void {
                // العرض/التعيين السابق ← BACKED_OUT (نفس أثر اعتذار الفني، O-07)
                Offer::query()
                    ->where('order_id', $fresh->getKey())
                    ->where('status', OfferStatus::Accepted->value)
                    ->update([
                        'status' => OfferStatus::BackedOut->value,
                        'decided_at' => now(),
                    ]);

                $assignment = Offer::query()->create([
                    'order_id' => $fresh->getKey(),
                    'provider_profile_id' => $provider->getKey(),
                    'source' => OfferSource::AdminAssignment,
                    'price' => '0.00',
                    'status' => OfferStatus::Accepted,
                    'submitted_at' => now(),
                    'decided_at' => now(),
                ]);

                $fresh->provider_profile_id = $provider->getKey();
                $fresh->accepted_offer_id = $assignment->getKey();
                $fresh->assigned_by_admin_id = $admin->getKey();
                $fresh->assigned_at = now();
                $fresh->trip_started_at = null; // يبدأ التحرك من جديد
                $fresh->reopen_count = $fresh->reopen_count + 1;
            },
        );
    }
}
