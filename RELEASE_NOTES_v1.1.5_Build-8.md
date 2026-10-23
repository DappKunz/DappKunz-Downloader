# Fluxen 1.1.5 Build 8

The app is now called Fluxen. Same downloader underneath — engines, queue, music player and the rest are untouched — so updating is all you need to do.

## What's new

- The launcher label and the About screen now say Fluxen. The icon is still the old one; a new logo will come separately.
- About now shows real people instead of a single name. The developer card opens my GitHub profile (DappKunz), and below it there is a maintainer card for me and n4zzuu, each with their GitHub profile photo and a tap that opens the profile.
- Update checks point to the repository's new address (DappKunz/DappKunz-Downloader), since my GitHub account was renamed.
- Downloads still go to the same folder, so files and playlists you already have stay where they are.

## Build

- `versionName 1.1.5_Build-8`, `versionCode 10105`.
- 129 unit tests, all passing. Debug and release builds go through.
- No emulator in my setup, so I couldn't do a real device pass. If the About layout looks off on your phone, send a screenshot.

## Downloads

Pick the one that matches your phone's CPU. If you're not sure, arm64-v8a is the safe bet for anything from the last few years.

- **app-arm64-v8a-release.apk** (24.39 MB) — SHA-256: `E4EAF3BAE5ADA4456373D665CDD45C44D1D8F17EAD3B8507FD00551A520F3901`
- **app-armeabi-v7a-release.apk** (22.78 MB) — older 32-bit phones — SHA-256: `7C60DACB0F491395162D1954E03CB2EF85B868C8BC113F9AE580C776D6EC3106`
- **app-x86_64-release.apk** (24.96 MB) — emulators and a few tablets — SHA-256: `319437365F5AFF347BF37979E68F66BBEF8DC01252AB487AD141432779C1B120`

Signing cert SHA-256: `74ca6ebfec387891eb6d5e86941599df665c43e72c57d4984dbdaa14ca40e832` — same key as before, so updates install over the old version without uninstalling.
