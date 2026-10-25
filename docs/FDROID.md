# Publishing Sumvalo on F-Droid

Sumvalo has no proprietary dependencies (no Google Play Services, Firebase or trackers), so it qualifies for
the main F-Droid repository. F-Droid builds the app from source and signs it with its own key.

## Before you submit

1. **Make the GitHub repository public** (the origin remote: `gh repo edit --visibility public` from
   the repo root). F-Droid can only build from public source.
2. **Release first**: bumping `versionName`/`versionCode` on main makes the Release workflow tag `v<versionName>`
   and attach `Sumvalo.apk`. Write a fresh F-Droid metadata file for `com.acuranza.app` at that version. After inclusion F-Droid picks up new tags on its own.
3. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for that release. F-Droid shows the
   title, descriptions, icon and screenshots from `fastlane/metadata/android/en-US/`.

## Submitting

1. Create an account on [GitLab](https://gitlab.com) and fork
   [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. In your fork, add `metadata/com.acuranza.app.yml` (copy from this repository's `metadata/` folder).
   3. Optional local check (needs Docker or Linux):
   `fdroid readmeta && fdroid lint com.acuranza.app && fdroid build -v -l com.acuranza.app`.
4. Commit with the message `New app: Sumvalo` and open a merge request against `fdroid/fdroiddata`.
   Fill in the template; mention that the app reads SMS to import bank transactions and that all
   processing is on-device.
5. Reviewers may ask questions in the merge request; answer them there. After it's merged, the app appears
   in F-Droid within a few days, and new tags are picked up automatically (`UpdateCheckMode: Tags`).

## Notes

- **Signing (reproducible builds):** the recipes set `Binaries` (the APK on the GitHub release) and
  `AllowedAPKSigningKeys` (the Sumvalo release key). F-Droid builds the tag from source, checks that its unsigned
  APK is identical to the release APK, and then publishes the release APK with the Sumvalo signature. F-Droid and
  GitHub updates therefore install over each other. If a rebuild ever differs, F-Droid publishes nothing for
  that version, so check before tagging:

  ```
  ./gradlew :app:assembleStandardRelease        # without keystore/ → unsigned APK
  apksigcopier compare Sumvalo.apk --unsigned app/build/outputs/apk/standard/release/app-standard-release-unsigned.apk
  ```

  Builds with JDK 17 and 21 produce the same APK. Native libraries are never stripped (`keepDebugSymbols`), so
  the NDK on the build machine doesn't matter. Release builds skip the Gradle build cache (`--no-build-cache`):
  a cached Kotlin compile can differ from a clean one (Compose stability markers), and F-Droid always builds
  clean.
- **Anti-features:** none expected. Sumvalo has no network permission at all — nothing in it can
  reach the internet, so there is nothing to disclose and nothing to opt out of.
