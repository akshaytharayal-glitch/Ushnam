# Glad

A free, ad-free Android app that puts wallpapers, anime tracking, a movie diary, local music, AI text tools, offline games and home-screen widgets in one place.

- **Platform:** native Android (Kotlin, Jetpack Compose, Material 3), min SDK 26 (Android 8.0)
- **Storage:** Room and DataStore, all on-device. No accounts, no ads, no analytics.
- **Download site:** `https://akshaytharayal-glitch.github.io/Ushnam/` (GitHub Pages, from `/docs`)

## Sections

| Section | What it does | Network |
|---|---|---|
| Home | Dashboard counts, continue watching, recently used, shortcuts | Offline |
| Wallpapers | Online photos (Lorem Picsum), 16 built-in gradients, favourites, save to gallery, set as home/lock | Gradients offline |
| Anime | Top and airing feeds, search, details via the [Jikan API](https://jikan.moe); local list with episode progress and scores | List offline |
| Movies | Watchlist, watched history, 1–5 star ratings, search and sort | Offline |
| AI Hub | Chat, rewrite, summarize, translate, ideas; offline text counter | Counter offline |
| Games | 2048, Memory Match, Tic-Tac-Toe (easy or unbeatable) with saved records | Offline |
| Music | Local audio library, playlists, shuffle/repeat, background playback with media notification | Offline |
| Widgets | "Glad Today" (clock + stats) and "Glad Shortcuts" (4 launchers) | Offline |
| Appearance | Dark/light/system, 8 accents, pure-black mode, Material You colours | Offline |

Network features show clear offline and error states with a retry button.

### AI Hub and keys

No API key is bundled. By default AI Hub uses the free public Pollinations text endpoint (no account). In **AI Hub → Settings** you can switch to any OpenAI-compatible HTTPS endpoint with your own key. The key is stored only on the device and is sent only to the endpoint you entered.

## Repository layout

```
app/                  Android application
docs/                 GitHub Pages download website
.github/workflows/    build-apk.yml · release.yml · pages.yml
```

## Build locally

Requires JDK 17, the Android SDK (platform 35) and Gradle 8.9+.

```bash
gradle :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

A Gradle wrapper is not committed yet. In Android Studio, open the project and let it sync, or run `gradle wrapper --gradle-version 8.9` once and commit the generated files.

## GitHub Actions

| Workflow | Runs on | Result |
|---|---|---|
| **Build APK** | Pushes to `main` that touch app or Gradle files, pull requests, manual run | Debug and release APKs as a downloadable workflow artifact |
| **Release APK** | Pushing a tag like `v1.0.0`, or manual run | Builds `Glad.apk` and publishes it to GitHub Releases (manual runs only upload an artifact) |
| **Deploy website** | Pushes touching `docs/`, a published release, manual run | Deploys `/docs` to GitHub Pages |

### One-time repository setup

1. **Pages:** Settings → Pages → Source: **GitHub Actions**.
2. **Actions permissions:** Settings → Actions → General → Workflow permissions: **Read and write** (needed to create releases).

### Publishing a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The version name comes from the tag; the version code is the workflow run number. The website reads the latest release through the GitHub API and links its download button to that release's real APK. Until a release exists the button opens the releases page instead.

### Release signing

Without secrets, release APKs are signed with the **debug key**. They install and run, and the release title says **(debug-signed)**, but they can only be updated by builds signed with the same debug key.

For a permanent signing key, create a keystore once:

```bash
keytool -genkeypair -v -keystore glad-release.jks -alias glad -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 glad-release.jks     # macOS: base64 -i glad-release.jks
```

Add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Output of the `base64` command |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (e.g. `glad`) |
| `ANDROID_KEY_PASSWORD` | Key password |

Keep a private backup of the keystore: losing it means users must uninstall to update. Never commit it (`*.jks` and `*.keystore` are git-ignored).

## Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Anime, online wallpapers, AI |
| `SET_WALLPAPER` | Apply wallpapers |
| `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (≤12) | Read your music, requested when you open Music |
| `WRITE_EXTERNAL_STORAGE` (Android 9 and older) | Save wallpapers to Pictures/Glad |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Playback controls while the screen is off |

## Third-party services

[Jikan](https://jikan.moe) (anime data), [Lorem Picsum](https://picsum.photos) (photos), [Pollinations](https://pollinations.ai) (default AI endpoint). All are free and keyless; any of them can be unavailable or rate-limited, and the app says so instead of failing silently.
