package com.example.animstudio.render;
import com.example.animstudio.data.*;
public final class PoseEval {
    /** Per part: [posX,posY,posZ, rotX,rotY,rotZ, scaleX,scaleY,scaleZ, visible]. */
    public static final int N = 10;
    public static int idx(Channel c) {
        return switch (c) { case POS_X -> 0; case POS_Y -> 1; case POS_Z -> 2; case ROT_X -> 3; case ROT_Y -> 4; case ROT_Z -> 5;
            case SCALE_X -> 6; case SCALE_Y -> 7; case SCALE_Z -> 8; case VISIBLE -> 9; default -> -1; };
    }
    public static float def(BodyPart p, Channel c) {
        if (c == Channel.VISIBLE) return (p == BodyPart.EAR_L || p == BodyPart.EAR_R) ? 0 : 1;
        return (c == Channel.SCALE_X || c == Channel.SCALE_Y || c == Channel.SCALE_Z) ? 1 : 0;
    }
    public static float[][] eval(ActorData a, float frame) {
        float[][] p = new float[BodyPart.values().length][N];
        for (BodyPart bp : BodyPart.values()) for (Channel c : Channel.values()) { int i = idx(c); if (i >= 0) p[bp.ordinal()][i] = def(bp, c); }
        for (Track t : a.tracks) {
            int i = idx(t.channel); if (i < 0 || t.part == BodyPart.FACE) continue;
            float v = t.sample(frame); if (!Float.isNaN(v)) p[t.part.ordinal()][i] = v;
        }
        return p;
    }
    public static float value(ActorData a, BodyPart p, Channel c, float frame) { return a.sample(p, c, frame, def(p, c)); }
}
