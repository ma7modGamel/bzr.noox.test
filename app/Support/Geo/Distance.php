<?php

declare(strict_types=1);

namespace App\Support\Geo;

/** مسافة بين نقطتين بالأمتار — تُستخدم في تحذير الوصول (BR-111، CFG-082). */
final class Distance
{
    private const EARTH_RADIUS_M = 6_371_000;

    public static function meters(float $lat1, float $lng1, float $lat2, float $lng2): int
    {
        $dLat = deg2rad($lat2 - $lat1);
        $dLng = deg2rad($lng2 - $lng1);

        $a = sin($dLat / 2) ** 2
            + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) ** 2;

        return (int) round(self::EARTH_RADIUS_M * 2 * atan2(sqrt($a), sqrt(1 - $a)));
    }
}
