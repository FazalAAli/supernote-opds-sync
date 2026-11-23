# Supernote OPDS Sync

An Android app for Supernote e-ink tablets that downloads the books in an
[OPDS](https://specs.opds.io/opds-1.2) catalog into `Document/`. Once the books are there,
they show up in the Supernote's own library.

It works with Bindery, Calibre-Web, Kavita, Komga and any other server that serves an OPDS 1.x
acquisition feed.

Tested on a Supernote Manta (Chauvet firmware, Android 11).

## How it works

- You give the app one acquisition feed URL, such as a "recently added" or "all books" feed.
  It follows `rel="next"` until it has read every page.
- For each entry it downloads the EPUB, or the PDF if there is no EPUB. Entries in other formats are skipped.
- Each entry `<id>` is downloaded once. If you delete a book on the device, it isn't downloaded again.
- Each book is written to a `.part` file and renamed only when the download completes, so a
  failed sync never leaves a half-written book in your library.
- Sync runs every N hours (WorkManager) and from the "Sync now" button. If the server can't
  be reached, the run is skipped until the next interval.

## Setup

1. Install the APK: `adb install app-debug.apk`.
2. Open **OPDS Sync** and allow storage access when asked. The app needs it to write to `Document/`.
3. Fill in the settings:
   - **Feed URL:** must be `https://`.
   - **Username / password:** optional, sent with HTTP Basic auth.
   - **API key header name / API key:** optional, sent as an extra header on every request.
     For Bindery, use `X-Api-Key` and your Bindery API key.
   - **Folder:** relative to the storage root. The default is `Document/Books`.
   - **Interval:** in hours. `0` turns off periodic sync.
4. Tap **Save**, then **Sync now**.

## Build

You need Nix with flakes enabled:

```sh
nix develop
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Without Nix, you need JDK 17 and an Android SDK with platform 35. Then run `./gradlew assembleDebug`.

## Notes

- Some servers behind a TLS proxy emit `http://` links to their own host. When the feed itself
  was fetched over https, the app upgrades those links to https. It never sends credentials
  over cleartext.
- The password is stored in app-private storage without encryption.

## License

MIT
