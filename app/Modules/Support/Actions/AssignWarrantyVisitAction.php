<?php

declare(strict_types=1);

namespace App\Modules\Support\Actions;

use App\Modules\Communication\Services\ConversationService;
use App\Modules\Identity\Models\Admin;
use App\Modules\Offers\Enums\OfferSource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Models\WarrantyClaim;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Carbon\CarbonImmutable;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Support\Facades\DB;

final readonly class AssignWarrantyVisitAction
{
    public function __construct(
        private ProviderEligibility $eligibility,
        private OrderStateMachine $stateMachine,
        private ConversationService $conversations,
    ) {}

    public function execute(WarrantyClaim $claim, ProviderProfile $provider, Admin $admin, CarbonImmutable $scheduledAt, string $note): WarrantyClaim
    {
        if (! $admin->is_active) {
            throw new AuthorizationException;
        }

        return DB::transaction(function () use ($claim, $provider, $admin, $scheduledAt, $note): WarrantyClaim {
            $original = Order::query()->lockForUpdate()->findOrFail($claim->order_id);
            $fresh = WarrantyClaim::query()->lockForUpdate()->findOrFail($claim->id);
            $provider = ProviderProfile::query()->lockForUpdate()->findOrFail($provider->id);
            $candidate = clone $original;
            $candidate->timing_type = TimingType::Scheduled;
            $candidate->slot_start = $scheduledAt;
            $candidate->slot_end = $scheduledAt->addHours(2);
            if ($fresh->status !== 'REQUESTED' || $scheduledAt->isPast()
                || $provider->id === $original->provider_profile_id
                || ! $this->eligibility->isEligible($candidate, $provider)
                || WarrantyClaim::query()->where('provider_profile_id', $provider->id)->where('status', 'ASSIGNED')
                    ->where('scheduled_at', '>', $scheduledAt->subHours(2))->where('scheduled_at', '<', $scheduledAt->addHours(2))->exists()) {
                throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_assignment_invalid'));
            }
            $fresh->update(['provider_profile_id' => $provider->id, 'scheduled_at' => $scheduledAt, 'assigned_by_admin_id' => $admin->id,
                'assigned_at' => now(), 'admin_note' => $note, 'status' => 'ASSIGNED']);

            $visit = $fresh->visit()->first();
            if ($visit === null) {
                $visit = Order::query()->create([
                    ...$original->only(['customer_id', 'operating_mode', 'city_id', 'area_id', 'category_id', 'problem_type_id',
                        'customer_address_id', 'address_text', 'building', 'floor', 'apartment', 'landmark', 'lat', 'lng',
                        'materials_responsibility', 'terms_version', 'terms_accepted_at']),
                    'number' => DB::table('order_numbers')->insertGetId([]), 'warranty_claim_id' => $fresh->id,
                    'description' => $fresh->description, 'timing_type' => TimingType::Scheduled,
                    'slot_start' => $scheduledAt, 'slot_end' => $scheduledAt->addHours(2),
                    'pricing_mode' => PricingMode::Execution, 'status' => OrderStatus::Open,
                    'payment_method' => PaymentMethod::Cash, 'commission_rate' => '0.0000',
                ]);
            }
            $this->stateMachine->apply(
                $visit, 'assignWarrantyVisit', ActorType::Admin, $admin->id,
                meta: ['provider_profile_id' => $provider->id, 'original_order_id' => $original->id],
                mutate: function (Order $order) use ($provider, $admin, $scheduledAt): void {
                    $assignment = Offer::query()->create([
                        'order_id' => $order->id, 'provider_profile_id' => $provider->id,
                        'source' => OfferSource::AdminAssignment, 'status' => OfferStatus::Accepted,
                        'price' => '0.00', 'submitted_at' => now(), 'decided_at' => now(),
                    ]);
                    $order->fill(['provider_profile_id' => $provider->id, 'accepted_offer_id' => $assignment->id,
                        'assigned_by_admin_id' => $admin->id, 'assigned_at' => now(), 'confirmed_at' => now(),
                        'slot_start' => $scheduledAt, 'slot_end' => $scheduledAt->addHours(2), 'commission_rate' => '0.0000']);
                    $this->conversations->ensureAssignedConversation($order, $provider->id);
                },
            );

            return $fresh->refresh()->load('visit');
        });
    }
}
