# Board To Contact

Snap a shop board, save it as a contact. No typing.

![CI](https://github.com/pranvirsingh/BoardToContact/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/pranvirsingh/BoardToContact)
![License](https://img.shields.io/github/license/pranvirsingh/BoardToContact)

| Board says | App fills | You save |
|---|---|---|
| SHARMA GENERAL STORE, 98765 43210, Main Road | Name + phone + address, all editable | One tap into Contacts |

## How it works

1. Capture or pick a photo of the board.
2. On-device reading pulls out the shop name, phone numbers, and address.
3. Check the fields, tap Save — it opens Contacts prefilled, you confirm.

## Install

Download the APK from [Releases](https://github.com/pranvirsingh/BoardToContact/releases).

Made for delivery, sales, and field work — boards become contacts in seconds.

## Feels like a system tweak, not an app

- Lives in the **Quick Settings** shade as a tile.
- Optional **Hide icon** switch removes it from the launcher (it stays in Settings → Apps — Android allows nothing less without root).
- Shows its own **update box** when a newer version is out.

## Build

Android Studio → open this folder → Run. Or with Gradle + Android SDK:

```bash
gradle :app:assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Privacy

On-device reading, on-device parsing. Photos never upload anywhere. No
account, no analytics. The only network call is one check for a newer
release on GitHub each time the app opens.

## Docs

- [docs/why.md](docs/why.md) — why this exists
- [docs/status.md](docs/status.md) — what's shipped, what's next
- [docs/shipping.md](docs/shipping.md) — how versions ship

## License

MIT — see [LICENSE](LICENSE).
