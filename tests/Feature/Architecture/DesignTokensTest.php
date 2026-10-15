<?php

declare(strict_types=1);

namespace Tests\Feature\Architecture;

use App\Providers\Filament\AdminPanelProvider;
use Filament\Panel;
use Illuminate\Support\Facades\Artisan;
use PHPUnit\Framework\Attributes\Test;
use Symfony\Component\Process\Process;
use Tests\TestCase;

final class DesignTokensTest extends TestCase
{
    #[Test]
    public function لوحة_الإدارة_تقرأ_ألوان_الهوية_والحالات_من_الثيم(): void
    {
        config(['design.colors.primary' => '#6B21A8', 'design.colors.success' => '#14532D']);

        $colors = (new AdminPanelProvider($this->app))->panel(Panel::make())->getColors();

        $this->assertSame('#6B21A8', $colors['primary']);
        $this->assertSame('#14532D', $colors['success']);
    }

    #[Test]
    public function تغيير_لوحة_الألوان_يصل_إلى_الهوية_والحالات_والشعار(): void
    {
        require_once base_path('tools/design-theme.php');
        $tokens = json_decode((string) file_get_contents(base_path('design/tokens.json')), true, flags: JSON_THROW_ON_ERROR);
        $tokens['theme']['primary'] = '#6B21A8';
        $tokens['theme']['success'] = '#14532D';
        $tokens['theme']['secondaryText'] = '#334155';

        $resolved = resolveThemeTokens($tokens);
        $symbol = resolveBrandSymbol((string) file_get_contents(base_path('design/brand/symbol.svg')), $resolved['color']);

        $this->assertSame('#6B21A8', $resolved['color']['primary600']);
        $this->assertSame('#6B21A8', $resolved['color']['brandPrimary']);
        $this->assertSame('#6B21A8', $resolved['gradient']['brand']['from']);
        $this->assertSame('#6B21A8', $resolved['gradient']['splash']['from']);
        $this->assertSame('#6B21A8', $resolved['shadow']['brand']['color']);
        $this->assertSame('#14532D', $resolved['color']['verifiedText']);
        $this->assertSame('#334155', $resolved['color']['secondaryButtonText']);
        $this->assertStringContainsString('stop-color="#6B21A8"', $symbol);
        $this->assertStringNotContainsString('stop-color="#00ACB6"', $symbol);
    }

    #[Test]
    public function كل_الألوان_الخام_موجودة_داخل_لوحة_الثيم_فقط(): void
    {
        $tokens = json_decode((string) file_get_contents(base_path('design/tokens.json')), true, flags: JSON_THROW_ON_ERROR);
        unset($tokens['theme']);

        array_walk_recursive($tokens, function (mixed $value): void {
            if (is_string($value)) {
                $this->assertDoesNotMatchRegularExpression('/^#[0-9A-Fa-f]{6}$/', $value);
            }
        });
    }

    #[Test]
    public function مراجع_الألوان_الدائرية_ترفض_برسالة_واضحة(): void
    {
        require_once base_path('tools/design-theme.php');
        $this->expectException(\RuntimeException::class);
        $this->expectExceptionMessage('circular colour reference');

        resolveThemeTokens(['theme' => ['primary' => '{color.primary}'], 'color' => ['primary' => '{theme.primary}']]);
    }

    #[Test]
    public function نسبة_مزج_غير_رقمية_ترفض_قبل_توليد_الأصول(): void
    {
        require_once base_path('tools/design-theme.php');
        $this->expectException(\RuntimeException::class);
        $this->expectExceptionMessage('mix amount must be numeric');

        resolveThemeTokens(['theme' => ['primary' => '#0054D5'], 'color' => ['tint' => ['mix' => '{theme.primary}', 'with' => '#FFFFFF', 'amount' => 'invalid']]]);
    }

    #[Test]
    public function الملفات_المولدة_للمنصتين_مطابقة_للمصدر_المشترك(): void
    {
        $exitCode = Artisan::call('design:tokens', ['--check' => true]);

        $this->assertSame(0, $exitCode, Artisan::output());
    }

    #[Test]
    public function بصمة_الأيقونة_تربط_الرسم_بنسخة_المتجر_المولدة(): void
    {
        $cache = json_decode((string) file_get_contents(base_path('design/generated/brand/app-icon.json')), true, flags: JSON_THROW_ON_ERROR);

        $this->assertSame(hash_file('sha256', base_path('design/generated/brand/app-icon.svg')), $cache['svg_sha256']);
        $this->assertSame(hash_file('sha256', base_path('design/brand/app-icon-1024.png')), $cache['png_sha256']);
    }

