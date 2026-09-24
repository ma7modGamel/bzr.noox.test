<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Services\OrderPresentation;
use Illuminate\Console\Command;
use Illuminate\Routing\Route as RoutingRoute;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\Str;

/**
 * يولّد `docs/api/openapi.json` من **جدول المسارات الحقيقي** لا من ملف يدوي.
 *
 * السبب (41 §البوابة 1.9): التطبيقان يُبنيان على هذا العقد، فانحرافه عن الكود
 * يعني تطبيقين مكسورين لا يكتشفهما أحد قبل المتجر. التوليد يجعل الانحراف مستحيلًا،
 * و`--check` يكسر البناء إذا تغيّر مسار بلا إعادة توليد.
 */
final class GenerateOpenApiSpec extends Command
{
    protected $signature = 'api:spec {--check : يتحقق أن الملف مطابق للمسارات بلا كتابة}';

    protected $description = 'توليد عقد OpenAPI من مسارات API v1 (31-API-CONTRACT)';

    public function handle(): int
    {
        $spec = json_encode($this->buildSpec(), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)."\n";
        $path = base_path('docs/api/openapi.json');

        if ($this->option('check')) {
            if (! is_file($path) || file_get_contents($path) !== $spec) {
                $this->error('عقد OpenAPI غير محدَّث. شغّل: php artisan api:spec');

                return self::FAILURE;
            }

            $this->info('العقد مطابق للمسارات.');

            return self::SUCCESS;
        }

        if (! is_dir(dirname($path))) {
            mkdir(dirname($path), 0o755, true);
        }

        file_put_contents($path, $spec);
        $this->info('تم توليد '.str_replace(base_path().'/', '', $path));

        return self::SUCCESS;
    }

    /** @return array<string, mixed> */
    private function buildSpec(): array
    {
        $paths = [];

        foreach ($this->apiRoutes() as $route) {
            $uri = '/'.ltrim(preg_replace('/\{(\w+)\}/', '{$1}', $route->uri()) ?? '', '/');
            $uri = Str::after($uri, '/api/v1');

            foreach ($route->methods() as $method) {
                if (in_array($method, ['HEAD', 'OPTIONS'], true)) {
                    continue;
                }

                $paths[$uri][strtolower($method)] = $this->operation($route, $uri);
            }
        }

        ksort($paths);

        return [
            'openapi' => '3.1.0',
            'info' => [
                'title' => config('app.name').' — API v1',
                'version' => '1.0.0',
                'description' => 'مولَّد من مسارات لارافيل. المرجع الملزم: docs/31-API-CONTRACT. '
                    .'لا يوجد أي مسار لتعديل حالة الطلب مباشرة؛ كل تغيير يمر بانتقال في docs/10.',
            ],
            'servers' => [['url' => '/api/v1']],
            'components' => [
                'securitySchemes' => [
                    'sanctum' => ['type' => 'http', 'scheme' => 'bearer'],
                ],
                'parameters' => [
                    'AppMode' => [
                        'name' => 'X-App-Mode', 'in' => 'header', 'required' => false,
                        'schema' => ['type' => 'string', 'enum' => ['CUSTOMER', 'PROVIDER']],
                        'description' => 'يحدد ما يُعرض فقط؛ الصلاحية من الخادم (23).',
                    ],
                    'IdempotencyKey' => [
                        'name' => 'Idempotency-Key', 'in' => 'header', 'required' => false,
                        'schema' => ['type' => 'string', 'format' => 'uuid'],
                        'description' => 'نفس المفتاح خلال 24 ساعة يعيد نفس النتيجة.',
                    ],
                ],
                'schemas' => [
                    'Error' => [
                        'type' => 'object',
                        'properties' => [
                            'error' => [
                                'type' => 'object',
                                'required' => ['code', 'message'],
                                'properties' => [
                                    'code' => ['type' => 'string', 'enum' => $this->errorCodes()],
                                    'message' => ['type' => 'string'],
                                    'rule' => ['type' => 'string', 'description' => 'معرّف القاعدة في docs/04 عند BUSINESS_RULE_VIOLATION'],
                                ],
                            ],
                        ],
                    ],
                    'OrderStatus' => [
                        'type' => 'string',
                        'enum' => array_map(fn (OrderStatus $s) => $s->value, OrderStatus::cases()),
                    ],
                    'OrderAction' => [
                        'type' => 'string',
                        'description' => 'أسماء available_actions الثابتة في docs/31. التطبيق يعرضها ولا يستنتجها.',
                        'enum' => OrderPresentation::actionNames(),
                    ],
                    'StepperState' => [
                        'type' => 'string',
                        'enum' => ['done', 'active', 'pending', 'on_hold'],
                    ],
                    'StatusStepper' => [
                        'type' => ['array', 'null'],
                        'description' => 'null في OPEN/CANCELLED/EXPIRED. في DISPUTED تستخدم المرحلة السابقة on_hold.',
                        'items' => [
                            'type' => 'object',
                            'required' => ['key', 'state'],
                            'properties' => [
                                'key' => ['type' => 'string'],
                                'state' => ['$ref' => '#/components/schemas/StepperState'],
                            ],
                        ],
                    ],
                ],
            ],
            'security' => [['sanctum' => []]],
            'paths' => $paths,
        ];
    }

