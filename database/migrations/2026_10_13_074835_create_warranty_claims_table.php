<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('warranty_claims', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained('orders');
            $table->string('status', 20)->default('REQUESTED');
            $table->text('description');
            $table->foreignId('provider_profile_id')->nullable()->constrained('provider_profiles');
            $table->foreignId('assigned_by_admin_id')->nullable()->constrained('admins');
            $table->timestamp('assigned_at')->nullable();
            $table->timestamp('scheduled_at')->nullable();
            $table->string('admin_note', 500)->nullable();
            $table->index(['order_id', 'status']);
            $table->index(['provider_profile_id', 'status', 'scheduled_at']);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('warranty_claims');
    }
};
