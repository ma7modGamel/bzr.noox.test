<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

/**
 * الأساس لكل أخطاء العمل. كل خطأ يحمل رمزًا من جدول الأخطاء في 31-API-CONTRACT
 * وينعكس بنفس الشكل في الـ API وفي لوحة الإدارة.
 */
abstract class DomainException extends RuntimeException
{
    /** @param array<string, mixed> $context */
    public function __construct(
        string $message,
        public readonly array $context = [],
    ) {
        parent::__construct($message);
    }

    /** الرمز الثابت في 31 (مثل INVALID_TRANSITION). */
    abstract public function errorCode(): string;

    abstract public function httpStatus(): int;

    public function render(): JsonResponse
    {
        return new JsonResponse([
            'error' => [
                'code' => $this->errorCode(),
                'message' => $this->getMessage(),
                ...$this->context,
            ],
        ], $this->httpStatus());
    }
}
