<?php

use App\Http\Controllers\AccountLinkController;
use App\Http\Controllers\FawryWebhookController;
use App\Http\Controllers\LegalPageController;
use App\Http\Controllers\ShareVisitController;
use Illuminate\Support\Facades\Route;

Route::get('/v/{token}', ShareVisitController::class)
    ->middleware('throttle:60,1')
    ->name('share.visit');

Route::post('/webhooks/fawry', FawryWebhookController::class)
    ->middleware('throttle:120,1')
    ->name('webhooks.fawry');

// SCR-W02 — روابط البريد (NTF-27، DEC-049).
Route::middleware('throttle:20,1')->group(function (): void {
    Route::get('/email/verify/{id}/{hash}', [AccountLinkController::class, 'verify'])
        ->whereNumber('id')
        ->name('web.verification.verify');
    Route::get('/password/reset/{token}', [AccountLinkController::class, 'showReset'])->name('password.reset');
    Route::post('/password/reset', [AccountLinkController::class, 'reset'])->name('password.update');
});

// DEC-051 — الصفحات القانونية العامة (المتاجر، روابط البريد، C34 على الويب).
Route::get('/{slug}', LegalPageController::class)
    ->where('slug', 'terms|privacy|cancellation-policy|faq')
    ->middleware('throttle:60,1')
    ->name('legal.show');

Route::get('/', function () {
    return view('welcome');
});
