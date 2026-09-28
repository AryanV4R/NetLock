# NetLock

A lightweight Android app that keeps an eye on your mobile network type and alerts you the moment it switches — for example when 5G drops to 4G, or the signal falls back to E / No Service.

Built with Kotlin and Jetpack Compose.

## Features

- **Live network monitoring** — shows the current network type (5G SA, 5G NSA, 4G, 3G, 2G) and flight mode status on the home screen
- **Switch alerts** — vibration and/or sound whenever your network type changes
- **E / No Service alert** — optional alert when the connection drops to 3G/2G or loses service
- **Vibrate in DND / silent** — alert vibration can bypass Do Not Disturb
- **Last switch tracking** — see when your network last changed
- **Recent events** — network changes from the last 24 hours at a glance
- **Stats** — total, today and 7-day switch counts, plus a time distribution across 5G SA / 5G NSA / 4G / 3G / 2G
- **Carrier & signal info** — operator name, MCC-MNC, radio type, RSRP and RSRQ
- **SIM slot selection** — auto, SIM 1 or SIM 2
- **Light and dark themes** with a smooth transition
- **Start on boot** and quick access to battery-optimization / autostart settings
- **Quick Settings tile** for the current network type
- Guided first-run onboarding for the permissions the app needs


## Download

Grab the latest APK from the [Releases](../../releases) page.

**Install steps**

1. Download `app-release.apk` from the latest release.
2. Open the file on your phone.
3. If prompted, allow installs from your browser / file manager ("Install unknown apps").
4. Open NetLock and follow the onboarding screens.

## Permissions

| Permission | Why it's needed |
|---|---|
| Notifications (Android 13+) | To show network switch alerts |
| Phone state | To read the current network type and carrier / signal details |
| Ignore battery optimization (optional) | So monitoring keeps running reliably in the background |

Some phone brands also need **Autostart** enabled for the app to run after a reboot. NetLock has a shortcut to that setting.

## Build from source

1. Clone the repo:
   ```bash
   git clone https://github.com/AryanV4R/NetLock.git
   ```
2. Open the project in Android Studio.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.

To build a release APK: **Build → Generate Signed App Bundle / APK → APK**.

## Tech stack

- Kotlin
- Jetpack Compose (Material 3)
- Android foreground service and Quick Settings tile

## Author

Made by [AryanV4R](https://github.com/AryanV4R).
