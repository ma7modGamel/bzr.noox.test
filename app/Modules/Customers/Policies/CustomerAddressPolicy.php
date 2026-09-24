<?php

declare(strict_types=1);

namespace App\Modules\Customers\Policies;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use Illuminate\Contracts\Auth\Authenticatable;

/** العنوان يخص صاحبه وحده (23 §قاعدة الملكية). */
final class CustomerAddressPolicy
{
    /** الإدارة خارج قاعدة الملكية (23، DEC-021). */
    public function before(Authenticatable $user, string $ability): ?bool
    {
        return $user instanceof Admin ? true : null;
    }

    public function view(User $user, CustomerAddress $address): bool
    {
        return $address->user_id === $user->getKey();
    }

    public function update(User $user, CustomerAddress $address): bool
    {
        return $this->view($user, $address);
    }

    public function delete(User $user, CustomerAddress $address): bool
    {
        return $this->view($user, $address);
    }
}
