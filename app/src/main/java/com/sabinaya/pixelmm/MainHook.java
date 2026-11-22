package com.sabinaya.pixelmm;

import android.graphics.drawable.Drawable;
import android.widget.SeekBar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private boolean warned;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        XposedBridge.log("PixelMM: loaded in " + lpparam.packageName);

        hookSeekBarObserver(lpparam.classLoader);   // Hook 1: install the drawable
        hookOnChanged(lpparam.classLoader);         // Hook 2: animate while playing
    }

    private void hookSeekBarObserver(ClassLoader cl) {
        try {
            final Class<?> squiggly = XposedHelpers.findClass(
                    "com.android.systemui.media.SquigglyProgress", cl);

            XposedHelpers.findAndHookConstructor(
                    "com.android.systemui.media.SeekBarObserver", cl,
                    "com.android.systemui.media.MediaViewHolder",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
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

    private void hookOnChanged(ClassLoader cl) {
        try {
            final String media = "com.android.systemui.media.";
            final Class<?> squiggly = XposedHelpers.findClass(media + "SquigglyProgress", cl);
            final Class<?> progressCls = XposedHelpers.findClass(media + "SeekBarViewModel$Progress", cl);

            final Method setAnimate = XposedHelpers.findMethodExact(squiggly, "setAnimate", boolean.class);
            final Field fHolder = XposedHelpers.findField(
                    XposedHelpers.findClass(media + "SeekBarObserver", cl), "holder");
            final Field fSeekBar = XposedHelpers.findField(
                    XposedHelpers.findClass(media + "MediaViewHolder", cl), "seekBar");
            final Field fEnabled = XposedHelpers.findField(progressCls, "enabled");
            final Field fPlaying = XposedHelpers.findField(progressCls, "playing");
            final Field fScrubbing = XposedHelpers.findField(progressCls, "scrubbing");

            XposedHelpers.findAndHookMethod(media + "SeekBarObserver", cl, "onChanged",
                    progressCls,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object progress = param.args[0];
                                boolean wave = fEnabled.getBoolean(progress)
                                        && fPlaying.getBoolean(progress)
                                        && !fScrubbing.getBoolean(progress);

                                SeekBar bar = (SeekBar) fSeekBar.get(fHolder.get(param.thisObject));
                                Drawable d = bar.getProgressDrawable();
                                if (squiggly.isInstance(d)) {
                                    setAnimate.invoke(d, wave);
                                }
                            } catch (Throwable t) {
                                if (!warned) {
                                    warned = true;
                                    XposedBridge.log("PixelMM: animate failed: " + t);
                                }
                            }
                        }
                    });
            XposedBridge.log("PixelMM: hooked onChanged");
        } catch (Throwable t) {
            XposedBridge.log("PixelMM: onChanged setup failed: " + t);
        }
    }
}