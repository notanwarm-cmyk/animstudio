package com.example.animstudio.anim;
import com.example.animstudio.StudioConfig;

public final class FrameLimiter {
    public static boolean active() { return StudioConfig.I.fps15; }
    /** Snap a timeline frame so animation only changes limitFps times per second. */
    public static float snapFrame(float f, float tempo) {
        float fps = StudioConfig.I.limitFps;
        return (float) (Math.floor(f / tempo * fps) / fps * tempo);
    }
    /** Wall-clock bucket; entity render state is re-extracted only when this changes. */
    public static long bucket() { return (long) (System.nanoTime() / 1.0e9 * StudioConfig.I.limitFps); }
}
