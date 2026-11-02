package com.example.animstudio.record;
import java.util.*;
public final class Recording {
    public static final int SNEAK = 1, SPRINT = 2, SWING = 4, USING = 8, GROUND = 16;
    public static final class Frame {
        public double x, y, z; public float yRot, xRot, headYaw;
        public String pose = "STANDING"; public int flags; public String mainHand = "minecraft:air";
    }
    public String name; public long createdMs = System.currentTimeMillis();
    public List<Frame> frames = new ArrayList<>();      // one per client tick (20 Hz)
    public float seconds() { return frames.size() / 20f; }
}
