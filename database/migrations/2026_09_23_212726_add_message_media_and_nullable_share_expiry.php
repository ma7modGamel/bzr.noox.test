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
        Schema::create('message_media', function (Blueprint $table): void {
            $table->foreignId('message_id')->constrained()->cascadeOnDelete();
            $table->foreignId('order_media_id')->constrained('order_media')->cascadeOnDelete();
            $table->primary(['message_id', 'order_media_id']);
        });

        Schema::table('share_links', function (Blueprint $table): void {
            $table->timestamp('expires_at')->nullable()->change();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('message_media');

        Schema::table('share_links', function (Blueprint $table): void {
            $table->timestamp('expires_at')->nullable(false)->change();
        });
    }
};
