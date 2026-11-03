package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.actor.FaceSkin;
import com.example.animstudio.actor.SkinManager;
import com.example.animstudio.anim.BlinkScheduler;
import com.example.animstudio.anim.AnimationPlayer;
import com.example.animstudio.anim.FrameLimiter;
import com.example.animstudio.data.*;
import com.example.animstudio.render.ModelRenderer;
import com.example.animstudio.render.PoseEval;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.*;

/**
 * Standalone studio (NO world needed). Layout: top bar | left: actors + every body part | centre: software-rendered 3D viewport |
 * right: inspector (position / rotation / scale / visibility per part, face editor) | bottom: keyframe timeline.
 */
public final class StudioScreen extends UiScreen {
    static final int TOP = 22, LEFT = 150, RIGHT = 215, BOT = 110, LABELS = 120, CELL = 14;
    private final Screen parent; private final StudioProject project; private final ViewportTexture vt = new ViewportTexture();
    private final Map<String, FaceSkin> faces = new HashMap<>();
    private ActorData sel; private BodyPart part = BodyPart.HEAD;
    private double camYaw = 25, camPitch = 15, zoom = 7; private boolean follow, orbit, scrubbing, keyDrag, faceDrag;
    private FaceRegion.Kind tool; private int fsx, fsy, fcx, fcy, gridX, gridY; private boolean gridOk;
    private int leftScroll, rightScroll, rightEnd, leftEnd; private float ppf = 6f, scroll = 0f;
    private Track selTrack; private Keyframe selKey; private String status = "";

    public StudioScreen(Screen parent, StudioProject project) {
        super("Studio"); this.parent = parent; this.project = project;
        AnimationPlayer.project = project; AnimationPlayer.frame = 0; AnimationPlayer.playing = false;
        if (!project.actors.isEmpty()) sel = project.actors.get(0);
        follow = sel != null && sel.track(BodyPart.ROOT, Channel.POS_X, false) != null;
    }
    private float frame() { return AnimationPlayer.frame; }
    private void key(BodyPart p, Channel c, float v) { sel.track(p, c, true).set(frame(), v, c == Channel.VISIBLE ? Easing.STEP : Easing.SMOOTH); }
    private float val(BodyPart p, Channel c) { return PoseEval.value(sel, p, c, frame()); }
    private FaceSkin face(ActorData a) { return faces.computeIfAbsent(a.id, k -> new FaceSkin(SkinManager.loadPixels(a))); }
    private boolean down(int k) { return InputConstants.isKeyDown(minecraft.getWindow(), k); }
    private boolean vis(int y, int h) { return y >= TOP + 1 && y + h <= height - BOT - 1; }

