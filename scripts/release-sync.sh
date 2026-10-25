#!/usr/bin/env bash
# Sumvalo release sync — publishes a finished release to GitHub.
#
# Run AFTER a release has been built, signed and has passed the test suite and the security
# audit (scripts/security-audit.sh). The script:
#   1. verifies the branch and the working tree are the release state,
#   2. stages every tracked change — added, edited, renamed and deleted — honouring .gitignore,
#      and refuses to sync anything that looks like a secret or a build artifact,
#   3. commits with the release version if there is anything to commit,
#   4. reconciles with the remote with fetch + rebase (never merges over, never force-pushes),
#   5. pushes the branch and records version, commit hash and result to the sync log.
#
# Usage: scripts/release-sync.sh [signed.apk...] [--tag]
#   --tag       also push the vX.Y.Z tag, which makes the GitHub Release workflow build and
#               attach the APK (needs the repo's SUMVALO_* secrets configured on GitHub).
#   APK path(s) default to both release variants when none given:
#     /data/workspace/Sumvalo-<version>.apk   (standard)
#     /data/workspace/Sumvalo-Full-<version>.apk  (full)
#   APKs are never pushed to git.
#
# Environment:
#   SYNC_REMOTE   remote to push to (default: origin) — used by tests, leave it alone otherwise.
#   SYNC_LOG      sync record file (default: /data/workspace/release-sync.log)
#
# The script uses the machine's ambient git credentials (credential helper or gh). It embeds no
# tokens, and any failure — auth, conflict, dirty tree — stops here for a human instead of
# forcing anything through.

set -euo pipefail

REMOTE="${SYNC_REMOTE:-origin}"
LOG="${SYNC_LOG:-/data/workspace/release-sync.log}"
REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts)"
[ -n "$VERSION" ] && [ -n "$VERSION_CODE" ] || { echo "SYNC-FAIL: cannot read version from app/build.gradle.kts"; exit 1; }

BRANCH="$(git branch --show-current)"
if [ "$BRANCH" != "main" ]; then
  echo "SYNC-FAIL: expected branch 'main', currently on '$BRANCH'"
  exit 1
fi

# The signing key, gradle secrets and build outputs must never travel.
for ignored in keystore/ local.properties app/build; do
  git check-ignore -q "$ignored" || { echo "SYNC-FAIL: '$ignored' is not gitignored"; exit 1; }
done

git add -A

# A last guard between us and an embarrassing push: nothing staged may be an artifact or secret.
if git diff --cached --name-only | grep -Ei '\.(apk|jks|keystore|aab)$|^keystore/|^local\.properties$|gradle\.properties$|RELEASE-KEY'; then
  echo "SYNC-FAIL: staged files look like artifacts or secrets — refusing to sync"
  exit 1
fi

if git diff --cached --quiet; then
  echo "Nothing new to commit."
else
  git -c user.name="Sumvalo Build" -c user.email="build@sumvalo.local" \
    commit -m "v$VERSION: release source sync (versionCode $VERSION_CODE)"
fi

# Reconcile first: rebase on top of the remote branch so we never overwrite remote commits.
# A branch that doesn't exist remotely yet (first sync) has nothing to reconcile; a conflict
# stops the sync for a human to resolve.
if GIT_TERMINAL_PROMPT=0 git ls-remote --exit-code --heads "$REMOTE" main >/dev/null 2>&1; then
  git fetch "$REMOTE" main
  if ! git rebase "$REMOTE/main"; then
    echo "SYNC-FAIL: rebase onto $REMOTE/main hit a conflict — resolve it and rerun"
    git rebase --abort 2>/dev/null || true
    exit 1
  fi
fi

