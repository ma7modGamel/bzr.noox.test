<?php

declare(strict_types=1);

use App\Http\Api\V1\Controllers\AccountController;
use App\Http\Api\V1\Controllers\AddressController;
use App\Http\Api\V1\Controllers\AuthController;
use App\Http\Api\V1\Controllers\ConfigController;
use App\Http\Api\V1\Controllers\ConversationController;
use App\Http\Api\V1\Controllers\HelpController;
use App\Http\Api\V1\Controllers\MediaController;
use App\Http\Api\V1\Controllers\NotificationController;
use App\Http\Api\V1\Controllers\OrderController;
use App\Http\Api\V1\Controllers\PageController;
use App\Http\Api\V1\Controllers\PaymentController;
use App\Http\Api\V1\Controllers\ProviderController;
use App\Http\Api\V1\Controllers\ProviderOrderController;
use App\Http\Api\V1\Controllers\RatingController;
use App\Http\Api\V1\Controllers\ReferenceController;
use App\Http\Api\V1\Controllers\ShareLinkController;
use App\Http\Api\V1\Controllers\SupportController;
use App\Http\Api\V1\Controllers\TrackingController;
use App\Http\Middleware\CheckExpectedVersion;
use Illuminate\Support\Facades\Route;

/*
| واجهات API v1 — العقد في 31-API-CONTRACT.
|
| البادئة `api/v1` من bootstrap/app.php. لا يوجد أي مسار لتعديل الحالة مباشرة:
| كل تغيير حالة يمر بإجراء مرتبط بانتقال في 10 (30 §آلة حالات الطلب).
*/

// ── عامة ──────────────────────────────────────────────────────────────
Route::get('config', ConfigController::class);                     // DEC-041
Route::get('cities', [ReferenceController::class, 'cities']);
Route::get('cities/{city}/areas', [ReferenceController::class, 'areas']);
Route::get('catalog', [ReferenceController::class, 'catalog']);
Route::get('slots', [ReferenceController::class, 'slots']);
Route::get('terms/current', [PageController::class, 'terms']);          // BR-018، DEC-051
Route::get('pages/{slug}', [PageController::class, 'show'])->where('slug', '[a-z-]+'); // DEC-051
Route::get('support/faqs', [HelpController::class, 'faqs']);

Route::post('auth/register', [AuthController::class, 'register'])->middleware('throttle:5,1');
Route::post('auth/login', [AuthController::class, 'login'])->middleware('throttle:5,1');
Route::post('auth/password/forgot', [AuthController::class, 'forgotPassword'])->middleware('throttle:5,1');
Route::post('auth/password/reset', [AuthController::class, 'resetPassword'])->middleware('throttle:5,1');
Route::get('auth/email/verify/{id}/{hash}', [AuthController::class, 'verifyEmail'])
    ->middleware(['signed', 'throttle:6,1'])
    ->name('verification.verify');