    // ============================== DRAW ==============================
    @Override protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        Theme t = t(); int W = width, H = height;
        viewport(g, dt);
        g.fill(0, TOP, LEFT, H - BOT, t.panel); g.fill(W - RIGHT, TOP, W, H - BOT, t.panel);
        g.fill(0, H - BOT, W, H, t.panelAlt); g.fill(0, 0, W, TOP, t.panel);
        g.fill(0, TOP - 1, W, TOP, t.line); g.fill(0, H - BOT, W, H - BOT + 1, t.line);
        g.fill(LEFT, TOP, LEFT + 1, H - BOT, t.line); g.fill(W - RIGHT, TOP, W - RIGHT + 1, H - BOT, t.line);
        topBar(g, mx, my); leftPanel(g, mx, my);
        if (sel != null) { inspector(g, mx, my); timeline(g, mx, my); } else txt(g, "Click  + Actor  to start", W - RIGHT + 8, TOP + 8, t.dim);
    }

    private void viewport(GuiGraphicsExtractor g, float dt) {
        Theme t = t(); int vx = LEFT + 1, vy = TOP, vw = width - LEFT - RIGHT - 1, vh = height - TOP - BOT;
        g.fill(vx, vy, vx + vw, vy + vh, t.bg);
        if (sel == null || vw < 20 || vh < 20) return;
        float f = AnimationPlayer.renderFrame(dt);                       // snapped to 15 FPS when that mode is on
        double blinkT = AnimationPlayer.playing ? f / project.tempo : System.nanoTime() / 1e9;   // paused: live preview of blinking
        if (StudioConfig.I.fps15) blinkT = Math.floor(blinkT * StudioConfig.I.limitFps) / StudioConfig.I.limitFps;
        FaceSkin fs = face(sel);
        float eyes = sel.sample(BodyPart.FACE, Channel.EYES_CLOSED, f, 0f);
        if (sel.autoBlink && sel.hasRegion(FaceRegion.Kind.EYE)) eyes = Math.max(eyes, BlinkScheduler.closed(sel.blinkSeed, blinkT));
        fs.update(sel.regions, eyes, sel.sample(BodyPart.FACE, Channel.MOUTH_OPEN, f, 0f));
        float[][] pose = PoseEval.eval(sel, f);
        double[] target = {0, 16, 0};
        if (follow) { float[] r = pose[BodyPart.ROOT.ordinal()]; target = new double[]{r[0] * 16, r[1] * 16 + 16, r[2] * 16}; }
        vt.ensure(vw, vh);
        ModelRenderer.render(vt.px, vw, vh, fs.work, sel.slim, pose, camYaw, camPitch, zoom, target, (t.line & 0x00FFFFFF) | 0xFF000000, true);
        vt.upload();
        g.blit(RenderPipelines.GUI_TEXTURED, ViewportTexture.ID, vx, vy, 0f, 0f, vw, vh, vw, vh);
        txt(g, String.format("Frame %.1f / %d  %s%s%s", frame(), project.lengthFrames, AnimationPlayer.playing ? "PLAYING" : "paused",
            StudioConfig.I.fps15 ? "  [15 FPS]" : "", sel.autoBlink && sel.hasRegion(FaceRegion.Kind.EYE) ? "  [auto-blink ON]" : ""), vx + 6, vy + 5, t.text);
        txt(g, "Drag: orbit   Wheel: zoom   Space: play/pause" + (status.isEmpty() ? "" : "   |   " + status), vx + 6, vy + vh - 11, t.dim);
    }

    private int tb(GuiGraphicsExtractor g, String s, int x, int mx, int my, Runnable r) { return btn(g, s, x, 3, font.width(s) + 12, 16, mx, my, r); }
    private void topBar(GuiGraphicsExtractor g, int mx, int my) {
        int x = 4;
        x = tb(g, AnimationPlayer.playing ? "Pause" : "Play", x, mx, my, () -> AnimationPlayer.playing = !AnimationPlayer.playing);
        x = tb(g, "Stop", x, mx, my, () -> { AnimationPlayer.playing = false; AnimationPlayer.frame = 0; });
        x = tb(g, "+ Actor", x + 6, mx, my, this::addActor);
        x = tb(g, "Select Eyes", x + 6, mx, my, this::pickEyes);
        x = tb(g, "Follow: " + (follow ? "on" : "off"), x + 6, mx, my, () -> follow = !follow);
        x = tb(g, "15 FPS: " + (StudioConfig.I.fps15 ? "ON" : "off"), x, mx, my, () -> { StudioConfig.I.fps15 = !StudioConfig.I.fps15; StudioConfig.I.save(); });
        x = tb(g, StudioConfig.I.dark ? "Dark" : "Light", x, mx, my, () -> { StudioConfig.I.dark = !StudioConfig.I.dark; StudioConfig.I.save(); });
        x = tb(g, "Fix colors", x, mx, my, () -> { StudioConfig.I.swapRB = !StudioConfig.I.swapRB; StudioConfig.I.save(); });
        x = tb(g, "Save", x + 6, mx, my, project::save);
        tb(g, "Exit", x, mx, my, this::onClose);
    }
    private void pickEyes() { if (sel == null) addActor(); part = BodyPart.FACE; tool = FaceRegion.Kind.EYE; rightScroll = 0; status = "Drag a box over the eyes in the face grid (or press Auto-pick eyes)"; }

    private void leftPanel(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int y = TOP + 6 - leftScroll;
        if (vis(y, 10)) txt(g, "Actors", 8, y, t.dim); y += 12;
        for (ActorData a : new ArrayList<>(project.actors)) {
            if (vis(y, 14)) {
                if (a == sel) g.fill(2, y - 1, LEFT - 2, y + 13, (t.accent & 0xFFFFFF) | 0x55000000);
                txt(g, a.name, 8, y + 2, t.text); hits.add(new Hit(2, y - 1, LEFT - 24, 14, () -> { sel = a; selKey = null; }, null));
                btn(g, "x", LEFT - 20, y - 1, 14, 14, mx, my, () -> { project.actors.remove(a); faces.remove(a.id); sel = project.actors.isEmpty() ? null : project.actors.get(0); });
            }
            y += 16;
        }
        y += 6; if (vis(y, 10)) txt(g, "Body parts  (click to edit)", 8, y, t.dim); y += 12;
        for (BodyPart p : BodyPart.values()) {
            int depth = 0; for (BodyPart q = ModelRenderer.parentOf(p); q != null; q = ModelRenderer.parentOf(q)) depth++;
            if (vis(y, 13)) {
                if (p == part) g.fill(2, y - 1, LEFT - 2, y + 12, (t.accent & 0xFFFFFF) | 0x55000000);
                boolean keyed = sel != null && sel.tracks.stream().anyMatch(tr -> tr.part == p && !tr.keys.isEmpty());
                txt(g, (keyed ? "\u2022 " : "") + p.label(), 8 + depth * 6, y + 2, t.text);
                hits.add(new Hit(2, y - 1, LEFT - 4, 13, () -> { part = p; rightScroll = 0; }, null));
            }
            y += 13;
        }
        leftEnd = y + leftScroll - (height - BOT - 6);
    }

    // ---------- inspector ----------
    private int iy;
    private boolean iv() { return vis(iy, 14); }
    private void ibtn(GuiGraphicsExtractor g, String s, int mx, int my, Runnable r) { if (iv()) btn(g, s, width - RIGHT + 8, iy, RIGHT - 16, 14, mx, my, r); iy += 17; }
    private void isl(GuiGraphicsExtractor g, String s, float min, float max, float v, java.util.function.DoubleConsumer set) {
        if (iv()) slider(g, s, width - RIGHT + 8, iy, RIGHT - 16, min, max, v, set); iy += 15;
    }
    private void inspector(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int x = width - RIGHT + 8; iy = TOP + 6 - rightScroll;
        if (iv()) txt(g, sel.name + "  >  " + part.label(), x, iy + 2, t.text); iy += 16;
        if (part == BodyPart.FACE) { facePanel(g, mx, my); rightEnd = iy + rightScroll - (height - BOT - 6); return; }
        boolean root = part == BodyPart.ROOT; boolean shown = val(part, Channel.VISIBLE) > 0.5f;
        ibtn(g, "Visible: " + (shown ? "yes" : "NO") + "  (click to toggle)", mx, my, () -> key(part, Channel.VISIBLE, shown ? 0 : 1));
        if (iv()) txt(g, root ? "Position (blocks)" : "Position (pixels)", x, iy + 2, t.dim); iy += 12;
        float pr = root ? 10 : 16;
        isl(g, "Pos X", -pr, pr, val(part, Channel.POS_X), v -> key(part, Channel.POS_X, (float) v));
        isl(g, "Pos Y", -pr, pr, val(part, Channel.POS_Y), v -> key(part, Channel.POS_Y, (float) v));
        isl(g, "Pos Z", -pr, pr, val(part, Channel.POS_Z), v -> key(part, Channel.POS_Z, (float) v));
        if (iv()) txt(g, "Rotation (degrees)", x, iy + 2, t.dim); iy += 12;
        isl(g, "Rot X", -180, 180, val(part, Channel.ROT_X), v -> key(part, Channel.ROT_X, (float) v));
        isl(g, "Rot Y", -180, 180, val(part, Channel.ROT_Y), v -> key(part, Channel.ROT_Y, (float) v));
        isl(g, "Rot Z", -180, 180, val(part, Channel.ROT_Z), v -> key(part, Channel.ROT_Z, (float) v));
        if (iv()) txt(g, "Scale", x, iy + 2, t.dim); iy += 12;
        float sa = (val(part, Channel.SCALE_X) + val(part, Channel.SCALE_Y) + val(part, Channel.SCALE_Z)) / 3f;
        isl(g, "All", 0.1f, 3f, sa, v -> { for (Channel c : new Channel[]{Channel.SCALE_X, Channel.SCALE_Y, Channel.SCALE_Z}) key(part, c, (float) v); });
        isl(g, "X", 0.1f, 3f, val(part, Channel.SCALE_X), v -> key(part, Channel.SCALE_X, (float) v));
        isl(g, "Y", 0.1f, 3f, val(part, Channel.SCALE_Y), v -> key(part, Channel.SCALE_Y, (float) v));
        isl(g, "Z", 0.1f, 3f, val(part, Channel.SCALE_Z), v -> key(part, Channel.SCALE_Z, (float) v));
        iy += 4;
        ibtn(g, "Key all values of this part here", mx, my, () -> { for (Channel c : new Channel[]{Channel.POS_X, Channel.POS_Y, Channel.POS_Z, Channel.ROT_X, Channel.ROT_Y,
            Channel.ROT_Z, Channel.SCALE_X, Channel.SCALE_Y, Channel.SCALE_Z}) key(part, c, val(part, c)); });
        ibtn(g, "Reset this part", mx, my, () -> sel.tracks.removeIf(tr -> tr.part == part));
        if (iv()) txt(g, "Moving a slider keys it at the playhead.", x, iy + 2, t.dim); iy += 14;
        skinRows(g, mx, my);
        rightEnd = iy + rightScroll - (height - BOT - 6);
    }
    private void skinRows(GuiGraphicsExtractor g, int mx, int my) {
        List<String> skins = SkinManager.list(); int idx = skins.indexOf(sel.skinFile); iy += 4;
        ibtn(g, "Skin: " + (sel.skinFile == null ? "Steve (default)" : sel.skinFile) + "  >", mx, my, () -> {
            sel.skinFile = skins.isEmpty() || idx + 1 >= skins.size() ? null : skins.get(idx + 1); reloadSkin(); });
        ibtn(g, "Arms: " + (sel.slim ? "slim (Alex)" : "wide (Steve)"), mx, my, () -> sel.slim = !sel.slim);
        if (iv()) txt(g, "Drop a .png onto the window to import a skin", width - RIGHT + 8, iy + 2, t().dim); iy += 14;
    }
    private void facePanel(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int x = width - RIGHT + 8; FaceSkin fs = face(sel);
        if (iv()) { int bx = btn(g, "Select eyes", x, iy, 66, 14, mx, my, () -> { tool = FaceRegion.Kind.EYE; status = "Drag a box over the eyes"; });
            bx = btn(g, "Select mouth", bx, iy, 72, 14, mx, my, () -> tool = FaceRegion.Kind.MOUTH);
            btn(g, "Clear", bx, iy, 40, 14, mx, my, () -> { sel.regions.clear(); fs.invalidate(); }); } iy += 17;
        ibtn(g, "Auto-pick eyes (standard skins)", mx, my, () -> { sel.regions.removeIf(r -> r.kind == FaceRegion.Kind.EYE);
            sel.regions.add(new FaceRegion(FaceRegion.Kind.EYE, 1, 4, 2, 1)); sel.regions.add(new FaceRegion(FaceRegion.Kind.EYE, 5, 4, 2, 1));
            sel.autoBlink = true; fs.invalidate(); status = "Eyes selected - blinking automatically"; });
        if (iv()) txt(g, tool == null ? "Pick a tool, then drag on the face" : "Tool: " + tool.name().toLowerCase() + " - drag on the face", x, iy + 2, t.dim); iy += 13;
        gridOk = vis(iy, 8 * CELL); gridX = x; gridY = iy;
        if (gridOk) {
            for (int py = 0; py < 8; py++) for (int px = 0; px < 8; px++)
                g.fill(x + px * CELL, iy + py * CELL, x + (px + 1) * CELL, iy + (py + 1) * CELL, 0xFF000000 | fs.facePixel(px, py));
            for (FaceRegion r : sel.regions) g.outline(x + r.x * CELL, iy + r.y * CELL, r.w * CELL, r.h * CELL, r.kind == FaceRegion.Kind.EYE ? 0xFF22DDFF : 0xFFFF5577);
            if (faceDrag) { int x0 = Math.min(fsx, fcx), y0 = Math.min(fsy, fcy), x1 = Math.max(fsx, fcx), y1 = Math.max(fsy, fcy);
                g.outline(x + x0 * CELL, iy + y0 * CELL, (x1 - x0 + 1) * CELL, (y1 - y0 + 1) * CELL, 0xFFFFFF00); }
        }
        iy += 8 * CELL + 6;
        ibtn(g, "Auto-blink: " + (sel.autoBlink ? "ON" : "off"), mx, my, () -> sel.autoBlink = !sel.autoBlink);
        isl(g, "Eyes closed", 0, 1, val(BodyPart.FACE, Channel.EYES_CLOSED), v -> key(BodyPart.FACE, Channel.EYES_CLOSED, (float) v));
        isl(g, "Mouth open", 0, 1, val(BodyPart.FACE, Channel.MOUTH_OPEN), v -> key(BodyPart.FACE, Channel.MOUTH_OPEN, (float) v));
        skinRows(g, mx, my);
    }

    // ---------- timeline ----------
    private int fx(float f) { return LABELS + (int) ((f - scroll) * ppf); }
    private float xf(double x) { return (float) ((x - LABELS) / ppf + scroll); }
    private int rowsTop() { return height - BOT + 18; }
    private List<Track> tracks() { return sel.tracks.stream().filter(tr -> tr.part == part).toList(); }
    private void timeline(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int y0 = height - BOT, W = width;
        g.fill(LABELS, y0 + 2, W, y0 + 16, t.panel);
        int step = ppf >= 8 ? 1 : ppf >= 3 ? 5 : ppf >= 1 ? 25 : 100;
        g.enableScissor(LABELS, y0 + 1, W, height);
        for (int f = (int) (Math.floor(scroll / step) * step); fx(f) < W; f += step) { int x = fx(f); g.fill(x, y0 + 10, x + 1, y0 + 16, t.dim); txt(g, "" + f, x + 2, y0 + 3, t.dim); }
        int y = rowsTop();
        for (Track tr : tracks()) {
            if (y > height - 12) break;
            g.fill(LABELS, y, W, y + 11, t.panel); int lastX = Integer.MIN_VALUE;
            for (Keyframe k : tr.keys) { int x = fx(k.frame); if (x < LABELS - 4 || x > W) continue; if (x == lastX && k != selKey) continue; lastX = x;
                g.fill(x - 2, y + 2, x + 3, y + 9, k == selKey ? t.accent : t.text); }
            y += 12;
        }
        g.fill(fx(frame()), y0 + 2, fx(frame()) + 1, height, t.accent);
        g.disableScissor();
        y = rowsTop();
        for (Track tr : tracks()) { txt(g, tr.channel.name(), 6, y + 2, t.dim); y += 12; if (y > height - 12) break; }
        txt(g, selKey != null ? "Key easing: " + selKey.easing + "  (E = change, Del = delete)" : part.label() + " - timeline", 6, y0 + 4, t.text);
    }

    // ============================== INPUT ==============================
    private boolean overHit(double mx, double my) { for (Hit h : hits) if (h.in(mx, my)) return true; return false; }
    private boolean inViewport(double mx, double my) { return mx > LEFT && mx < width - RIGHT && my > TOP && my < height - BOT; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        if (sel != null && part == BodyPart.FACE && tool != null && gridOk && mx >= gridX && mx < gridX + 8 * CELL && my >= gridY && my < gridY + 8 * CELL) {
            fsx = fcx = (int) ((mx - gridX) / CELL); fsy = fcy = (int) ((my - gridY) / CELL); faceDrag = true; return true;
        }
        if (super.mouseClicked(e, dbl)) return true;
        if (inViewport(mx, my)) { orbit = true; return true; }
        if (sel != null && my >= height - BOT && mx >= LABELS) {
            if (my < height - BOT + 17) { scrubbing = true; AnimationPlayer.frame = Math.max(0, xf(mx)); return true; }
            int row = (int) ((my - rowsTop()) / 12); List<Track> trs = tracks();
            if (row >= 0 && row < trs.size()) {
                selKey = null; selTrack = trs.get(row);
                for (Keyframe k : selTrack.keys) if (Math.abs(fx(k.frame) - mx) <= 4) { selKey = k; keyDrag = true; break; }
                if (selKey != null) AnimationPlayer.frame = selKey.frame;
                return true;
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (faceDrag) { fcx = Mth.clamp((int) ((e.x() - gridX) / CELL), 0, 7); fcy = Mth.clamp((int) ((e.y() - gridY) / CELL), 0, 7); return true; }
        if (orbit) { camYaw += dx * 0.6; camPitch = Mth.clamp(camPitch + dy * 0.4, -85, 85); return true; }
        if (scrubbing) { AnimationPlayer.frame = Mth.clamp(xf(e.x()), 0, project.lengthFrames); return true; }
        if (keyDrag && selKey != null) { selKey.frame = Math.max(0, Math.round(xf(e.x()))); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (faceDrag) {
            faceDrag = false; int x0 = Math.min(fsx, fcx), y0 = Math.min(fsy, fcy), x1 = Math.max(fsx, fcx), y1 = Math.max(fsy, fcy);
            if (tool == FaceRegion.Kind.MOUTH) sel.regions.removeIf(r -> r.kind == FaceRegion.Kind.MOUTH);
            else if (sel.regions.stream().filter(r -> r.kind == FaceRegion.Kind.EYE).count() >= 2) sel.regions.removeIf(r -> r.kind == FaceRegion.Kind.EYE);
            sel.regions.add(new FaceRegion(tool, x0, y0, x1 - x0 + 1, y1 - y0 + 1));
            if (tool == FaceRegion.Kind.EYE) { sel.autoBlink = true; status = "Eyes selected - blinking automatically"; }
            face(sel).invalidate();
        }
        if (keyDrag && selTrack != null) selTrack.keys.sort(Comparator.comparingDouble(k -> k.frame));
        orbit = scrubbing = keyDrag = false; return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my > height - BOT) { if (down(GLFW.GLFW_KEY_LEFT_SHIFT)) scroll = Math.max(0, scroll - (float) sy * 20 / ppf); else ppf = Mth.clamp(ppf * (sy > 0 ? 1.2f : 0.83f), 0.3f, 40f); return true; }
        if (mx < LEFT) { leftScroll = Mth.clamp(leftScroll - (int) (sy * 14), 0, Math.max(0, leftEnd)); return true; }
        if (mx > width - RIGHT) { rightScroll = Mth.clamp(rightScroll - (int) (sy * 14), 0, Math.max(0, rightEnd)); return true; }
        if (inViewport(mx, my)) { zoom = Mth.clamp(zoom * (sy > 0 ? 1.12 : 0.89), 2, 40); return true; }
        return false;
    }
    @Override public boolean keyPressed(KeyEvent k) {
        if (k.key() == GLFW.GLFW_KEY_SPACE) { AnimationPlayer.playing = !AnimationPlayer.playing; return true; }
        if (k.key() == GLFW.GLFW_KEY_DELETE && selKey != null && selTrack != null) { selTrack.keys.remove(selKey); selKey = null; return true; }
        if (k.key() == GLFW.GLFW_KEY_E && selKey != null) { selKey.easing = selKey.easing.next(); return true; }
        return super.keyPressed(k);
    }
    @Override public void onFilesDrop(List<Path> paths) {
        if (sel == null) return;
        for (Path p : paths) if (p.toString().toLowerCase().endsWith(".png")) {
            try { sel.skinFile = SkinManager.importSkin(p); reloadSkin(); status = "Skin imported"; } catch (Exception ex) { ex.printStackTrace(); } return;
        }
    }
    private void reloadSkin() { faces.remove(sel.id); }
    private void addActor() {
        ActorData a = new ActorData(); a.name = "Actor " + (project.actors.size() + 1);
        project.actors.add(a); sel = a;
    }
    @Override public void onClose() { project.save(); minecraft.setScreenAndShow(parent); }
}
