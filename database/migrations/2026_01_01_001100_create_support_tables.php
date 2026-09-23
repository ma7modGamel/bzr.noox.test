<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/** النزاعات والبلاغات — 16، BR-120، BR-121. البلاغ ملف مستقل لا يغير أي حالة (ASM-11). */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('disputes', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained();
            $table->string('opened_by_type', 16);
            $table->unsignedBigInteger('opened_by_id');
            $table->string('reason_code', 32);
            $table->text('description');
            $table->boolean('is_post_close')->default(false); // BR-120
            $table->string('status', 16)->default('OPEN');
            $table->string('resolution', 32)->nullable();
            $table->string('resolution_note', 1000)->nullable();
            $table->foreignId('resolved_by')->nullable()->constrained('admins');
            $table->timestamp('resolved_at')->nullable();
            $table->timestamps();

            // نزاع مفتوح واحد لكل طلب
            $table->unsignedBigInteger('open_key')
                ->storedAs("IF(`status` = 'OPEN', `order_id`, NULL)");
            $table->unique('open_key');

            $table->index('status');
        });

        Schema::create('dispute_attachments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('dispute_id')->constrained()->cascadeOnDelete();
            $table->string('path');
            $table->string('uploaded_by_type', 16);
            $table->unsignedBigInteger('uploaded_by_id');
            $table->timestamps();
        });

        Schema::create('provider_reports', function (Blueprint $table) {
            $table->id();
            $table->foreignId('reporter_user_id')->constrained('users');
            $table->foreignId('provider_profile_id')->constrained();
            $table->string('reason_code', 32);
            $table->text('description')->nullable();
            $table->string('status', 16)->default('NEW');
            $table->foreignId('reviewed_by')->nullable()->constrained('admins');
            $table->timestamp('reviewed_at')->nullable();
            $table->timestamps();

            $table->index('status');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('provider_reports');
        Schema::dropIfExists('dispute_attachments');
        Schema::dropIfExists('disputes');
    }
};
