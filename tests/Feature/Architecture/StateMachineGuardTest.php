<?php

declare(strict_types=1);

namespace Tests\Feature\Architecture;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\StateMachine\Transition;
use App\Modules\Orders\StateMachine\TransitionTable;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * حراس معماريون — 30 §آلة حالات الطلب، و37 §قائمة الفحص.
 * يكسران البناء إذا انحرف الكود عن الوثائق.
 */
final class StateMachineGuardTest extends TestCase
{
    /** لا يوجد في أي مكان `$order->status = ...` خارج الآلة (قاعدة مراجعة كود + اختبار — 30). */
    #[Test]
    public function حالة_الطلب_لا_تُكتب_خارج_آلة_الحالات(): void
    {
        $offenders = [];

        foreach ($this->phpFiles(app_path()) as $file) {
            if (str_contains($file, 'StateMachine')) {
                continue;
            }

            if ($this->assignsStatus(file_get_contents($file) ?: '')) {
                $offenders[] = str_replace(base_path().'/', '', $file);
            }
        }

        $this->assertSame([], $offenders, 'تغيير الحالة يجب أن يمر بـ OrderStateMachine فقط.');
    }

    /** 37: كل حالة غير نهائية لها مخرج واحد على الأقل — لا طريق مسدود. */
    #[Test]
    public function كل_حالة_غير_نهائية_لها_مخرج(): void
    {
        foreach (OrderStatus::cases() as $status) {
            if ($status->isFinal()) {
                continue;
            }

            $this->assertNotEmpty(
                TransitionTable::fromStatus($status),
                "الحالة {$status->value} بلا مخرج في جدول الانتقالات.",
            );
        }
    }

    /** كل حالة نهائية لها مدخل واحد على الأقل. */
    #[Test]
    public function كل_حالة_نهائية_لها_مدخل(): void
    {
        $targets = array_map(fn (Transition $t) => $t->to, TransitionTable::all());

        foreach (OrderStatus::cases() as $status) {
            if (! $status->isFinal()) {
                continue;
            }

            $this->assertContains($status, $targets, "الحالة النهائية {$status->value} بلا مدخل.");
        }
    }

    /** رموز الانتقالات فريدة وتطابق ترقيم 10-ORDER-LIFECYCLE. */
    #[Test]
    public function رموز_الانتقالات_فريدة_وبالصيغة_المعتمدة(): void
    {
        $codes = array_map(fn (Transition $t) => $t->code, TransitionTable::all());
        $actions = array_map(fn (Transition $t) => $t->action, TransitionTable::all());

        $this->assertSame($codes, array_unique($codes), 'رمز انتقال مكرر.');
        $this->assertSame($actions, array_unique($actions), 'اسم إجراء مكرر.');

        foreach ($codes as $code) {
            $this->assertMatchesRegularExpression('/^T-\d{2}$/', $code);
        }
    }

    /** انتقالات وضع الموظفين محروسة بمفتاح CFG-090 (39). */
    #[Test]
    public function انتقالات_الوضعين_محروسة_بالمفتاح(): void
    {
        $this->assertFalse(TransitionTable::find('assignProvider')?->requiresOffers);
        $this->assertFalse(TransitionTable::find('reassignProvider')?->requiresOffers);
        $this->assertTrue(TransitionTable::find('acceptOffer')?->requiresOffers);

        // الانتقالات المشتركة بلا شرط وضع
        $this->assertNull(TransitionTable::find('markArrived')?->requiresOffers);
        $this->assertNull(TransitionTable::find('closeFreeInspection')?->requiresOffers);
    }

    /**
     * يفحص التوكِنز لا النص الخام، فلا تُحسب التعليقات والنصوص التي تذكر القاعدة نفسها.
     */
    private function assignsStatus(string $source): bool
    {
        $tokens = array_values(array_filter(
            token_get_all($source),
            fn ($token) => ! is_array($token)
                || ! in_array($token[0], [T_COMMENT, T_DOC_COMMENT, T_WHITESPACE], true),
        ));

        foreach ($tokens as $i => $token) {
            $isStatusProperty = is_array($token)
                && $token[0] === T_STRING
                && $token[1] === 'status'
                && isset($tokens[$i - 1])
                && is_array($tokens[$i - 1])
                && $tokens[$i - 1][0] === T_OBJECT_OPERATOR;

            if ($isStatusProperty && ($tokens[$i + 1] ?? null) === '=') {
                return true;
            }
        }

        return false;
    }

    /** @return list<string> */
    private function phpFiles(string $directory): array
    {
        $iterator = new \RecursiveIteratorIterator(new \RecursiveDirectoryIterator($directory));
        $files = [];

        foreach ($iterator as $file) {
            if ($file->isFile() && $file->getExtension() === 'php') {
                $files[] = $file->getPathname();
            }
        }

        return $files;
    }
}
