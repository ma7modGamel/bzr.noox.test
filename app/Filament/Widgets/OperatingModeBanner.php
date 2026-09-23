<?php

declare(strict_types=1);

namespace App\Filament\Widgets;

use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\FeatureGate;
use Filament\Widgets\Widget;

/**
 * شريط ثابت أعلى اللوحة يوضح وضع التشغيل الحالي (39).
 * سبب وجوده: كل قرار تشغيلي — التعيين، العروض، رسوم المعاينة — يتغير معناه بتبديل المفتاح.
 */
final class OperatingModeBanner extends Widget
{
    protected string $view = 'filament.widgets.operating-mode-banner';

    protected static ?int $sort = -10;

    /** معلومة ثابتة صغيرة: تُعرض مع الصفحة لا بتحميل كسول. */
    protected static bool $isLazy = false;

    protected int|string|array $columnSpan = 'full';

    public function getMode(): OperatingMode
    {
        return app(FeatureGate::class)->mode();
    }

    public function isInspectionFree(): bool
    {
        return app(FeatureGate::class)->inspectionIsFree();
    }
}
