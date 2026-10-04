<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\User;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * الدفعة 9أ، المرحلة 1 (DEC-061): ردود الهوية الحقيقية التي يقرؤها موديل أندرويد المكتوب.
 *
 * كل تشغيل يتحقق من الحقول التي يعتمد عليها التطبيقان. ومع RECORD_API_RESPONSES=1 تُكتب الردود
 * إلى androidapp/app/src/test/resources/api/auth/ لتقرأها اختبارات القراءة في أندرويد.
 */
final class MobileAuthResponsesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class]);
    }

    #[Test]
    public function ردود_الهوية_تحمل_حقول_الموديل(): void
    {
        $register = $this->postJson('/api/v1/auth/register', [
            'name' => 'أحمد',
            'email' => 'new@test.local',
            'phone' => '01012345678',
            'password' => 'secret-password',
        ])->assertCreated();
        $this->assertUserPayload($register);
        $this->record('register_201', $register);

        // A rated customer: decimal fields arrive as strings ("4.50"), which the model must read.
        User::factory()->create(['name' => 'منى', 'email' => 'customer@test.local', 'phone' => '01011111111', 'password' => 'secret-password', 'customer_rating_avg' => '4.50']);
        $login = $this->postJson('/api/v1/auth/login', ['email' => 'customer@test.local', 'password' => 'secret-password'])
            ->assertOk();
        $this->assertUserPayload($login);
        $this->assertSame('4.50', $login->json('user.rating_avg'));
        $this->record('login_200', $login);

        $provider = User::factory()->create(['name' => 'كريم', 'email' => 'provider@test.local', 'phone' => '01022222222', 'password' => 'secret-password']);
        ProviderProfile::factory()->for($provider)->create();
        $providerLogin = $this->postJson('/api/v1/auth/login', ['email' => 'provider@test.local', 'password' => 'secret-password'])
            ->assertOk()
            ->assertJsonStructure(['user' => ['provider' => ['id', 'status', 'available_now', 'is_verified']]]);
        $this->record('login_200_provider', $providerLogin);

        $unverified = User::factory()->unverified()->create(['name' => 'سارة', 'email' => 'unverified@test.local', 'phone' => '01033333333']);
        $me = $this->withToken($unverified->createToken('mobile')->plainTextToken)->getJson('/api/v1/me')->assertOk();
        $this->assertSame(false, $me->json('user.is_verified'));
        $this->record('me_200_unverified', $me);

        $this->record('error_422_validation', $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@test.local',
            'password' => 'wrong',
        ])->assertStatus(422)->assertJsonPath('error.code', 'VALIDATION_FAILED')->assertJsonStructure(['error' => ['fields' => ['email']]]));

        User::factory()->blocked()->create(['email' => 'blocked@test.local', 'password' => 'secret-password']);
        $this->record('error_403_blocked', $this->postJson('/api/v1/auth/login', [
            'email' => 'blocked@test.local',
            'password' => 'secret-password',
        ])->assertForbidden()->assertJsonPath('error.code', 'ACCOUNT_BLOCKED'));

        $verified = User::factory()->create();
        $this->app['auth']->forgetGuards();
        $this->record('error_409_already_verified', $this->withToken($verified->createToken('mobile')->plainTextToken)
            ->postJson('/api/v1/auth/email/resend')
            ->assertStatus(409)
            ->assertJsonPath('error.code', 'EMAIL_ALREADY_VERIFIED'));

        $this->app['auth']->forgetGuards();
        $this->record('error_401', $this->withToken('invalid')->getJson('/api/v1/me')
            ->assertUnauthorized()
            ->assertJsonPath('error.code', 'UNAUTHENTICATED'));
    }

    private function assertUserPayload(TestResponse $response): void
    {
        $response->assertJsonStructure(['token', 'user' => ['id', 'name', 'email', 'phone', 'is_verified', 'rating_avg', 'provider']]);
        $this->assertIsString($response->json('token'));
        $this->assertIsBool($response->json('user.is_verified'));
    }

    /** Writes the body with stable ids and token, so re-recording only shows real contract changes. */
    private function record(string $name, TestResponse $response): void
    {
        if (! env('RECORD_API_RESPONSES')) {
            return;
        }

        $body = $response->json();
        if (isset($body['token'])) {
            $body['token'] = '1|recorded-token';
        }
        if (isset($body['user']['id'])) {
            $body['user']['id'] = 1;
        }
        if (isset($body['user']['provider']['id'])) {
            $body['user']['provider']['id'] = 1;
        }

        $directory = base_path('androidapp/app/src/test/resources/api/auth');
        if (! is_dir($directory)) {
            mkdir($directory, 0755, true);
        }
        file_put_contents(
            "{$directory}/{$name}.json",
            json_encode($body, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES).PHP_EOL,
        );
    }
}
