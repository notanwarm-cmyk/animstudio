package com.example.animstudio.actor;
import com.example.animstudio.data.FaceRegion;
import java.util.List;
/** CPU skin: `base` is the original, `work` is what the viewport draws. Only the 8x8 face (px 8..15, 8..15) is ever repainted. */
public final class FaceSkin {
    public static final int FX = 8, FY = 8;
    public final int[] base, work; private int lastE = -1, lastM = -1;
    public FaceSkin(int[] pixels) { base = pixels; work = pixels.clone(); }
    public int facePixel(int x, int y) { return base[(FY + y) * 64 + FX + x]; }
    public void invalidate() { lastE = lastM = -1; }
    /** @param eyes 0..1 closure  @param mouth 0..1 openness */
    public void update(List<FaceRegion> regions, float eyes, float mouth) {
        int qe = Math.round(eyes * 4), qm = Math.round(mouth * 4);
        if (qe == lastE && qm == lastM) return; lastE = qe; lastM = qm;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) work[(FY + y) * 64 + FX + x] = base[(FY + y) * 64 + FX + x];
        for (FaceRegion r : regions) {
            if (r.kind == FaceRegion.Kind.EYE && qe > 0) {
                int rows = Math.max(1, Math.round(r.h * qe / 4f)), above = Math.max(0, r.y - 1);
                for (int dy = 0; dy < rows; dy++) for (int dx = 0; dx < r.w; dx++)
                    work[(FY + r.y + dy) * 64 + FX + r.x + dx] = base[(FY + above) * 64 + FX + r.x + dx];   // lid = skin tone just above the eye
            } else if (r.kind == FaceRegion.Kind.MOUTH && qm > 0) {
                int rows = Math.max(1, Math.round(r.h * qm / 4f));
                for (int dy = 0; dy < rows; dy++) for (int dx = 0; dx < r.w; dx++) work[(FY + r.y + r.h - 1 - dy) * 64 + FX + r.x + dx] = 0xFF2A0E12;
            }
        }
    }
}
