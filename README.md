# Sumvalo

Offline-first personal finance for Android. Local-only, encrypted, no network permission. Your money data never leaves the phone.

Track income, expenses, credit cards, bills, budgets, and goals entirely on your device. Add transactions by hand, import bank-statement CSVs (HDFC, ICICI, SBI, Axis, Kotak and others), or share bank SMS messages — every insight is computed locally, never uploaded.

## Features

| Area | Capabilities |
| --- | --- |
| Home dashboard | Net worth card, this-month income / spent / saved, upcoming bills, quick-add actions, “safe to spend” pill that respects your salary cycle |
| Accounts & cards | Balances that reconcile with your bank, card outstanding and utilisation, mark-paid recording |
| Budgets & goals | Monthly budgets that follow your salary cycle with pace projection and rollover; savings goals with progress rings |
| Transactions | Add by hand or import bank-statement CSV; tags, splits, IOUs, encrypted receipts |
| Recurring & bills | Subscriptions, EMIs, UPI AutoPay / NACH mandates; due-date reminders with price-change alerts |
| Reports & insights | Monthly & yearly PDF reports, Beancount & Ledger export, year-in-review, daily spending calendar, spending spikes, savings rate |
| Everyday tools | Home-screen widgets, Quick Settings tile, haptic feedback (Light / Standard / Strong), default-landing-tab choice |
| Security | Biometric / PIN app lock with decoy-PIN mode, privacy mode that masks amounts, SQLCipher AES-256 database |

## Get it

Download signed release APKs from the [Releases page](https://github.com/itsmeayusharyan/sumvalo/releases/latest).

| Build | SMS permission | Best for |
| --- | --- | --- |
| Standard | Not requested | Sideloaded. No SMS permission, no notification listener — Play Protect installs it like any app. Share or paste bank messages instead. |
| Full | Requested | Automatic inbox scan and notification capture for `READ_SMS` money-management. May trigger Play Protect warnings on sideload — accepted by choice. |

Both variants share identical code, features, and signing key — upgrades install cleanly over previous versions.

Sumvalo targets Android 8.0+ (minSdk 26, targetSdk 35). The app requests no network permission — nothing can leave your phone.

## Privacy

Sumvalo holds no permission to access the network. It cannot upload, sync, or share your data — not with us, not with any server, not with any third party. Everything stays on your device, in an encrypted database.

- Encrypted at rest — SQLCipher database (AES-256), key held in the Android Keystore
- Backups — AES-256-GCM with PBKDF2 (600k iterations), password only you know; Android cloud backup disabled
- SMS is opt-in — standard build never reads your inbox; full build reads bank SMS only after explicit consent, message bodies parsed in memory and never stored
- App lock — biometric or PIN, with decoy-PIN mode; privacy mode masks amounts everywhere
- Local-only — database, preferences, and datastore excluded from cloud backup and device transfer

Full policy: [docs/PRIVACY.md](docs/PRIVACY.md)

## Security

- No servers, no analytics, no third-party SDKs
- No INTERNET permission — verified against every release artifact
- `allowBackup="false"`, not debuggable
- PendingIntent uses `FLAG_IMMUTABLE`
- Optional biometric / device-credential app lock that re-locks after 60 seconds in the background
- Screenshot and recents-preview blocking
- Lock-screen notifications hide amounts and merchants

See [SECURITY.md](SECURITY.md) for the full security policy and vulnerability reporting contact.

## License

Sumvalo is free software under the [GNU General Public License v3.0 or later](LICENSE). It derives from the upstream **Kosh** project; each file keeps its original copyright notice.

Built with Kotlin + Jetpack Compose · SQLCipher · Material 3 · Offline first.
