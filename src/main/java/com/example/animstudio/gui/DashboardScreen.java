package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.data.StudioProject;
import com.example.animstudio.record.*;
import com.example.animstudio.world.StudioWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
/** Dashboard: browse recordings + projects and open them in the standalone studio (no world needed). */
public final class DashboardScreen extends UiScreen {
    private final Screen parent; private String selRec, selProj; private int scrollR, scrollP;
    public DashboardScreen(Screen parent) { super("Animation Studio"); this.parent = parent; }

    @Override protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        Theme t = t(); Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0, width, height, t.bg);
        g.fill(0, 0, width, 24, t.panel); txt(g, "Animation Studio  -  Dashboard", 10, 8, t.text);
        int x = 330;
        x = btn(g, "New project", x, 4, 80, 16, mx, my, () -> { StudioProject p = new StudioProject(); p.name = "Project " + (StudioProject.list().size() + 1); open(p); });
        x = btn(g, "Build world", x, 4, 76, 16, mx, my, () -> { StudioWorld.runAfterJoin(() -> mc.setScreenAndShow(null)); StudioWorld.launch(this); });
        x = btn(g, "Shaders", x, 4, 56, 16, mx, my, () -> mc.setScreenAndShow(new ShaderScreen(this)));
        x = btn(g, StudioConfig.I.dark ? "Dark" : "Light", x, 4, 44, 16, mx, my, () -> { StudioConfig.I.dark = !StudioConfig.I.dark; StudioConfig.I.save(); });
        btn(g, "Close", x, 4, 44, 16, mx, my, this::onClose);

        int colW = (width - 30) / 2, top = 34;
        panel(g, 10, top, colW, height - top - 10, "Recorded sessions", t);
        List<String> recs = RecordingStore.list(); int y = top + 20;
        for (int i = scrollR; i < recs.size() && y < height - 46; i++, y += 15) { String n = recs.get(i); row(g, n, 16, y, colW - 12, n.equals(selRec), () -> selRec = n); }
        if (recs.isEmpty()) txt(g, "None yet. Use 'Start Recording' in any pause menu.", 16, y, t.dim);
        int by = height - 32;
        int bx = btn(g, "Open in Studio", 16, by, 100, 16, mx, my, () -> { if (selRec != null) openRecording(selRec); });
        btn(g, "Delete", bx, by, 56, 16, mx, my, () -> { if (selRec != null) { RecordingStore.delete(selRec); selRec = null; } });
        int px = 20 + colW;
        panel(g, px, top, colW, height - top - 10, "Projects", t);
        List<String> projs = StudioProject.list(); y = top + 20;
        for (int i = scrollP; i < projs.size() && y < height - 46; i++, y += 15) { String n = projs.get(i); row(g, n, px + 6, y, colW - 12, n.equals(selProj), () -> selProj = n); }
        btn(g, "Open", px + 6, by, 50, 16, mx, my, () -> { if (selProj != null) open(StudioProject.load(selProj)); });
    }
    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, Theme t) {
        g.fill(x, y, x + w, y + h, t.panel); g.outline(x, y, w, h, t.line); txt(g, title, x + 6, y + 6, t.dim);
    }
    private void row(GuiGraphicsExtractor g, String s, int x, int y, int w, boolean sel, Runnable r) {
        Theme t = t(); if (sel) g.fill(x, y, x + w, y + 14, (t.accent & 0xFFFFFF) | 0x55000000);
        txt(g, s, x + 4, y + 3, t.text); hits.add(new Hit(x, y, w, 14, r, null));
    }
    private void openRecording(String name) {
        Recording r = RecordingStore.load(name); if (r == null) return;
        StudioProject p = new StudioProject(); p.name = "Rec " + name;
        p.actors.add(Baker.bake(r, p.tempo, null)); p.lengthFrames = Math.max(48, (int) (r.frames.size() * p.tempo / 20f));
        open(p);
    }
    private void open(StudioProject p) { Minecraft.getInstance().setScreenAndShow(new StudioScreen(this, p)); }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < width / 2.0) scrollR = Math.max(0, scrollR - (int) Math.signum(sy)); else scrollP = Math.max(0, scrollP - (int) Math.signum(sy));
        return true;
    }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
}
