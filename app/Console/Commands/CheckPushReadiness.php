<?php

declare(strict_types=1);

namespace App\Console\Commands;

use DOMDocument;
use DOMXPath;
use Illuminate\Console\Command;

final class CheckPushReadiness extends Command
{
    protected $signature = 'notifications:push-check {--json} {--android-config=} {--ios-config=}';

    protected $description = 'فحص إعدادات Push المحلية دون إرسال أو كشف مفاتيح؛ لا يثبت وصول إشعار إلى جهاز';

    public function handle(): int
    {
        $project = (string) config('services.fcm.project_id');
        $credentials = $this->readJson((string) config('services.fcm.credentials'));
        $android = $this->readJson((string) ($this->option('android-config') ?: base_path('androidapp/app/google-services.json')));
        $iosPath = (string) ($this->option('ios-config') ?: base_path('iosapp/BzrApp/GoogleService-Info.plist'));
        $ios = $this->readPlist($iosPath);
        $privateKey = $credentials['private_key'] ?? null;
        $checks = [
            'fcm_driver' => config('services.fcm.driver') === 'fcm',
            'project_configured' => $project !== '',
            'service_account' => ($credentials['type'] ?? null) === 'service_account' && ! empty($credentials['client_email'])
                && is_string($privateKey) && openssl_pkey_get_private($privateKey) !== false,
            'service_account_project' => $project !== '' && ($credentials['project_id'] ?? null) === $project,
            'android_project' => $project !== '' && data_get($android, 'project_info.project_id') === $project,
            'android_package' => collect($android['client'] ?? [])->contains(fn (mixed $client): bool => is_array($client) && data_get($client, 'client_info.android_client_info.package_name') === config('services.app_links.package')),
            'ios_project' => $project !== '' && ($ios['PROJECT_ID'] ?? null) === $project,
            'ios_bundle' => ($ios['BUNDLE_ID'] ?? null) === config('services.app_links.bundle_id'),
        ];
        $ready = ! in_array(false, $checks, true);
        $result = ['configuration_ready' => $ready, 'checks' => $checks, 'queue_connection' => config('queue.default'),
            'device_delivery_verified' => false, 'remaining' => ['Upload APNs key in Firebase', 'Run the notifications queue worker', 'Test foreground, background and cold-start order links on Android and iOS']];
        if ($this->option('json')) {
            $this->line(json_encode($result, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES));
        } else {
            foreach ($checks as $name => $passed) {
                $this->line(($passed ? 'OK ' : 'MISSING ').$name);
            }
            $this->warn('هذا فحص إعدادات فقط. ربط APNs وتشغيل العامل ووصول الإشعار وفتح الطلب تحتاج اختبار أجهزة.');
        }

        return $ready ? self::SUCCESS : self::FAILURE;
    }

    /** @return array<string, mixed> */
    private function readJson(string $path): array
    {
        if ($path === '' || ! is_readable($path)) {
            return [];
        }
        $data = json_decode((string) file_get_contents($path), true);

        return is_array($data) ? $data : [];
    }

    /** @return array<string, string> */
    private function readPlist(string $path): array
    {
        if (! is_readable($path)) {
            return [];
        }
        $xml = new DOMDocument;
        if (! @$xml->load($path, LIBXML_NONET)) {
            return [];
        }
        $xpath = new DOMXPath($xml);
        $result = [];
        foreach (['PROJECT_ID', 'BUNDLE_ID'] as $key) {
            $result[$key] = $xpath->evaluate('string(/plist/dict/key[.="'.$key.'"]/following-sibling::*[1])');
        }

        return $result;
    }
}
