# Play Store submission pack — SMS-based money management

Sumvalo ships two capability variants of the same release. This pack covers the **full** variant
(`Sumvalo-<version>-full.apk`), which requests `READ_SMS` for automatic bank-SMS transaction
import, and the Play Console steps needed to distribute it under Google Play's
[SMS/Call Log permissions policy](https://support.google.com/googleplay/android-developer/answer/10208820).

## Why Sumvalo qualifies

The policy's exception table lists **"SMS-based money management — for example, apps that track
and manage budget"** as a permitted use for `READ_SMS` (and companions). Sumvalo is exactly that:

- A personal-finance app whose core functionality is transaction tracking and budgeting; bank
  transaction SMS are the primary data Indian users have for automatic entry.
- The permission enables a **critical core feature** (automatic transaction import + historical
  inbox import); without it the app is still usable via manual entry, bank-statement CSV import,
  and share/paste capture — this is precisely why the standard variant exists.
- No alternative method provides the functionality: the SMS Retriever API only handles
  single OTP-like messages containing an app hash, which banks never send; notification access
  is restricted differently and is not a Play-permitted substitute for inbox history.

## Permissions declaration form (draft answers)

- **Permission:** `android.permission.READ_SMS` (SMS permission group).
- **Core feature it enables:** automatic detection and import of bank transaction messages
  (debits, credits, UPI, card payments, refunds, fees) into the user's local ledger, with a
  user-reviewed import flow.
- **User-facing disclosure (in-app, shown before the permission is requested):** the consent
  screen explains that messages are read only on the device, that only transaction fields
  (amount, date, merchant, reference, account digits) are stored, that message bodies are never
  saved or transmitted, and that OTP/PIN/authentication/promotional messages are never processed.
- **Data handling:** all processing is on-device. The app has **no INTERNET permission** and no
  network-capable dependency — verified by an automated DEX scan run against every release APK.
- **Alternatives implemented:** manual entry, bank-statement CSV import, Android share-sheet
  capture, paste capture — all shipped in the standard variant that carries no SMS permission.

## Data safety form (draft answers)

| Question | Answer |
|---|---|
| Data collected | None sent off device. Transaction fields derived from SMS are stored **locally only** (SQLCipher-encrypted database). |
| Data shared | No sharing with third parties; no SDKs; no ads; no analytics. |
| Encryption in transit | N/A — the app cannot make network connections (no INTERNET permission). |
| Deletion | In-app: delete individual imports, undo a batch, or "Delete all SMS data" — removes every SMS-derived record. |
| Optional vs required | The SMS permission is optional: the app is fully usable without it (standard flows). |

## Blocking prerequisites (must be done by the developer)

1. **Privacy policy URL** — DONE: https://itsmeayusharyan.github.io/acuranza-privacy/
   (public repo `itsmeayusharyan/acuranza-privacy`, GitHub Pages, verified HTTP 200; source of
   truth is docs/PRIVACY.md in the main repo — keep them in sync).
2. **Play Console account + listing** — store listing (fastlane metadata in
   `fastlane/metadata/android/en-US/` is the draft), content rating, target-audience answers.
3. **Permissions declaration form** — declare READ_SMS with the exception category above;
   review is case-by-case and Google may still decline.

## Engineering states verified on this repo

- Standard variant: 0 SMS permissions, 0 listener components — manifest shape identical to the
  builds that install cleanly on sideloaded devices.
- Full variant: `READ_SMS` + notification listener declared; identical signing key.
- Both variants build from one source tree; the standard build's SMS features are compile-time
  removed (`BuildConfig.SMS_FULL = false` → R8 strips the scanner/listener paths).
