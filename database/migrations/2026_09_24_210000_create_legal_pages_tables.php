<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * DEC-051 — الصفحات القانونية من اللوحة، بسجل نسخ ونشر/مسودة.
 * BR-018 — نسخة الشروط التي وافق عليها العميل تُحفظ عليه، والنسخة الجديدة تتطلب موافقة عند أول طلب بعدها.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('legal_pages', function (Blueprint $table): void {
            $table->id();
            $table->string('slug', 32)->unique();
            $table->string('title', 150);
            $table->unsignedSmallInteger('sort')->default(0);
            $table->timestamps();
        });

        Schema::create('legal_page_versions', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('legal_page_id')->constrained()->cascadeOnDelete();
            $table->unsignedInteger('version');
            $table->string('title', 150);
            $table->longText('body');
            $table->timestamp('effective_at')->nullable();
            $table->string('status', 16)->default('DRAFT');
            $table->boolean('requires_legal_review')->default(true);
            $table->unsignedInteger('terms_version')->nullable(); // نسخة terms_versions المقابلة عند نشر الشروط
            $table->timestamp('published_at')->nullable();
            $table->foreignId('published_by')->nullable()->constrained('admins');
            $table->foreignId('created_by')->nullable()->constrained('admins');
            $table->timestamps();

            $table->unique(['legal_page_id', 'version']);
            $table->index(['legal_page_id', 'status', 'effective_at']);
        });

        Schema::table('users', function (Blueprint $table): void {
            $table->unsignedInteger('accepted_terms_version')->nullable();
            $table->timestamp('terms_accepted_at')->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table): void {
            $table->dropColumn(['accepted_terms_version', 'terms_accepted_at']);
        });
        Schema::dropIfExists('legal_page_versions');
        Schema::dropIfExists('legal_pages');
    }
};
