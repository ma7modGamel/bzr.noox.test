<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('provider_reports', function (Blueprint $table) {
            $table->foreignId('order_id')
                ->nullable()
                ->after('reporter_user_id')
                ->constrained()
                ->nullOnDelete();
        });
    }

    public function down(): void
    {
        Schema::table('provider_reports', function (Blueprint $table) {
            $table->dropConstrainedForeignId('order_id');
        });
    }
};
