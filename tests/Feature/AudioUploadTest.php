<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use PHPUnit\Framework\Attributes\Test;
use Symfony\Component\Process\Process;
use Tests\TestCase;

final class AudioUploadTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function يقبل_aac_m4a_أحادي_القناة_بمعدل_64kbps(): void
    {
        Storage::fake('local');
        $path = $this->audioFile(channels: 1, bitRate: '64k');

        try {
            $file = new UploadedFile($path, 'voice.m4a', 'audio/mp4', null, true);

            $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => $file], ['Accept' => 'application/json'])
                ->assertCreated()
                ->assertJsonPath('media.type', 'AUDIO');
        } finally {
            @unlink($path);
        }
    }

    #[Test]
    public function يرفض_التسجيل_غير_الأحادي_حتى_لو_كان_m4a(): void
    {
        Storage::fake('local');
        $path = $this->audioFile(channels: 2, bitRate: '64k');

        try {
            $file = new UploadedFile($path, 'voice.m4a', 'audio/mp4', null, true);

            $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => $file], ['Accept' => 'application/json'])
                ->assertUnprocessable()
                ->assertJsonPath('error.rule', 'BR-014');
        } finally {
            @unlink($path);
        }
    }

    #[Test]
    public function يرفض_أي_صيغة_صوت_أخرى(): void
    {
        Storage::fake('local');
        $path = sys_get_temp_dir().'/bzr-'.Str::uuid().'.mp3';
        $process = new Process([
            'ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i', 'sine=frequency=1000:duration=1',
            '-ac', '1', '-b:a', '64k', $path,
        ]);
        $process->mustRun();

        try {
            $file = new UploadedFile($path, 'voice.mp3', 'audio/mpeg', null, true);

            $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => $file], ['Accept' => 'application/json'])
                ->assertUnprocessable()
                ->assertJsonPath('error.rule', 'BR-014');
        } finally {
            @unlink($path);
        }
    }

    #[Test]
    public function يرفض_التسجيل_الأكبر_من_خمسة_ميجابايت(): void
    {
        Storage::fake('local');
        $path = $this->audioFile(channels: 1, bitRate: '64k');
        file_put_contents($path, str_repeat('x', 5 * 1024 * 1024), FILE_APPEND);

        try {
            $file = new UploadedFile($path, 'voice.m4a', 'audio/mp4', null, true);

            $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => $file], ['Accept' => 'application/json'])
                ->assertUnprocessable()
                ->assertJsonPath('error.rule', 'BR-014');
        } finally {
            @unlink($path);
        }
    }

    #[Test]
    public function يرفض_التسجيل_الأطول_من_120_ثانية(): void
    {
        Storage::fake('local');
        $path = sys_get_temp_dir().'/bzr-'.Str::uuid().'.m4a';
        $process = new Process([
            'ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i', 'sine=frequency=1000:duration=121',
            '-ac', '1', '-c:a', 'aac', '-b:a', '64k', $path,
        ]);
        $process->mustRun();

        try {
            $file = new UploadedFile($path, 'voice.m4a', 'audio/mp4', null, true);

            $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => $file], ['Accept' => 'application/json'])
                ->assertUnprocessable()
                ->assertJsonPath('error.rule', 'BR-014');
        } finally {
            @unlink($path);
        }
    }

    private function audioFile(int $channels, string $bitRate): string
    {
        $path = sys_get_temp_dir().'/bzr-'.Str::uuid().'.m4a';
        $process = new Process([
            'ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i', 'sine=frequency=1000:duration=1',
            '-ac', (string) $channels, '-c:a', 'aac', '-b:a', $bitRate, $path,
        ]);
        $process->mustRun();

        return $path;
    }
}
