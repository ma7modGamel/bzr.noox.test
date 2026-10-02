<?php

declare(strict_types=1);

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * الدفعة 6 (DEC-058): تصفية الإشعارات حسب الوضع، تفضيل NTF-18، ومنع تكرار الإشعارات المجدولة والمجمّعة.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->boolean('rating_reminders_enabled')->default(true)->after('customer_rating_count');
        });

        Schema::table('notifications', function (Blueprint $table) {
            $table->string('app_mode', 16)->nullable()->after('notifiable_id');
            $table->index(['notifiable_type', 'notifiable_id', 'app_mode'], 'notifications_notifiable_mode_index');
        });

        Schema::create('notification_dispatches', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('code', 8);
            $table->foreignId('order_id')->nullable()->constrained()->nullOnDelete();
            $table->string('key')->unique();
            $table->uuid('notification_id')->nullable();
            $table->timestamp('created_at')->useCurrent();
            $table->index(['order_id', 'code', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('notification_dispatches');

        Schema::table('notifications', function (Blueprint $table) {
            $table->dropIndex('notifications_notifiable_mode_index');
            $table->dropColumn('app_mode');
        });

        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('rating_reminders_enabled');
        });
    }
};
