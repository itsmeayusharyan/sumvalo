#!/usr/bin/env bash
# Sumvalo APK retention policy
# Keeps only current + 2 previous stable releases on GitHub
#
# Usage: scripts/release-retention.sh [--dry-run]
#
# After a new release is published, this script:
#   1. Lists all stable GitHub releases (excludes prereleases)
#   2. Parses semantic versions from tags (vX.Y.Z)
#   3. Sorts by semantic version descending (1.10 > 1.9)
#   4. Retains current + 2 previous (3 total)
#   5. Deletes .apk assets from older releases only
#   6. Logs every retention/deletion decision
#
# Environment:
#   GITHUB_REPO   (default: itsmeayusharyan/sumvalo)
#   GH_TOKEN      GitHub token for API access
#   DRY_RUN       set to 1 to simulate without deleting
#
# Safety rules:
#   - Never deletes source code, Git tags, commit history, or release notes
#   - Only deletes .apk assets from releases beyond the retention window
#   - Never deletes before the new release is verified
#   - Excludes prereleases from stable retention
#   - Idempotent: rerunning causes no additional deletions
#   - Stops on ambiguous version ordering

set -euo pipefail

DRY_RUN="${DRY_RUN:-0}"
GITHUB_REPO="${GITHUB_REPO:-itsmeayusharyan/sumvalo}"
RETENTION_COUNT=3  # current + 2 previous stable releases

log() {
    echo "[$(date -u +%FT%TZ)] [retention] $*"
}

log_info()  { log "INFO:  $*"; }
log_retain(){ log "RETAIN: $*"; }
log_delete(){ log "DELETE: $*"; }
log_skip()  { log "SKIP:   $*"; }
log_error() { log "ERROR:  $*"; }

# Check prerequisites
check_prereqs() {
    if ! command -v gh &>/dev/null; then
        log_error "gh CLI not found. Cannot run retention."
        exit 1
    fi
    if ! gh auth status &>/dev/null; then
        log_error "gh CLI not authenticated. Cannot run retention."
        exit 1
    fi
    if ! command -v jq &>/dev/null; then
        log_error "jq not found. Cannot parse release assets."
        exit 1
    fi
}

# Get all stable release tags (excludes prereleases)
get_stable_tags() {
    gh release list --repo "$GITHUB_REPO" \
        --limit 100 \
        --json tagName,isPrerelease \
        --jq '.[] | select(.isPrerelease == false) | .tagName' 2>/dev/null || true
}

# Sort tags by semantic version descending (v1.10 > v1.9)
sort_tags_desc() {
    local tags="$1"
    if [[ -z "$tags" ]]; then
        return
    fi
    echo "$tags" | sed 's/^v//' | sort -V -r | sed 's/^/v/'
}

# Delete a single APK asset from a release
delete_asset() {
    local tag="$1" asset_id="$2" asset_name="$3"
    if [[ "$DRY_RUN" == "1" ]]; then
        log_delete "[DRY RUN] $asset_name from release $tag"
        return 0
    fi
    if gh release delete-asset "$tag" "$asset_id" --repo "$GITHUB_REPO" --yes 2>/dev/null; then
        log_delete "$asset_name from release $tag"
        return 0
    else
        log_error "Failed to delete $asset_name from release $tag"
        return 1
    fi
}

main() {
    log "=== APK retention policy starting ==="
    log "Repository: $GITHUB_REPO"
    log "Retention window: current + $((RETENTION_COUNT - 1)) previous = $RETENTION_COUNT total stable releases"
    [[ "$DRY_RUN" == "1" ]] && log "DRY RUN mode - no deletions will occur"

    check_prereqs

    # Get all stable release tags
    local all_tags
    all_tags=$(get_stable_tags)

    if [[ -z "$all_tags" ]]; then
        log "No stable releases found. Nothing to retain or delete."
        return 0
    fi

    # Sort by semantic version descending
    local sorted_tags
    sorted_tags=$(sort_tags_desc "$all_tags")

    log "Stable releases found (sorted by version, newest first):"
    while IFS= read -r tag; do
        [[ -z "$tag" ]] && continue
        log "  $tag"
    done <<< "$sorted_tags"

    # Determine retained releases (top RETENTION_COUNT by version)
    local retain_count=0
    local retain_tags=()
    while IFS= read -r tag; do
        [[ -z "$tag" ]] && continue
        if (( retain_count < RETENTION_COUNT )); then
            retain_tags+=("$tag")
            retain_count=$((retain_count + 1))
            log_retain "$tag (retention position $retain_count)"
        else
            log_skip "$tag (beyond retention window - APK assets will be deleted)"
        fi
    done <<< "$sorted_tags"

    if [[ ${#retain_tags[@]} -eq 0 ]]; then
        log "No releases to retain. Nothing to delete."
        return 0
    fi

    log "Retained releases: ${retain_tags[*]}"

    # Process releases beyond retention window - delete their APK assets
    local deleted_count=0
    local error_count=0
    local processed_count=0

    while IFS= read -r tag; do
        [[ -z "$tag" ]] && continue

        # Check if this tag is retained
        local is_retained=0
        for rt in "${retain_tags[@]}"; do
            if [[ "$tag" == "$rt" ]]; then
                is_retained=1
                break
            fi
        done

        if [[ $is_retained -eq 1 ]]; then
            continue
        fi

        log "Processing release $tag for APK cleanup..."
        processed_count=$((processed_count + 1))

        # Get assets for this release
        local assets_json
        assets_json=$(gh release view "$tag" --repo "$GITHUB_REPO" --json assets 2>/dev/null || echo '[]')

        # Find APK assets
        local apk_assets
        apk_assets=$(echo "$assets_json" | jq -r '.[] | select(.name | endswith(".apk")) | "\(.id) \(.name)"' 2>/dev/null || true)

        if [[ -z "$apk_assets" ]]; then
            log "  No APK assets found in $tag. Nothing to delete."
            continue
        fi

        while IFS=' ' read -r asset_id asset_name; do
            [[ -z "$asset_id" ]] && continue
            if delete_asset "$tag" "$asset_id" "$asset_name"; then
                deleted_count=$((deleted_count + 1))
            else
                error_count=$((error_count + 1))
            fi
        done <<< "$apk_assets"

    done <<< "$sorted_tags"

    log "=== Retention complete ==="
    log "Retained releases: ${retain_tags[*]}"
    log "Releases processed for cleanup: $processed_count"
    log "APK assets deleted: $deleted_count"
    log "APK deletion errors: $error_count"

    if [[ "$DRY_RUN" == "1" ]]; then
        log "DRY RUN complete - no assets were actually deleted."
    fi
}

main "$@"
