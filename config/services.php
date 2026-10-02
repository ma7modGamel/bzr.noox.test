<?php

return [

    // DEC-049 — ZeptoMail (Zoho) للبريد التلقائي. المفتاح من env فقط، ولا يُكتب في السجل.
    'zeptomail' => [
        'url' => env('ZEPTOMAIL_API_URL', 'https://api.zeptomail.com/v1.1/email'),
        'key' => env('ZEPTOMAIL_API_KEY'),
        'timeout' => (int) env('ZEPTOMAIL_TIMEOUT', 10),
    ],

    /*
    |--------------------------------------------------------------------------
    | Third Party Services
    |--------------------------------------------------------------------------
    |
    | This file is for storing the credentials for third party services such
    | as Resend, Postmark, AWS, and more. This file provides the de facto
    | location for this type of information, allowing packages to have
    | a conventional file to locate the various service credentials.
    |
    */

    'postmark' => [
        'key' => env('POSTMARK_API_KEY'),
    ],

    'resend' => [
        'key' => env('RESEND_API_KEY'),
    ],

    'ses' => [
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
    ],

    'slack' => [
        'notifications' => [
            'bot_user_oauth_token' => env('SLACK_BOT_USER_OAUTH_TOKEN'),
            'channel' => env('SLACK_BOT_USER_DEFAULT_CHANNEL'),
        ],
    ],

    'google' => [
        'routes_key' => env('GOOGLE_MAPS_SERVER_KEY'),
        'routes_url' => env('GOOGLE_ROUTES_URL', 'https://routes.googleapis.com/directions/v2:computeRoutes'),
    ],

    // DEC-058 — مسار Push واحد عبر FCM HTTP v1 للمنصتين. `log` للتطوير والاختبار فقط.
    'fcm' => [
        'driver' => env('PUSH_DRIVER', 'log'),
        'project_id' => env('FCM_PROJECT_ID'),
        'credentials' => env('FCM_CREDENTIALS_PATH'),
        'timeout' => (int) env('FCM_TIMEOUT', 10),
    ],

    // DEC-058 — App Links وUniversal Links لروابط البريد `https://{APP_DOMAIN}/app/*`.
    'app_links' => [
        'package' => 'com.bremo.app',
        'android_sha256' => array_values(array_filter(array_map('trim', explode(',', (string) env('ANDROID_APP_LINK_SHA256', ''))))),
        'apple_team_id' => env('APPLE_TEAM_ID'),
        'bundle_id' => 'com.bremo.app',
    ],

    'fawry' => [
        'driver' => env('PAYMENT_GATEWAY_DRIVER', 'staging'),
        'merchant_code' => env('FAWRY_MERCHANT_CODE'),
        'security_key' => env('FAWRY_SECURITY_KEY', 'local-staging-secret'),
        'base_url' => env('FAWRY_BASE_URL', 'https://atfawry.fawrystaging.com'),
        'checkout_url' => env('FAWRY_CHECKOUT_URL', 'https://staging-pay.example.test/checkout'),
    ],

];
