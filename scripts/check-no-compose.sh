#!/usr/bin/env bash
# DEC-047 proof: the Android app contains no Jetpack Compose and no cross-platform UI framework.
# Runs on every change in CI (.github/workflows/android-no-compose.yml) and fails on the first trace found.
#   a. Gradle: plugins, build features, dependencies (declared and resolved, every configuration)
#   b. Source: Compose APIs in src/ of every module (main, test, androidTest)
#   c. Release APK and AAB: no class under androidx.compose / org.jetbrains.compose in any dex
#   d. No Flutter, React Native, Compose Multiplatform or Kotlin Multiplatform UI
# Usage: scripts/check-no-compose.sh [--skip-build]   (--skip-build reuses existing release outputs)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/androidapp"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
SKIP_BUILD=0
[[ "${1:-}" == "--skip-build" ]] && SKIP_BUILD=1
failures=0

section() { printf '\n== %s\n' "$1"; }
pass() { printf 'PASS  %s\n' "$1"; }
fail() { printf 'FAIL  %s\n' "$1"; failures=$((failures + 1)); }

# Prints matches (path:line) of an extended regex in the given files; empty output means none.
scan() {
    local pattern="$1"; shift
    grep -nHE "$pattern" "$@" 2>/dev/null || true
}

gradle_files() {
    find "$APP" \( -name build -o -name .gradle -o -name .kotlin \) -prune -o \
        \( -name '*.gradle' -o -name '*.gradle.kts' -o -name '*.toml' -o -name 'gradle.properties' \) -type f -print
}

check() { # label, matches
    if [[ -z "$2" ]]; then pass "$1"; else fail "$1"; printf '%s\n' "$2" | sed 's/^/      /'; fi
}

section "a. Gradle"
mapfile -t GRADLE < <(gradle_files)
check "no org.jetbrains.kotlin.plugin.compose / org.jetbrains.compose plugin" \
    "$(scan 'kotlin\.plugin\.compose|org\.jetbrains\.compose|kotlin\("plugin\.compose"\)' "${GRADLE[@]}")"
check "no buildFeatures compose = true, no composeOptions" \
    "$(scan '\bcompose\s*=\s*true|composeOptions|kotlinCompilerExtensionVersion' "${GRADLE[@]}")"
check "no compose dependency in any module or version catalog" \
    "$(scan 'androidx\.compose|compose-bom|activity-compose|lifecycle-[a-z-]*compose|navigation-compose|hilt-navigation-compose|coil-compose|maps-compose|accompanist|material3|ui-tooling' "${GRADLE[@]}")"

cd "$APP"
resolved=""
for project in $(grep -oE '":[^"]+"' settings.gradle.kts | tr -d '"'); do
    tree="$(./gradlew -q "$project:dependencies" 2>/dev/null)"
    configurations="$(printf '%s\n' "$tree" | grep -cE '^[a-zA-Z]+[A-Za-z0-9]* - ' || true)"
    printf '      %s: %s configurations resolved\n' "$project" "$configurations"
    resolved+="$(printf '%s\n' "$tree" | grep -E 'androidx\.compose|org\.jetbrains\.compose' | sed "s|^|$project |" || true)"
done
check "./gradlew <module>:dependencies (every configuration, incl. test / androidTest): no androidx.compose, no org.jetbrains.compose" "$resolved"

section "b. Source"
mapfile -t SOURCES < <(find "$APP" \( -name build -o -name .gradle -o -name .kotlin \) -prune -o -path '*/src/*' \( -name '*.kt' -o -name '*.java' -o -name '*.xml' \) -type f -print)
check "no @Composable, setContent {, ComposeView, import androidx.compose, createComposeRule, @Preview in src/ (main, test, androidTest)" \
    "$(scan '@Composable|setContent\s*\{|ComposeView|import\s+androidx\.compose|createComposeRule|createAndroidComposeRule|@Preview\b|androidx\.compose\.ui\.platform' "${SOURCES[@]}")"

section "c. Release APK and AAB"
if [[ $SKIP_BUILD -eq 0 ]]; then
    ./gradlew -q :app:assembleRelease :app:bundleRelease >/dev/null
fi
APK="$(find "$APP/app/build/outputs/apk/release" -name '*.apk' | head -1)"
AAB="$(find "$APP/app/build/outputs/bundle/release" -name '*.aab' | head -1)"
DEXDUMP="$(find "$SDK/build-tools" -name dexdump -type f | sort -V | tail -1)"
[[ -n "$APK" && -n "$AAB" && -n "$DEXDUMP" ]] || { fail "release outputs or dexdump missing (APK='$APK' AAB='$AAB')"; exit 1; }
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
classes() { # archive, dex glob inside it
    local dir="$work/$(basename "$1")"
    mkdir -p "$dir" && unzip -qo "$1" "$2" -d "$dir"
    find "$dir" -name '*.dex' -print0 | xargs -0 -n1 "$DEXDUMP" 2>/dev/null | grep -E '^  Class descriptor' | sed -E "s/.*'L(.*);'/\1/" | tr '/' '.'
}
for archive in "$APK:classes*.dex" "$AAB:base/dex/*.dex"; do
    file="${archive%%:*}"
    all="$(classes "$file" "${archive#*:}")"
    printf '      %s: %s classes\n' "$(basename "$file")" "$(printf '%s\n' "$all" | grep -c . || true)"
    check "$(basename "$file"): 0 classes under androidx.compose / org.jetbrains.compose" \
        "$(printf '%s\n' "$all" | grep -E '^(androidx\.compose|org\.jetbrains\.compose)\.' | head -20 || true)"
done

section "d. No cross-platform UI framework"
cd "$ROOT"
check "no Flutter (pubspec.yaml, io.flutter, flutter plugin)" \
    "$( (find "$APP" -name pubspec.yaml -not -path '*/build/*'; scan 'io\.flutter|dev\.flutter|flutter' "${GRADLE[@]}" "${SOURCES[@]}") | head -20)"
check "no React Native (com.facebook.react, react-native)" \
    "$( (find "$APP" -name package.json -not -path '*/build/*'; scan 'com\.facebook\.react|react-native' "${GRADLE[@]}" "${SOURCES[@]}") | head -20)"
check "no Compose Multiplatform / Kotlin Multiplatform (org.jetbrains.compose, kotlin(\"multiplatform\"), kotlin.multiplatform)" \
    "$(scan 'org\.jetbrains\.compose|kotlin\("multiplatform"\)|kotlin\.multiplatform|kotlin-multiplatform|commonMain' "${GRADLE[@]}" "${SOURCES[@]}")"

section "Result"
if [[ $failures -gt 0 ]]; then
    echo "FAILED: $failures check(s) found Compose or a cross-platform framework."
    exit 1
fi
echo "OK: no Compose and no cross-platform UI framework in the Android app."
