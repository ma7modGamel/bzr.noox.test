<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        foreach (['categories', 'problem_types', 'cities', 'areas'] as $table) {
            Schema::table($table, function (Blueprint $blueprint): void {
                $blueprint->string('name_en', 150)->nullable();
            });
        }
        Schema::table('support_reasons', function (Blueprint $table): void {
            $table->string('label_en', 120)->nullable();
        });
        Schema::table('legal_page_versions', function (Blueprint $table): void {
            $table->string('locale', 2)->default('ar')->index();
        });
        Schema::table('users', function (Blueprint $table): void {
            $table->string('locale', 2)->default('ar');
        });
        Schema::table('device_tokens', function (Blueprint $table): void {
            $table->string('locale', 2)->default('ar');
        });
    }

    public function down(): void
    {
        foreach (['categories', 'problem_types', 'cities', 'areas'] as $table) {
            Schema::table($table, function (Blueprint $blueprint): void {
                $blueprint->dropColumn('name_en');
            });
        }
        Schema::table('support_reasons', function (Blueprint $table): void {
            $table->dropColumn('label_en');
        });
        foreach (['legal_page_versions', 'users', 'device_tokens'] as $table) {
            Schema::table($table, function (Blueprint $blueprint): void {
                $blueprint->dropColumn('locale');
            });
        }
    }
};
