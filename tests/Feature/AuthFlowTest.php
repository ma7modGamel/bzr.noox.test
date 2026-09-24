<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Facades\URL;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

class AuthFlowTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function registration_returns_a_session_and_sends_a_verification_email(): void
    {
        Notification::fake();

        $response = $this->postJson('/api/v1/auth/register', [
            'name' => 'أحمد علي',
            'email' => 'ahmed@example.test',
            'phone' => '01012345678',
            'password' => 'password123',
        ]);

        $response->assertCreated()
            ->assertJsonPath('user.is_verified', false)
            ->assertJsonPath('email_verification_required', true)
            ->assertJsonStructure(['token']);

        $user = User::query()->where('email', 'ahmed@example.test')->firstOrFail();
        Notification::assertSentTo($user, VerifyEmail::class);
    }

    #[Test]
    public function an_authenticated_user_can_resend_and_complete_email_verification(): void
    {
        Notification::fake();
        $user = User::factory()->create(['email_verified_at' => null, 'status' => UserStatus::Active]);
        $token = $user->createToken('mobile')->plainTextToken;

        $this->withToken($token)->postJson('/api/v1/auth/email/resend')->assertNoContent();
        Notification::assertSentTo($user, VerifyEmail::class);

        $url = URL::temporarySignedRoute(
            'verification.verify',
            now()->addMinutes(60),
            ['id' => $user->getKey(), 'hash' => sha1($user->getEmailForVerification())],
        );

        $this->getJson($url)->assertOk()->assertJsonPath('message', 'تم تفعيل البريد.');
        $this->assertNotNull($user->fresh()->email_verified_at);
    }

    #[Test]
    public function forgot_password_does_not_reveal_whether_the_account_exists(): void
    {
        Notification::fake();
        $user = User::factory()->create();

        $this->postJson('/api/v1/auth/password/forgot', ['email' => $user->email])->assertNoContent();
        $this->postJson('/api/v1/auth/password/forgot', ['email' => 'missing@example.test'])->assertNoContent();

        Notification::assertSentTo($user, ResetPassword::class);
    }

    #[Test]
    public function a_valid_reset_token_changes_the_password_and_revokes_mobile_tokens(): void
    {
        $user = User::factory()->create(['password' => 'old-password']);
        $user->createToken('mobile');
        $token = Password::createToken($user);

        $this->postJson('/api/v1/auth/password/reset', [
            'email' => $user->email,
            'token' => $token,
            'password' => 'new-password',
        ])->assertNoContent();

        $user->refresh();
        $this->assertTrue(Hash::check('new-password', $user->password));
        $this->assertCount(0, $user->tokens);
    }

    #[Test]
    public function an_invalid_reset_token_is_rejected_without_changing_the_password(): void
    {
        $user = User::factory()->create(['password' => 'old-password']);

        $this->postJson('/api/v1/auth/password/reset', [
            'email' => $user->email,
            'token' => 'invalid-token',
            'password' => 'new-password',
        ])->assertUnprocessable()->assertJsonPath('error.code', 'VALIDATION_FAILED');

        $this->assertTrue(Hash::check('old-password', $user->fresh()->password));
    }
}
