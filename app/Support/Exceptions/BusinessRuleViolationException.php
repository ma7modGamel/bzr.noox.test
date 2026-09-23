<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

/**
 * مخالفة قاعدة عمل معرّفة في 04-BUSINESS-RULES. الرمز يحمل معرّف القاعدة (BR-0xx).
 */
final class BusinessRuleViolationException extends DomainException
{
    public static function rule(string $rule, string $message): self
    {
        return new self($message, ['rule' => $rule]);
    }

    public function errorCode(): string
    {
        return 'BUSINESS_RULE_VIOLATION';
    }

    public function httpStatus(): int
    {
        return 422;
    }
}
