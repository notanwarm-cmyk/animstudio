package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.*;
import java.util.function.DoubleConsumer;
/** Tiny immediate-mode UI: widgets register hit boxes while drawing; clicks/drags are dispatched to the last frame's boxes. */
public abstract class UiScreen extends Screen {
    protected record Hit(int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    protected final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    protected UiScreen(String title) { super(Component.literal(title)); }
    private String error;
    /** Every screen draws inside try/catch: a bug shows a message here instead of crashing the whole game. */
    @Override public final void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        hits.clear();
        try { draw(g, mx, my, dt); error = null; }
        catch (Throwable t) {
            if (error == null) t.printStackTrace();
            error = t.toString(); g.fill(0, 0, width, height, 0xFF200000);
            g.text(font, "Animation Studio error (game kept running):", 10, 10, 0xFFFFAAAA, false);
            g.text(font, error.length() > 110 ? error.substring(0, 110) : error, 10, 24, 0xFFFFFFFF, false);
            g.text(font, "Press ESC and send latest.log to the developer.", 10, 38, 0xFFFFAAAA, false);
        }
    }
    protected abstract void draw(GuiGraphicsExtractor g, int mx, int my, float dt);
    protected Theme t() { return StudioConfig.I.theme(); }
    protected void txt(GuiGraphicsExtractor g, String s, int x, int y, int c) { g.text(font, s, x, y, c, false); }

    protected int btn(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, int mx, int my, Runnable r) {
        Theme t = t(); boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hov ? t.accent : t.field);
        g.outline(x, y, w, h, t.line);
        txt(g, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hov ? 0xFFFFFFFF : t.text);
        hits.add(new Hit(x, y, w, h, r, null)); return x + w + 3;
    }
    protected void slider(GuiGraphicsExtractor g, String label, int x, int y, int w, float min, float max, float v, DoubleConsumer set) {
        Theme t = t(); g.fill(x, y, x + w, y + 12, t.field);
        g.fill(x, y, x + (int) (w * Mth.clamp((v - min) / (max - min), 0f, 1f)), y + 12, (t.accent & 0x00FFFFFF) | 0x99000000);
        txt(g, label + ": " + String.format("%.1f", v), x + 3, y + 2, t.text);
        hits.add(new Hit(x, y, w, 12, null, m -> set.accept(min + (max - min) * Mth.clamp((float) ((m - x) / w), 0f, 1f))));
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(e.x(), e.y())) {
                if (h.click != null) h.click.run(); else if (h.drag != null) { h.drag.accept(e.x()); dragging = h; }
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != null) { dragging.drag.accept(e.x()); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { dragging = null; return super.mouseReleased(e); }
    @Override public boolean isPauseScreen() { return false; }
}
