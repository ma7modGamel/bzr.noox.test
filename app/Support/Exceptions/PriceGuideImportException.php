<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use RuntimeException;

/** فشل تحقق ملف دليل الأسعار؛ لا تُحفظ أي صفوف عند رمي هذا الاستثناء. */
final class PriceGuideImportException extends RuntimeException
{
    /** @param list<string> $errors */
    public function __construct(public readonly array $errors)
    {
        parent::__construct(implode(PHP_EOL, $errors));
    }
}
