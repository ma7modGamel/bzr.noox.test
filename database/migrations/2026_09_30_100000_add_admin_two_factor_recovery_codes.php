<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/** ASM-14، DEC-060: رموز استرداد المصادقة الثنائية للإدارة (مشفرة). */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('admins', function (Blueprint $table) {
            $table->text('two_factor_recovery_codes')->nullable()->after('two_factor_secret');
        });
    }

    public function down(): void
    {
        Schema::table('admins', function (Blueprint $table) {
            $table->dropColumn('two_factor_recovery_codes');
        });
    }
};
