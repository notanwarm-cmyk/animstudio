package com.example.animstudio.data;
public final class Keyframe {
    public float frame, value;
    public Easing easing = Easing.SMOOTH;
    public Keyframe() {}
    public Keyframe(float f, float v, Easing e) { frame = f; value = v; easing = e; }
}
