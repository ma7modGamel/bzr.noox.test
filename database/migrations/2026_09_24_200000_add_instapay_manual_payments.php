<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * DEC-050 / BR-057 — تحويل إنستاباي يدوي بتأكيد المدير العام.
 * المحاولة المعلّقة الواحدة لكل طلب تشمل الآن PENDING_VERIFICATION أيضًا (19).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('payments', function (Blueprint $table): void {
            $table->string('transfer_reference', 64)->nullable()->after('channel');
            $table->foreignId('receipt_media_id')->nullable()->after('transfer_reference')->constrained('order_media')->nullOnDelete();
            $table->timestamp('submitted_at')->nullable()->after('receipt_media_id');
            $table->foreignId('verified_by_admin_id')->nullable()->after('submitted_at')->constrained('admins');
            $table->timestamp('verified_at')->nullable()->after('verified_by_admin_id');
            $table->string('rejection_note', 500)->nullable()->after('failure_reason');
            $table->index('transfer_reference');
            $table->dropUnique(['pending_key']);
        });

        Schema::table('payments', function (Blueprint $table): void {
            $table->dropColumn('pending_key');
        });

        Schema::table('payments', function (Blueprint $table): void {
            $table->unsignedBigInteger('pending_key')
                ->storedAs("IF(`status` IN ('PENDING', 'PENDING_VERIFICATION'), `order_id`, NULL)");
            $table->unique('pending_key');
        });
    }

    public function down(): void
    {
        Schema::table('payments', function (Blueprint $table): void {
            $table->dropUnique(['pending_key']);
        });

        Schema::table('payments', function (Blueprint $table): void {
            $table->dropColumn('pending_key');
        });

        Schema::table('payments', function (Blueprint $table): void {
            $table->dropConstrainedForeignId('receipt_media_id');
            $table->dropConstrainedForeignId('verified_by_admin_id');
            $table->dropIndex(['transfer_reference']);
            $table->dropColumn(['transfer_reference', 'submitted_at', 'verified_at', 'rejection_note']);
            $table->unsignedBigInteger('pending_key')->storedAs("IF(`status` = 'PENDING', `order_id`, NULL)");
            $table->unique('pending_key');
        });
    }
};
