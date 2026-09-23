<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الدفع والتسويات — 19. لا كيان "رصيد" ولا "قيد": الرصيد محسوب لا مخزّن (BR-062).
 * في وضع الموظفين: التوريد `provider_remittances` ورد الخامات `provider_payouts` (BR-065).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('payments', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained(); // بلا cascade — عمود pending_key المولّد
            $table->string('method', 16);
            $table->decimal('amount', 10, 2);
            $table->string('status', 32);
            $table->string('gateway', 32)->nullable();                 // FAWRY — DEC-038
            $table->string('merchant_ref', 64)->unique();              // مرجعنا لكل محاولة
            $table->string('gateway_reference', 64)->nullable()->unique();
            $table->string('fawry_reference_number', 64)->nullable();
            $table->string('channel', 16)->nullable();                 // CARD / WALLET / KIOSK
            $table->timestamp('expires_at')->nullable();               // CFG-052
            $table->timestamp('paid_at')->nullable();
            $table->string('recorded_by_type', 16)->nullable();
            $table->unsignedBigInteger('recorded_by_id')->nullable();
            $table->string('failure_reason')->nullable();
            $table->timestamps();

            // محاولة PENDING واحدة فقط لكل طلب (19)
            $table->unsignedBigInteger('pending_key')
                ->storedAs("IF(`status` = 'PENDING', `order_id`, NULL)");
            $table->unique('pending_key');

            $table->index(['order_id', 'status']);
        });

        Schema::create('refunds', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained();
            $table->foreignId('payment_id')->constrained();
            $table->decimal('amount', 10, 2);
            $table->decimal('labor_amount', 10, 2);
            $table->decimal('materials_amount', 10, 2);
            $table->string('gateway_reference', 64);
            $table->string('reason', 500);
            $table->foreignId('admin_id')->constrained('admins'); // المدير العام فقط (23)
            $table->timestamps();
        });

        Schema::create('provider_payouts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('provider_profile_id')->constrained();
            $table->decimal('amount', 10, 2);
            $table->string('method', 16);
            $table->string('reference', 64);
            $table->timestamp('paid_at');
            $table->foreignId('admin_id')->constrained('admins');
            $table->string('note', 500)->nullable();
            $table->timestamps();

            $table->index(['provider_profile_id', 'paid_at']);
        });

        Schema::create('provider_remittances', function (Blueprint $table) {
            $table->id();
            $table->foreignId('provider_profile_id')->constrained();
            $table->decimal('amount', 10, 2);
            $table->string('method', 16);
            $table->string('reference', 64);
            $table->timestamp('received_at');
            $table->foreignId('admin_id')->constrained('admins');
            $table->string('note', 500)->nullable();
            $table->timestamps();

            $table->index(['provider_profile_id', 'received_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('provider_remittances');
        Schema::dropIfExists('provider_payouts');
        Schema::dropIfExists('refunds');
        Schema::dropIfExists('payments');
    }
};
