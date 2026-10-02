<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Contracts;

use App\Modules\Notifications\Push\PushMessage;
use App\Modules\Notifications\Push\PushResult;

interface PushSender
{
    public function send(string $token, PushMessage $message): PushResult;
}
