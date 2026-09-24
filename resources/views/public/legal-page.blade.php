@extends('layouts.public', ['title' => $title])

@section('content')
    <article class="card">
        <h1>{{ $title }}</h1>
        @if ($version === null)
            <p class="meta">هذه الصفحة قيد الإعداد وستُنشر قريبًا.</p>
        @else
            <p class="meta">النسخة {{ $version->version }} — سارية من {{ $version->effective_at?->timezone('Africa/Cairo')->format('Y-m-d') }}</p>
            <div class="legal-body">{!! $version->safeBody() !!}</div>
        @endif
    </article>
@endsection
