@extends('layouts.public', ['title' => $title, 'noindex' => true])

@section('content')
    <form class="card" method="post" action="{{ route('password.update', ['locale' => \App\Support\Localization::locale()]) }}">
        @csrf
        <h1>{{ $title }}</h1>
        <input type="hidden" name="token" value="{{ $token }}">
        <div>
            <label for="email">{{ \App\Support\Localization::text('web.email_label') }}</label>
            <input id="email" name="email" type="email" dir="ltr" value="{{ old('email', $email) }}" required autocomplete="email">
        </div>
        <div>
            <label for="password">{{ \App\Support\Localization::text('web.password_label') }}</label>
            <input id="password" name="password" type="password" required minlength="8" autocomplete="new-password">
        </div>
        <div>
            <label for="password_confirmation">{{ \App\Support\Localization::text('web.password_confirmation') }}</label>
            <input id="password_confirmation" name="password_confirmation" type="password" required minlength="8" autocomplete="new-password">
        </div>
        @if ($errors->any())
            <p class="error-text" role="alert">{{ $errors->first() }}</p>
        @endif
        <button type="submit">{{ \App\Support\Localization::text('web.save_password') }}</button>
    </form>
@endsection
