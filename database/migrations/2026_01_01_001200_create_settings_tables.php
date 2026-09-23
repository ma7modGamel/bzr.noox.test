<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الإعدادات — 04 §الإعدادات، ومنها مفتاحا وضع التشغيل CFG-090/CFG-091 (39).
 * القراءة دائمًا عبر SettingsRepository بتخزين مؤقت (30).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('settings', function (Blueprint $table) {
            $table->string('key', 100)->primary();
            $table->json('value')->nullable();
            $table->foreignId('updated_by')->nullable()->constrained('admins');
            $table->timestamp('updated_at')->nullable();
        });

        Schema::create('terms_versions', function (Blueprint $table) {
            $table->id();
            $table->unsignedInteger('version')->unique();
            $table->longText('body');
            $table->timestamp('effective_at');
            $table->timestamps();
        });

        // تسلسل أرقام الطلبات المعروضة (#1248) مستقل عن المفتاح الأساسي
        Schema::create('order_numbers', function (Blueprint $table) {
            $table->id();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('order_numbers');
        Schema::dropIfExists('terms_versions');
        Schema::dropIfExists('settings');
    }
};