# GIT_TERMINAL_PROMPT=0: with no credential helper this fails immediately instead of hanging.
if ! GIT_TERMINAL_PROMPT=0 git push -u "$REMOTE" main; then
  COMMIT="$(git rev-parse --short HEAD)"
  echo "v$VERSION (code $VERSION_CODE) commit=$COMMIT result=SYNC-FAIL push-failed $(date -u +%FT%TZ)" >> "$LOG"
  echo "SYNC-FAIL: push to $REMOTE failed — usually missing git credentials (gh auth login) or no network write access. Nothing was force-pushed."
  exit 1
fi

PUSH_TAG=no
APKS=()
for arg in "$@"; do
  case "$arg" in
    --tag) PUSH_TAG=yes ;;
    *) APKS+=("$arg") ;;
  esac
done

# When no APK is passed, default to both release variants
if [ ${#APKS[@]} -eq 0 ]; then
  APKS+=("/data/workspace/Sumvalo-${VERSION}.apk")
  APKS+=("/data/workspace/Sumvalo-Full-${VERSION}.apk")
fi

if [ "$PUSH_TAG" = "yes" ]; then
  if ! GIT_TERMINAL_PROMPT=0 git push "$REMOTE" "v$VERSION"; then
    echo "v$VERSION (code $VERSION_CODE) result=SYNC-FAIL tag-push-failed $(date -u +%FT%TZ)" >> "$LOG"
    echo "SYNC-FAIL: branch pushed, but tag v$VERSION could not be pushed."
    exit 1
  fi
fi

COMMIT="$(git rev-parse HEAD)"

# Publish the signed APK as a GitHub release for this version. The release notes come from the
# version's fastlane changelog (what changed in this release); an existing release gets its
# asset refreshed so re-syncing is safe.
GITHUB_REPO="${GITHUB_REPO:-itsmeayusharyan/sumvalo}"
TAG="v$VERSION"
NOTES="$(mktemp)"
CHANGELOG="fastlane/metadata/android/en-US/changelogs/$VERSION_CODE.txt"
if [ -s "$CHANGELOG" ]; then
  cat "$CHANGELOG" > "$NOTES"
else
  echo "See the commit history for what changed in this release." > "$NOTES"
fi
if [ ${#APKS[@]} -gt 0 ]; then
  for apk in "${APKS[@]}"; do
    if [ ! -f "$apk" ]; then
      echo "release=$TAG result=RELEASE-SKIPPED apk-not-found $(basename "$apk") $(date -u +%FT%TZ)" >> "$LOG"
      echo "NOTE: APK not found, skipping: $apk"
      continue
    fi
    if gh release view "$TAG" -R "$GITHUB_REPO" >/dev/null 2>&1; then
      gh release edit "$TAG" -R "$GITHUB_REPO" --notes-file "$NOTES"
      gh release upload "$TAG" -R "$GITHUB_REPO" "$apk" --clobber
    else
      gh release create "$TAG" -R "$GITHUB_REPO" "$apk" \
        --title "Sumvalo v$VERSION" --notes-file "$NOTES" --target "$COMMIT"
    fi
    echo "release=$TAG apk=$(basename "$apk") result=RELEASE-UPLOADED $(date -u +%FT%TZ)" >> "$LOG"
  done
else
  echo "release=$TAG result=RELEASE-SKIPPED no-apk $(date -u +%FT%TZ)" >> "$LOG"
  echo "NOTE: no signed APK passed in, so no GitHub release was created."
fi
rm -f "$NOTES"

# Run APK retention policy after publishing
if command -v gh &>/dev/null && gh auth status &>/dev/null 2>&1; then
  echo "Running APK retention policy..."
  bash scripts/release-retention.sh || echo "WARNING: APK retention cleanup had errors"
else
  echo "Skipping APK retention: gh CLI not authenticated"
fi

echo "v$VERSION (code $VERSION_CODE) commit=$COMMIT apks=${APKS[*]} result=PUSHED remote=$REMOTE $(date -u +%FT%TZ)" >> "$LOG"
echo "SYNC-OK: v$VERSION (code $VERSION_CODE) pushed to $REMOTE/main at $COMMIT"
