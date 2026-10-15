<?php

declare(strict_types=1);

/**
 * Resolves the shared palette, aliases and mixed shades before platform generation.
 * Alpha wrappers remain intact so each platform can encode opacity correctly.
 *
 * @param  array<string, mixed>  $tokens
 * @return array<string, mixed>
 */
function resolveThemeTokens(array $tokens): array
{
    $theme = $tokens['theme'] ?? throw new RuntimeException('tokens.json needs a "theme" section (DEC-072).');
    $resolving = [];
    $resolved = [];

    $hex = static function (mixed $value, string $path) use (&$hex, &$colour): string {
        if (is_array($value) && array_key_exists('mix', $value)) {
            $from = $hex($value['mix'], $path.'.mix');
            $to = $hex($value['with'] ?? throw new RuntimeException("{$path}: a mix needs \"with\"."), $path.'.with');
            $amount = $value['amount'] ?? throw new RuntimeException("{$path}: a mix needs \"amount\".");
            if (! is_int($amount) && ! is_float($amount)) {
                throw new RuntimeException("{$path}: mix amount must be numeric.");
            }
            if ($amount < 0 || $amount > 1) {
                throw new RuntimeException("{$path}: mix amount must be between 0 and 1.");
            }

            return mixHex($from, $to, $amount);
        }
        if (! is_string($value)) {
            throw new RuntimeException("{$path}: expected a colour.");
        }
        if (preg_match('/^\{(theme|color)\.([A-Za-z0-9]+)\}$/', $value, $match)) {
            return $colour($match[1], $match[2], $path);
        }
        if (! preg_match('/^#[0-9A-Fa-f]{6}$/', $value)) {
            throw new RuntimeException("{$path}: \"{$value}\" is not a colour or a reference.");
        }

        return strtoupper($value);
    };

    $colour = static function (string $section, string $name, string $path) use (&$hex, &$resolving, &$resolved, $theme, $tokens): string {
        $key = $section.'.'.$name;
        if (isset($resolved[$key])) {
            return $resolved[$key];
        }
        if (isset($resolving[$key])) {
            throw new RuntimeException("{$path}: circular colour reference through {$key}.");
        }
        $source = $section === 'theme' ? $theme : $tokens['color'];
        if (! array_key_exists($name, $source)) {
            throw new RuntimeException("{$path}: unknown colour {$key}.");
        }
        $value = $source[$name];
        if (is_array($value) && isset($value['hex'])) {
            $value = $value['hex'];
        }
        $resolving[$key] = true;
        $resolved[$key] = $hex($value, $key);
        unset($resolving[$key]);

        return $resolved[$key];
    };

    $walk = static function (mixed $node, string $path) use (&$walk, $hex): mixed {
        if (is_string($node) && str_starts_with($node, '{')) {
            return $hex($node, $path);
        }
        if (is_string($node) && preg_match('/^#[0-9A-Fa-f]{6}$/', $node)) {
            return $hex($node, $path);
        }
        if (! is_array($node)) {
            return $node;
        }
        if (array_key_exists('mix', $node)) {
            return $hex($node, $path);
        }
        foreach ($node as $key => $child) {
            $node[$key] = $walk($child, $path.'.'.$key);
        }

        return $node;
    };

    foreach ($tokens as $section => $value) {
        if ($section !== 'theme' && $section !== '$meta') {
            $tokens[$section] = $walk($value, (string) $section);
        }
    }

    return $tokens;
}

/**
 * Applies palette colours to the source logo's gradient stops, preserving its geometry.
 *
 * @param  array<string, string|array{hex: string, alpha?: float}>  $colors
 */
function resolveBrandSymbol(string $symbol, array $colors): string
{
    $document = new DOMDocument;
    $document->loadXML($symbol, LIBXML_NONET);
    $xpath = new DOMXPath($document);
    $xpath->registerNamespace('svg', 'http://www.w3.org/2000/svg');
    foreach (['teal' => ['brandPrimary', 'brandSecondary'], 'ribbon' => ['onPrimary', 'brandTint']] as $gradient => $stops) {
        $nodes = $xpath->query('//svg:linearGradient[@id="'.$gradient.'"]/svg:stop');
        if ($nodes === false || $nodes->length !== count($stops)) {
            throw new RuntimeException("Brand symbol needs two stops in the {$gradient} gradient.");
        }
        foreach ($stops as $index => $color) {
            $nodes->item($index)->setAttribute('stop-color', $colors[$color]);
        }
    }

    return $document->saveXML($document->documentElement)."\n";
}

function mixHex(string $from, string $to, float $amount): string
{
    $a = str_split(ltrim($from, '#'), 2);
    $b = str_split(ltrim($to, '#'), 2);
    $channels = array_map(
        static fn (string $x, string $y): string => str_pad(strtoupper(dechex((int) round(hexdec($x) + (hexdec($y) - hexdec($x)) * $amount))), 2, '0', STR_PAD_LEFT),
        $a, $b,
    );

    return '#'.implode('', $channels);
}

/** @param array<string, mixed> $tokens */
function themedBrandIcon(string $symbol, array $tokens, float $heightRatio): string
{
    $body = preg_replace('/^<svg[^>]*>|<\/svg>\s*$/', '', $symbol);
    $scale = 1024 * $heightRatio / 400;
    $x = (1024 - 320 * $scale) / 2;
    $y = (1024 - 400 * $scale) / 2;
    $primary = $tokens['color']['brandPrimary'];
    $backdrop = $tokens['color']['brandBackdrop'];

    return '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024"><defs><linearGradient id="background" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="'.$primary.'"/><stop offset="1" stop-color="'.$backdrop.'"/></linearGradient></defs><rect width="1024" height="1024" fill="url(#background)"/><g transform="translate('.$x.' '.$y.') scale('.$scale.')">'.$body.'</g></svg>';
}

/** Renders the existing vector geometry with palette colours for native icon assets. */
function renderBrandPng(string $root, string $svg, int $width, int $height, bool $opaque = true): string
{
    $source = tempnam(sys_get_temp_dir(), 'bzr-theme-svg-');
    $output = tempnam(sys_get_temp_dir(), 'bzr-theme-png-');
    try {
        file_put_contents($source, $svg);
        $arguments = ['python3', $root.'/tools/render-brand-svg', $source, $output, (string) $width, (string) $height];
        if ($opaque) {
            $arguments[] = '--opaque';
        }
        $process = proc_open($arguments, [1 => ['pipe', 'w'], 2 => ['pipe', 'w']], $pipes);
        if (! is_resource($process)) {
            throw new RuntimeException('Unable to start the brand SVG renderer.');
        }
        $error = stream_get_contents($pipes[2]);
        fclose($pipes[1]);
        fclose($pipes[2]);
        if (proc_close($process) !== 0) {
            throw new RuntimeException('Unable to render the themed brand icon: '.$error);
        }

        return (string) file_get_contents($output);
    } finally {
        unlink($source);
        unlink($output);
    }
}
