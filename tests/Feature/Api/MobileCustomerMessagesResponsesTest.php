<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\RecordsMobileResponses;
use Tests\TestCase;

/**
 * 9أ المرحلة 3 (DEC-061) — ردود المحادثات (C18/C19) ورفع الوسائط (C17) كما يقرؤها التطبيقان.
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/customer/.
 */
final class MobileCustomerMessagesResponsesTest extends TestCase
{
    use RecordsMobileResponses;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
        Storage::fake('local');
    }

    #[Test]
    public function ردود_المحادثات_والوسائط(): void
    {
        $customer = User::factory()->create(['name' => 'منى حسن']);
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $provider->user->update(['name' => 'كريم السباك']);
        app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        $conversationId = $this->as($customer)->getJson('/api/v1/conversations?page=1')->assertOk()->json('data.0.id');
        $this->actingAs($provider->user, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->postJson("/api/v1/conversations/{$conversationId}/messages", ['body' => 'تمام، هوصل خلال نص ساعة.'])
            ->assertCreated();

        // بعد التعيين لا حجب لوسائل التواصل (18)؛ الحجب قبل الاختيار مغطى في BatchTwoApiTest.
        $this->recordResponse('customer', 'message_sent', $this->as($customer)
            ->postJson("/api/v1/conversations/{$conversationId}/messages", ['body' => 'التسريب تحت الحوض، الباب مفتوح.'])
            ->assertCreated()
            ->assertJsonPath('data.is_mine', true)
            ->assertJsonStructure(['data' => ['id', 'is_mine', 'body', 'was_masked', 'media', 'created_at']]));

        $media = $this->as($customer)
            ->post('/api/v1/media', ['file' => UploadedFile::fake()->image('leak.jpg', 300, 200)], ['Accept' => 'application/json'])
            ->assertCreated()
            ->assertJsonStructure(['media' => ['id', 'type']]);
        $this->recordResponse('customer', 'media_uploaded', $media);
        $this->as($customer)->postJson("/api/v1/conversations/{$conversationId}/messages", ['body' => null, 'media_ids' => [$media->json('media.id')]])
            ->assertCreated();

        $this->recordResponse('customer', 'conversations', $this->as($customer)->getJson('/api/v1/conversations?page=1')->assertOk()
            ->assertJsonStructure(['data' => [['id', 'status', 'order' => ['id', 'number', 'status_label', 'category'],
                'provider' => ['id', 'name', 'is_verified'], 'last_message']]]));
        $this->recordResponse('customer', 'conversation_messages', $this->as($customer)
            ->getJson("/api/v1/conversations/{$conversationId}/messages?page=1")->assertOk()
            ->assertJsonCount(3, 'data')
            ->assertJsonStructure(['conversation' => ['id', 'status', 'order', 'provider'], 'data' => [['id', 'is_mine', 'body', 'was_masked', 'media', 'created_at']]]));
    }

    private function as(User $user): static
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER']);
    }
}
