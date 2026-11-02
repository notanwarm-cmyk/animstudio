package com.example.animstudio.record;
import com.example.animstudio.data.*;
/** Converts a raw recording into keyframes on an actor so it can be edited on the timeline. */
public final class Baker {
    public static ActorData bake(Recording r, float tempo, String skinFile) {
        ActorData a = new ActorData(); a.name = "Recorded " + r.name; a.skinFile = skinFile;
        float k = tempo / 20f;
        for (int i = 0; i < r.frames.size(); i++) {
            Recording.Frame f = r.frames.get(i); float fr = i * k;
            key(a, BodyPart.ROOT, Channel.POS_X, fr, (float) f.x); key(a, BodyPart.ROOT, Channel.POS_Y, fr, (float) f.y);
            key(a, BodyPart.ROOT, Channel.POS_Z, fr, (float) f.z); key(a, BodyPart.ROOT, Channel.ROT_Y, fr, f.yRot);
            key(a, BodyPart.HEAD, Channel.ROT_X, fr, f.xRot);
            key(a, BodyPart.HEAD, Channel.ROT_Y, fr, net.minecraft.util.Mth.wrapDegrees(f.headYaw - f.yRot));
        }
        if (!r.frames.isEmpty()) { var f0 = r.frames.get(0); a.spawnX = f0.x; a.spawnY = f0.y; a.spawnZ = f0.z; a.spawnYaw = f0.yRot; }
        return a;
    }
    private static void key(ActorData a, BodyPart p, Channel c, float fr, float v) {
        a.track(p, c, true).keys.add(new Keyframe(fr, v, Easing.LINEAR));   // appended in time order
    }
}
