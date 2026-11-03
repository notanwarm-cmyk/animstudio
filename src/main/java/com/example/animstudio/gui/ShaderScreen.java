package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.iris.IrisBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
public final class ShaderScreen extends UiScreen {
    private final Screen parent; private final List<String> packs = IrisBridge.packs();
    public ShaderScreen(Screen parent) { super("Shaders"); this.parent = parent; }
    @Override protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        hits.clear(); Theme t = t(); int w = 260, x = (width - w) / 2, y = 30;
        g.fill(x - 8, y - 8, x + w + 8, height - 20, t.panel); g.outline(x - 8, y - 8, w + 16, height - 28, t.line);
        txt(g, "Shader packs (Iris)", x, y, t.text); y += 16;
        if (!IrisBridge.present()) { txt(g, "Iris is not installed. Drop Iris into /mods.", x, y, t.dim); }
        else {
            btn(g, IrisBridge.packInUse() ? "Shaders: ON  (click to disable)" : "Shaders: OFF (click to enable)", x, y, w, 16, mx, my,
                () -> IrisBridge.setEnabled(!IrisBridge.packInUse())); y += 22;
            for (String p : packs) {
                if (y > height - 50) break;
                btn(g, (p.equals(StudioConfig.I.irisPack) ? "> " : "") + p, x, y, w, 14, mx, my, () -> { StudioConfig.I.irisPack = p; StudioConfig.I.save(); IrisBridge.selectPack(p); });
                y += 16;
            }
            if (packs.isEmpty()) txt(g, "No packs found in /shaderpacks.", x, y, t.dim);
        }
        btn(g, "Back", x, height - 44, 60, 16, mx, my, this::onClose);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
}
