<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\OrderResource;
use App\Http\Api\V1\Resources\ProposalResource;
use App\Modules\Offers\Actions\SubmitOfferAction;
use App\Modules\Orders\Actions\BackOutAction;
use App\Modules\Orders\Actions\CompleteInspectionOnlyAction;
use App\Modules\Orders\Actions\CompleteWorkAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\RecordLocationAction;
use App\Modules\Orders\Actions\ReportUnableAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Actions\StartWorkAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Payments\Actions\ConfirmCashReceivedAction;
use App\Modules\Pricing\Actions\SubmitProposalAction;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Support\Facades\Gate;

/** واجهات الفني — 31 §الفني. كل إجراء يتحقق من الإسناد عبر OrderPolicy::perform. */
final class ProviderOrderController
{
    /** الطلبات المتاحة — فارغة في وضع الموظفين (39): لا بث ولا عروض. */
    public function availableRequests(Request $request, FeatureGate $features, ProviderEligibility $eligibility): AnonymousResourceCollection
    {
        if (! $features->offersEnabled()) {
            return OrderResource::collection(collect());
        }

        $profile = $this->profile($request);

        $orders = Order::query()
            ->with($this->resourceRelations())
            ->where('status', 'OPEN')
            ->where('offers_close_at', '>', now())
            ->whereHas('category', fn ($q) => $q->whereIn('id', $profile->categories()->pluck('categories.id')))
            ->whereIn('area_id', $profile->areas()->pluck('areas.id'))
            ->where('customer_id', '!=', $request->user()->getKey())
            ->latest('id')
            ->paginate(20);

        return OrderResource::collection($orders);
    }

    /** طلبات الفني المسنَدة إليه. */
    public function myOrders(Request $request): AnonymousResourceCollection
    {
        $profile = $this->profile($request);
        $scope = $request->query('scope', 'current');

        $orders = Order::query()
            ->with($this->resourceRelations())
            ->where('provider_profile_id', $profile->getKey())
            ->when($scope === 'current', fn ($q) => $q->active())
            ->when($scope === 'past', fn ($q) => $q->whereIn('status', ['CLOSED', 'CANCELLED', 'EXPIRED']))
            ->latest('id')
            ->paginate(20);

        return OrderResource::collection($orders);
    }

    /** O-01 — وضع السوق فقط. */
    public function submitOffer(Request $request, Order $order, SubmitOfferAction $action): JsonResponse
    {
        $data = $request->validate([
            'price' => ['required', 'numeric', 'min:0'],
            'eta_minutes' => ['nullable', 'integer', 'between:5,180'],
            'inspection_fee_deductible' => ['nullable', 'boolean'],
            'includes_text' => ['nullable', 'string', 'max:150'],
            'note' => ['nullable', 'string', 'max:300'],
        ]);

        $price = number_format((float) $data['price'], 2, '.', '');

        $offer = $action->execute(
            $order,
            $this->profile($request),
            $price,
            isset($data['eta_minutes']) ? (int) $data['eta_minutes'] : null,
            $data['inspection_fee_deductible'] ?? null,
            $data['includes_text'] ?? null,
            $data['note'] ?? null,
        );

        return new JsonResponse([
            'offer' => ['id' => $offer->getKey(), 'status' => $offer->status->value, 'price' => $offer->price],
            'net_amount' => $action->netAmount($price),   // "صافي لك" (09)
        ], 201);
    }

    public function startTrip(Request $request, Order $order, StartTripAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        $data = $request->validate([
            'lat' => ['required', 'numeric', 'between:-90,90'],
            'lng' => ['required', 'numeric', 'between:-180,180'],
        ]);

        return new OrderResource($action->execute(
            $order,
            $this->profile($request),
            (float) $data['lat'],
            (float) $data['lng'],
        ));
    }

    /** BR-110 — كل CFG-080 أثناء "في الطريق" فقط. */
    public function recordLocation(Request $request, Order $order, RecordLocationAction $action): JsonResponse
    {
        Gate::authorize('perform', $order);

        $data = $request->validate([
            'lat' => ['required', 'numeric', 'between:-90,90'],
            'lng' => ['required', 'numeric', 'between:-180,180'],
        ]);

        $action->execute($order, $this->profile($request), (float) $data['lat'], (float) $data['lng']);

        return new JsonResponse(status: 204);
    }

