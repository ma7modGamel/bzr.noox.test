<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Catalog\Models\Category;
use App\Modules\Geography\Models\Area;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\CatalogSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderApplicationApiTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function config_exposes_provider_payout_methods_with_dynamic_field_labels(): void
    {
        $response = $this->getJson('/api/v1/config')->assertOk();

        $this->assertSame(
            ['INSTAPAY', 'WALLET', 'BANK'],
            array_column($response->json('option_lists.provider_payout_methods'), 'code'),
        );
        $response->assertJsonPath(
            'option_lists.provider_payout_methods.0.field_label',
            'رقم أو عنوان إنستاباي',
        );
    }

    #[Test]
    public function get_returns_401_when_no_token_is_provided(): void
    {
        $this->getJson('/api/v1/provider/application')->assertUnauthorized();
    }

    #[Test]
    public function post_returns_401_when_no_token_is_provided(): void
    {
        $this->postJson('/api/v1/provider/application')->assertUnauthorized();
    }

    #[Test]
    public function get_returns_403_when_email_is_not_verified(): void
    {
        $user = User::factory()->unverified()->create();

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/provider/application')
            ->assertForbidden()
            ->assertJsonPath('error.code', 'EMAIL_NOT_VERIFIED');
    }

    #[Test]
    public function get_returns_not_started_actions_when_profile_does_not_exist(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/provider/application')
            ->assertOk()
            ->assertJsonPath('data.status', 'NOT_STARTED')
            ->assertJsonPath('data.available_actions', [
                'start_provider_application',
                'submit_provider_application',
            ])
            ->assertJsonMissingPath('data.employment_type');
    }

    #[Test]
    public function valid_payload_creates_pending_application_and_returns_201(): void
    {
        Storage::fake('local');
        $this->seed(CatalogSeeder::class);
        $user = User::factory()->create();
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $specialty = $category->problemTypes()->where('is_active', true)->firstOrFail();
        $area = Area::query()->where('is_active', true)->firstOrFail();
        $media = [
            'profile_photo_media_id' => $this->ownedImage($user, 'profile.jpg'),
            'id_front_media_id' => $this->ownedImage($user, 'front.jpg'),
            'id_back_media_id' => $this->ownedImage($user, 'back.jpg'),
        ];

        $response = $this->actingAs($user, 'sanctum')->postJson('/api/v1/provider/application', [
            'experience_years' => 7,
            'bio' => 'خبرة في أعمال الصيانة المنزلية.',
            'category_ids' => [$category->id],
            'specialty_ids' => [$specialty->id],
            'area_ids' => [$area->id],
            'payout_method' => 'INSTAPAY',
            'payout_details' => 'provider@instapay',
            'employment_type' => 'EMPLOYEE',
            ...$media,
        ]);

        $response->assertCreated()
            ->assertJsonPath('data.status', 'PENDING_REVIEW')
            ->assertJsonPath('data.available_actions', [])
            ->assertJsonMissingPath('data.employment_type')
            ->assertJsonPath('data.documents.id_front', true)
            ->assertJsonPath('data.documents.id_back', true);

        $profile = ProviderProfile::query()->whereBelongsTo($user)->firstOrFail();
        $this->assertSame(ProviderStatus::PendingReview, $profile->status);
        $this->assertSame('INDEPENDENT', $profile->employment_type->value);
        $this->assertSame([$category->id], $profile->categories()->pluck('categories.id')->all());
        $this->assertSame([$specialty->id], $profile->specialties()->pluck('problem_types.id')->all());
        $this->assertSame([$area->id], $profile->areas()->pluck('areas.id')->all());
        $this->assertNotSame('provider@instapay', $profile->getRawOriginal('payout_details'));
        $this->assertSame(2, $profile->documents()->count());
        foreach ($media as $mediaId) {
            $this->assertDatabaseMissing('order_media', ['id' => $mediaId]);
        }
        Storage::disk('local')->assertExists($user->fresh()->avatar_path);
    }

    #[Test]
    public function invalid_specialty_and_inactive_area_return_422_without_creating_profile(): void
    {
        $this->seed(CatalogSeeder::class);
        $user = User::factory()->create();
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $otherCategory = Category::query()->where('is_active', true)->whereKeyNot($category->id)->firstOrFail();
        $foreignSpecialty = $otherCategory->problemTypes()->where('is_active', true)->firstOrFail();
        $inactiveArea = Area::query()->where('is_active', false)->firstOrFail();

        $this->actingAs($user, 'sanctum')->postJson('/api/v1/provider/application', [
            'experience_years' => 2,
            'category_ids' => [$category->id],
            'specialty_ids' => [$foreignSpecialty->id],
            'area_ids' => [$inactiveArea->id],
            'payout_method' => 'WALLET',
            'payout_details' => '01000000000',
        ])->assertUnprocessable()
            ->assertJsonPath(
                'error.fields.specialty_ids.0',
                'اختر تخصصات مفعلة تابعة للفئات المحددة.',
            )
            ->assertJsonPath('error.fields.area_ids.0', 'اختر مناطق عمل مفعلة.');

        $this->assertDatabaseMissing('provider_profiles', ['user_id' => $user->id]);
    }

    #[Test]
    public function media_owned_by_another_user_returns_422_without_consuming_it(): void
    {
        Storage::fake('local');
        $this->seed(CatalogSeeder::class);
        $user = User::factory()->create();
        $otherUser = User::factory()->create();
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $specialty = $category->problemTypes()->where('is_active', true)->firstOrFail();
        $area = Area::query()->where('is_active', true)->firstOrFail();
        $foreignMediaId = $this->ownedImage($otherUser, 'private.jpg');

        $this->actingAs($user, 'sanctum')->postJson('/api/v1/provider/application', [
            'experience_years' => 2,
            'category_ids' => [$category->id],
            'specialty_ids' => [$specialty->id],
            'area_ids' => [$area->id],
            'payout_method' => 'BANK',
            'payout_details' => 'EG001234',
            'profile_photo_media_id' => $foreignMediaId,
            'id_front_media_id' => $foreignMediaId,
            'id_back_media_id' => $foreignMediaId,
        ])->assertUnprocessable()
            ->assertJsonPath(
                'error.fields.profile_photo_media_id.0',
                'الصورة غير صالحة أو غير مملوكة للحساب.',
            );

        $this->assertDatabaseHas('order_media', ['id' => $foreignMediaId]);
        $this->assertDatabaseMissing('provider_profiles', ['user_id' => $user->id]);
    }

    #[Test]
    public function pending_application_returns_409_and_is_not_modified(): void
    {
        $this->seed(CatalogSeeder::class);
        $profile = ProviderProfile::factory()->create([
            'status' => ProviderStatus::PendingReview,
            'bio' => 'النبذة الأصلية',
        ]);
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $specialty = $category->problemTypes()->where('is_active', true)->firstOrFail();
        $area = Area::query()->where('is_active', true)->firstOrFail();

        $this->actingAs($profile->user, 'sanctum')
            ->postJson('/api/v1/provider/application', $this->minimalPayload(
                $category->id,
                $specialty->id,
                $area->id,
            ))
            ->assertConflict()
            ->assertJsonPath('error.code', 'APPLICATION_NOT_EDITABLE');

        $this->assertSame('النبذة الأصلية', $profile->fresh()->bio);
    }

    #[Test]
    public function rejected_application_can_be_resubmitted_without_reuploading_stored_images(): void
    {
        Storage::fake('local');
        $this->seed(CatalogSeeder::class);
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $specialty = $category->problemTypes()->where('is_active', true)->firstOrFail();
        $area = Area::query()->where('is_active', true)->firstOrFail();
        $profile = ProviderProfile::factory()->create([
            'status' => ProviderStatus::Rejected,
            'rejection_reason' => 'الصورة غير واضحة',
            'bio' => 'قديم',
        ]);
        Storage::disk('local')->put('provider/avatar.jpg', 'avatar');
        Storage::disk('local')->put('provider/front.jpg', 'front');
        Storage::disk('local')->put('provider/back.jpg', 'back');
        $profile->user->forceFill(['avatar_path' => 'provider/avatar.jpg'])->save();
        $profile->documents()->createMany([
            ['type' => 'ID_FRONT', 'path' => 'provider/front.jpg'],
            ['type' => 'ID_BACK', 'path' => 'provider/back.jpg'],
        ]);

        $this->actingAs($profile->user, 'sanctum')->postJson('/api/v1/provider/application', [
            'experience_years' => 9,
            'bio' => 'بيانات محدثة',
            'category_ids' => [$category->id],
            'specialty_ids' => [$specialty->id],
            'area_ids' => [$area->id],
            'payout_method' => 'BANK',
            'payout_details' => 'EG001234',
        ])->assertOk()
            ->assertJsonPath('data.status', 'PENDING_REVIEW')
            ->assertJsonPath('data.rejection_reason', null);

        $profile->refresh();
        $this->assertSame(ProviderStatus::PendingReview, $profile->status);
        $this->assertSame('بيانات محدثة', $profile->bio);
        $this->assertNull($profile->rejection_reason);
        Storage::disk('local')->assertExists('provider/avatar.jpg');
        Storage::disk('local')->assertExists('provider/front.jpg');
        Storage::disk('local')->assertExists('provider/back.jpg');
    }

    private function ownedImage(User $user, string $path): int
    {
        Storage::disk('local')->put($path, 'image');

        return OrderMedia::query()->create([
            'uploaded_by' => $user->getKey(),
            'type' => 'IMAGE',
            'path' => $path,
            'size_bytes' => 5,
            'expires_at' => now()->addHour(),
        ])->getKey();
    }

    /** @return array<string, mixed> */
    private function minimalPayload(int $categoryId, int $specialtyId, int $areaId): array
    {
        return [
            'experience_years' => 1,
            'bio' => 'لن تحفظ',
            'category_ids' => [$categoryId],
            'specialty_ids' => [$specialtyId],
            'area_ids' => [$areaId],
            'payout_method' => 'INSTAPAY',
            'payout_details' => 'test',
        ];
    }
}
