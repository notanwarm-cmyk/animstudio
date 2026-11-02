package com.example.animstudio.data;
/** dark=false: emitter (vanilla `light` block, level 0..15).  dark=true: darkness zone of `radius` blocks for entity lighting. */
public final class LightPoint {
    public int x, y, z, level = 15; public boolean dark; public float radius = 6;
}
