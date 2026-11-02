package com.example.animstudio.gui;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.actor.*;
import com.example.animstudio.anim.AnimationPlayer;
import com.example.animstudio.data.*;
import com.example.animstudio.world.LightRig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.*;

/**
 * Mine-imator style layout:  top bar | outliner (left) | VIEWPORT (centre, the real world) | inspector (right) | timeline (bottom).
 * The centre is intentionally transparent: you are looking through a free-fly camera at the live world (with Iris shaders if enabled).
 */
public final class StudioScreen extends UiScreen {
    static final int TOP = 22, LEFT = 150, RIGHT = 210, BOT = 120, LABELS = 100;
    private final Screen parent; private final StudioProject project;
    private ActorData sel; private BodyPart part = BodyPart.HEAD;
    private FaceRegion.Kind tool; private int fsx, fsy, fcx, fcy; private boolean faceDrag;
    private int gridX, gridY; private static final int CELL = 16;
    private float ppf = 6f, scroll = 0f; private boolean scrubbing, rmb; private Track selTrack; private Keyframe selKey; private boolean keyDrag;
    private int lightLevel = 15;

    public StudioScreen(Screen parent, StudioProject project) {
        super("Studio"); this.parent = parent; this.project = project;
        AnimationPlayer.project = project; AnimationPlayer.frame = 0; AnimationPlayer.playing = false;
        if (!project.actors.isEmpty()) sel = project.actors.get(0);
        ActorManager.sync(project);
    }
    private float frame() { return AnimationPlayer.frame; }
    private void key(BodyPart p, Channel c, float v) { sel.track(p, c, true).set(frame(), v, Easing.SMOOTH); }
    private float val(BodyPart p, Channel c, float def) { return sel.sample(p, c, frame(), def); }

