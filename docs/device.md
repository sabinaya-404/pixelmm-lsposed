# Test device

| | |
|---|---|
| Device | Redmi Note 11 4G (2201117TG, codename spes) |
| SoC | Snapdragon 680 (SM6225), arm64 |
| Screen | 1080x2400, 440 dpi |
| RAM | 5.5 GB (usually close to full in daily use) |
| ROM | Xiaomi.eu HyperOS 1, Android 13 (SDK 33) |
| Build | V816.0.11.0.TGKMIXM (TKQ1.221114.001) |
| Security patch | 2025-04-01 |
| Kernel | 4.19.157 |
| Root | Magisk, Zygisk, LSPosed |

## SystemUI

- Package: `com.android.systemui`
- APK: `/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk`
- Runs as the system user in one process, so one LSPosed scope covers it.
- The Control Center media card is not in this APK. It is drawn by a separate plugin APK (MiuiSystemUIPlugin.apk), which the      module does not cover.