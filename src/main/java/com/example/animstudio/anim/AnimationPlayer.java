package com.example.animstudio.anim;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.data.StudioProject;
public final class AnimationPlayer {
    public static StudioProject project = new StudioProject();
    public static float frame; public static boolean playing;
    /** Called once per client tick (20 Hz). */
    public static void tick() {
        if (!playing) return;
        frame += project.tempo / 20f;
        if (frame > project.lengthFrames) { if (project.loop) frame = 0; else { frame = project.lengthFrames; playing = false; } }
    }
    /** Frame used for rendering: sub-tick interpolated, then snapped to the 15-FPS grid when enabled. */
    public static float renderFrame(float partial) {
        float f = frame + (playing ? partial * project.tempo / 20f : 0f);
        return StudioConfig.I.fps15 ? FrameLimiter.snapFrame(f, project.tempo) : f;
    }
}
