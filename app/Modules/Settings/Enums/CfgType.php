<?php

declare(strict_types=1);

namespace App\Modules\Settings\Enums;

enum CfgType: string
{
    case Bool = 'bool';
    case Int = 'int';
    case Decimal = 'decimal';
    case Time = 'time';
    case Json = 'json';
    case String = 'string';
}
