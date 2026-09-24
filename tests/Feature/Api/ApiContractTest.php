<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Orders\Services\OrderPresentation;
use Illuminate\Routing\Route as RoutingRoute;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Route;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * حراس عقد API — 41 §البوابة 1.9.
 * التطبيقان يُبنيان على هذا العقد، فأي انحراف يجب أن يكسر البناء هنا لا في المتجر.
 */
final class ApiContractTest extends TestCase
{
    #[Test]
    public function ملف_العقد_مطابق_للمسارات(): void
    {
        $this->assertSame(0, Artisan::call('api:spec', ['--check' => true]), Artisan::output());
    }

    /** 31 — لا يوجد أي endpoint لتعديل الحالة مباشرة. */
    #[Test]
    public function لا_يوجد_مسار_يعدل_الحالة_مباشرة(): void
    {
        foreach ($this->apiRoutes() as $route) {
            $this->assertStringNotContainsString(
                'status',
                $route->uri(),
                "المسار {$route->uri()} يوحي بتعديل الحالة مباشرة.",
            );

            $this->assertNotContains(
                'PUT',
                $route->methods(),
                "المسار {$route->uri()} يستخدم PUT؛ الإجراءات تُنفَّذ بـ POST مرتبطة بانتقال في 10.",
            );
        }
    }

    /** كل مسار كتابة (عدا الهوية العامة) خلف مصادقة. */
    #[Test]
    public function كل_مسارات_الكتابة_محمية(): void
    {
        $publicWrites = [
            'api/v1/auth/register',
            'api/v1/auth/login',
            'api/v1/auth/password/forgot',
            'api/v1/auth/password/reset',
        ];

        foreach ($this->apiRoutes() as $route) {
            if (! in_array('POST', $route->methods(), true) || in_array($route->uri(), $publicWrites, true)) {
                continue;
            }

            $this->assertContains(
                'auth:sanctum',
                $route->gatherMiddleware(),
                "المسار {$route->uri()} مكشوف بلا مصادقة.",
            );
        }
    }

    /** العقد يعلن أسماء إجراءات الواجهات الثابتة، فلا يخترع أي تطبيق اسمًا آخر. */
    #[Test]
    public function العقد_يعلن_إجراءات_آلة_الحالات(): void
    {
        $spec = json_decode((string) file_get_contents(base_path('docs/api/openapi.json')), true, flags: JSON_THROW_ON_ERROR);

        $declared = $spec['components']['schemas']['OrderAction']['enum'];
        $actual = OrderPresentation::actionNames();

        $this->assertSame($actual, $declared);
    }

    #[Test]
    public function العقد_يعلن_حالة_التوقف_في_شريط_المراحل(): void
    {
        $spec = json_decode((string) file_get_contents(base_path('docs/api/openapi.json')), true, flags: JSON_THROW_ON_ERROR);

        $this->assertSame(
            ['done', 'active', 'pending', 'on_hold'],
            $spec['components']['schemas']['StepperState']['enum'],
        );
    }

    /** جدول الأخطاء في 31 معلن بالكامل. */
    #[Test]
    public function العقد_يعلن_كل_رموز_الأخطاء(): void
    {
        $spec = json_decode((string) file_get_contents(base_path('docs/api/openapi.json')), true, flags: JSON_THROW_ON_ERROR);

        $this->assertEqualsCanonicalizing([
            'VALIDATION_FAILED', 'UNAUTHENTICATED', 'EMAIL_NOT_VERIFIED', 'ACCOUNT_BLOCKED',
            'NOT_FOUND', 'INVALID_TRANSITION', 'CONFLICT', 'BUSINESS_RULE_VIOLATION',
            'FEATURE_DISABLED', 'RATE_LIMITED',
        ], $spec['components']['schemas']['Error']['properties']['error']['properties']['code']['enum']);
    }

    /** @return list<RoutingRoute> */
    private function apiRoutes(): array
    {
        return array_values(array_filter(
            Route::getRoutes()->getRoutes(),
            fn (RoutingRoute $route) => str_starts_with($route->uri(), 'api/v1'),
        ));
    }
}
