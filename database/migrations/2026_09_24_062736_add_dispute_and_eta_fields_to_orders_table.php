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
        Schema::table('orders', function (Blueprint $table) {
            $table->string('disputed_from_status', 32)->nullable()->after('status');
            $table->unsignedSmallInteger('eta_minutes')->nullable()->after('arrival_distance_m');
            $table->boolean('eta_approximate')->default(false)->after('eta_minutes');
            $table->timestamp('eta_calculated_at')->nullable()->after('eta_approximate');
            $table->decimal('eta_origin_lat', 10, 7)->nullable()->after('eta_calculated_at');
            $table->decimal('eta_origin_lng', 10, 7)->nullable()->after('eta_origin_lat');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropColumn([
                'disputed_from_status',
                'eta_minutes',
                'eta_approximate',
                'eta_calculated_at',
                'eta_origin_lat',
                'eta_origin_lng',
            ]);
        });
    }
};
