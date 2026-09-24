<?php

declare(strict_types=1);

return [
    'tokens_version' => env('DESIGN_TOKENS_VERSION', '1'),
    'minimum_supported_app_version' => [
        'android' => env('MINIMUM_ANDROID_APP_VERSION', '1.0.0'),
        'ios' => env('MINIMUM_IOS_APP_VERSION', '1.0.0'),
    ],
    'support_faqs' => [
        ['code' => 'REQUEST_STATUS', 'title' => 'كيف أتابع حالة طلبي؟', 'body' => 'افتح «طلباتي» ثم اختر الطلب لعرض حالته الحالية والخطوات المتاحة.'],
        ['code' => 'PAYMENT', 'title' => 'كيف يتم الدفع؟', 'body' => 'تظهر طرق الدفع المتاحة بعد انتهاء التنفيذ، ويؤكد الخادم نجاح الدفع قبل إغلاق الطلب.'],
        ['code' => 'CANCELLATION', 'title' => 'متى يمكن إلغاء الطلب؟', 'body' => 'يمكن الإلغاء من الإجراءات الظاهرة داخل الطلب. بعد وصول الفني استخدم «فتح مشكلة».'],
        ['code' => 'DISPUTE', 'title' => 'كيف أفتح مشكلة؟', 'body' => 'افتح تفاصيل الطلب واختر «فتح مشكلة» عندما يظهر هذا الإجراء.'],
    ],
];
