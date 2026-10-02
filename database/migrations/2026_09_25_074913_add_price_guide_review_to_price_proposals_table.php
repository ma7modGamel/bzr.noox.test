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
        Schema::table('price_proposals', function (Blueprint $table) {
            $table->decimal('price_guide_min', 10, 2)->nullable()->after('photo_path');
            $table->decimal('price_guide_max', 10, 2)->nullable()->after('price_guide_min');
            $table->boolean('outside_price_guide')->default(false)->after('price_guide_max');
            $table->string('price_review_reason', 32)->nullable()->after('outside_price_guide');
            $table->string('outside_price_guide_reason', 300)->nullable()->after('price_review_reason');

            $table->index(['outside_price_guide', 'price_review_reason']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('price_proposals', function (Blueprint $table) {
            $table->dropIndex(['outside_price_guide', 'price_review_reason']);
            $table->dropColumn([
                'price_guide_min',
                'price_guide_max',
                'outside_price_guide',
                'price_review_reason',
                'outside_price_guide_reason',
            ]);
        });
    }
};
