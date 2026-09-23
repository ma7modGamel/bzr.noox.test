<?php

declare(strict_types=1);

namespace App\Modules\Orders\StateMachine;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;

/**
 * صف واحد من جدول الانتقالات في 10-ORDER-LIFECYCLE.
 * `requires` يصف شرط الوضع: null = الوضعان، true = وضع السوق، false = وضع الموظفين.
 */
final readonly class Transition
{
    /**
     * @param list<OrderStatus> $from
     * @param list<ActorType> $actors
     */
    public function __construct(
        public string $code,
        public string $action,
        public array $from,
        public OrderStatus $to,
        public array $actors,
        public OrderEventCode $event,
        public ?bool $requiresOffers = null,
        public string $note = '',
    ) {}

    public function allowsFrom(OrderStatus $status): bool
    {
        return in_array($status, $this->from, true);
    }

    public function allowsActor(ActorType $actor): bool
    {
        return in_array($actor, $this->actors, true);
    }
}
