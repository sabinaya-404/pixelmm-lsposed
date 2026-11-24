/*
 * Copyright 2026 sabinaya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sabinaya.pixelmm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.SeekBar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        XposedBridge.log("PixelMM: loaded in " + lpparam.packageName);

        hookSeekBarObserver(lpparam.classLoader);   // install the drawable + padding
        hookOnChanged(lpparam.classLoader);         // animate while playing
        hookRoundCaps(lpparam.classLoader);         // Round the right end
    }

    // Used by hooks that run many times per second.
    private void warnOnce(String what, Throwable t) {
     if (warned.add(what)) {   // true only the first time this label is seen
        XposedBridge.log("PixelMM: " + what + " failed: " + t);
    }
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

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                SeekBar bar = (SeekBar) XposedHelpers.getObjectField(param.args[0], "seekBar");
                                Drawable d = bar.getProgressDrawable();
                                if (!squiggly.isInstance(d)) return;

                                if (XposedHelpers.getAdditionalInstanceField(bar, "pixelmm_padded") != null) return;
                                XposedHelpers.setAdditionalInstanceField(bar, "pixelmm_padded", Boolean.TRUE);

                                int pad = (int) Math.ceil(XposedHelpers.getFloatField(d, "strokeWidth") / 2f);
                                bar.setPadding(bar.getPaddingLeft() + pad, bar.getPaddingTop(),
                                        bar.getPaddingRight() + pad, bar.getPaddingBottom());
                            } catch (Throwable t) {
                                XposedBridge.log("PixelMM: padding failed: " + t);
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
                                warnOnce("animate", t);
                            }
                        }
                    });
            XposedBridge.log("PixelMM: hooked onChanged");
        } catch (Throwable t) {
            XposedBridge.log("PixelMM: onChanged setup failed: " + t);
        }
    }

    // The missing round cap at the right end of the track.
    private void hookRoundCaps(ClassLoader cl) {
        try {
            final Class<?> squiggly = XposedHelpers.findClass(
                    "com.android.systemui.media.SquigglyProgress", cl);
            final Field fLine = XposedHelpers.findField(squiggly, "linePaint");

            final Paint cap = new Paint(Paint.ANTI_ALIAS_FLAG);
            cap.setStyle(Paint.Style.FILL);

            XposedHelpers.findAndHookMethod(squiggly, "draw", Canvas.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Drawable d = (Drawable) param.thisObject;
                                Canvas c = (Canvas) param.args[0];
                                Paint line = (Paint) fLine.get(d);
                                Rect b = d.getBounds();

                                float r = line.getStrokeWidth() / 2f;   // cap radius
                                float cy = b.centerY();                 // same centre line the drawable uses

                                cap.setColor(line.getColor());          // same translucent colour as the track
                                c.save();
                                c.clipRect(b.right, cy - r - 1f, b.right + r + 1f, cy + r + 1f);
                                c.drawCircle(b.right, cy, r, cap);
                                c.restore();
                            } catch (Throwable t) {
                                warnOnce("round cap", t);
                            }
                        }
                    });
            XposedBridge.log("PixelMM: hooked draw");
        } catch (Throwable t) {
            XposedBridge.log("PixelMM: draw setup failed: " + t);
        }
    }
}