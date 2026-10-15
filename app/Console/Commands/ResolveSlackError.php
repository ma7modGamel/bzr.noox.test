<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Support\SlackErrorReporter;
use Illuminate\Console\Command;

final class ResolveSlackError extends Command
{
    protected $signature = 'errors:resolve {fingerprint : Issue ID from the Slack alert}';

    protected $description = 'Mark a server error fixed so its next occurrence sends a regressed alert';

    public function handle(SlackErrorReporter $reporter): int
    {
        if (! $reporter->resolve((string) $this->argument('fingerprint'))) {
            $this->error('Unknown or invalid issue ID.');

            return self::FAILURE;
        }
        $this->info('Marked resolved. Its next occurrence will send a regressed alert.');

        return self::SUCCESS;
    }
}
