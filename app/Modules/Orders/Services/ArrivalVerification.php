<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

final readonly class ArrivalVerification
{
    public function __construct(private SettingsRepository $settings) {}

    public function required(Order $order): bool
    {
        return $order->operating_mode === OperatingMode::Employee;
    }

    /** @return array{code: string, expires_at: string, server_time: string} */
    public function issue(Order $order, User $customer, bool $refresh = false): array
    {
        return DB::transaction(function () use ($order, $customer, $refresh): array {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->id);
            abort_unless($fresh->customer_id === $customer->id, 404);
            $this->guardTrip($fresh);
            if ($fresh->arrival_code_locked_until?->isFuture()) {
                throw BusinessRuleViolationException::rule('ARRIVAL-CODE', Localization::text('copy.arrival_code_locked'));
            }
            if ($refresh || ! $this->usable($fresh)) {
                do {
                    $code = (string) random_int(100000, 999999);
                } while ($code === $fresh->arrival_code);
                $fresh->forceFill(['arrival_code' => $code,
                    'arrival_code_expires_at' => now()->addMinutes(max(1, min(60, $this->settings->int(Cfg::ArrivalCodeValidityMinutes)))),
                    'arrival_code_provider_id' => $fresh->provider_profile_id, 'arrival_code_trip_started_at' => $fresh->trip_started_at,
                    'arrival_code_attempts' => 0, 'arrival_code_locked_until' => null])->save();
            }

            return ['code' => $fresh->arrival_code, 'expires_at' => $fresh->arrival_code_expires_at->toIso8601String(), 'server_time' => now()->toIso8601String()];
        });
    }

    /** Called with the order locked. Failure is returned so the attempt counter can commit. */
    public function attempt(Order $fresh, ?string $code): ?string
    {
        if (! $this->required($fresh)) {
            return null;
        }
        if ($fresh->arrival_code_locked_until?->isFuture()) {
            return Localization::text('copy.arrival_code_locked');
        }
        if (! $this->usable($fresh)) {
            return Localization::text('copy.arrival_code_expired');
        }
        if ($code === null || ! hash_equals($fresh->arrival_code, $code)) {
            $fresh->arrival_code_attempts++;
            if ($fresh->arrival_code_attempts >= max(1, min(10, $this->settings->int(Cfg::ArrivalCodeMaxAttempts)))) {
                $fresh->arrival_code_locked_until = now()->addMinutes(max(1, min(60, $this->settings->int(Cfg::ArrivalCodeLockMinutes))));
                $fresh->arrival_code_expires_at = now();
            }
            $fresh->save();

            return Localization::text($fresh->arrival_code_locked_until?->isFuture() ? 'copy.arrival_code_locked' : 'copy.arrival_code_invalid');
        }

        return null;
    }

    public function report(Order $order, ProviderProfile $provider, float $lat, float $lng, string $note, int $version): Order
    {
        return DB::transaction(function () use ($order, $provider, $lat, $lng, $note, $version): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->id);
            abort_unless($fresh->provider_profile_id === $provider->id, 404);
            abort_if($fresh->version !== $version, 409);
            $this->guardTrip($fresh);
            if (mb_strlen(trim($note)) < 5 || mb_strlen($note) > 500) {
                throw BusinessRuleViolationException::rule('ARRIVAL-CODE', Localization::text('copy.arrival_exception_note_required'));
            }
            if ($fresh->arrival_exception_report !== null) {
                return $fresh;
            }
            $fresh->forceFill(['arrival_exception_report' => ['provider_id' => $provider->id, 'lat' => $lat, 'lng' => $lng,
                'note' => $note, 'reported_at' => now()->toIso8601String()], 'version' => $fresh->version + 1])->save();
            OrderEvent::query()->create(['order_id' => $fresh->id, 'event_code' => OrderEventCode::ArrivalExceptionReported,
                'actor_type' => ActorType::Provider, 'actor_id' => $provider->id, 'from_status' => $fresh->status,
                'to_status' => $fresh->status, 'meta' => ['note' => $note], 'created_at' => now()]);

            return $fresh;
        });
    }

    private function guardTrip(Order $order): void
    {
        if (! $this->required($order) || $order->status !== OrderStatus::OnTheWay || $order->provider_profile_id === null || $order->trip_started_at === null) {
            throw BusinessRuleViolationException::rule('ARRIVAL-CODE', Localization::text('copy.arrival_code_unavailable'));
        }
    }

    private function usable(Order $order): bool
    {
        return $order->arrival_code !== null && $order->arrival_code_expires_at?->isFuture()
            && $order->arrival_code_provider_id === $order->provider_profile_id
            && $order->arrival_code_trip_started_at?->equalTo($order->trip_started_at);
    }
}
