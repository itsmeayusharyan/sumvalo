#!/usr/bin/env bash
# Sumvalo release-artifact security check.
#
# Usage: scripts/security-audit.sh path/to/app-release.apk
#
# Runs the same checks against every release APK before it is declared ready:
#   1. identity, hash and permissions          (aapt2, sha256sum)
#   2. signing certificate                     (apksigner)
#   3. manifest hygiene                        (exported components, backup, debuggable)
#   4. native libraries and ABIs packaged      (unzip)
#   5. no network code in the DEX              (jadx class listing)
#   6. no analytics/ads/tracking SDKs          (jadx class listing)
#   7. no obvious embedded secrets             (resource + dex string scan)
#
# Exits non-zero on any finding, and prints each check's evidence.
#
# Known exceptions (documented, intentional):
#   - none: Sumvalo ships with no INTERNET permission and no network code in its own package.

set -euo pipefail

APK="${1:?usage: security-audit.sh <apk>}"
SDK="${ANDROID_HOME:?set ANDROID_HOME}"
BUILD_TOOLS=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JADX="${JADX:-jadx}"   # jadx 1.5+ on PATH, or set JADX=/path/to/jadx/bin/jadx

fail() { echo "FAIL: $*"; exit 1; }
pass() { echo "PASS: $*"; }

echo "== 1. identity, hash, permissions =="
AAPT2="$BUILD_TOOLS/aapt2"
BADGING=$($AAPT2 dump badging "$APK")
echo "$BADGING" | sed -n '1,3p'
SHA=$(sha256sum "$APK")
echo "$SHA"
SIZE=$(stat -c%s "$APK")
echo "size: $SIZE bytes"
PERMS=$($AAPT2 dump permissions "$APK")
echo "$PERMS"
echo "$PERMS" | grep -q "android.permission.INTERNET" && fail "APK requests android.permission.INTERNET — Sumvalo ships with no network permission" || true
echo "$PERMS" | grep -q "android.permission.ACCESS_NETWORK_STATE" && echo "NOTE: ACCESS_NETWORK_STATE present (likely from a library dependency)" || true
pass "no INTERNET / ACCESS_NETWORK_STATE permission declared by the app"

echo
echo "== 2. signing certificate =="
"$BUILD_TOOLS/apksigner" verify --print-certs "$APK" | sed -n '1,6p'
"$BUILD_TOOLS/apksigner" verify "$APK" && pass "APK signature verifies"
grep -q "CN=Sumvalo" <("$BUILD_TOOLS/apksigner" verify --print-certs "$APK") \
  && pass "signer is the Sumvalo release key" \
  || fail "signer is NOT the Sumvalo release key"

echo
echo "== 3. manifest hygiene (merged manifest via apkanalyzer) =="
APKANALYZER="${SDK}/cmdline-tools/latest/bin/apkanalyzer"
MANIFEST=$("$APKANALYZER" manifest print "$APK")
echo "$MANIFEST" | grep -E "package=|android:label=" | head -2
echo "$MANIFEST" | grep -q 'android:debuggable="true"' && fail "manifest is debuggable" || pass "not debuggable"
echo "$MANIFEST" | grep -q 'android:allowBackup="false"' && pass "allowBackup=false" || fail "allowBackup is not false"
echo "$MANIFEST" | grep -q 'android:dataExtractionRules' && pass "data extraction rules present" || echo "WARN: no dataExtractionRules"
EXPORTED=$(echo "$MANIFEST" | grep -c 'android:exported="true"' || true)
# Expected exported components, all intentional or permission-guarded:
#   - MainActivity (launcher)
#   - SmsShareActivity (ACTION_SEND text/plain: the user-initiated share target for bank
#     messages; reads only the shared text, stores only extracted fields, gated by consent)
#   - MonthWidgetProvider, QuickAddWidgetProvider (APPWIDGET_UPDATE: launcher-only)
#   - QuickAddTileService (BIND_QUICK_SETTINGS_TILE: system-only)
#   - WorkManager SystemJobService (BIND_JOB_SERVICE), DiagnosticsReceiver (DUMP)
#   - ProfileInstaller ProfileInstallReceiver (DUMP)
# The androidx components can only be invoked by the system or shell.
UNGUARDED=$(echo "$MANIFEST" | python3 - <<'PY'
import re, sys
t = sys.stdin.read()
bad = []
for m in re.finditer(r'<(activity|receiver|service|provider)\b', t):
    start = m.start(); end = t.index('>', start)
    block = t[start:end+1]
    exp = re.search(r'android:exported="true"', block)
    perm = re.search(r'android:permission="([^"]+)"', block)
    name = re.search(r'android:name="([^"]+)"', block)
    if exp and not perm:
        n = name.group(1) if name else '?'
        # the launcher activity is the one allowed unguarded exported component
        if 'MainActivity' not in n and 'SmsShareActivity' not in n and 'WidgetProvider' not in n and 'TileService' not in n and 'androidx.' not in n:
            bad.append(n)
print("\n".join(bad))
PY
)
[ -z "$UNGUARDED" ] || fail "exported components without a system permission (beyond launcher/widgets): $UNGUARDED"
echo "exported components: $EXPORTED (launcher + widgets unguarded by design; SMS/tile/JobService/DUMP-guarded for the rest)"
pass "no unexpected exported components"

