# Sumvalo — Privacy Policy

*Last updated: 21 October 2026*

Sumvalo is an offline personal-finance app. This policy explains what the app does with your data.

## The short version

Sumvalo holds **no permission to access the network**. It cannot upload, sync, or share your data with anyone — not with us, not with any server, not with any third party. Everything you enter or import stays on your device, in an encrypted database.

## Data storage

- All data — transactions, accounts, categories, budgets, preferences — is stored **only on the device** in an encrypted SQLCipher database whose key is held in the Android Keystore.
- Backups are created by you, on your device's storage, and are optionally password-protected (encrypted with AES-256-GCM). They go wherever you put them; the app never transmits them.
- The app contains no analytics, no crash-reporting SDKs, no advertising, and no trackers.

## SMS transaction tracking (optional feature)

SMS tracking is off until you explicitly enable it in Settings → Financial tools, and it works in two modes:

- **Standard build (default):** you choose which bank messages Sumvalo sees — by sharing them from your Messages app, pasting them, or copying them (the app offers to parse the clipboard only while its tracking screen is open, and clears it after parsing). The app **never reads your inbox or your notifications** in this build and holds no permission to do so.
- **Full build (distributed via Google Play):** with your consent and the SMS permission, the app reads bank and payment messages on the device to detect transactions — including a one-time historical import and automatic detection of new messages. Processing is entirely on-device.

In both builds:

- Only transaction details are stored: amount, date, merchant, reference number, account/card last four digits, and the suggested category. **The message text itself is parsed in memory and never saved.**
- Messages that are not bank transactions are ignored and discarded immediately: one-time passwords (OTPs), PINs, passwords, authentication messages, promotions, personal conversations, and balance-only alerts.
- You can delete any imported record, undo an entire import batch, or delete all SMS data at once from Settings → Financial tools. Revoking consent stops all processing and deletes nothing without your say-so.

## Permissions

- Standard build: notifications (optional), biometrics (optional lock), vibration (haptic
  feedback; a normal permission with no prompt), and the system-level permissions Android
  needs for reminders to survive a restart (receive boot completed, wake lock, foreground
  service) and for installing an app update you deliberately chose to download from this
  repository's releases (request install packages). Nothing else.
- Full build: additionally `READ_SMS` for the SMS-based money-management feature described above, used only while the feature is enabled, on-device only.

## Contact

Sumvalo is a personal, open-source project. Questions about this policy can be raised in the project's GitHub repository.
