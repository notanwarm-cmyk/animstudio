package com.example.animstudio.actor;
import com.example.animstudio.data.FaceRegion;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import java.awt.image.BufferedImage;
import java.util.List;
/**
 * Owns the actor's skin pixels (ARGB) and re-paints ONLY the 8x8 face (skin px 8..15, 8..15) to fake
 * blinking and mouth movement. Eyelids copy the pixel just above each eye column (forehead/skin tone).
 */
public final class FaceSkin {
    public static final int FX = 8, FY = 8;
    public final int[] base = new int[64 * 64];
    public final DynamicTexture texture;
    private int lastEyes = -1, lastMouth = -1;

    public FaceSkin(BufferedImage img) {
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
            base[y * 64 + x] = (x < img.getWidth() && y < img.getHeight()) ? img.getRGB(x, y) : 0;   // 64x32 legacy skins are padded only
        NativeImage ni = new NativeImage(64, 64, true);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) ni.setPixel(x, y, base[y * 64 + x]);   // ARGB in 26.x; verify
        texture = new DynamicTexture(() -> "animstudio_skin", ni);                                   // ctor takes a label supplier
    }
    public int facePixel(int x, int y) { return base[(FY + y) * 64 + FX + x]; }

    /** @param eyes 0..1 closure  @param mouth 0..1 openness */
    public void update(List<FaceRegion> regions, float eyes, float mouth) {
        int qe = Math.round(eyes * 4), qm = Math.round(mouth * 4);        // quantise: avoid uploading every frame
        if (qe == lastEyes && qm == lastMouth) return;
        lastEyes = qe; lastMouth = qm;
        NativeImage img = texture.getPixels(); if (img == null) return;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) img.setPixel(FX + x, FY + y, base[(FY + y) * 64 + FX + x]);
        for (FaceRegion r : regions) {
            if (r.kind == FaceRegion.Kind.EYE && qe > 0) {
                int rows = Math.max(1, Math.round(r.h * qe / 4f));       // lid sweeps down from the top of the region
                for (int dy = 0; dy < rows; dy++) for (int dx = 0; dx < r.w; dx++) {
                    int ly = Math.max(0, r.y - 1);
                    img.setPixel(FX + r.x + dx, FY + r.y + dy, base[(FY + ly) * 64 + FX + r.x + dx]);
                }
            } else if (r.kind == FaceRegion.Kind.MOUTH && qm > 0) {
                int rows = Math.max(1, Math.round(r.h * qm / 4f));       // dark cavity grows from the bottom
                for (int dy = 0; dy < rows; dy++) for (int dx = 0; dx < r.w; dx++)
                    img.setPixel(FX + r.x + dx, FY + r.y + r.h - 1 - dy, 0xFF2A0E12);
            }
        }
        texture.upload();
    }
}