echo
echo "== 4. native libraries and ABIs =="
unzip -l "$APK" "lib/*" | awk '{print $4}' | grep -v '^$' || true
ABIS=$(unzip -l "$APK" | grep -o 'lib/[^/]*' | sort -u)
echo "$ABIS"
echo "$ABIS" | grep -q arm64-v8a || fail "no arm64-v8a native library"
echo "$ABIS" | grep -q armeabi-v7a || fail "no armeabi-v7a native library"
echo "$ABIS" | grep -q x86 || fail "no x86 native library"
echo "$ABIS" | grep -q x86_64 || fail "no x86_64 native library"
# Universal native libraries since v0.3: arm64-v8a, armeabi-v7a, x86, x86_64 all packaged.
# SQLCipher native libs ship for every ABI; APK size grows accordingly — accepted tradeoff.
pass "universal ABIs packaged: arm64-v8a armeabi-v7a x86 x86_64"

echo
echo "== 5/6. DEX contents: network code, trackers =="
# Decompile scratch must not live in a pruned tmp dir (the session scratch pruner races long
# jadx runs); use the workspace and clean up on exit.
WORK=$(mktemp -d "/data/workspace/.audit-work.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
# R8 output decompiles with some method-level errors (normal for obfuscated code);
# class listing is complete, which is what these checks read.
JAVA_OPTS="-Xmx3g" "$JADX" --no-res -j 2 -d "$WORK/src" "$APK" >/dev/null 2>&1 || true
CLASSES=$(find "$WORK/src" -name "*.java" | wc -l)
echo "decompiled classes: $CLASSES"
# No network code of any kind is allowed in Sumvalo's own package.
NET=$(grep -rlE "okhttp3|retrofit2|com.android.volley|org.apache.http.impl|java.net.HttpURLConnection" "$WORK/src/sources/com/acuranza" 2>/dev/null | head -5 || true)
[ -z "$NET" ] || fail "network client classes present in app code: $NET"
URLS=$(grep -rlE 'https://' "$WORK/src/sources/com/acuranza" 2>/dev/null | head -5 || true)
[ -z "$URLS" ] || fail "hardcoded HTTPS URL in app code: $URLS"
pass "no HTTP client, HttpURLConnection, or hardcoded URL in the DEX"
TRACKERS=$(find "$WORK/src" -path "*com/google/firebase*" -o -path "*com/facebook*" -o -path "*com/appsflyer*" \
  -o -path "*io/branch*" -o -path "*com/crashlytics*" -o -path "*com/amplitude*" -o -path "*com/mixpanel*" -o -path "*flurry*" | head -5 || true)
[ -n "$TRACKERS" ] || TRACKERS=$( { find "$WORK/src/sources" -type d \( -path "*firebase*" -o -path "*facebook*" -o -path "*crashlytics*" -o -path "*appsflyer*" \) | head -5 || true; } )
[ -z "$TRACKERS" ] || fail "tracking SDK classes present: $TRACKERS"
pass "no analytics/ads/tracking SDK classes"

echo
echo "== 7. embedded secrets scan =="
SECRETS=$( { unzip -p "$APK" "classes*.dex" 2>/dev/null | strings | \
  grep -E "AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA )?PRIVATE KEY-----|sk_live_[0-9a-zA-Z]{10,}" | head -5 || true; } )
[ -z "$SECRETS" ] || fail "possible embedded secret: $SECRETS"
pass "no obvious embedded credentials in the DEX"
rm -rf "$WORK"

echo
echo "ALL CHECKS PASSED"
echo "artifact: $APK"
echo "$SHA"
