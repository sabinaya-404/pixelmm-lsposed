# Media seekbar in MiuiSystemUI (build V816.0.11.0.TGKMIXM)

Build: V816.0.11.0.TGKMIXM (Xiaomi.eu HyperOS 1, Android 13). Names come from the decompiled APK and may differ on other builds.

## How the media card is built

- `MediaCarouselController` creates one `MiuiMediaControlPanel` perplayer. This is the xiaomi subclass of Google's `MediaControlPanel`.
- `attachPlayer()` builds a `MediaViewHolder` from the layout `media_session_view` and creates a `SeekBarObserver`, which updates the seekbar from `SeekBarViewModel`.
- `media_session_view` is the only layout with a media seekbar apart from the output dialog and volume panel. The lock screen and notification shade probably share it. Not tested yet.

## Possible reason the bar is flat

- The seekbar style `MediaNotificationSeekBar` uses a plain layer-list drawable as its progress drawable.
- `SquigglyProgress` exists in `com.android.systemui.media`. `SeekBarObserver` configures it when the drawable is one (wavelength 20dp, amplitude 1.5dp, phase 8dp, stroke 2dp).
- Nothing installs a `SquigglyProgress`, and the only `setAnimate` call is `setAnimate(false)` for the disabled state. The wave only gets height after `setAnimate(true)`.

## Hook plan

1. Before the `SeekBarObserver` constructor runs, set the seekbar's progress drawable to a new `SquigglyProgress`.
2. After `SeekBarObserver.onChanged`, call `setAnimate(enabled && playing && !scrubbing)`. `Progress` has a `playing` field but no getter, so read the field.