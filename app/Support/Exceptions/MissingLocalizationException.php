<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use App\Support\Localization;

final class MissingLocalizationException extends DomainException
{
    public function __construct(string $field)
    {
        parent::__construct(Localization::text('messages.localization_unavailable'), ['field' => $field]);
    }

    public function errorCode(): string
    {
        return 'LOCALIZATION_UNAVAILABLE';
    }

    public function httpStatus(): int
    {
        return 409;
    }
}
