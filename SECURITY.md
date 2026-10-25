# Security policy

## Reporting a vulnerability

Please **don't open a public issue** for security problems. Email **jha.8@alumni.iitj.ac.in** with:

- what you found and how to reproduce it,
- the Sumvalo version and your Android version.

You'll get a reply within a few days. Once a fix is released, you'll be credited if you wish.

## Threat model (summary)

Sumvalo holds sensitive financial data, entered by the user and imported from bank-statement CSVs. It is designed so that
this data never leaves the phone.

**Protections**

- **No servers or accounts:** Sumvalo has no backend, no analytics and no third-party SDKs.
- **Self-update (opt-in):** the standard build holds `REQUEST_INSTALL_PACKAGES`, used only when
  the user picks "Install Full build from APK" under Settings → Financial tools. The picked file
  is verified to be Sumvalo, signed with Sumvalo's key, and not older than the installed build;
  the install then runs through Android's own confirmation dialog with all system security checks
  intact. The permission is inert until the user initiates the flow.
- **Network:** none. The APK requests no network permission, so no code path can reach the
  internet; this is verified against every release artifact.
- **Encryption at rest:** the database is encrypted with SQLCipher (AES-256). Its key is random, generated on
  the device and stored wrapped by a non-exportable AES-GCM key in the Android Keystore.
- **Backups:** user-initiated exports are encrypted with AES-256-GCM and a PBKDF2-SHA256 key (600,000
  iterations) from a password of at least 8 characters. Android cloud backup and device transfer are disabled
  for the database and settings.
- **On the device:** optional biometric/device-credential app lock that re-locks after 60 seconds in the
  background, screenshot and recents-preview blocking, privacy mode, and lock-screen notifications that hide
  amounts and merchants.
- **Messages:** the APK holds no SMS permission — Play Protect hard-blocks sideloaded APKs that
  request one (this is documented history for Sumvalo itself). SMS transaction tracking works
  permissionlessly: the user shares or pastes a bank message, which is parsed in memory and
  discarded; only the extracted transaction fields (amount, date, merchant, account, reference)
  plus a dedupe fingerprint are stored, and only after the user's explicit review. Authentication
  messages (OTP/PIN/password), promotions, balance-only alerts and ambiguous text are rejected
  before storage. Nothing is processed before consent, and nothing can leave the device (no
  network permission exists). The exported share activity (SmsShareActivity) reads only the
  Android share intent's text, writes only to the private encrypted database, and is a no-op
  without consent. Consent can be withdrawn and all SMS data deleted from the same screen.

**Out of scope**

- A device that is already compromised (rooted with malware, or unlocked and in an attacker's hands while
  Sumvalo is unlocked).
- Loss of the Android Keystore key (e.g. a factory reset): the encrypted database then can't be recovered.
  Keep an encrypted backup.
