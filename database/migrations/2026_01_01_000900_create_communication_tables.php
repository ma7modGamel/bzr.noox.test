<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * المحادثة — 18، BR-100..BR-102.
 * في وضع الموظفين تُنشأ عند التعيين (CONFIRMED) بلا حجب، لأن مرحلة ما قبل الاختيار غير موجودة.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('conversations', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained()->cascadeOnDelete();
            $table->foreignId('provider_profile_id')->constrained();
            $table->foreignId('customer_id')->constrained('users');
            $table->string('status', 16)->default('OPEN');
            $table->timestamps();

            $table->unique(['order_id', 'provider_profile_id']);
        });

        Schema::create('messages', function (Blueprint $table) {
            $table->id();
            $table->foreignId('conversation_id')->constrained()->cascadeOnDelete();
            $table->foreignId('sender_user_id')->constrained('users');
            $table->string('body', 1000);
            $table->text('original_body_encrypted')->nullable(); // يُكشف للإدارة عند النزاع فقط
            $table->boolean('was_masked')->default(false);       // BR-100
            $table->string('attachment_path')->nullable();
            $table->timestamp('read_at')->nullable();
            $table->timestamps();

            $table->index(['conversation_id', 'id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('messages');
        Schema::dropIfExists('conversations');
    }
};
