<?php

declare(strict_types=1);

$brand = readJson($root.'/design/brand/brand.json');
$brandSymbol = (string) file_get_contents($root.'/'.$brand['symbol']);
preg_match_all('/<path\b[^>]*d="([^"]+)"[^>]*\/>/', $brandSymbol, $brandPaths);
if (count($brandPaths[1]) !== 2) {
    throw new RuntimeException('The Bremo symbol must have its teal fold and white ribbon.');
}
$tealPath = xml(normalizePath($brandPaths[1][0]));
$ribbonPath = xml(normalizePath($brandPaths[1][1]));

function brandNativeVector(string $tealPath, string $ribbonPath, float $symbolHeight, bool $background = false, bool $monochrome = false, int $intrinsic = 108): string
{
    $scale = $symbolHeight / 400;
    $x = (108 - 320 * $scale) / 2;
    $y = (108 - $symbolHeight) / 2;
    $backgroundPath = $background ? '<path android:pathData="M0 0 H108 V108 H0 Z"><aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108" android:startColor="@color/bremo_brand_primary" android:endColor="@color/bremo_brand_backdrop"/></aapt:attr></path>' : '';
    $tealPaint = $monochrome ? 'android:fillColor="@color/bremo_on_primary"' : '';
    $ribbonPaint = $tealPaint;
    $tealGradient = $monochrome ? '' : '<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="2" android:startY="78" android:endX="214" android:endY="389" android:startColor="@color/bremo_brand_primary" android:endColor="@color/bremo_brand_secondary"/></aapt:attr>';
    $ribbonGradient = $monochrome ? '' : '<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="2" android:startY="2" android:endX="122" android:endY="346" android:startColor="@color/bremo_on_primary" android:endColor="@color/bremo_brand_tint"/></aapt:attr>';

    return '<?xml version="1.0" encoding="utf-8"?>'."\n".'<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt" android:width="'.$intrinsic.'dp" android:height="'.$intrinsic.'dp" android:viewportWidth="108" android:viewportHeight="108">'.$backgroundPath.'<group android:translateX="'.num($x).'" android:translateY="'.num($y).'" android:scaleX="'.num($scale).'" android:scaleY="'.num($scale).'"><path '.$tealPaint.' android:pathData="'.$tealPath.'">'.$tealGradient.'</path><path '.$ribbonPaint.' android:pathData="'.$ribbonPath.'">'.$ribbonGradient.'</path></group></vector>'."\n";
}

$adaptive = $brand['androidAdaptive']['symbolHeight'];
syncText($root, 'androidapp/core/design/src/main/res/drawable/bremo_brand_icon.xml', brandNativeVector($tealPath, $ribbonPath, 108 * $brand['icon']['symbolHeightRatio'], true), $checkOnly, $errors, $outputs);
syncText($root, 'androidapp/core/design/src/main/res/drawable/bremo_launch_symbol.xml', brandNativeVector($tealPath, $ribbonPath, $brand['androidSplash']['symbolHeight'], intrinsic: $tokens['size']['brandLaunchLogo']), $checkOnly, $errors, $outputs);
syncText($root, 'androidapp/app/src/main/res/drawable/ic_launcher_foreground.xml', brandNativeVector($tealPath, $ribbonPath, $adaptive), $checkOnly, $errors, $outputs);
syncText($root, 'androidapp/app/src/main/res/drawable/ic_launcher_monochrome.xml', brandNativeVector($tealPath, $ribbonPath, $adaptive, monochrome: true), $checkOnly, $errors, $outputs);
$adaptiveIcon = '<?xml version="1.0" encoding="utf-8"?>'."\n".'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/bremo_brand_backdrop"/><foreground android:drawable="@drawable/ic_launcher_foreground"/><monochrome android:drawable="@drawable/ic_launcher_monochrome"/></adaptive-icon>'."\n";
foreach (['ic_launcher', 'ic_launcher_round'] as $name) {
    syncText($root, 'androidapp/app/src/main/res/mipmap-anydpi-v26/'.$name.'.xml', $adaptiveIcon, $checkOnly, $errors, $outputs);
}

