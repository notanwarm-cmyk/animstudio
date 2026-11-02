package com.example.animstudio.data;
import java.util.*;
public final class Track {
    public BodyPart part; public Channel channel;
    public List<Keyframe> keys = new ArrayList<>();
    public Track() {}
    public Track(BodyPart p, Channel c) { part = p; channel = c; }

    public Keyframe set(float frame, float v, Easing e) {
        for (Keyframe k : keys) if (Math.abs(k.frame - frame) < 0.01f) { k.value = v; return k; }
        Keyframe k = new Keyframe(frame, v, e); keys.add(k);
        keys.sort(Comparator.comparingDouble(a -> a.frame)); return k;
    }
    /** NaN when the track has no keys, so callers can fall back to vanilla animation. */
    public float sample(float f) {
        if (keys.isEmpty()) return Float.NaN;
        Keyframe first = keys.get(0), last = keys.get(keys.size() - 1);
        if (f <= first.frame) return first.value;
        if (f >= last.frame) return last.value;
        int lo = 0, hi = keys.size() - 1;
        while (hi - lo > 1) { int mid = (lo + hi) >>> 1; if (keys.get(mid).frame <= f) lo = mid; else hi = mid; }
        Keyframe a = keys.get(lo), b = keys.get(hi);
        float t = (f - a.frame) / (b.frame - a.frame);
        return a.value + (b.value - a.value) * a.easing.apply(t);
    }
}
