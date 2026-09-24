@extends('layouts.public', ['title' => $title, 'noindex' => true])

@section('content')
    <form class="card" method="post" action="{{ route('password.update') }}">
        @csrf
        <h1>{{ $title }}</h1>
        <input type="hidden" name="token" value="{{ $token }}">
        <div>
            <label for="email">البريد الإلكتروني</label>
            <input id="email" name="email" type="email" value="{{ old('email', $email) }}" required autocomplete="email">
        </div>
        <div>
            <label for="password">كلمة المرور الجديدة</label>
            <input id="password" name="password" type="password" required minlength="8" autocomplete="new-password">
        </div>
        <div>
            <label for="password_confirmation">تأكيد كلمة المرور</label>
            <input id="password_confirmation" name="password_confirmation" type="password" required minlength="8" autocomplete="new-password">
        </div>
        @if ($errors->any())
            <p class="error-text" role="alert">{{ $errors->first() }}</p>
        @endif
        <button type="submit">حفظ كلمة المرور</button>
    </form>
@endsection
