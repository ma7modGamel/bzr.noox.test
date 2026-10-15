<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('orders', function (Blueprint $table): void {
            $table->text('arrival_code')->nullable();
            $table->timestamp('arrival_code_expires_at')->nullable();
            $table->unsignedTinyInteger('arrival_code_attempts')->default(0);
            $table->timestamp('arrival_code_locked_until')->nullable();
            $table->unsignedBigInteger('arrival_code_provider_id')->nullable();
            $table->timestamp('arrival_code_trip_started_at')->nullable();
            $table->timestamp('arrival_verified_at')->nullable();
            $table->json('arrival_exception_report')->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table): void {
            $table->dropColumn(['arrival_code', 'arrival_code_expires_at', 'arrival_code_attempts', 'arrival_code_locked_until', 'arrival_code_provider_id', 'arrival_code_trip_started_at', 'arrival_verified_at', 'arrival_exception_report']);
        });
    }
};
