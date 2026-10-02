<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::table('problem_types', function (Blueprint $table) {
            $table->decimal('employee_price_min', 10, 2)->nullable()->after('is_other');
            $table->decimal('employee_price_max', 10, 2)->nullable()->after('employee_price_min');
            $table->text('employee_price_notes')->nullable()->after('employee_price_max');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('problem_types', function (Blueprint $table) {
            $table->dropColumn([
                'employee_price_min',
                'employee_price_max',
                'employee_price_notes',
            ]);
        });
    }
};