    #[Test]
    public function لا_قيم_بصرية_ولا_نصوص_مكتوبة_مباشرة_في_كود_المنصتين(): void
    {
        $process = new Process([PHP_BINARY, base_path('tools/lint-design')], base_path());
        $process->run();

        $this->assertSame(0, $process->getExitCode(), $process->getErrorOutput());
    }

    #[Test]
    public function ملف_التطابق_يغطي_المكونات_والشاشات_والنصوص_والبيانات_المشتركة(): void
    {
        $process = new Process([PHP_BINARY, base_path('tools/check-parity')], base_path());
        $process->run();

        $this->assertSame(0, $process->getExitCode(), $process->getErrorOutput());
    }

    #[Test]
    public function وثائق_النشر_تعطل_سجل_الوصول_لروابط_المشاركة(): void
    {
        $process = new Process([PHP_BINARY, base_path('tools/check-deployment-docs')], base_path());
        $process->run();

        $this->assertSame(0, $process->getExitCode(), $process->getErrorOutput());
    }

    #[Test]
    public function لقطات_معرض_اندرويد_المعتمدة_تطابق_لقطات_الاختبار(): void
    {
        $process = new Process(
            [PHP_BINARY, base_path('tools/compare-android-xml'), 'gallery', '--strict'],
            base_path(),
        );
        $process->run();

        $this->assertSame(0, $process->getExitCode(), $process->getErrorOutput().$process->getOutput());
    }

    #[Test]
    public function الخط_مضمن_بملف_ثابت_لكل_وزن(): void
    {
        $tokens = json_decode((string) file_get_contents(base_path('design/tokens.json')), true, flags: JSON_THROW_ON_ERROR);

        $this->assertSame(['regular', 'medium', 'semibold', 'bold'], array_keys($tokens['font']['files']));
        foreach ($tokens['font']['files'] as $file) {
            $this->assertFileExists(base_path("design/fonts/{$file}.ttf"));
        }
        foreach ($tokens['type'] as $name => $style) {
            $this->assertArrayHasKey($style['weight'], $tokens['font']['files'], "{$name} uses a weight with no font file.");
        }
    }

    #[Test]
    public function المصدر_المشترك_يحتوي_قيود_التطابق_الملزمة(): void
    {
        $tokens = json_decode(
            (string) file_get_contents(base_path('design/tokens.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );

        $this->assertSame('IBM Plex Sans Arabic', $tokens['font']['family']); // DEC-070
        $this->assertSame(1.3, $tokens['a11y']['maxFontScale']);
        $this->assertSame(250, $tokens['motion']['screenTransitionMs']);
        $this->assertSame(0.4, $tokens['opacity']['disabled']);
        $this->assertSame('Material Symbols, single tone (Figma Primo App)', $tokens['categoryIconStyle']['default']); // DEC-070
    }

    #[Test]
    public function كل_إجراءات_العقد_لها_نص_مشترك(): void
    {
        $strings = json_decode(
            (string) file_get_contents(base_path('design/strings.ar.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );

        $actions = [
            'accept_offer', 'edit_request', 'republish', 'cancel', 'change_payment_method',
            'pay_electronic', 'approve_proposal', 'reject_proposal', 'confirm_completion',
            'open_dispute', 'rate', 'share_visit', 'call', 'chat', 'report_provider',
            'submit_offer', 'withdraw_offer', 'start_trip', 'back_out', 'mark_arrived',
            'start_work', 'submit_execution_quote', 'complete_inspection_only',
            'submit_proposal', 'withdraw_proposal', 'complete_work', 'report_unable',
            'report_no_show', 'confirm_cash', 'rate_customer', 'navigate',
        ];

        foreach ($actions as $action) {
            $this->assertArrayHasKey("action.{$action}", $strings);
        }
    }

    #[Test]
    public function كل_مكونات_المعرض_موجودة_في_المنصتين_وكل_صفحة_لها_لقطة(): void
    {
        $fixture = json_decode(
            (string) file_get_contents(base_path('design/fixtures/gallery/components.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );
        $parity = json_decode(
            (string) file_get_contents(base_path('design/parity.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );
        $components = collect($parity['components'])->keyBy('id');

        foreach ($fixture['sections'] as $section) {
            foreach ($section['components'] as $component) {
                $entry = $components->get($component);

                $this->assertIsArray($entry, "Parity manifest is missing {$component}.");
                foreach (['android', 'ios'] as $platform) {
                    $source = (string) file_get_contents(base_path($entry[$platform]['file']));
                    $this->assertStringContainsString(
                        $entry[$platform]['symbol'],
                        $source,
                        "{$platform} is missing {$component}.",
                    );
                }
            }
        }

        $coveredPages = array_values(array_unique(array_column($fixture['snapshot_cases'], 'page')));
        sort($coveredPages);

        $this->assertSame(range(0, count($fixture['snapshot_pages']) - 1), $coveredPages);
    }
}
