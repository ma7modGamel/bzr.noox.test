<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('dispute_attachments', function (Blueprint $table) {
            $table->foreignId('order_media_id')
                ->nullable()
                ->after('dispute_id')
                ->constrained('order_media')
                ->nullOnDelete();
        });
    }

    public function down(): void
    {
        Schema::table('dispute_attachments', function (Blueprint $table) {
            $table->dropConstrainedForeignId('order_media_id');
        });
    }
};
