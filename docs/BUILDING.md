# Building and releasing Sumvalo

Sumvalo builds with the standard Android toolchain: JDK 17+ (21 tested), the Android SDK with
`platforms;android-35` and `build-tools;35.0.0`, and the Gradle wrapper (Gradle 8.9). Point the SDK
out either way Gradle expects:

```bash
echo "sdk.dir=/opt/android-sdk" > local.properties   # or export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
```

## Debug build

```bash
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Release build and signing

The signing configuration reads `keystore/sumvalo-release.jks` and two Gradle properties,
`SUMVALO_STORE_PASSWORD` and `SUMVALO_KEY_PASSWORD`, which live in `~/.gradle/gradle.properties`
on the build machine — never in the repository.

One-time keystore setup (keep the file and the passwords safe; a lost key means a new app identity):

```bash
keytool -genkeypair -v \
  -keystore keystore/sumvalo-release.jks -alias sumvalo \
  -keyalg RSA -keysize 4096 -validity 10000
# then add to ~/.gradle/gradle.properties:
#   SUMVALO_STORE_PASSWORD=...
#   SUMVALO_KEY_PASSWORD=...
```

Build, verify and hash:

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk

$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app-release.apk
sha256sum app-release.apk
```

The release APK is minified (R8) and resource-shrunk. A debug build never signs with the release
key, so a debuggable APK can never install over the real app.

## Universal APK

Sumvalo ships a single universal APK for all supported ABIs. The dependency set is pure JVM/Kotlin
(SQLCipher's native library is packaged for every architecture it ships: arm64-v8a, armeabi-v7a and
x86_64), so no `abiFilters` or splits are configured — nothing is excluded. Minimum SDK 26
(Android 8.0), target/compile SDK 35.

## Release checklist

Run against the signed artifact, not the source tree:

1. **Permissions** — `aapt2 dump permissions` shows no INTERNET and no other network permission.
2. **Manifest** — no exported component beyond the launcher activity, the SMS receiver (guarded by
   `BROADCAST_SMS`) and the widget/tile components; `debuggable` is false; `allowBackup` is false.
3. **Signing** — `apksigner verify` names the Sumvalo release certificate.
4. **Network code** — the APK contains no HTTP client classes (no OkHttp, no `java.net.URL`
   usage in app code; see the audit report for the class-level scan).
5. **Trackers** — no analytics/ads SDK classes in the DEX.
6. **Data** — no secrets, keystore files or passwords in the APK.
7. **Hash** — record the SHA-256 of the exact artifact alongside the release notes.

CI (`.github/workflows/ci.yml`) runs the unit tests and the INTERNET-permission check on every
push; `release.yml` builds and signs on a tag and runs the same checks before publishing.

## APK Retention Policy

To minimize disk usage while retaining recent builds for recovery, Sumvalo applies an APK retention
policy to GitHub releases:

- **Retain:** current release + 2 most recent previous stable releases (3 total)
- **Delete:** APK assets (`.apk` files) from releases beyond the retention window
- **Version ordering:** semantic version (e.g., `1.10` > `1.9`), not upload date
- **Variants:** both `Sumvalo-{version}.apk` and `Sumvalo-Full-{version}.apk` are retained/deleted together
- **Prereleases:** excluded from stable retention
- **Safety:** only APK assets are deleted — never source code, tags, commits, release notes, or checksums
- **Idempotent:** rerunning causes no additional deletions
- **Automation:** runs after every successful release via `scripts/release-retention.sh` and the GitHub Actions release workflow

Example retention:

| New release | APK versions retained |
|-------------|----------------------|
| 1.5 | 1.4 and 1.3 |
| 1.6 | 1.5 and 1.4 |
| 1.7 | 1.6 and 1.5 |
| 1.10 | 1.9 and 1.8 |
| 2.0 | 1.9 and 1.8 |

Run manually (dry-run first to verify):

```bash
DRY_RUN=1 ./scripts/release-retention.sh
./scripts/release-retention.sh
```

## Publishing the source

The build has two capability variants. **standard** is the sideloaded release: no SMS
permission, no notification listener — Play Protect installs it like any app. **full** restores
automatic SMS capture (historical inbox import + notification tracking) for Play distribution
with the declared money-management exception, or a knowing sideload that accepts Play Protect's
warning; see docs/PLAY-SUBMISSION.md. The gate ships standard:

```bash
scripts/release.sh
```

It stops at the first failed test or audit finding. Releasing does not touch GitHub: push the
source when you choose, with `!sync` in chat or:

```bash
scripts/release-sync.sh app/build/outputs/apk/release/app-release.apk
```

The sync also publishes the signed APK as a GitHub release — `v<version>` with the version's
fastlane changelog as the notes and the APK attached for download — so every version you
release is downloadable from the repository's Releases page. Re-running it refreshes the
existing release instead of duplicating it.

It stages every tracked change (added, edited, renamed, deleted — `.gitignore` keeps build
outputs, the keystore and local config out), commits with the release version, rebases onto the
remote branch so remote commits are never overwritten, and pushes. Add `--tag` to also push the
`vX.Y.Z` tag, which makes the GitHub Release workflow build and attach the APK. Every run appends
the version, commit hash and result to `/data/workspace/release-sync.log`. It uses the machine's
ambient git credentials (`gh auth login` once); tokens are never embedded, and an authentication
failure, rebase conflict or refused artifact stops the run instead of force-pushing.
