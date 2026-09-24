<?php

declare(strict_types=1);

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Symfony\Component\Process\Process;

final class GenerateDesignTokens extends Command
{
    protected $signature = 'design:tokens {--check : يتحقق أن كل الملفات المولَّدة مطابقة للمصدر بلا كتابة}';

    protected $description = 'تشغيل المصدر الواحد tools/gen-design لأندرويد وiOS والويب';

    public function handle(): int
    {
        $arguments = [base_path('tools/gen-design')];

        if ($this->option('check')) {
            $arguments[] = '--check';
        }

        $process = new Process($arguments, base_path());
        $process->run(function (string $type, string $buffer): void {
            $this->output->write($buffer);
        });

        return $process->isSuccessful() ? self::SUCCESS : self::FAILURE;
    }
}
