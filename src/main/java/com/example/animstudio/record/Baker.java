package com.example.animstudio.record;
import com.example.animstudio.data.*;
/** Recording -> keyframes (root path, head look, sneak lean and a procedural walk cycle driven by speed). */
public final class Baker {
    public static ActorData bake(Recording r, float tempo, String skinFile) {
        ActorData a = new ActorData(); a.name = "Recorded " + r.name; a.skinFile = skinFile;
        if (r.frames.isEmpty()) return a;
        float k = tempo / 20f; Recording.Frame f0 = r.frames.get(0), prev = null; double phase = 0;
        for (int i = 0; i < r.frames.size(); i++) {
            Recording.Frame f = r.frames.get(i); float fr = i * k;
            key(a, BodyPart.ROOT, Channel.POS_X, fr, (float) (f.x - f0.x)); key(a, BodyPart.ROOT, Channel.POS_Y, fr, (float) (f.y - f0.y));
            key(a, BodyPart.ROOT, Channel.POS_Z, fr, (float) (f.z - f0.z)); key(a, BodyPart.ROOT, Channel.ROT_Y, fr, -f.yRot);
            key(a, BodyPart.HEAD, Channel.ROT_X, fr, f.xRot);
            key(a, BodyPart.HEAD, Channel.ROT_Y, fr, -net.minecraft.util.Mth.wrapDegrees(f.headYaw - f.yRot));
            key(a, BodyPart.TORSO, Channel.ROT_X, fr, (f.flags & Recording.SNEAK) != 0 ? 25f : 0f);
            double d = prev == null ? 0 : Math.hypot(f.x - prev.x, f.z - prev.z);
            if (d > 0.003) phase += d * 3.2;
            float swing = (float) (Math.sin(phase) * Math.min(1.0, d / 0.2) * 50);
            key(a, BodyPart.RIGHT_LEG, Channel.ROT_X, fr, swing); key(a, BodyPart.LEFT_LEG, Channel.ROT_X, fr, -swing);
            key(a, BodyPart.RIGHT_ARM, Channel.ROT_X, fr, -swing); key(a, BodyPart.LEFT_ARM, Channel.ROT_X, fr, swing);
            prev = f;
        }
        return a;
    }
    private static void key(ActorData a, BodyPart p, Channel c, float fr, float v) { a.track(p, c, true).keys.add(new Keyframe(fr, v, Easing.LINEAR)); }
}