$startingTheme = '<?xml version="1.0" encoding="utf-8"?>'."\n".'<resources><style name="Theme.Bremo.Starting" parent="Theme.Bremo"><item name="android:windowBackground">@drawable/bremo_launch_background</item><item name="android:statusBarColor">@color/bremo_brand_backdrop</item><item name="android:navigationBarColor">@color/bremo_brand_backdrop</item><item name="android:windowLightStatusBar">false</item></style></resources>'."\n";
syncText($root, 'androidapp/app/src/main/res/values/brand_launch.xml', $startingTheme, $checkOnly, $errors, $outputs);
$startingThemeV27 = str_replace('</style>', '<item name="android:windowLightNavigationBar">false</item></style>', $startingTheme);
syncText($root, 'androidapp/app/src/main/res/values-v27/brand_launch.xml', $startingThemeV27, $checkOnly, $errors, $outputs);
$startingThemeV31 = '<?xml version="1.0" encoding="utf-8"?>'."\n".'<resources><style name="Theme.Bremo.Starting" parent="Theme.Bremo"><item name="android:windowSplashScreenBackground">@color/bremo_brand_backdrop</item><item name="android:windowSplashScreenAnimatedIcon">@drawable/bremo_launch_symbol</item><item name="android:windowBackground">@drawable/bremo_launch_background</item><item name="android:statusBarColor">@color/bremo_brand_backdrop</item><item name="android:navigationBarColor">@color/bremo_brand_backdrop</item><item name="android:windowLightStatusBar">false</item><item name="android:windowLightNavigationBar">false</item></style></resources>'."\n";
syncText($root, 'androidapp/app/src/main/res/values-v31/brand_launch.xml', $startingThemeV31, $checkOnly, $errors, $outputs);
$launchBackground = '<?xml version="1.0" encoding="utf-8"?>'."\n".'<layer-list xmlns:android="http://schemas.android.com/apk/res/android"><item android:drawable="@color/bremo_brand_backdrop"/><item android:gravity="center" android:width="@dimen/bremo_size_brand_launch_logo" android:height="@dimen/bremo_size_brand_launch_logo" android:drawable="@drawable/bremo_launch_symbol"/></layer-list>'."\n";
syncText($root, 'androidapp/app/src/main/res/drawable/bremo_launch_background.xml', $launchBackground, $checkOnly, $errors, $outputs);

$jsonFlags = JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES;
$iconContents = ['images' => [['filename' => 'AppIcon-1024.png', 'idiom' => 'universal', 'platform' => 'ios', 'size' => '1024x1024']], 'info' => ['author' => 'tools/gen-design', 'version' => 1]];
syncText($root, 'iosapp/BzrApp/Assets.xcassets/AppIcon.appiconset/Contents.json', (string) json_encode($iconContents, $jsonFlags)."\n", $checkOnly, $errors, $outputs);
syncBinary($root, $root.'/design/brand/app-icon-1024.png', 'iosapp/BzrApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png', $checkOnly, $errors, $outputs);
$brandIconContents = ['images' => [['filename' => 'bremo-icon.png', 'idiom' => 'universal']], 'info' => ['author' => 'tools/gen-design', 'version' => 1], 'properties' => ['template-rendering-intent' => 'original']];
$brandIconDirectory = 'iosapp/Packages/DesignSystem/Sources/DesignSystem/Resources/Assets.xcassets/bremo_brand_icon.imageset';
syncText($root, $brandIconDirectory.'/Contents.json', (string) json_encode($brandIconContents, $jsonFlags)."\n", $checkOnly, $errors, $outputs);
syncBinary($root, $root.'/design/brand/app-icon-1024.png', $brandIconDirectory.'/bremo-icon.png', $checkOnly, $errors, $outputs);
$launchImageContents = ['images' => [['filename' => 'symbol.svg', 'idiom' => 'universal']], 'info' => ['author' => 'tools/gen-design', 'version' => 1], 'properties' => ['preserves-vector-representation' => true, 'template-rendering-intent' => 'original']];
syncText($root, 'iosapp/BzrApp/Assets.xcassets/BremoLaunchSymbol.imageset/Contents.json', (string) json_encode($launchImageContents, $jsonFlags)."\n", $checkOnly, $errors, $outputs);
syncBinary($root, $root.'/'.$brand['symbol'], 'iosapp/BzrApp/Assets.xcassets/BremoLaunchSymbol.imageset/symbol.svg', $checkOnly, $errors, $outputs);
$rawBackdrop = ltrim($tokens['color']['brandBackdrop'], '#');
$launchColor = ['colors' => [['idiom' => 'universal', 'color' => ['color-space' => 'srgb', 'components' => ['red' => sprintf('%.6f', hexdec(substr($rawBackdrop, 0, 2)) / 255), 'green' => sprintf('%.6f', hexdec(substr($rawBackdrop, 2, 2)) / 255), 'blue' => sprintf('%.6f', hexdec(substr($rawBackdrop, 4, 2)) / 255), 'alpha' => '1.000000']]]], 'info' => ['author' => 'tools/gen-design', 'version' => 1]];
syncText($root, 'iosapp/BzrApp/Assets.xcassets/BremoLaunchBackground.colorset/Contents.json', (string) json_encode($launchColor, $jsonFlags)."\n", $checkOnly, $errors, $outputs);