    public function markArrived(Request $request, Order $order, MarkArrivedAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        $data = $request->validate([
            'lat' => ['required', 'numeric', 'between:-90,90'],
            'lng' => ['required', 'numeric', 'between:-180,180'],
            'confirm_far_arrival' => ['boolean'],
        ]);

        return new OrderResource($action->execute(
            $order,
            $this->profile($request),
            (float) $data['lat'],
            (float) $data['lng'],
            $request->boolean('confirm_far_arrival'),
        ));
    }

    public function startWork(Request $request, Order $order, StartWorkAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        return new OrderResource($action->execute($order, $this->profile($request)));
    }

    /** T-12 / خامات / أعمال إضافية — 12. */
    public function submitProposal(Request $request, Order $order, SubmitProposalAction $action): JsonResponse
    {
        Gate::authorize('perform', $order);

        $data = $request->validate([
            'type' => ['required', 'string', 'in:EXECUTION_QUOTE,MATERIALS,EXTRA_WORK'],
            'amount' => ['required', 'numeric', 'min:0'],
            'reason' => ['required', 'string', 'min:5', 'max:300'],
            'photo_path' => ['nullable', 'string'],
        ]);

        $proposal = $action->execute(
            $order,
            $this->profile($request),
            ProposalType::from($data['type']),
            number_format((float) $data['amount'], 2, '.', ''),
            $data['reason'],
            $data['photo_path'] ?? null,
        );

        return (new ProposalResource($proposal))->response()->setStatusCode(201);
    }

    /** T-13 أو T-28 حسب رسوم المعاينة (BR-056). */
    public function completeInspectionOnly(Request $request, Order $order, CompleteInspectionOnlyAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        return new OrderResource($action->execute(
            $order,
            ActorType::Provider,
            $this->profile($request)->getKey(),
        ));
    }

    public function complete(Request $request, Order $order, CompleteWorkAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        return new OrderResource($action->execute($order, $this->profile($request)));
    }

    /** T-19 — BR-053. */
    public function cashReceived(Request $request, Order $order, ConfirmCashReceivedAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        $data = $request->validate(['amount' => ['required', 'numeric', 'min:0']]);

        return new OrderResource($action->execute(
            $order,
            number_format((float) $data['amount'], 2, '.', ''),
            ActorType::Provider,
            $this->profile($request)->getKey(),
        ));
    }

    /** T-06 — اعتذار قبل الوصول (BR-036). */
    public function backOut(Request $request, Order $order, BackOutAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        $data = $request->validate(['reason_code' => ['required', 'string']]);
        $profile = $this->profile($request);

        return new OrderResource($action->execute(
            $order,
            ActorType::Provider,
            $profile->getKey(),
            CancelReason::from($data['reason_code']),
            $profile,
        ));
    }

    /** T-16 / T-18 — BR-071. */
    public function unable(Request $request, Order $order, ReportUnableAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        $data = $request->validate([
            'reason_code' => ['required', 'string'],
            'note' => ['required', 'string', 'min:5', 'max:500'],
        ]);

        return new OrderResource($action->unableToPerform(
            $order,
            $this->profile($request),
            CancelReason::from($data['reason_code']),
            $data['note'],
        ));
    }

    public function customerNoShow(Request $request, Order $order, ReportUnableAction $action): OrderResource
    {
        Gate::authorize('perform', $order);

        return new OrderResource($action->customerNoShow($order, $this->profile($request)));
    }

    /** وضع الفني يتطلب ملفًا معتمدًا (03، 15). */
    private function profile(Request $request): ProviderProfile
    {
        $profile = $request->user()->providerProfile;

        if ($profile === null) {
            throw BusinessRuleViolationException::rule('BR-022', 'لا يوجد ملف فني لهذا الحساب.');
        }

        return $profile;
    }

    /** @return list<string> */
    private function resourceRelations(): array
    {
        return [
            'category', 'problemType', 'area', 'city', 'customer', 'providerProfile.user',
            'offers', 'pendingProposalRecord', 'latestSuccessfulPayment', 'review',
            'customerRating', 'conversations',
        ];
    }
}
