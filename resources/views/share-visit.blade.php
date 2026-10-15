<!doctype html>
<html lang="{{ \App\Support\Localization::locale() }}" dir="{{ \App\Support\Localization::locale() === 'ar' ? 'rtl' : 'ltr' }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow,noarchive">
    <meta name="referrer" content="no-referrer">
    @unless ($expired)
        <meta http-equiv="refresh" content="30">
    @endunless
    <title>{{ $expired ? \App\Support\Localization::text('web.visit_expired') : $visit['app_name'] }}</title>
    <link rel="stylesheet" href="{{ asset('design/tokens.css') }}">
    <link rel="stylesheet" href="{{ asset('design/share-visit.css') }}">
</head>
<body>
<main class="share-shell">
    @if ($expired)
        <p class="brand">{{ config('app.name') }}</p>
        <section class="card expired" role="status">
            <h1>{{ \App\Support\Localization::text('web.visit_expired') }}</h1>
        </section>
    @else
        <p class="brand">{{ $visit['app_name'] }}</p>
        <div class="stack">
            <section class="card provider" aria-label="{{ \App\Support\Localization::text('web.provider_details') }}">
                <div class="avatar">
                    @if ($visit['provider']['avatar_url'])
                        <img src="{{ $visit['provider']['avatar_url'] }}" alt="{{ $visit['provider']['first_name'] }}">
                    @else
                        <span aria-hidden="true">{{ mb_substr($visit['provider']['first_name'], 0, 1) }}</span>
                    @endif
                </div>
                <div class="provider-copy">
                    <h1 class="provider-name">{{ $visit['provider']['first_name'] }}</h1>
                    <div class="provider-meta">
                        <img class="verified" src="{{ asset('design/icons/shield.svg') }}" alt="{{ \App\Support\Localization::text('web.verified_account') }}">
                        <span>{{ \App\Support\Localization::text('web.verified_provider', ['category' => $visit['provider']['category_name']]) }}</span>
                    </div>
                </div>
            </section>

            <section class="card status-card" aria-label="{{ \App\Support\Localization::text('web.order_status') }}">
                <p class="eyebrow">{{ \App\Support\Localization::text('web.visit_status') }}</p>
                <p class="status-title">{{ $visit['display_status'] }}</p>
                @if ($visit['eta'] !== null && $visit['eta']['minutes'] !== null)
                    <div class="eta">
                        <span class="eta-value">{{ \App\Support\Localization::text('web.minutes', ['count' => $visit['eta']['minutes']]) }}</span>
                        @if ($visit['eta']['approximate'])
                            <span class="approximate">{{ \App\Support\Localization::text('web.approximate') }}</span>
                        @endif
                    </div>
                @endif
            </section>

            @if ($visit['stepper'] !== null)
                <section aria-label="{{ \App\Support\Localization::text('web.order_steps') }}">
                    <div class="stepper">
                        @foreach ($visit['stepper'] as $step)
                            <div class="step {{ $step['state'] }}">
                                <div class="step-rail"><span class="step-dot" aria-hidden="true"></span></div>
                                <span class="step-label">{{ $step['label'] }}</span>
                            </div>
                        @endforeach
                    </div>
                    @if ($visit['warning'] !== null)
                        <div class="warning" role="status"><span class="warning-mark" aria-hidden="true">!</span>{{ $visit['warning'] }}</div>
                    @endif
                </section>
            @endif

            <section class="card details" aria-label="{{ \App\Support\Localization::text('web.order_details') }}">
                <div class="detail-row"><span class="detail-label">{{ \App\Support\Localization::text('web.order_number') }}</span><span class="detail-value">#{{ $visit['order_number'] }}</span></div>
                <div class="detail-row"><span class="detail-label">{{ \App\Support\Localization::text('web.area') }}</span><span class="detail-value">{{ $visit['area_name'] }}</span></div>
            </section>
        </div>
    @endif
</main>
</body>
</html>
