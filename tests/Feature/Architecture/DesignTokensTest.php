<?php

declare(strict_types=1);

namespace Tests\Feature\Architecture;

use Illuminate\Support\Facades\Artisan;
use PHPUnit\Framework\Attributes\Test;
use Symfony\Component\Process\Process;
use Tests\TestCase;

final class DesignTokensTest extends TestCase
{
    #[Test]
    public function الملفات_المولدة_للمنصتين_مطابقة_للمصدر_المشترك(): void
    {
        $exitCode = Artisan::call('design:tokens', ['--check' => true]);

        $this->assertSame(0, $exitCode, Artisan::output());
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
        $fixture = json_decode(
            (string) file_get_contents(base_path('design/fixtures/gallery/components.json')),
            true,
            flags: JSON_THROW_ON_ERROR,
        );

        foreach ($fixture['snapshot_cases'] as $case) {
            $matches = glob(base_path("androidapp/core/design/src/test/snapshots/images/*_gallery-{$case['id']}.png"));
            $this->assertCount(1, $matches, "Android snapshot {$case['id']} is missing or duplicated.");
            $this->assertFileEquals(
                $matches[0],
                base_path("design/reference/gallery/gallery-{$case['id']}.png"),
                "Approved Android reference {$case['id']} has drifted.",
            );
        }
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

        $this->assertSame('Cairo', $tokens['font']['family']);
        $this->assertSame(1.3, $tokens['a11y']['maxFontScale']);
        $this->assertSame(250, $tokens['motion']['screenTransitionMs']);
        $this->assertSame(0.4, $tokens['opacity']['disabled']);
        $this->assertSame('flat-two-tone (C01)', $tokens['categoryIconStyle']['default']);
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
        $androidComponents = (string) file_get_contents(base_path('androidapp/core/design/src/main/kotlin/noox/bzr/design/BzrComponents.kt'));
        $iosComponents = (string) file_get_contents(base_path('iosapp/Packages/DesignSystem/Sources/DesignSystem/BzrComponents.swift'));

        foreach ($fixture['sections'] as $section) {
            foreach ($section['components'] as $component) {
                $escapedComponent = preg_quote($component, '/');

                $this->assertMatchesRegularExpression("/fun\\s+{$escapedComponent}\\b/", $androidComponents, "Android is missing {$component}.");
                $this->assertMatchesRegularExpression("/struct\\s+{$escapedComponent}\\b/", $iosComponents, "iOS is missing {$component}.");
            }
        }

        $coveredPages = array_values(array_unique(array_column($fixture['snapshot_cases'], 'page')));
        sort($coveredPages);

        $this->assertSame(range(0, count($fixture['snapshot_pages']) - 1), $coveredPages);
    }
}
