<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Support\SlackErrorReporter;
use Illuminate\Console\Command;

final class TestSlackErrorConnection extends Command
{
    protected $signature = 'errors:slack-test';

    protected $description = 'Send a labelled Slack connection test without triggering an application crash';

    public function handle(SlackErrorReporter $reporter): int
    {
        if (! $reporter->testConnection()) {
            $this->error('Slack test failed. Check private configuration and network access.');

            return self::FAILURE;
        }
        $this->info('Slack accepted the Laravel connection test.');

        return self::SUCCESS;
    }
}
