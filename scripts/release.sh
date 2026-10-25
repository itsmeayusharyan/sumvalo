#!/usr/bin/env bash
# Sumvalo release pipeline — the one command that ships a release.
#
#   scripts/release.sh
#
# Runs the whole gate in order and stops at the first failure:
#   1. unit tests                          (./gradlew :app:testStandardDebugUnitTest)
#   2. signed release build                (:app:assembleStandardRelease with the configured keystore)
#   The build has two capability flavors: "standard" (the sideloaded release — no SMS permission,
#   no listener) and "full" (automatic SMS capture restored; for Play distribution or a knowing
#   sideload that accepts Play Protect's warning). This script ships STANDARD.
#   3. security audit of the signed APK    (scripts/security-audit.sh — 11 checks, non-zero on findings)
#   4. artifact copy + hash                (/data/workspace/Sumvalo-<version>.apk)
#
# Releasing does NOT touch GitHub — run !sync (or scripts/release-sync.sh) when you want the
# source pushed. A release that fails anywhere reports and stops.

set -euo pipefail

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

export ANDROID_HOME="${ANDROID_HOME:-/data/workspace/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JADX="${JADX:-/data/workspace/jadx/bin/jadx}"

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts)"
[ -n "$VERSION" ] && [ -n "$VERSION_CODE" ] || { echo "RELEASE-FAIL: cannot read version"; exit 1; }
echo "== Release v$VERSION (code $VERSION_CODE) =="

echo "== 1/5 unit tests =="
./gradlew --no-daemon :app:testStandardDebugUnitTest --console=plain

echo "== 2/5 signed release build =="
./gradlew --no-daemon :app:assembleStandardRelease --console=plain
APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "RELEASE-FAIL: no APK at $APK"; exit 1; }

echo "== 3/5 security audit of the signed APK =="
AUDIT_LOG="/data/workspace/release-audit-v$VERSION.log"
bash scripts/security-audit.sh "$APK" > "$AUDIT_LOG" 2>&1 || { tail -5 "$AUDIT_LOG"; echo "RELEASE-FAIL: audit findings — see $AUDIT_LOG"; exit 1; }
grep -q "ALL CHECKS PASSED" "$AUDIT_LOG" || { echo "RELEASE-FAIL: audit incomplete — see $AUDIT_LOG"; exit 1; }

echo "== 4/4 artifact =="
ARTIFACT="/data/workspace/Sumvalo-$VERSION.apk"
cp "$APK" "$ARTIFACT"
SHA="$(sha256sum "$ARTIFACT" | awk '{print $1}')"
echo "artifact=$ARTIFACT sha256=$SHA"

echo "RELEASE-OK: v$VERSION (code $VERSION_CODE) tested, signed, audited ($SHA)."
echo "GitHub not touched — run !sync (or scripts/release-sync.sh) to push the source."
