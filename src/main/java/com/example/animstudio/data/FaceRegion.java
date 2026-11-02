package com.example.animstudio.data;
/** Rectangle in FACE-LOCAL pixels (0..7). The face is the 8x8 block at (8,8) in a 64x64 skin. */
public final class FaceRegion {
    public enum Kind { EYE, MOUTH }
    public Kind kind; public int x, y, w, h;
    public FaceRegion() {}
    public FaceRegion(Kind k, int x, int y, int w, int h) { kind = k; this.x = x; this.y = y; this.w = w; this.h = h; }
}
