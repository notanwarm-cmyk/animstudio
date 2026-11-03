package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
/** One dynamic texture holding the software-rendered viewport. Recreated only when the viewport size changes. */
public final class ViewportTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("animstudio", "viewport");
    public int w, h; public int[] px = new int[0]; private DynamicTexture tex;
    public void ensure(int W, int H) {
        if (tex != null && W == w && H == h) return;
        w = W; h = H; px = new int[W * H];
        tex = new DynamicTexture(() -> "animstudio_viewport", new NativeImage(W, H, true));
        Minecraft.getInstance().getTextureManager().register(ID, tex);
    }
    public void upload() {
        NativeImage img = tex.getPixels(); if (img == null) return;
        boolean swap = StudioConfig.I.swapRB;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = px[y * w + x];
            if (swap) c = (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
            img.setPixel(x, y, c);
        }
        tex.upload();
    }
}
