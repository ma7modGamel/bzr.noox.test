<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الوسائط، العروض، المقترحات، السجل، التتبع، روابط المشاركة — 29 §الطلبات.
 * القيود الشرطية بأعمدة مولّدة (MySQL/MariaDB لا يدعمان الفهارس الفريدة الجزئية).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('order_media', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->nullable()->constrained()->cascadeOnDelete();
            $table->foreignId('uploaded_by')->nullable()->constrained('users'); // رفع مؤقت قبل النشر
            $table->timestamp('expires_at')->nullable();                        // يُحذف بعد 24 ساعة
            $table->string('type', 16);  // IMAGE / VIDEO / AUDIO — BR-014
            $table->string('path');
            $table->unsignedBigInteger('size_bytes');
            $table->unsignedSmallInteger('duration_sec')->nullable();
            $table->timestamps();

            $table->index(['order_id', 'type']);
            $table->index('expires_at');
        });

        Schema::create('offers', function (Blueprint $table) {
            $table->id();
            // بلا cascade: الأعمدة المولّدة تمنعه في MySQL، ولا حذف فعلي للطلبات أصلًا (29 §الحذف)
            $table->foreignId('order_id')->constrained();
            $table->foreignId('provider_profile_id')->constrained();

            // BR-007 — صف التعيين الإداري يُكتب هنا بمصدر ADMIN_ASSIGNMENT وسعر 0
            $table->string('source', 24)->default('PROVIDER');

            $table->decimal('price', 10, 2);                          // ≥ CFG-012، أو 0 للتعيين
            $table->unsignedSmallInteger('eta_minutes')->nullable();  // NOW فقط — BR-032
            $table->boolean('inspection_fee_deductible')->nullable(); // معاينة فقط — DEC-005
            $table->string('includes_text', 150)->nullable();
            $table->string('note', 300)->nullable();
            $table->string('status', 32);
            $table->foreignId('previous_offer_id')->nullable()->constrained('offers'); // BR-030
            $table->timestamp('submitted_at');
            $table->timestamp('withdrawn_at')->nullable();
            $table->timestamp('decided_at')->nullable();
            $table->timestamps();

            // BR-030: عرض نشط واحد لكل فني على الطلب
            $table->string('active_key', 64)
                ->storedAs("IF(`status` IN ('SUBMITTED','NOT_SELECTED','ACCEPTED'), CONCAT(`order_id`,':',`provider_profile_id`), NULL)");
            $table->unique('active_key');

            $table->index(['order_id', 'status']);
            $table->index(['provider_profile_id', 'status']);
        });

        Schema::table('orders', function (Blueprint $table) {
            $table->foreign('accepted_offer_id')->references('id')->on('offers');
        });

        Schema::create('price_proposals', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained();
            $table->foreignId('provider_profile_id')->constrained();
            $table->string('type', 32);                 // ProposalType — BR-042
            $table->decimal('amount', 10, 2);           // > 0 — BR-041
            $table->string('reason', 300);
            $table->string('photo_path')->nullable();
            $table->string('status', 32);
            $table->timestamp('expires_at');            // CFG-040
            $table->timestamp('decided_at')->nullable();
            $table->string('decided_by_type', 16)->nullable();
            $table->timestamps();

            // BR-041: مقترح معلّق واحد فقط على الطلب
            $table->unsignedBigInteger('pending_key')
                ->storedAs("IF(`status` = 'PENDING', `order_id`, NULL)");
            $table->unique('pending_key');

            $table->index(['status', 'expires_at']);
            $table->index(['order_id', 'type']);
        });

        Schema::create('order_events', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained()->cascadeOnDelete();
            $table->string('event_code', 16);    // OrderEventCode
            $table->string('actor_type', 16);
            $table->unsignedBigInteger('actor_id')->nullable();
            $table->string('from_status', 32)->nullable();
            $table->string('to_status', 32)->nullable();
            $table->string('ref_type', 32)->nullable();
            $table->unsignedBigInteger('ref_id')->nullable();
            $table->json('meta')->nullable();
            $table->timestamp('created_at');

            $table->index(['order_id', 'id']);
            $table->index('event_code');
        });

        Schema::create('tracking_points', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained()->cascadeOnDelete();
            $table->decimal('lat', 10, 7);
            $table->decimal('lng', 10, 7);
            $table->timestamp('recorded_at');

            $table->index(['order_id', 'recorded_at']); // يُحذف عند الإغلاق — BR-112
        });

        Schema::create('share_links', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained()->cascadeOnDelete();
            $table->char('token', 43)->unique();  // DEC-037
            $table->timestamp('expires_at');
            $table->timestamp('revoked_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('share_links');
        Schema::dropIfExists('tracking_points');
        Schema::dropIfExists('order_events');
        Schema::dropIfExists('price_proposals');

        Schema::table('orders', function (Blueprint $table) {
            $table->dropForeign(['accepted_offer_id']);
        });

        Schema::dropIfExists('offers');
        Schema::dropIfExists('order_media');
    }
};
