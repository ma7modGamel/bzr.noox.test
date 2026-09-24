<!doctype html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow,noarchive">
    <meta name="referrer" content="no-referrer">
    @unless ($expired)
        <meta http-equiv="refresh" content="30">
    @endunless
    <title>{{ $expired ? 'انتهت صلاحية هذا الرابط' : $visit['app_name'] }}</title>
    <link rel="stylesheet" href="{{ asset('design/tokens.css') }}">
    <link rel="stylesheet" href="{{ asset('design/share-visit.css') }}">
</head>
<body>
<main class="share-shell">
    @if ($expired)
        <p class="brand">{{ config('app.name') }}</p>
        <section class="card expired" role="status">
            <h1>انتهت صلاحية هذا الرابط</h1>
        </section>
    @else
        <p class="brand">{{ $visit['app_name'] }}</p>
        <div class="stack">
            <section class="card provider" aria-label="بيانات الفني">
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
                        <img class="verified" src="{{ asset('design/icons/shield.svg') }}" alt="حساب موثّق">
                        <span>فني {{ $visit['provider']['category_name'] }} موثّق</span>
                    </div>
                </div>
            </section>

            <section class="card status-card" aria-label="حالة الطلب">
                <p class="eyebrow">حالة الزيارة</p>
                <p class="status-title">{{ $visit['display_status'] }}</p>
                @if ($visit['eta'] !== null && $visit['eta']['minutes'] !== null)
                    <div class="eta">
                        <span class="eta-value">{{ $visit['eta']['minutes'] }} دقيقة</span>
                        @if ($visit['eta']['approximate'])
                            <span class="approximate">تقريبي</span>
                        @endif
                    </div>
                @endif
            </section>

            @if ($visit['stepper'] !== null)
                <section aria-label="مراحل الطلب">
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

            <section class="card details" aria-label="بيانات الطلب">
                <div class="detail-row"><span class="detail-label">رقم الطلب</span><span class="detail-value">#{{ $visit['order_number'] }}</span></div>
                <div class="detail-row"><span class="detail-label">المنطقة</span><span class="detail-value">{{ $visit['area_name'] }}</span></div>
            </section>
        </div>
    @endif
</main>
</body>
</html>