// ── محمية ─────────────────────────────────────────────────────────────
Route::middleware('auth:sanctum')->group(function (): void {
    Route::post('auth/logout', [AuthController::class, 'logout']);
    Route::post('auth/email/resend', [AuthController::class, 'resendVerification'])->middleware('throttle:3,60');
    Route::get('me', [AccountController::class, 'show']);
    Route::get('me/avatar', [AccountController::class, 'avatar'])->name('api.account.avatar');
    Route::patch('me', [AccountController::class, 'update']);
    Route::post('me/password', [AccountController::class, 'password'])->middleware('throttle:5,1');
    Route::delete('me', [AccountController::class, 'destroy'])->middleware('throttle:3,60');
    Route::post('support/messages', [HelpController::class, 'message'])->middleware('throttle:5,60');
    Route::get('notifications', [NotificationController::class, 'index']);
    Route::post('notifications/read', [NotificationController::class, 'read']);

    // العناوين
    Route::get('addresses', [AddressController::class, 'index']);
    Route::post('addresses', [AddressController::class, 'store']);
    Route::patch('addresses/{address}', [AddressController::class, 'update']);
    Route::delete('addresses/{address}', [AddressController::class, 'destroy']);

    // الوسائط والمحادثات المشتركة
    Route::post('media', [MediaController::class, 'store']);
    Route::get('media/{media}', [MediaController::class, 'show'])->name('api.media.show');
    Route::delete('media/{media}', [MediaController::class, 'destroy']);
    Route::get('conversations', [ConversationController::class, 'index']);
    Route::post('conversations', [ConversationController::class, 'store']);
    Route::get('conversations/{conversation}/messages', [ConversationController::class, 'messages']);
    Route::post('conversations/{conversation}/messages', [ConversationController::class, 'send'])
        ->middleware('throttle:30,1');

    // ── العميل ────────────────────────────────────────────────────────
    Route::get('orders', [OrderController::class, 'index']);
    Route::post('orders', [OrderController::class, 'store'])->middleware('throttle:10,60'); // 31 §حدود المعدل
    Route::patch('orders/{order}', [OrderController::class, 'update'])->middleware('throttle:10,60');
    Route::get('orders/{order}', [OrderController::class, 'show']);
    Route::get('orders/{order}/offers', [OrderController::class, 'offers']);
    Route::post('orders/{order}/republish', [OrderController::class, 'republish'])->middleware('throttle:10,60');
    Route::get('orders/{order}/proposals', [OrderController::class, 'proposals']);
    Route::get('orders/{order}/tracking', [TrackingController::class, 'show']);
    Route::get('providers/{provider}', [ProviderController::class, 'show']);
    Route::post('orders/{order}/share-links', [ShareLinkController::class, 'store'])
        ->middleware('throttle:60,1');
    Route::delete('share-links/{shareLink}', [ShareLinkController::class, 'destroy']);
    Route::post('orders/{order}/review', [RatingController::class, 'review']);
    Route::get('orders/{order}/disputes', [SupportController::class, 'disputes']);
    Route::post('orders/{order}/disputes', [SupportController::class, 'openDispute']);
    Route::post('providers/{provider}/reports', [SupportController::class, 'reportProvider'])
        ->middleware('throttle:5,1');

    Route::middleware(CheckExpectedVersion::class)->group(function (): void {
        Route::post('orders/{order}/offers/{offer}/accept', [OrderController::class, 'acceptOffer']);
        Route::post('orders/{order}/cancel', [OrderController::class, 'cancel']);
        Route::post('orders/{order}/proposals/{proposal}/decide', [OrderController::class, 'decideProposal']);
        Route::post('orders/{order}/confirm-completion', [OrderController::class, 'confirmCompletion']);
        Route::patch('orders/{order}/payment-method', [PaymentController::class, 'changeMethod']);
        Route::post('orders/{order}/instapay-transfers', [PaymentController::class, 'instapayTransfer'])
            ->middleware('throttle:10,60'); // BR-057
        Route::post('orders/{order}/payments', [PaymentController::class, 'store'])
            ->middleware('throttle:10,1');
    });

    // ── الفني ─────────────────────────────────────────────────────────
    Route::prefix('provider')->group(function (): void {
        Route::get('requests', [ProviderOrderController::class, 'availableRequests']);
        Route::get('orders', [ProviderOrderController::class, 'myOrders']);
        Route::post('orders/{order}/customer-rating', [RatingController::class, 'customerRating']);

        Route::middleware(CheckExpectedVersion::class)->group(function (): void {
            Route::post('requests/{order}/offers', [ProviderOrderController::class, 'submitOffer'])
                ->middleware('throttle:60,60');
            Route::post('orders/{order}/start-trip', [ProviderOrderController::class, 'startTrip']);
            Route::post('orders/{order}/location', [ProviderOrderController::class, 'recordLocation'])
                ->middleware('throttle:4,1');  // BR-110 — كل 30 ثانية
            Route::post('orders/{order}/arrived', [ProviderOrderController::class, 'markArrived']);
            Route::post('orders/{order}/start-work', [ProviderOrderController::class, 'startWork']);
            Route::post('orders/{order}/proposals', [ProviderOrderController::class, 'submitProposal']);
            Route::post('orders/{order}/complete-inspection-only', [ProviderOrderController::class, 'completeInspectionOnly']);
            Route::post('orders/{order}/complete', [ProviderOrderController::class, 'complete']);
            Route::post('orders/{order}/cash-received', [ProviderOrderController::class, 'cashReceived']);
            Route::post('orders/{order}/back-out', [ProviderOrderController::class, 'backOut']);
            Route::post('orders/{order}/unable', [ProviderOrderController::class, 'unable']);
            Route::post('orders/{order}/customer-no-show', [ProviderOrderController::class, 'customerNoShow']);
        });
    });
});