    // ============================== RENDER ==============================
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        hits.clear(); Theme t = t(); int W = width, H = height;
        g.fill(0, TOP, LEFT, H - BOT, t.panel); g.fill(W - RIGHT, TOP, W, H - BOT, t.panel);   // side panels
        g.fill(0, H - BOT, W, H, t.panelAlt); g.fill(0, 0, W, TOP, t.panel);
        g.fill(0, TOP - 1, W, TOP, t.line); g.fill(0, H - BOT, W, H - BOT + 1, t.line);
        g.fill(LEFT, TOP, LEFT + 1, H - BOT, t.line); g.fill(W - RIGHT, TOP, W - RIGHT + 1, H - BOT, t.line);
        topBar(g, mx, my); outliner(g, mx, my);
        if (sel != null) { inspector(g, mx, my); timeline(g, mx, my); }
        else txt(g, "Add an actor with  + Actor", W - RIGHT + 8, TOP + 8, t.dim);
        // viewport HUD (the world itself shows through)
        txt(g, String.format("Frame %.1f / %d   %s%s", frame(), project.lengthFrames, AnimationPlayer.playing ? "PLAYING" : "paused",
            StudioConfig.I.fps15 ? "   [15 FPS mode]" : ""), LEFT + 8, TOP + 6, 0xFFFFFFFF);
        txt(g, "RMB drag: look   WASD/Ctrl up, Shift down: fly, Space: play/pause   Drop a .png: skin", LEFT + 8, H - BOT - 12, 0xFFDDDDDD);
    }

    private int tb(GuiGraphicsExtractor g, String s, int x, int mx, int my, Runnable r) { return btn(g, s, x, 3, font.width(s) + 12, 16, mx, my, r); }
    private void topBar(GuiGraphicsExtractor g, int mx, int my) {
        int x = 4;
        x = tb(g, AnimationPlayer.playing ? "Pause" : "Play", x, mx, my, () -> AnimationPlayer.playing = !AnimationPlayer.playing);
        x = tb(g, "Stop", x, mx, my, () -> { AnimationPlayer.playing = false; AnimationPlayer.frame = 0; });
        x = tb(g, "+ Actor", x + 6, mx, my, this::addActor);
        x = tb(g, "Light+", x + 6, mx, my, () -> LightRig.placeEmitter(lightLevel));
        x = tb(g, "Lv " + lightLevel, x, mx, my, () -> lightLevel = lightLevel <= 3 ? 15 : lightLevel - 3);
        x = tb(g, "Dark+", x, mx, my, () -> LightRig.placeDark(6));
        x = tb(g, "Shaders", x + 6, mx, my, () -> minecraft.setScreenAndShow(new ShaderScreen(this)));
        x = tb(g, "15 FPS: " + (StudioConfig.I.fps15 ? "ON" : "off"), x, mx, my, () -> { StudioConfig.I.fps15 = !StudioConfig.I.fps15; StudioConfig.I.save(); });
        x = tb(g, StudioConfig.I.dark ? "Dark" : "Light", x, mx, my, () -> { StudioConfig.I.dark = !StudioConfig.I.dark; StudioConfig.I.save(); });
        x = tb(g, "Save", x + 6, mx, my, project::save);
        tb(g, "Exit", x, mx, my, this::onClose);
    }
    private void outliner(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); txt(g, "Scene", 8, TOP + 6, t.dim); int y = TOP + 20;
        for (ActorData a : project.actors) {
            if (a == sel) g.fill(2, y - 1, LEFT - 2, y + 13, (t.accent & 0xFFFFFF) | 0x55000000);
            txt(g, a.name, 8, y + 2, t.text); hits.add(new Hit(2, y - 1, LEFT - 22, 14, () -> { sel = a; selKey = null; }, null));
            btn(g, "x", LEFT - 20, y - 1, 14, 14, mx, my, () -> { project.actors.remove(a); if (sel == a) sel = project.actors.isEmpty() ? null : project.actors.get(0); ActorManager.sync(project); });
            y += 16;
        }
        txt(g, project.lights.size() + " light point(s)", 8, height - BOT - 12, t.dim);
    }

    private void inspector(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int x = width - RIGHT + 8, w = RIGHT - 16, y = TOP + 6;
        txt(g, sel.name, x, y, t.text); y += 14;
        List<String> skins = SkinManager.list(); int idx = skins.indexOf(sel.skinFile);
        btn(g, "Skin: " + (sel.skinFile == null ? "Steve (default)" : sel.skinFile), x, y, w, 14, mx, my, () -> {
            sel.skinFile = skins.isEmpty() ? null : (idx + 1 >= skins.size() ? null : skins.get(idx + 1)); reloadSkin(); }); y += 17;
        btn(g, "Arms: " + (sel.slim ? "slim" : "wide"), x, y, w, 14, mx, my, () -> { sel.slim = !sel.slim; reloadSkin(); }); y += 20;
        int bx = x, by = y;
        for (BodyPart p : BodyPart.values()) {
            String l = p.label(); int bw = (w - 3) / 2;
            btn(g, (p == part ? "> " : "") + l, bx, by, bw, 14, mx, my, () -> part = p);
            if (bx == x) bx = x + bw + 3; else { bx = x; by += 17; }
        }
        y = by + (bx == x ? 0 : 17) + 6;
        switch (part) {
            case ROOT -> {
                btn(g, "Key position = my position", x, y, w, 14, mx, my, () -> { LocalPlayer p = minecraft.player; if (p == null) return;
                    key(BodyPart.ROOT, Channel.POS_X, (float) p.getX()); key(BodyPart.ROOT, Channel.POS_Y, (float) p.getY());
                    key(BodyPart.ROOT, Channel.POS_Z, (float) p.getZ()); key(BodyPart.ROOT, Channel.ROT_Y, p.getYRot()); }); y += 18;
                slider(g, "Yaw", x, y, w, -180, 180, val(BodyPart.ROOT, Channel.ROT_Y, sel.spawnYaw), v -> key(BodyPart.ROOT, Channel.ROT_Y, (float) v));
            }
            case FACE -> face(g, x, y, w, mx, my);
            default -> {
                slider(g, "Rot X", x, y, w, -180, 180, val(part, Channel.ROT_X, 0), v -> key(part, Channel.ROT_X, (float) v)); y += 16;
                slider(g, "Rot Y", x, y, w, -180, 180, val(part, Channel.ROT_Y, 0), v -> key(part, Channel.ROT_Y, (float) v)); y += 16;
                slider(g, "Rot Z", x, y, w, -180, 180, val(part, Channel.ROT_Z, 0), v -> key(part, Channel.ROT_Z, (float) v)); y += 18;
                txt(g, "Moving a slider keys it at the playhead.", x, y, t.dim);
            }
        }
    }
    private void face(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        Theme t = t(); ActorEntity e = ActorManager.get(sel);
        int bx = btn(g, "Select eyes", x, y, 70, 14, mx, my, () -> tool = FaceRegion.Kind.EYE);
        bx = btn(g, "Select mouth", bx, y, 76, 14, mx, my, () -> tool = FaceRegion.Kind.MOUTH);
        btn(g, "Clear", bx, y, 36, 14, mx, my, () -> sel.regions.clear()); y += 18;
        txt(g, tool == null ? "Pick a tool, then drag on the face" : "Drag a box over the " + tool.name().toLowerCase(), x, y, t.dim); y += 12;
        gridX = x; gridY = y;
        if (e != null) for (int py = 0; py < 8; py++) for (int px = 0; px < 8; px++)
            g.fill(x + px * CELL, y + py * CELL, x + (px + 1) * CELL, y + (py + 1) * CELL, 0xFF000000 | e.skin.face().facePixel(px, py));
        for (FaceRegion r : sel.regions) g.outline(x + r.x * CELL, y + r.y * CELL, r.w * CELL, r.h * CELL, r.kind == FaceRegion.Kind.EYE ? 0xFF22DDFF : 0xFFFF5577);
        if (faceDrag) { int x0 = Math.min(fsx, fcx), y0 = Math.min(fsy, fcy), x1 = Math.max(fsx, fcx), y1 = Math.max(fsy, fcy);
            g.outline(x + x0 * CELL, y + y0 * CELL, (x1 - x0 + 1) * CELL, (y1 - y0 + 1) * CELL, 0xFFFFFF00); }
        y += 8 * CELL + 6;
        btn(g, "Auto-blink: " + (sel.autoBlink ? "ON" : "off"), x, y, w, 14, mx, my, () -> sel.autoBlink = !sel.autoBlink); y += 18;
        slider(g, "Eyes closed", x, y, w, 0, 1, val(BodyPart.FACE, Channel.EYES_CLOSED, 0), v -> key(BodyPart.FACE, Channel.EYES_CLOSED, (float) v)); y += 16;
        slider(g, "Mouth open", x, y, w, 0, 1, val(BodyPart.FACE, Channel.MOUTH_OPEN, 0), v -> key(BodyPart.FACE, Channel.MOUTH_OPEN, (float) v));
    }

    // ============================== TIMELINE ==============================
    private int fx(float f) { return LABELS + (int) ((f - scroll) * ppf); }
    private float xf(double x) { return (float) ((x - LABELS) / ppf + scroll); }
    private int rowsTop() { return height - BOT + 18; }
    private List<Track> visibleTracks() { return sel.tracks.stream().filter(tr -> tr.part == part).toList(); }
    private void timeline(GuiGraphicsExtractor g, int mx, int my) {
        Theme t = t(); int y0 = height - BOT, W = width;
        g.fill(LABELS, y0 + 2, W, y0 + 16, t.panel);                                     // ruler
        int step = ppf >= 8 ? 1 : ppf >= 3 ? 5 : ppf >= 1 ? 25 : 100;
        g.enableScissor(LABELS, y0 + 1, W, height);
        for (int f = (int) (Math.floor(scroll / step) * step); fx(f) < W; f += step) {
            int x = fx(f); g.fill(x, y0 + 10, x + 1, y0 + 16, t.dim); txt(g, "" + f, x + 2, y0 + 3, t.dim);
        }
        int y = rowsTop();
        for (Track tr : visibleTracks()) {
            if (y > height - 12) break;
            g.fill(LABELS, y, W, y + 11, t.panel); int lastX = Integer.MIN_VALUE;
            for (Keyframe k : tr.keys) {
                int x = fx(k.frame); if (x < LABELS - 4 || x > W) continue; if (x == lastX && k != selKey) continue; lastX = x;
                g.fill(x - 2, y + 2, x + 3, y + 9, k == selKey ? t.accent : t.text);
            }
            y += 12;
        }
        int px = fx(frame()); g.fill(px, y0 + 2, px + 1, height, t.accent);                // playhead
        g.disableScissor();
        y = rowsTop();
        for (Track tr : visibleTracks()) { txt(g, tr.part.label() + "." + tr.channel.name(), 6, y + 2, t.dim); y += 12; if (y > height - 12) break; }
        if (selKey != null) txt(g, "Key easing: " + selKey.easing + "  (E to cycle, Del to delete)", 6, y0 + 4, t.text);
        else txt(g, "Timeline", 6, y0 + 4, t.dim);
    }

    // ============================== INPUT ==============================
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        if (e.button() == 1 && inViewport(mx, my)) { rmb = true; return true; }
        if (sel != null && part == BodyPart.FACE && tool != null && mx >= gridX && mx < gridX + 8 * CELL && my >= gridY && my < gridY + 8 * CELL && !overHit(mx, my)) {
            fsx = fcx = (int) ((mx - gridX) / CELL); fsy = fcy = (int) ((my - gridY) / CELL); faceDrag = true; return true;
        }
        if (sel != null && my >= height - BOT && mx >= LABELS && !overHit(mx, my)) {
            if (my < height - BOT + 17) { scrubbing = true; AnimationPlayer.frame = Math.max(0, xf(mx)); return true; }
            int row = (int) ((my - rowsTop()) / 12); List<Track> trs = visibleTracks();
            if (row >= 0 && row < trs.size()) {
                selKey = null; selTrack = trs.get(row);
                for (Keyframe k : selTrack.keys) if (Math.abs(fx(k.frame) - mx) <= 4) { selKey = k; keyDrag = true; break; }
                if (selKey != null) AnimationPlayer.frame = selKey.frame;
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }
    private boolean overHit(double mx, double my) { for (Hit h : hits) if (h.in(mx, my)) return true; return false; }
    private boolean inViewport(double mx, double my) { return mx > LEFT && mx < width - RIGHT && my > TOP && my < height - BOT; }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (rmb && minecraft.player != null) { LocalPlayer p = minecraft.player; p.setYRot(p.getYRot() + (float) dx * 0.25f);
            p.setXRot(Mth.clamp(p.getXRot() + (float) dy * 0.25f, -90f, 90f)); return true; }
        if (faceDrag) { fcx = Mth.clamp((int) ((e.x() - gridX) / CELL), 0, 7); fcy = Mth.clamp((int) ((e.y() - gridY) / CELL), 0, 7); return true; }
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
        }
        if (keyDrag && selTrack != null) selTrack.keys.sort(Comparator.comparingDouble(k -> k.frame));
        rmb = scrubbing = keyDrag = false; return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my > height - BOT) { if (hasShiftDown()) scroll = Math.max(0, scroll - (float) sy * 10 / ppf * 3); else ppf = Mth.clamp(ppf * (sy > 0 ? 1.2f : 0.83f), 0.3f, 40f); return true; }
        return false;
    }
    @Override public boolean keyPressed(KeyEvent k) {
        if (k.key() == GLFW.GLFW_KEY_SPACE && !inTextFieldFly()) { AnimationPlayer.playing = !AnimationPlayer.playing; return true; }
        if (k.key() == GLFW.GLFW_KEY_DELETE && selKey != null && selTrack != null) { selTrack.keys.remove(selKey); selKey = null; return true; }
        if (k.key() == GLFW.GLFW_KEY_E && selKey != null) { selKey.easing = selKey.easing.next(); return true; }
        return super.keyPressed(k);
    }
    private boolean inTextFieldFly() { return false; }       // Space plays/pauses; fly-up uses Space only via held-key polling in tick()
    @Override public void onFilesDrop(List<Path> paths) {
        if (sel == null) return;
        for (Path p : paths) if (p.toString().toLowerCase().endsWith(".png")) {
            try { sel.skinFile = SkinManager.importSkin(p); reloadSkin(); } catch (Exception ex) { ex.printStackTrace(); } return;
        }
    }
    private void reloadSkin() { ActorEntity e = ActorManager.get(sel); if (e != null) e.reloadSkin(); }

    // free-fly camera: the studio keeps the player as the camera
    @Override public void tick() {
        LocalPlayer p = minecraft.player; if (p == null) return;
        double yaw = Math.toRadians(p.getYRot()); Vec3 f = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), r = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 v = Vec3.ZERO; double sp = 0.45;
        if (down(GLFW.GLFW_KEY_W)) v = v.add(f); if (down(GLFW.GLFW_KEY_S)) v = v.subtract(f);
        if (down(GLFW.GLFW_KEY_D)) v = v.add(r); if (down(GLFW.GLFW_KEY_A)) v = v.subtract(r);
        if (down(GLFW.GLFW_KEY_LEFT_SHIFT)) v = v.add(0, -1, 0);
        if (down(GLFW.GLFW_KEY_E) && false) v = v.add(0, 1, 0);
        if (down(GLFW.GLFW_KEY_LEFT_CONTROL)) v = v.add(0, 1, 0);   // Space is play/pause here; Ctrl = up
        p.getAbilities().flying = p.getAbilities().mayfly;
        p.setDeltaMovement(v.lengthSqr() > 0 ? v.normalize().scale(sp) : Vec3.ZERO);
    }
    private boolean down(int key) { return InputConstants.isKeyDown(minecraft.getWindow(), key); }

    private void addActor() {
        LocalPlayer p = minecraft.player; ActorData a = new ActorData(); a.name = "Actor " + (project.actors.size() + 1);
        if (p != null) { Vec3 fwd = Vec3.directionFromRotation(0, p.getYRot()).scale(3); a.spawnX = p.getX() + fwd.x; a.spawnY = p.getY(); a.spawnZ = p.getZ() + fwd.z; a.spawnYaw = p.getYRot() + 180; }
        project.actors.add(a); sel = a; ActorManager.sync(project);
    }
    @Override public void onClose() { project.save(); minecraft.setScreenAndShow(parent); }
}