    /** @return list<RoutingRoute> */
    private function apiRoutes(): array
    {
        return array_values(array_filter(
            Route::getRoutes()->getRoutes(),
            fn (RoutingRoute $route) => str_starts_with($route->uri(), 'api/v1'),
        ));
    }

    /** @return array<string, mixed> */
    private function operation(RoutingRoute $route, string $uri): array
    {
        $isPublic = ! in_array('auth:sanctum', $route->gatherMiddleware(), true);
        $isWrite = in_array('POST', $route->methods(), true);

        $parameters = [['$ref' => '#/components/parameters/AppMode']];

        if ($isWrite) {
            $parameters[] = ['$ref' => '#/components/parameters/IdempotencyKey'];
        }

        foreach ($route->parameterNames() as $name) {
            $parameters[] = [
                'name' => $name,
                'in' => 'path',
                'required' => true,
                'schema' => ['type' => 'integer'],
            ];
        }

        if ($uri === '/providers/{provider}') {
            $parameters[] = [
                'name' => 'order_id',
                'in' => 'query',
                'required' => true,
                'schema' => ['type' => 'integer'],
                'description' => 'سياق الطلب المملوك للعميل لحساب available_actions ومنع استعراض ملف غير مرتبط.',
            ];
        }

        return array_filter([
            'operationId' => $this->operationId($route),
            'tags' => [$this->tag($uri)],
            'summary' => $this->summary($route),
            'security' => $isPublic ? [] : [['sanctum' => []]],
            'parameters' => $parameters,
            'responses' => [
                '200' => ['description' => 'نجاح'],
                '400' => ['description' => 'خطأ', 'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Error']]]],
            ],
        ], fn ($v) => $v !== []);
    }

    private function operationId(RoutingRoute $route): string
    {
        $action = $route->getActionName();

        if (! str_contains($action, '@')) {
            return Str::camel(class_basename($action));
        }

        [$controller, $method] = explode('@', $action);

        return Str::camel(str_replace('Controller', '', class_basename($controller)).ucfirst($method));
    }

    private function tag(string $uri): string
    {
        return match (true) {
            str_starts_with($uri, '/provider') => 'الفني',
            str_starts_with($uri, '/orders') => 'الطلبات',
            str_starts_with($uri, '/auth'), $uri === '/me' => 'الهوية',
            str_starts_with($uri, '/addresses') => 'العناوين',
            default => 'المراجع',
        };
    }

    private function summary(RoutingRoute $route): string
    {
        return $this->operationId($route);
    }

    /** @return list<string> جدول الأخطاء في 31. */
    private function errorCodes(): array
    {
        return [
            'VALIDATION_FAILED', 'UNAUTHENTICATED', 'EMAIL_NOT_VERIFIED', 'ACCOUNT_BLOCKED',
            'NOT_FOUND', 'INVALID_TRANSITION', 'CONFLICT', 'BUSINESS_RULE_VIOLATION',
            'FEATURE_DISABLED', 'RATE_LIMITED',
        ];
    }
}
