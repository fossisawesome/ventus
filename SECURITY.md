# Security Policy

## Supported Versions

Only the latest release receives security fixes.

| Version | Supported |
| ------- | --------- |
| 1.x.x   | ✓         |

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Report vulnerabilities by emailing the maintainer directly or opening a [GitHub Security Advisory](https://github.com/fossisawesome/ventus/security/advisories/new) (private disclosure).

Include:
- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- Affected version(s)

You can expect an acknowledgement within 72 hours and a resolution timeline within 14 days for
confirmed issues.

## Security Design

### Location Data
- Location is resolved on-device via Google Play Services' fused location provider and is only
  ever sent to Open-Meteo (as bare latitude/longitude query parameters) to fetch a forecast.
- No location data, query history, or personal information is sent to any server operated by the
  app's developer — there is no backend.
- Coarse location permission (`ACCESS_COARSE_LOCATION`) is requested at first launch; the app is
  fully usable without granting it via manual city search.

### Network
- All network requests go to Open-Meteo's public forecast and geocoding APIs
  (`api.open-meteo.com`, `geocoding-api.open-meteo.com`) over HTTPS. No API key or account is
  involved.
- The app makes no other network calls and includes no analytics or crash-reporting SDKs.

### Local Storage
- Preferences (theme, font, units, saved location, cached forecast) are stored in Android
  DataStore, sandboxed to the app's private storage — not accessible to other apps without root.
- Imported `.toml` theme files are read from a user-picked file (via the system file picker) and
  copied into the app's private storage; a 50 KB size cap applies to prevent abuse.

## Out of Scope

- Vulnerabilities requiring physical access to the device
- Social engineering attacks
- Issues in Open-Meteo's own API or infrastructure (report those upstream)
