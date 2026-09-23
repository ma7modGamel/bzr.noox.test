<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الطلب كيان واحد من النشر حتى الإغلاق (DEC-027) — 29 §الطلبات.
 * `operating_mode` يُثبَّت عند النشر فلا يتأثر بتبديل CFG-090 لاحقًا (BR-009).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('number')->unique(); // الرقم المعروض (#1248)

            // الوضع المثبت — BR-009
            $table->string('operating_mode', 16); // EMPLOYEE / MARKETPLACE

            $table->foreignId('customer_id')->constrained('users');
            $table->foreignId('provider_profile_id')->nullable()->constrained();
            $table->unsignedBigInteger('accepted_offer_id')->nullable(); // FK يُضاف بعد جدول offers

            // التعيين الإداري — T-27 / T-29 (وضع الموظفين)
            $table->foreignId('assigned_by_admin_id')->nullable()->constrained('admins');
            $table->timestamp('assigned_at')->nullable();

            $table->foreignId('city_id')->constrained();
            $table->foreignId('area_id')->constrained();
            $table->foreignId('category_id')->constrained();
            $table->foreignId('problem_type_id')->constrained();
            $table->text('description')->nullable(); // BR-013

            // نسخة ثابتة من العنوان — لا مرجع حي
            $table->foreignId('customer_address_id')->nullable()->constrained('customer_addresses');
            $table->string('address_text', 255);
            $table->string('building', 50)->nullable();
            $table->string('floor', 20)->nullable();
            $table->string('apartment', 20)->nullable();
            $table->string('landmark', 150)->nullable();
            $table->decimal('lat', 10, 7);
            $table->decimal('lng', 10, 7);

            // الموعد — BR-017
            $table->string('timing_type', 16);
            $table->timestamp('slot_start')->nullable();
            $table->timestamp('slot_end')->nullable();

            $table->string('pricing_mode', 16);             // BR-008: INSPECTION دائمًا في وضع الموظفين
            $table->decimal('budget_amount', 10, 2)->nullable(); // BR-015
            $table->string('materials_responsibility', 32); // BR-016

            $table->string('status', 32)->default('OPEN');

            // المهلات — CFG-010/013 (سوق) أو CFG-093 (موظفين)
            $table->timestamp('offers_close_at')->nullable();
            $table->timestamp('selection_deadline_at')->nullable();

            $table->unsignedSmallInteger('reopen_count')->default(0);
            $table->foreignId('republished_from_id')->nullable()->constrained('orders');

            $table->string('payment_method', 16)->nullable();
            $table->string('payment_status', 32)->default('UNPAID');

            // نسبة العمولة مثبتة لحظة التأكيد — DEC-009؛ وفي وضع الموظفين = 1.0000 (BR-065)
            $table->decimal('commission_rate', 5, 4)->nullable();

            // المبالغ المثبتة عند AWAITING_PAYMENT / CLOSED — BR-044، BR-060
            $table->decimal('labor_total', 10, 2)->nullable();
            $table->decimal('materials_total', 10, 2)->nullable();
            $table->decimal('final_amount', 10, 2)->nullable();
            $table->decimal('commission_amount', 10, 2)->nullable();
            $table->decimal('labor_refunded', 10, 2)->default(0);
            $table->decimal('refunded_total', 10, 2)->default(0);

            // أوقات المراحل
            $table->timestamp('confirmed_at')->nullable();
            $table->timestamp('trip_started_at')->nullable();
            $table->timestamp('arrived_at')->nullable();
            $table->timestamp('work_started_at')->nullable();
            $table->timestamp('completed_at')->nullable();
            $table->timestamp('closed_at')->nullable();
            $table->timestamp('cancelled_at')->nullable();
            $table->timestamp('expired_at')->nullable();

            // نقطة الوصول — BR-111، BR-112
            $table->decimal('arrived_lat', 10, 7)->nullable();
            $table->decimal('arrived_lng', 10, 7)->nullable();
            $table->unsignedInteger('arrival_distance_m')->nullable();

            $table->string('cancelled_by_type', 16)->nullable();
            $table->unsignedBigInteger('cancelled_by_id')->nullable();
            $table->string('cancel_reason_code', 32)->nullable();
            $table->string('cancel_note', 500)->nullable();

            $table->timestamp('settlement_eligible_at')->nullable(); // BR-061

            $table->unsignedInteger('terms_version');   // BR-018
            $table->timestamp('terms_accepted_at');

            $table->unsignedInteger('version')->default(1); // القفل المتفائل (31)
            $table->timestamps();

            $table->index(['status', 'offers_close_at']);
            $table->index(['status', 'selection_deadline_at']);
            $table->index(['area_id', 'category_id', 'status']);
            $table->index(['customer_id', 'status']);
            $table->index(['provider_profile_id', 'status']);
            $table->index(['provider_profile_id', 'slot_start']);
            $table->index(['status', 'closed_at']);
            $table->index(['operating_mode', 'status']); // لوحة التعيين SCR-A15
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('orders');
    }
};
