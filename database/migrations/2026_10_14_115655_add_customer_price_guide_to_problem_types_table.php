<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('problem_types', function (Blueprint $table): void {
            $table->decimal('customer_price_min', 10, 2)->nullable();
            $table->decimal('customer_price_max', 10, 2)->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('problem_types', function (Blueprint $table): void {
            $table->dropColumn(['customer_price_min', 'customer_price_max']);
        });
    }
};
