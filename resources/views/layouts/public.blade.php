<!doctype html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="referrer" content="no-referrer">
    @isset($noindex)
        <meta name="robots" content="noindex,nofollow">
    @endisset
    <title>{{ $title }} — {{ config('app.name') }}</title>
    <link rel="stylesheet" href="{{ asset('design/tokens.css') }}">
    <link rel="stylesheet" href="{{ asset('design/public-page.css') }}">
</head>
<body>
<main class="page-shell">
    <header class="brand-bar"><a href="{{ url('/') }}">{{ config('app.name') }}</a></header>
    {{ $slot ?? '' }}
    @yield('content')
    <footer class="page-footer">
        <a href="{{ route('legal.show', 'terms') }}">الشروط والأحكام</a>
        <a href="{{ route('legal.show', 'privacy') }}">سياسة الخصوصية</a>
        <a href="{{ route('legal.show', 'cancellation-policy') }}">سياسة الإلغاء</a>
        <span>{{ config('mail.support_address') }}</span>
    </footer>
</main>
</body>
</html>
