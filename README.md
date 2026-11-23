# pixelmm-lsposed

An LSPosed module that gives the HyperOS media player the Pixel-style squiggly progress bar.

**Status:** working on the test device. Early and unreleased; there is no prebuilt APK.

## What it does

HyperOS SystemUI still contains Google's `SquigglyProgress` class, but its media card never uses it. This module hooks SystemUI at runtime and does three things:

- installs that existing class as the media seekbar's progress drawable;
- turns the wave on only while music is playing, and flattens it when paused or scrubbing;
- rounds the ends of the track, which would otherwise be cut flat.

Nothing in /system is modified, and the module can be switched off at any time.

## Tested on

Redmi Note 11 4G (spes), Xiaomi.eu HyperOS 1, Android 13, build V816.0.11.0.TGKMIXM. Verified on the notification shade. The lock screen is working but Control Center have not been verified. Other builds may use different class names and are not supported.

## Install

Requires Magisk with Zygisk enabled and LSPosed (Zygisk build). To build from source you also need JDK 17 and the Android SDK (platform 33).

1. Build: `.\gradlew.bat assembleDebug`
2. Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
3. In the LSPosed manager, enable PixelMM and tick System UI in its scope.
4. Restart SystemUI or reboot, then play some music.

## Disabling the module

Turn it off in the LSPosed manager and restart SystemUI. If the manager cannot be reached, run `adb uninstall com.sabinaya.pixelmm` and reboot.

## Notes

How the media card works and how the hooks were found is written up in `docs/research/`.

## Credits and license

The squiggly progress bar is Google's design from the Android Open Source Project. This repository does not contain that code. The module uses the copy already shipped in the device's SystemUI, through reflection. The module's own code is licensed under Apache-2.0 (see `LICENSE`).

This project is not affiliated with Xiaomi or Google, and Pixel is a Google trademark. Use it at your own risk, and back up your boot image before making system changes.