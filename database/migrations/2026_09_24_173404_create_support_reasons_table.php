<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('support_reasons', function (Blueprint $table) {
            $table->id();
            $table->string('type', 32);
            $table->string('code', 48);
            $table->string('label', 120);
            $table->boolean('is_active')->default(true);
            $table->unsignedSmallInteger('sort')->default(0);
            $table->timestamps();

            $table->unique(['type', 'code']);
            $table->index(['type', 'is_active', 'sort']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('support_reasons');
    }
};
