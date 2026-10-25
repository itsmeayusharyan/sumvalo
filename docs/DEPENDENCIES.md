# Dependency inventory — licenses and security review

Reviewed 21 October 2026 for v1.13.2 (versionCode 31). Direct dependencies only; every one is
pinned in `gradle/libs.versions.toml`.

| Dependency | Version | License | Notes |
|---|---|---|---|
| Kotlin stdlib / coroutines | 2.0.21 / 1.9.0 | Apache-2.0 | — |
| Compose BOM (UI, Material 3, icons-extended) | 2024.12.01 | Apache-2.0 (AndroidX) | — |
| Activity-, Lifecycle-, Navigation-Compose | 2.8.7 / 2.8.5 | Apache-2.0 | — |
| Room (runtime, ktx, compiler) | 2.6.1 | Apache-2.0 | No known CVEs at this version. |
| DataStore preferences | 1.1.1 | Apache-2.0 | — |
| SQLCipher for Android (community) | 4.6.1 | Zetetic (BSD-style, attribution required) | See below. |
| WorkManager | (BOM-managed) | Apache-2.0 | — |

## Security review findings

- **SQLCipher 4.6.1 / bundled OpenSSL components:** scanner tools report OpenSSL CVEs
  (CVE-2023-0464, CVE-2023-3446, CVE-2024-0727, CVE-2024-2511, CVE-2024-4741, CVE-2024-5535)
  against SQLCipher's statically linked OpenSSL. Zetetic's maintainers have confirmed on the
  record that these do not apply to `sqlcipher-android`: the vulnerable code paths
  (TLS, Diffie–Hellman key exchange, `SSL_free_buffers`) are not used by SQLCipher.
  (discuss.zetetic.net — "New vulnerabilities in openssl" threads.) Sumvalo additionally
  never passes network input to the database: the app holds **no INTERNET permission**, so the
  remote-exploitation surface for any library is zero by construction.
- **No network-capable dependencies:** verified per-release by the DEX scan in
  `scripts/security-audit.sh` (no HTTP client classes, no hardcoded URLs in app code,
  no analytics/ads/tracking SDKs).
- **No known CVEs** affect the pinned versions of Room 2.6.1, Compose BOM 2024.12.01,
  coroutines 1.9.0, DataStore 1.1.1 or WorkManager as of the review date.

## License obligations

- The project is GPL-3.0; all runtime dependencies are permissively licensed (Apache-2.0 or
  Zetetic BSD-style) and compatible with GPL distribution. SQLCipher's attribution requirement
  is satisfied by the in-app About → Licence sheet and the upstream attribution in README.md.
- No proprietary or AGPL-licensed components are used.
