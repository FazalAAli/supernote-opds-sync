# Supernote OPDS Sync

An Android app for Supernote e-ink tablets that downloads the books in an
[OPDS](https://specs.opds.io/opds-1.2) catalog into `Document/`. Once the books are there,
they show up in the Supernote's own library.

It works with Bindery, Calibre-Web, Kavita, Komga and any other server that serves an OPDS 1.x
acquisition feed.

Tested on a Supernote Manta (Chauvet firmware, Android 11).

## Demo

https://github.com/user-attachments/assets/442a7edc-98be-41b7-817a-57aaa8f11dba

## Installation

You need your Supernote, a computer (Windows, Mac or Linux), and a USB cable.

### 1. Allow apps on your Supernote

On the Supernote, open **Settings > Security and Privacy** and turn on **Sideloading**.
Read the warning and accept it.

### 2. Get the install tool on your computer

The app is installed with a small, free tool from Google called `adb`.

- **Windows:** open Terminal and run `winget install --id=Google.PlatformTools -e`.
- **Mac:** install [Homebrew](https://brew.sh), then run `brew install android-platform-tools`.
- **Linux:** install `adb` (often in a package called `android-tools`) from your package manager.

### 3. Download the app

Open the [latest release](https://github.com/FazalAAli/supernote-opds-sync/releases/latest)
and download the `.apk` file listed under **Assets**.

### 4. Install it

1. Plug the Supernote into your computer. If the Supernote asks whether to allow USB
   debugging, tap **Allow**.
2. In a terminal, run `adb devices`. You should see one line ending in `device`. If it says
   `unauthorized`, look for the prompt on the Supernote's screen.
3. Type `adb install ` (with a space at the end), drag the downloaded `.apk` file into the
   terminal window to fill in its path, and press Enter. It should print `Success`.

You can unplug the Supernote now.

### 5. Set it up

1. Open **OPDS Sync** on the Supernote and allow storage access when asked. The app needs it
   to save books.
2. Fill in the settings:
   - **Feed URL:** the OPDS link from your book server. It must start with `https://`.
     Pick a feed that lists books, such as "recently added" or "all books".
   - **Username / password:** only if your server asks you to log in.
   - **API key header name / API key:** only if your server uses an API key. For Bindery,
     use `X-Api-Key` and your Bindery API key.
   - **Folder:** where books are saved. The default, `Document/Books`, shows up in the
     Supernote library.
   - **Sync every N hours:** how often to check for new books. `0` turns automatic sync off.
3. Tap **Save**, then **Sync now**. The status line at the bottom shows how many new books
   were downloaded.

If you delete a book on the Supernote, it won't be downloaded again.

### Updating

Download the new `.apk` and repeat step 4. Your settings and sync history are kept.

## Development

### How it works

- You give the app one acquisition feed URL, such as a "recently added" or "all books" feed.
  It follows `rel="next"` until it has read every page.
- For each entry it downloads the EPUB, or the PDF if there is no EPUB. Entries in other formats are skipped.
- Each entry `<id>` is downloaded once. If you delete a book on the device, it isn't downloaded again.
- Each book is written to a `.part` file and renamed only when the download completes, so a
  failed sync never leaves a half-written book in your library.
- Sync runs every N hours (WorkManager) and from the "Sync now" button. If the server can't
  be reached, the run is skipped until the next interval.

### Build

You need Nix with flakes enabled:

```sh
nix develop
gradle testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Nix is required. The Gradle version comes from `flake.lock`. AGP 8.11.1 needs Gradle 8.13 or newer, so check compatibility when running `nix flake update`.

Debug and release builds are signed with different keys. Switching between them means
uninstalling first, which wipes the app's settings and sync history.

### Notes

- Some servers behind a TLS proxy emit `http://` links to their own host. When the feed itself
  was fetched over https, the app upgrades those links to https. It never sends credentials
  over cleartext.
- The password is stored in app-private storage without encryption.

## License

MIT
