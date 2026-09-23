<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/** التقييم المتبادل — 14، BR-090..BR-093. تقييم واحد لكل طلب لكل اتجاه. */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('reviews', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->unique()->constrained();
            $table->foreignId('provider_profile_id')->constrained();
            $table->foreignId('customer_id')->constrained('users');
            $table->unsignedTinyInteger('quality');
            $table->unsignedTinyInteger('punctuality');
            $table->unsignedTinyInteger('conduct');
            $table->string('comment', 500)->nullable();
            $table->timestamp('hidden_at')->nullable();
            $table->foreignId('hidden_by')->nullable()->constrained('admins');
            $table->string('hidden_reason')->nullable();
            $table->timestamps();

            $table->index(['provider_profile_id', 'hidden_at']);
        });

        Schema::create('customer_ratings', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->unique()->constrained();
            $table->foreignId('customer_id')->constrained('users');
            $table->foreignId('provider_profile_id')->constrained();
            $table->unsignedTinyInteger('stars');
            $table->timestamp('hidden_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('customer_ratings');
        Schema::dropIfExists('reviews');
    }
};
