package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.data.StudioProject;
import com.example.animstudio.record.*;
import com.example.animstudio.world.StudioWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
/** Main dashboard: browse recordings + projects, open them in the studio, or launch the flat build world. */
public final class DashboardScreen extends UiScreen {
    private final Screen parent; private String selRec, selProj; private int scrollR, scrollP;
    public DashboardScreen(Screen parent) { super("Animation Studio"); this.parent = parent; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        hits.clear(); Theme t = t(); Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0, width, height, t.bg);
        g.fill(0, 0, width, 24, t.panel); txt(g, "Animation Studio  -  Dashboard", 10, 8, t.text);
        int x = width - 330;
        x = btn(g, StudioConfig.I.dark ? "Dark" : "Light", x, 4, 50, 16, mx, my, () -> { StudioConfig.I.dark = !StudioConfig.I.dark; StudioConfig.I.save(); });
        x = btn(g, "Build Mode (flat world)", x, 4, 130, 16, mx, my, () -> { StudioWorld.runAfterJoin(() -> mc.setScreenAndShow(null)); StudioWorld.launch(this); });
        btn(g, "Close", x, 4, 50, 16, mx, my, this::onClose);

        int colW = (width - 40) / 2, top = 40;
        // Recordings
        panel(g, 10, top, colW, height - top - 10, "Recorded sessions", t);
        List<String> recs = RecordingStore.list(); int y = top + 20;
        for (int i = scrollR; i < recs.size() && y < height - 50; i++, y += 15) { String n = recs.get(i);
            row(g, n, 16, y, colW - 12, n.equals(selRec), mx, my, () -> selRec = n); }
        if (recs.isEmpty()) txt(g, "None yet. Use 'Start Recording' in any pause menu.", 16, y, t.dim);
        int by = height - 32;
        int bx = btn(g, "Open in Studio", 16, by, 100, 16, mx, my, () -> { if (selRec != null) openRecording(selRec); });
        btn(g, "Delete", bx, by, 60, 16, mx, my, () -> { if (selRec != null) { RecordingStore.delete(selRec); selRec = null; } });
        // Projects
        int px = 20 + colW;
        panel(g, px, top, colW, height - top - 10, "Projects", t);
        List<String> projs = StudioProject.list(); y = top + 20;
        for (int i = scrollP; i < projs.size() && y < height - 50; i++, y += 15) { String n = projs.get(i);
            row(g, n, px + 6, y, colW - 12, n.equals(selProj), mx, my, () -> selProj = n); }
        bx = btn(g, "Open", px + 6, by, 50, 16, mx, my, () -> { if (selProj != null) openProject(StudioProject.load(selProj)); });
        btn(g, "New project", bx, by, 80, 16, mx, my, () -> { StudioProject p = new StudioProject(); p.name = "Project " + (projs.size() + 1); openProject(p); });
    }
    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, Theme t) {
        g.fill(x, y, x + w, y + h, t.panel); g.outline(x, y, w, h, t.line); txt(g, title, x + 6, y + 6, t.dim);
    }
    private void row(GuiGraphicsExtractor g, String s, int x, int y, int w, boolean sel, int mx, int my, Runnable r) {
        Theme t = t(); if (sel) g.fill(x, y, x + w, y + 14, (t.accent & 0xFFFFFF) | 0x55000000);
        txt(g, s, x + 4, y + 3, t.text); hits.add(new Hit(x, y, w, 14, r, null));
    }
    private void openRecording(String name) {
        Recording r = RecordingStore.load(name); if (r == null) return;
        StudioProject p = new StudioProject(); p.name = "Rec " + name;
        p.actors.add(Baker.bake(r, p.tempo, null)); p.lengthFrames = Math.max(48, (int) (r.frames.size() * p.tempo / 20f));
        openProject(p);
    }
    private void openProject(StudioProject p) {
        Minecraft mc = Minecraft.getInstance();
        StudioWorld.runAfterJoin(() -> mc.setScreenAndShow(new StudioScreen(null, p)));
        StudioWorld.launch(this);                      // inside a world this runs the pending action immediately
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < width / 2.0) scrollR = Math.max(0, scrollR - (int) Math.signum(sy)); else scrollP = Math.max(0, scrollP - (int) Math.signum(sy));
        return true;
    }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
}
