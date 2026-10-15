<!doctype html>
<html lang="{{ \App\Support\Localization::locale() }}" dir="{{ \App\Support\Localization::locale() === 'ar' ? 'rtl' : 'ltr' }}">
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
        @foreach ([\App\Modules\Content\Enums\LegalPageSlug::Terms, \App\Modules\Content\Enums\LegalPageSlug::Privacy, \App\Modules\Content\Enums\LegalPageSlug::Cancellation] as $legalPage)
            <a href="{{ route('legal.show', ['slug' => $legalPage->value, 'locale' => \App\Support\Localization::locale()]) }}">{{ $legalPage->getLabel() }}</a>
        @endforeach
        <bdi dir="ltr">{{ config('mail.support_address') }}</bdi>
    </footer>
</main>
</body>
</html>