$launchSize = $tokens['size']['brandLaunchLogo'];
$launchHeight = $launchSize * 400 / 320;
$launchComponents = $launchColor['colors'][0]['color']['components'];
$launchStoryboard = <<<XML
<?xml version="1.0" encoding="UTF-8"?>
<document type="com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB" version="3.0" toolsVersion="23501" targetRuntime="iOS.CocoaTouch" propertyAccessControl="none" useAutolayout="YES" launchScreen="YES" useTraitCollections="YES" useSafeAreas="YES" colorMatched="YES" initialViewController="launch-controller">
    <device id="retina6_12" orientation="portrait" appearance="light"/>
    <dependencies><deployment identifier="iOS"/><plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="23500"/><capability name="Named colors" minToolsVersion="9.0"/><capability name="Safe area layout guides" minToolsVersion="9.0"/></dependencies>
    <scenes><scene sceneID="launch-scene"><objects>
        <viewController id="launch-controller" sceneMemberID="viewController"><view key="view" contentMode="scaleToFill" id="launch-view">
            <rect key="frame" x="0.0" y="0.0" width="390" height="844"/>
            <autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>
            <subviews><imageView userInteractionEnabled="NO" contentMode="scaleAspectFit" image="BremoLaunchSymbol" translatesAutoresizingMaskIntoConstraints="NO" id="launch-mark">
                <rect key="frame" x="139" y="352" width="$launchSize" height="$launchHeight"/>
                <constraints><constraint firstAttribute="width" constant="$launchSize" id="mark-width"/><constraint firstAttribute="height" constant="$launchHeight" id="mark-height"/></constraints>
            </imageView></subviews>
            <viewLayoutGuide key="safeArea" id="launch-safe-area"/>
            <color key="backgroundColor" name="BremoLaunchBackground"/>
            <constraints><constraint firstItem="launch-mark" firstAttribute="centerX" secondItem="launch-safe-area" secondAttribute="centerX" id="mark-center-x"/><constraint firstItem="launch-mark" firstAttribute="centerY" secondItem="launch-safe-area" secondAttribute="centerY" id="mark-center-y"/></constraints>
        </view></viewController>
        <placeholder placeholderIdentifier="IBFirstResponder" id="launch-responder" userLabel="First Responder" sceneMemberID="firstResponder"/>
    </objects></scene></scenes>
    <resources><image name="BremoLaunchSymbol" width="320" height="400"/><namedColor name="BremoLaunchBackground"><color red="{$launchComponents['red']}" green="{$launchComponents['green']}" blue="{$launchComponents['blue']}" alpha="1" colorSpace="custom" customColorSpace="sRGB"/></namedColor></resources>
</document>
XML;
syncText($root, 'iosapp/BzrApp/LaunchScreen.storyboard', $launchStoryboard."\n", $checkOnly, $errors, $outputs);
