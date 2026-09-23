<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الكتالوج مستويان: فئة ← نوع مشكلة (BR-012، DEC-024).
 * لكل فئة نوع "مشكلة أخرى" (`is_other`) يجعل الوصف إلزاميًا (BR-013).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('categories', function (Blueprint $table) {
            $table->id();
            $table->string('name', 100);
            $table->string('icon_path')->nullable();
            $table->unsignedSmallInteger('sort')->default(0);
            $table->boolean('is_active')->default(true);
            $table->timestamps();
        });

        Schema::create('problem_types', function (Blueprint $table) {
            $table->id();
            $table->foreignId('category_id')->constrained()->cascadeOnDelete();
            $table->string('name', 150);
            $table->boolean('is_other')->default(false);
            $table->unsignedSmallInteger('sort')->default(0);
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->unique(['category_id', 'name']);
        });

        Schema::create('city_categories', function (Blueprint $table) {
            $table->foreignId('city_id')->constrained()->cascadeOnDelete();
            $table->foreignId('category_id')->constrained()->cascadeOnDelete();
            $table->boolean('is_active')->default(true);

            $table->primary(['city_id', 'category_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('city_categories');
        Schema::dropIfExists('problem_types');
        Schema::dropIfExists('categories');
    }
};
