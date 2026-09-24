@extends('layouts.public', ['title' => $title, 'noindex' => true])

@section('content')
    <section class="card center" role="status">
        <div class="state-icon {{ $success ? '' : 'error' }}">
            <img src="{{ asset('design/icons/'.($success ? 'check' : 'error').'.svg') }}" alt="" width="24" height="24">
        </div>
        <h1>{{ $title }}</h1>
        <p class="meta">{{ $body }}</p>
    </section>
@endsection
