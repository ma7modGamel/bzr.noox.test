@extends('layouts.public', ['title' => $title])

@section('content')
    <article class="card">
        <h1>{{ $title }}</h1>
        @if ($version === null)
            <p class="meta">{{ \App\Support\Localization::text('messages.legal_unpublished') }}</p>
        @else
            <p class="meta">{{ \App\Support\Localization::text('messages.legal_metadata', ['version' => $version->version, 'date' => $version->effective_at?->timezone('Africa/Cairo')->format('Y-m-d') ?? '']) }}</p>
            <div class="legal-body">{!! $version->safeBody() !!}</div>
        @endif
    </article>
@endsection
