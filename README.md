\# pixelmm-lsposed



An LSPosed module that replaces the media progress bar in HyperOS

SystemUI with the squiggly progress bar found on Pixel devices.



\*\*Status:\*\* early research. The module does not work yet.



\## Background



The stock HyperOS media controls use a flat progress bar. The Pixel

version draws a wavy line that animates while playback is active.

This project aims to bring that bar to HyperOS without replacing

SystemUI.



An earlier attempt modified MiuiSystemUI.apk and deployed it as a

Magisk module. It caused serious UI failures and was removed. This

project hooks SystemUI at runtime through LSPosed instead, so nothing

in /system is changed and the module can be disabled at any time.



\## Progress



\- LSPosed and Zygisk are set up on the test device.

\- Next, identify the class in MiuiSystemUI that creates the media

&#x20; seekbar.

\- After that, port the SquigglyProgress drawable from AOSP and hook it

&#x20; into the seekbar.

\- The lock screen, notification shade and Control Center may use

&#x20; separate code, so each may need its own hook.



\## Tested device



Redmi Note 11 4G (spes), Xiaomi.eu HyperOS 1, Android 13, build

V816.0.11.0.TGKMIXM. Other builds may use different class names and are

not supported.



\## Disabling the module



Turn the module off in the LSPosed manager and restart SystemUI. If the

manager cannot be reached, disable it from adb or boot into Magisk safe

mode.



\## Credits and license



The squiggly progress design is based on SquigglyProgress from the

Android Open Source Project (Apache-2.0). The module runs on the

LSPosed framework.



This project is not affiliated with Xiaomi or Google. Use it at your

own risk, and back up your boot image before making system changes.

