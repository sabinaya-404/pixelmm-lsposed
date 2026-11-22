package com.sabinaya.pixelmm;

import android.graphics.drawable.Drawable;
import android.widget.SeekBar;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        XposedBridge.log("PixelMM: loaded in " + lpparam.packageName);

        hookSeekBarObserver(lpparam.classLoader);
    }

    private void hookSeekBarObserver(ClassLoader cl) {
        try {
            // SystemUI's own class, found by name at runtime.
            final Class<?> squiggly = XposedHelpers.findClass(
                    "com.android.systemui.media.SquigglyProgress", cl);

            // Hook the constructor SeekBarObserver(MediaViewHolder).
            XposedHelpers.findAndHookConstructor(
                    "com.android.systemui.media.SeekBarObserver", cl,
                    "com.android.systemui.media.MediaViewHolder",
                    new XC_MethodHook() {
                        // "before" matters: the constructor looks at the drawable
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                // args[0] is the MediaViewHolder; its public field "seekBar" is the SeekBar
                                SeekBar bar = (SeekBar) XposedHelpers.getObjectField(param.args[0], "seekBar");
                                if (!squiggly.isInstance(bar.getProgressDrawable())) {
                                    bar.setProgressDrawable(
                                            (Drawable) XposedHelpers.newInstance(squiggly));
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("PixelMM: swap failed: " + t);
                            }
                        }
                    });
            XposedBridge.log("PixelMM: hooked SeekBarObserver");
        } catch (Throwable t) {
            XposedBridge.log("PixelMM: hook setup failed: " + t);
        }
    }
}