package com.example.animstudio.anim;
import java.util.*;
/** Deterministic, seed-based blinking: scrubbing/looping the timeline always yields the same blinks. */
public final class BlinkScheduler {
    private static final class State { final Random r; double cursor = 1.0; final List<double[]> ev = new ArrayList<>(); State(long s){ r = new Random(s);} }
    private static final Map<Long, State> STATES = new HashMap<>();

    /** @return eyelid closure 0..1 at time t (seconds). */
    public static float closed(long seed, double t) {
        if (t < 0) return 0;
        State s = STATES.computeIfAbsent(seed, State::new);
        while (s.cursor < t + 10) {                       // generate ahead
            double dur = 0.12 + s.r.nextDouble() * 0.06;
            s.ev.add(new double[]{s.cursor, dur});
            if (s.r.nextDouble() < 0.12) s.ev.add(new double[]{s.cursor + dur + 0.1, dur}); // double blink
            s.cursor += 2.0 + s.r.nextDouble() * 4.0;      // 2-6 s between blinks
        }
        for (int i = s.ev.size() - 1; i >= 0; i--) {
            double[] e = s.ev.get(i);
            if (e[0] > t) continue;
            double u = (t - e[0]) / e[1];
            if (u > 1) return 0;
            return (float) (u < 0.35 ? u / 0.35 : 1 - (u - 0.35) / 0.65);   // fast close, slower open
        }
        return 0;
    }
}
