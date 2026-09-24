<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

/**
 * BR-001 — البريد يجب توثيقه قبل نشر أي طلب أو تقديم طلب انضمام (DEC-025).
 * العقد في 31 يخصص له رمزًا وحالة مستقلين ليفتح التطبيق شاشة التفعيل مباشرة (42 §الشبكة).
 */
final class EmailNotVerifiedException extends DomainException
{
    public static function make(): self
    {
        return new self('يجب توثيق البريد الإلكتروني أولًا.', ['rule' => 'BR-001']);
    }

    public function errorCode(): string
    {
        return 'EMAIL_NOT_VERIFIED';
    }

    public function httpStatus(): int
    {
        return 403;
    }
}
