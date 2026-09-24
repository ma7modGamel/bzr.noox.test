<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\OfferResource;
use App\Http\Api\V1\Resources\OrderResource;
use App\Http\Api\V1\Resources\ProposalResource;
use App\Modules\Offers\Actions\AcceptOfferAction;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Actions\CancelOrderAction;
use App\Modules\Orders\Actions\ConfirmCompletionAction;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Actions\RepublishOrderAction;
use App\Modules\Orders\Actions\UpdateRequestAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Support\Facades\Gate;

/** واجهات العميل على الطلب — 31 §العميل. الصلاحية عبر OrderPolicy (23). */
final class OrderController
{
    public function index(Request $request): AnonymousResourceCollection
    {
        $scope = $request->query('scope', 'current');

        $orders = Order::query()
            ->with($this->resourceRelations())
            ->where('customer_id', $request->user()->getKey())
            ->when($scope === 'current', fn ($q) => $q->active())
            ->when($scope === 'past', fn ($q) => $q->whereIn('status', ['CLOSED', 'CANCELLED', 'EXPIRED']))
            ->latest('created_at')
            ->latest('id')
            ->paginate(20);

        return OrderResource::collection($orders);
    }

    public function show(Request $request, Order $order): OrderResource
    {
        Gate::authorize('view', $order);

        return new OrderResource($order->load($this->resourceRelations()));
    }

    /** T-01 — 07. */
    public function store(Request $request, PublishRequestAction $action): JsonResponse
    {
        $data = $request->validate([
            'customer_address_id' => ['required', 'integer'],
            'category_id' => ['required', 'integer'],
            'problem_type_id' => ['required', 'integer'],
            'timing_type' => ['required', 'string', 'in:NOW,SCHEDULED'],
            'slot_start' => ['nullable', 'date'],
            'materials_responsibility' => ['required', 'string', 'in:CUSTOMER_HAS,PROVIDER_SUPPLIES,UNSURE'],
            'terms_accepted' => ['nullable', 'boolean'], // BR-018: إلزامية عند نسخة شروط جديدة فقط
            'description' => ['nullable', 'string', 'max:1000'],
            'media_ids' => ['array', 'max:7'],
            'media_ids.*' => ['integer'],
            'pricing_mode' => ['nullable', 'string', 'in:EXECUTION,INSPECTION'],
            'budget_amount' => ['nullable', 'numeric', 'min:0'],
        ]);

        $order = $action->execute($request->user(), new PublishRequestData(
            customerAddressId: (int) $data['customer_address_id'],
            categoryId: (int) $data['category_id'],
            problemTypeId: (int) $data['problem_type_id'],
            timingType: TimingType::from($data['timing_type']),
            materialsResponsibility: MaterialsResponsibility::from($data['materials_responsibility']),
            termsAccepted: (bool) ($data['terms_accepted'] ?? false),
            description: $data['description'] ?? null,
            mediaIds: array_map('intval', $data['media_ids'] ?? []),
            slotStart: isset($data['slot_start']) ? CarbonImmutable::parse($data['slot_start'])->utc() : null,
            pricingMode: isset($data['pricing_mode']) ? PricingMode::from($data['pricing_mode']) : null,
            budgetAmount: isset($data['budget_amount']) ? number_format((float) $data['budget_amount'], 2, '.', '') : null,
        ));

        return (new OrderResource($order->load(['category', 'problemType', 'area', 'city'])))
            ->response()
            ->setStatusCode(201);
    }

    public function update(Request $request, Order $order, UpdateRequestAction $action): OrderResource
    {
        Gate::authorize('view', $order);
        $data = $this->requestData($request);

        return new OrderResource($action->execute($order, $request->user(), $data)->load($this->resourceRelations()));
    }

    /** العروض — قائمة فارغة في وضع الموظفين لأن `offers()` يستبعد صف التعيين (BR-007). */
    public function offers(Request $request, Order $order): AnonymousResourceCollection
    {
        Gate::authorize('view', $order);

        $sort = $request->query('sort', 'rating');

        $offers = $order->offers()
            ->with('providerProfile.user')
            ->where('status', 'SUBMITTED')
            ->when($sort === 'price', fn ($q) => $q->orderBy('price'))
            ->when($sort === 'eta', fn ($q) => $q->orderBy('eta_minutes'))
            ->when($sort === 'rating', fn ($q) => $q->orderByDesc(
                ProviderProfile::query()
                    ->select('rating_avg')
                    ->whereColumn('provider_profiles.id', 'offers.provider_profile_id'),
            ))
            ->get();

        return OfferResource::collection($offers);
    }

