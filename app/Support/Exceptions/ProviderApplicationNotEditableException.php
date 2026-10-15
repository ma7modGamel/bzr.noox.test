<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use App\Support\Localization;

final class ProviderApplicationNotEditableException extends DomainException
{
    public static function make(): self
    {
        return new self(Localization::text('copy.this_application_cannot_be_edited_in_its_current_state'), ['rule' => 'BR-130']);
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
