<?php

declare(strict_types=1);

namespace Tests\Feature\Architecture;

use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;
use ZipArchive;

final class BrandAssetsTest extends TestCase
{
    #[Test]
    public function أيقونات_المتجرين_بالمقاسات_الصحيحة_ونسخة_أبل_دون_قناة_شفافية(): void
    {
        $apple = base_path('design/generated/store/app-store/app-icon-1024.png');
        $google = base_path('design/generated/store/google-play/app-icon-512.png');

        $this->assertSame([1024, 1024], array_slice(getimagesize($apple), 0, 2));
        $this->assertSame(2, ord(file_get_contents($apple, offset: 25, length: 1)));
        $this->assertSame([512, 512], array_slice(getimagesize($google), 0, 2));
        $this->assertLessThanOrEqual(1024 * 1024, filesize($google));
        $this->assertSame(
            hash_file('sha256', $apple),
            hash_file('sha256', base_path('iosapp/BzrApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png')),
        );
    }

    #[Test]
    public function بنرات_جوجل_بمقاس_صحيح_ودون_قناة_شفافية(): void
    {
        foreach (['ar', 'en'] as $locale) {
            $path = base_path("design/generated/store/google-play/feature-graphic-{$locale}.png");

            $this->assertSame([1024, 500], array_slice(getimagesize($path), 0, 2));
            $this->assertSame(2, ord(file_get_contents($path, offset: 25, length: 1)));
        }
    }

    #[Test]
    public function تصدير_الشعار_يحافظ_على_الشريط_الفاتح_والخلفية_الشفافة(): void
    {
        $icon = imagecreatefrompng(base_path('design/brand/app-icon-1024.png'));
        $symbol = imagecreatefrompng(base_path('design/generated/brand/symbol-transparent.png'));

        $ribbon = imagecolorsforindex($icon, imagecolorat($icon, 512, 220));
        $this->assertGreaterThan(220, $ribbon['red']);
        $this->assertGreaterThan(220, $ribbon['green']);
        $this->assertGreaterThan(220, $ribbon['blue']);
        $corner = imagecolorsforindex($symbol, imagecolorat($symbol, 0, 0));
        $this->assertSame(127, $corner['alpha']);
        imagedestroy($icon);
        imagedestroy($symbol);
    }

    #[Test]
    public function لقطات_اندرويد_المصدرة_تغطي_الرحلة_باللغتين_وبمقاس_عمودي(): void
    {
        foreach (['ar', 'en'] as $locale) {
            foreach (['01-home', '02-request', '03-offers', '04-chat'] as $screen) {
                $path = base_path("design/generated/store/google-play/screenshots/{$locale}/{$screen}.png");

                $this->assertFileExists($path);
                $this->assertSame([1080, 1920], array_slice(getimagesize($path), 0, 2));
                $this->assertSame(2, ord(file_get_contents($path, offset: 25, length: 1)));
            }
        }
    }

    #[Test]
    public function حزمة_الرفع_تحتوي_نسخ_الصور_الحالية_فقط(): void
    {
        $archive = new ZipArchive;
        $opened = $archive->open(base_path('design/generated/store/bremo-store-assets.zip'));

        $this->assertSame(true, $opened);
        $this->assertSame(12, $archive->numFiles);
        for ($index = 0; $index < $archive->numFiles; $index++) {
            $name = $archive->getNameIndex($index);
            $this->assertStringEndsWith('.png', $name);
            $this->assertSame(
                file_get_contents(base_path('design/generated/store/'.$name)),
                $archive->getFromIndex($index),
            );
        }
        $archive->close();
    }
}
