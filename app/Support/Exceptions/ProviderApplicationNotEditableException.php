<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

final class ProviderApplicationNotEditableException extends DomainException
{
    public static function make(): self
    {
        return new self('طلب الانضمام غير قابل للتعديل في حالته الحالية.', ['rule' => 'BR-130']);
    }

    public function errorCode(): string
    {
        return 'APPLICATION_NOT_EDITABLE';
    }

    public function httpStatus(): int
    {
        return 409;
    }
}
