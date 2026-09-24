<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\ShareLink;
use App\Modules\Orders\Services\OrderPresentation;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

final class ShareVisitController extends Controller
{
    public function __invoke(string $token, OrderPresentation $presentation): Response
    {
        $link = ShareLink::query()
            ->with(['order.providerProfile.user', 'order.category', 'order.area'])
            ->where('token', $token)
            ->first();

        if ($link === null || ! $link->isUsable()) {
            return $this->response('share-visit', ['expired' => true], 410);
        }

        $order = $link->order;
        $provider = $order->providerProfile;

        if ($provider === null || ! $provider->isVerified()) {
            return $this->response('share-visit', ['expired' => true], 410);
        }

        $firstName = Str::of($provider->user->name)->trim()->before(' ')->toString();
        $steps = $presentation->stepper($order);

        $viewModel = [
            'app_name' => (string) config('app.name'),
            'provider' => [
                'first_name' => $firstName,
                'avatar_url' => $provider->user->avatar_path === null
                    ? null
                    : Storage::disk('public')->url($provider->user->avatar_path),
                'category_name' => $order->category->name,
            ],
            'display_status' => $order->statusLabel(),
            'status' => $order->status,
            'eta' => $order->status === OrderStatus::OnTheWay ? [
                'minutes' => $order->eta_minutes,
                'approximate' => $order->eta_approximate,
            ] : null,
            'stepper' => $steps === null ? null : array_map(fn (array $step): array => [
                ...$step,
                'label' => $this->designString('status.'.$step['key']),
            ], $steps),
            'warning' => $order->status === OrderStatus::Disputed
                ? $this->designString('status.reviewing')
                : null,
            'order_number' => $order->number,
            'area_name' => $order->area->name,
        ];

        return $this->response('share-visit', ['expired' => false, 'visit' => $viewModel]);
    }

    /** @param array<string, mixed> $data */
    private function response(string $view, array $data, int $status = 200): Response
    {
        return response()
            ->view($view, $data, $status)
            ->header('Cache-Control', 'no-store, private')
            ->header('Pragma', 'no-cache')
            ->header('X-Robots-Tag', 'noindex, nofollow, noarchive')
            ->header('Referrer-Policy', 'no-referrer')
            ->header('Content-Security-Policy', "default-src 'none'; style-src 'self'; img-src 'self' data:; font-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
    }

    private function designString(string $key): string
    {
        static $strings;

        $strings ??= json_decode(
            (string) file_get_contents(base_path('design/strings.ar.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );

        return (string) ($strings[$key] ?? $key);
    }
}
