<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الفنيون — 15، 29 §الفنيون.
 * `employment_type` صفة تعاقدية لا حالة (39): الوضع على مستوى المنصة، والصفة على مستوى الملف.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('provider_profiles', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->unique()->constrained()->cascadeOnDelete();
            $table->string('status', 32)->default('PENDING_REVIEW');      // ProviderStatus
            $table->string('employment_type', 16)->default('INDEPENDENT'); // EmploymentType — 39
            $table->string('bio', 300)->nullable();
            $table->unsignedTinyInteger('experience_years')->default(0);
            $table->boolean('available_now')->default(false);
            $table->timestamp('phone_verified_at')->nullable();            // DEC-033
            $table->timestamp('submitted_at')->nullable();
            $table->foreignId('reviewed_by')->nullable()->constrained('admins');
            $table->timestamp('reviewed_at')->nullable();
            $table->string('rejection_reason')->nullable();
            $table->string('suspension_reason')->nullable();
            $table->string('payout_method', 16)->nullable();               // INSTAPAY / WALLET / BANK
            $table->text('payout_details')->nullable();                    // مشفَّر (32)
            $table->decimal('rating_avg', 3, 2)->nullable();
            $table->decimal('rating_quality_avg', 3, 2)->nullable();
            $table->decimal('rating_punctuality_avg', 3, 2)->nullable();
            $table->decimal('rating_conduct_avg', 3, 2)->nullable();
            $table->unsignedInteger('ratings_count')->default(0);
            $table->unsignedInteger('completed_orders_count')->default(0);
            $table->unsignedInteger('avg_response_minutes')->nullable();   // BR-132
            $table->timestamp('dues_blocked_at')->nullable();              // BR-063 — مهمة كل 10 دقائق
            $table->timestamps();

            $table->index(['status', 'available_now']);
            $table->index('employment_type');
        });

        Schema::create('provider_documents', function (Blueprint $table) {
            $table->id();
            $table->foreignId('provider_profile_id')->constrained()->cascadeOnDelete();
            $table->string('type', 16); // ID_FRONT / ID_BACK / SELFIE
            $table->string('path');
            $table->timestamps();

            $table->unique(['provider_profile_id', 'type']);
        });

        Schema::create('provider_categories', function (Blueprint $table) {
            $table->foreignId('provider_profile_id')->constrained()->cascadeOnDelete();
            $table->foreignId('category_id')->constrained()->cascadeOnDelete();

            $table->primary(['provider_profile_id', 'category_id']);
            $table->index('category_id');
        });

        Schema::create('provider_specialties', function (Blueprint $table) {
            $table->foreignId('provider_profile_id')->constrained()->cascadeOnDelete();
            $table->foreignId('problem_type_id')->constrained()->cascadeOnDelete();

            $table->primary(['provider_profile_id', 'problem_type_id']);
        });

        Schema::create('provider_areas', function (Blueprint $table) {
            $table->foreignId('provider_profile_id')->constrained()->cascadeOnDelete();
            $table->foreignId('area_id')->constrained()->cascadeOnDelete();

            $table->primary(['provider_profile_id', 'area_id']);
            $table->index('area_id'); // استعلام التوزيع/التعيين (29)
        });

        Schema::create('portfolio_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('provider_profile_id')->constrained()->cascadeOnDelete();
            $table->string('image_path');
            $table->string('caption', 150)->nullable();
            $table->unsignedSmallInteger('sort')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('portfolio_items');
        Schema::dropIfExists('provider_areas');
        Schema::dropIfExists('provider_specialties');
        Schema::dropIfExists('provider_categories');
        Schema::dropIfExists('provider_documents');
        Schema::dropIfExists('provider_profiles');
    }
};
