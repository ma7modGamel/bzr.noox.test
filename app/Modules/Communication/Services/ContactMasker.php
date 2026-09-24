<?php

declare(strict_types=1);

namespace App\Modules\Communication\Services;

/**
 * حجب وسائل التواصل — BR-100، BR-032، DEC-013.
 *
 * يستبدل المطابقات بـ ••• ويُبلغ إن حدث حجب، ليُحفظ النص الأصلي مشفرًا للإدارة عند النزاع.
 * الأنماط (18): تسلسل 8 أرقام فأكثر (عربية أو هندية، مع مسافات/شرطات)، `01` + 9 أرقام،
 * روابط ونطاقات وبريد، وكلمات مثل "واتس" متبوعة بأرقام.
 *
 * ليس حاجزًا مطلقًا (37 §المخاطر): الأرقام المكتوبة بالحروف تمر، وهذا مقبول للتجربة.
 */
final class ContactMasker
{
    private const MASK = '•••';

    private const PATTERNS = [
        // بريد إلكتروني
        '/[\p{L}\d._%+-]+@[\p{L}\d.-]+\.[a-z]{2,}/iu',
        // روابط ونطاقات
        '/\b(?:https?:\/\/|www\.)\S+/iu',
        '/\b[\p{L}\d-]+\.(?:com|net|org|eg|me|io|co)\b/iu',
        // "واتس" وما شابهها متبوعة بأرقام
        '/(?:واتس|واتساب|وتس|تليجرام|تلجرام)[^\d\p{Arabic}]{0,10}[\d\x{0660}-\x{0669}\s-]{6,}/u',
        // موبايل مصري: 01 + 9 أرقام (بأي فواصل)
        '/\b0\s*1[\d\x{0660}-\x{0669}\s-]{9,}/u',
        // أي تسلسل 8 أرقام فأكثر
        '/[\d\x{0660}-\x{0669}](?:[\s-]*[\d\x{0660}-\x{0669}]){7,}/u',
    ];

    /** @return array{body: string, masked: bool} */
    public function mask(string $text): array
    {
        $masked = preg_replace(self::PATTERNS, self::MASK, $text) ?? $text;

        return ['body' => $masked, 'masked' => $masked !== $text];
    }

    public function containsContactDetails(string $text): bool
    {
        return $this->mask($text)['masked'];
    }
}
