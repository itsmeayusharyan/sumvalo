# Distributing Sumvalo on F-Droid

F-Droid is the open-source app store: it builds the app **from this public source**, signs it with
its own key, and users install through the F-Droid client. This document is the recipe.

## What F-Droid needs

1. **Public, buildable source** — ✅ github.com/itsmeayusharyan/Sumvalo (GPL-3.0).
2. **A build recipe** in the fdroiddata repo (`metadata/com.acuranza.app.yml`) — draft below.
3. **An inclusion request**: open a merge request at gitlab.com/fdroid/fdroiddata adding the
   metadata file (a *Draft:* merge request is enough to start the process; maintainers review).

## Draft metadata (fdroiddata format)

```yaml
Categories:
  - Money
License: GPL-3.0-only
SourceCode: https://github.com/itsmeayusharyan/Sumvalo
IssueTracker: https://github.com/itsmeayusharyan/Sumvalo/issues

AutoName: Sumvalo
Summary: Offline personal finance tracker with bank-SMS import
Description: |-
    Fully offline personal finance app: transactions, budgets, analytics and reports
    computed on the device. Optional SMS transaction tracking — in the standard
    build you share/paste bank messages; in the full build the app reads bank SMS
    and captures notifications, with everything stored in an encrypted on-device
    database and reviewed before importing. No network permission at all.

RepoType: git
Repo: https://github.com/itsmeayusharyan/Sumvalo

Builds:
  - versionName: '1.13.2'
    versionCode: 31
    commit: v1.13.2
    gradle:
      - standard
    scanignore:
      # none — the repo carries no binaries that need scanning

MaintainerNotes: |-
    Two flavors exist. The "standard" flavor carries no SMS permission; the
    "full" flavor (gradle: full) adds READ_SMS and a notification listener for
    automatic SMS tracking. F-Droid policy allows SMS permissions for
    functionality that is core, optional and clearly disclosed — submit the
    STANDARD flavor first (zero sensitive permissions) and discuss the full
    flavor with maintainers if desired.

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '1.13.2'
CurrentVersionCode: 31
```

## The Play Protect caveat, stated honestly

F-Droid installs go through Android's package installer like any non-Play install. For the
**standard** flavor this is a non-event — no sensitive permissions, nothing to flag. For the
**full** flavor, Play Protect's reaction to an SMS-permission APK installed from F-Droid varies
by device and Play Protect version; some installs pass, some show the same "sensitive data"
warning sideloading does. Nobody can promise a silent install for a full-flavor SMS build
outside Google Play. The submission above therefore proposes the standard flavor as the F-Droid
default.
