# Media seekbar in MiuiSystemUI

Build: V816.0.11.0.TGKMIXM (Xiaomi.eu HyperOS 1, Android 13). Names come from the decompiled APK and may differ on other builds.

## How the media card is built

- `MediaCarouselController` creates one `MiuiMediaControlPanel` per player. This is Xiaomi's subclass of Google's `MediaControlPanel`.
- `attachPlayer()` builds a `MediaViewHolder` from the layout `media_session_view` and creates a `SeekBarObserver`, which updates the seekbar from `SeekBarViewModel`.
- `media_session_view` is the only layout with a media seekbar apart from the output dialog and volume panel.

## Why the bar was flat

- The seekbar style `MediaNotificationSeekBar` uses a plain layer-list drawable as its progress drawable.
- `SquigglyProgress` exists in `com.android.systemui.media`, and `SeekBarObserver` configures it when the drawable is one (wavelength 20dp, amplitude 1.5dp, phase 8dp, stroke 2dp). Nothing ever installs one.
- The only `setAnimate` call is `setAnimate(false)` for the disabled state. The wave only gets height after `setAnimate(true)`.

## What the module does

1. Before the `SeekBarObserver` constructor runs, it sets the seekbar's progress drawable to a new `SquigglyProgress`. After the constructor, it pads the seekbar by half the stroke width.
2. After `SeekBarObserver.onChanged`, it calls `setAnimate(enabled && playing && !scrubbing)`. `Progress` has a `playing` field but no getter, so the field is read through reflection.
3. After `SquigglyProgress.draw`, it paints a round cap at the right end of the track.

The fields and methods used by the frequently called hooks (onChanged and draw) are looked up once at setup. The hooks still read fields and call setAnimate through reflection, which is cheap at this call rate. Every hook body is wrapped in try/catch. Failures in the frequently called hooks are logged once per hook. Failures at setup or when a card is created are logged each time, which is rare.

## Why the ends were flat

The drawable clips its track at the exact end of the line, so the round cap on the right is sliced off. The module adds the missing cap itself after the original draw. The seekbar padding gives the start of the track room so it is not clipped by the card.

## Reproducing the research


    adb pull /system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk work/MiuiSystemUI.apk
    jadx --threads-count 2 -d work/jadx_out work/MiuiSystemUI.apk