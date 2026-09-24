<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\OrderMedia;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Symfony\Component\Process\Process;

/** BR-014 — تحقق من المحتوى الفعلي وتخزين خاص بأسماء عشوائية. */
final class MediaUploadService
{
    /** @var array<string, string> */
    private const TYPES = [
        'image/jpeg' => 'IMAGE', 'image/png' => 'IMAGE', 'image/webp' => 'IMAGE',
        'video/mp4' => 'VIDEO', 'video/quicktime' => 'VIDEO', 'video/webm' => 'VIDEO',
        'audio/mp4' => 'AUDIO', 'audio/x-m4a' => 'AUDIO',
    ];

    /** @var array<string, string> */
    private const EXTENSIONS = [
        'image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp',
        'video/mp4' => 'mp4', 'video/quicktime' => 'mov', 'video/webm' => 'webm',
        'audio/mp4' => 'm4a', 'audio/x-m4a' => 'm4a',
    ];

    private const AUDIO_MIN_BIT_RATE = 60_000;

    private const AUDIO_MAX_BIT_RATE = 68_000;

    public function store(User $user, UploadedFile $file): OrderMedia
    {
        $mime = (string) $file->getMimeType();
        $type = self::TYPES[$mime] ?? null;

        if ($type === null) {
            throw BusinessRuleViolationException::rule('BR-014', 'نوع الملف غير مدعوم.');
        }

        $size = (int) $file->getSize();
        $maxBytes = match ($type) {
            'IMAGE' => 10 * 1024 * 1024,
            'VIDEO' => 50 * 1024 * 1024,
            'AUDIO' => 5 * 1024 * 1024,
        };

        if ($maxBytes !== null && $size > $maxBytes) {
            throw BusinessRuleViolationException::rule('BR-014', 'حجم الملف أكبر من الحد المسموح.');
        }

        $duration = match ($type) {
            'IMAGE' => null,
            'VIDEO' => $this->durationInSeconds($file),
            'AUDIO' => $this->validatedAudioDuration($file),
        };
        $maxDuration = $type === 'VIDEO' ? 60 : 120;

        if ($duration !== null && $duration > $maxDuration) {
            throw BusinessRuleViolationException::rule('BR-014', 'مدة الملف أكبر من الحد المسموح.');
        }

        $path = 'order-media/'.Str::uuid().'.'.self::EXTENSIONS[$mime];
        $contents = $type === 'IMAGE' ? $this->sanitizedImage($file, $mime) : file_get_contents($file->getRealPath());

        if ($contents === false || ! Storage::disk('local')->put($path, $contents)) {
            throw BusinessRuleViolationException::rule('BR-014', 'تعذر حفظ الملف.');
        }

        return OrderMedia::query()->create([
            'uploaded_by' => $user->getKey(),
            'expires_at' => now()->addDay(),
            'type' => $type,
            'path' => $path,
            'size_bytes' => Storage::disk('local')->size($path),
            'duration_sec' => $duration,
        ]);
    }

    private function durationInSeconds(UploadedFile $file): int
    {
        $process = new Process([
            'ffprobe', '-v', 'error', '-show_entries', 'format=duration',
            '-of', 'default=noprint_wrappers=1:nokey=1', $file->getRealPath(),
        ]);
        $process->setTimeout(10);
        $process->run();

        if (! $process->isSuccessful() || ! is_numeric(trim($process->getOutput()))) {
            throw BusinessRuleViolationException::rule('BR-014', 'تعذر التحقق من مدة الملف.');
        }

        return (int) ceil((float) trim($process->getOutput()));
    }

    private function validatedAudioDuration(UploadedFile $file): int
    {
        if (strtolower($file->getClientOriginalExtension()) !== 'm4a') {
            throw BusinessRuleViolationException::rule('BR-014', 'التسجيل الصوتي يجب أن يكون AAC بامتداد m4a.');
        }

        $process = new Process([
            'ffprobe', '-v', 'error',
            '-show_entries', 'stream=codec_type,codec_name,channels,bit_rate:format=format_name,duration,bit_rate',
            '-of', 'json', $file->getRealPath(),
        ]);
        $process->setTimeout(10);
        $process->run();

        if (! $process->isSuccessful()) {
            throw BusinessRuleViolationException::rule('BR-014', 'تعذر التحقق من التسجيل الصوتي.');
        }

        try {
            $probe = json_decode($process->getOutput(), true, flags: JSON_THROW_ON_ERROR);
        } catch (\JsonException) {
            throw BusinessRuleViolationException::rule('BR-014', 'تعذر التحقق من التسجيل الصوتي.');
        }

        $streams = is_array($probe['streams'] ?? null) ? $probe['streams'] : [];
        $stream = count($streams) === 1 && is_array($streams[0]) ? $streams[0] : null;
        $format = is_array($probe['format'] ?? null) ? $probe['format'] : [];
        $formatNames = explode(',', (string) ($format['format_name'] ?? ''));
        $bitRate = (int) ($stream['bit_rate'] ?? $format['bit_rate'] ?? 0);
        $duration = $format['duration'] ?? null;

        $isM4a = count(array_intersect($formatNames, ['mov', 'mp4', 'm4a'])) > 0;
        $isValidAudio = is_array($stream)
            && ($stream['codec_type'] ?? null) === 'audio'
            && ($stream['codec_name'] ?? null) === 'aac'
            && (int) ($stream['channels'] ?? 0) === 1
            && $bitRate >= self::AUDIO_MIN_BIT_RATE
            && $bitRate <= self::AUDIO_MAX_BIT_RATE;

        if (! $isM4a || ! $isValidAudio || ! is_numeric($duration)) {
            throw BusinessRuleViolationException::rule('BR-014', 'التسجيل يجب أن يكون AAC/m4a، قناة واحدة، وبمعدل 64kbps.');
        }

        return (int) ceil((float) $duration);
    }

    private function sanitizedImage(UploadedFile $file, string $mime): string
    {
        $source = match ($mime) {
            'image/jpeg' => @imagecreatefromjpeg($file->getRealPath()),
            'image/png' => @imagecreatefrompng($file->getRealPath()),
            'image/webp' => @imagecreatefromwebp($file->getRealPath()),
        };

        if ($source === false) {
            throw BusinessRuleViolationException::rule('BR-014', 'ملف الصورة تالف.');
        }

        ob_start();
        match ($mime) {
            'image/jpeg' => imagejpeg($source, null, 90),
            'image/png' => imagepng($source),
            'image/webp' => imagewebp($source, null, 90),
        };
        $contents = ob_get_clean();
        imagedestroy($source);

        if (! is_string($contents)) {
            throw BusinessRuleViolationException::rule('BR-014', 'تعذر معالجة الصورة.');
        }

        return $contents;
    }
}