    public function republish(Request $request, Order $order, RepublishOrderAction $action): OrderResource
    {
        Gate::authorize('view', $order);

        return new OrderResource($action->execute($order, $request->user())->load($this->resourceRelations()));
    }

    /** O-03 / T-02 — وضع السوق فقط؛ الخادم يرفض بـ FEATURE_DISABLED خارجه. */
    public function acceptOffer(Request $request, Order $order, Offer $offer, AcceptOfferAction $action): OrderResource
    {
        Gate::authorize('acceptOffer', $order);

        $data = $request->validate([
            'payment_method' => ['required', 'string', 'in:CASH,ELECTRONIC'],
        ]);

        $order = $action->execute(
            $order,
            $offer,
            $request->user()->getKey(),
            PaymentMethod::from($data['payment_method']),
        );

        return new OrderResource($order->load(['providerProfile.user', 'category', 'area']));
    }

    /** T-04 / T-08 — BR-070. */
    public function cancel(Request $request, Order $order, CancelOrderAction $action): OrderResource
    {
        Gate::authorize('cancel', $order);

        $data = $request->validate([
            'reason_code' => ['required', 'string'],
            'note' => ['nullable', 'string', 'max:500'],
        ]);

        $order = $action->execute(
            $order,
            ActorType::Customer,
            $request->user()->getKey(),
            CancelReason::from($data['reason_code']),
            $data['note'] ?? null,
        );

        return new OrderResource($order);
    }

    /** T-14 / T-15 / T-28 — قرار العميل على مقترح السعر. */
    public function decideProposal(
        Request $request,
        Order $order,
        PriceProposal $proposal,
        DecideProposalAction $action,
    ): OrderResource {
        Gate::authorize('decideProposal', $order);

        $approve = $request->boolean('approve');

        $order = $approve
            ? $action->approve($order, $proposal, ActorType::Customer, $request->user()->getKey())
            : $action->reject($order, $proposal, ActorType::Customer, $request->user()->getKey());

        return new OrderResource($order->load(['category', 'area']));
    }

    public function proposals(Request $request, Order $order): AnonymousResourceCollection
    {
        Gate::authorize('view', $order);

        return ProposalResource::collection($order->proposals()->with('order')->orderBy('id')->get());
    }

    /** T-21 — تأكيد الإنهاء. */
    public function confirmCompletion(Request $request, Order $order, ConfirmCompletionAction $action): OrderResource
    {
        Gate::authorize('view', $order);

        $order = $action->execute($order, ActorType::Customer, $request->user()->getKey());

        return new OrderResource($order);
    }

    /** @return list<string> */
    private function resourceRelations(): array
    {
        return [
            'category', 'problemType', 'area', 'city', 'providerProfile.user', 'customer',
            'offers', 'pendingProposalRecord', 'latestSuccessfulPayment', 'latestPaymentAttempt', 'review',
            'customerRating', 'conversations', 'disputes',
        ];
    }

    private function requestData(Request $request): PublishRequestData
    {
        $data = $request->validate([
            'customer_address_id' => ['required', 'integer'],
            'category_id' => ['required', 'integer'],
            'problem_type_id' => ['required', 'integer'],
            'timing_type' => ['required', 'string', 'in:NOW,SCHEDULED'],
            'slot_start' => ['nullable', 'date'],
            'materials_responsibility' => ['required', 'string', 'in:CUSTOMER_HAS,PROVIDER_SUPPLIES,UNSURE'],
            'description' => ['nullable', 'string', 'max:1000'],
            'media_ids' => ['array', 'max:7'],
            'media_ids.*' => ['integer'],
            'pricing_mode' => ['nullable', 'string', 'in:EXECUTION,INSPECTION'],
            'budget_amount' => ['nullable', 'numeric', 'min:0'],
        ]);

        return new PublishRequestData(
            customerAddressId: (int) $data['customer_address_id'],
            categoryId: (int) $data['category_id'],
            problemTypeId: (int) $data['problem_type_id'],
            timingType: TimingType::from($data['timing_type']),
            materialsResponsibility: MaterialsResponsibility::from($data['materials_responsibility']),
            termsAccepted: true,
            description: $data['description'] ?? null,
            mediaIds: array_map('intval', $data['media_ids'] ?? []),
            slotStart: isset($data['slot_start']) ? CarbonImmutable::parse($data['slot_start'])->utc() : null,
            pricingMode: isset($data['pricing_mode']) ? PricingMode::from($data['pricing_mode']) : null,
            budgetAmount: isset($data['budget_amount']) ? number_format((float) $data['budget_amount'], 2, '.', '') : null,
        );
    }
}
