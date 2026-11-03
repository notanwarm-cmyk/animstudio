package com.example.animstudio.render;
import com.example.animstudio.data.BodyPart;
import java.util.*;
/**
 * Tiny software renderer for the Minecraft player model (orthographic). Needs NO world, entity or GPU API:
 * it fills an ARGB int[] which the GUI uploads as one texture. Every body part has its own transform + visibility.
 */
public final class ModelRenderer {
    public record Def(BodyPart part, BodyPart parent, float[] pivot, float[] min, float[] max, int u, int v, float inflate, int[] flat) {}

    public static BodyPart parentOf(BodyPart p) {
        return switch (p) {
            case TORSO, RIGHT_LEG, LEFT_LEG -> BodyPart.ROOT;
            case HEAD, JACKET, RIGHT_ARM, LEFT_ARM -> BodyPart.TORSO;
            case HAT, EAR_L, EAR_R -> BodyPart.HEAD;
            case RIGHT_SLEEVE -> BodyPart.RIGHT_ARM; case LEFT_SLEEVE -> BodyPart.LEFT_ARM;
            case RIGHT_PANTS -> BodyPart.RIGHT_LEG; case LEFT_PANTS -> BodyPart.LEFT_LEG;
            default -> null;
        };
    }
    public static List<Def> defs(boolean slim) {
        float aw = slim ? 3 : 4, ap = slim ? 5.5f : 6f; List<Def> d = new ArrayList<>();
        float[] tmin = {-4, 12, -2}, tmax = {4, 24, 2}, hmin = {-4, 24, -4}, hmax = {4, 32, 4};
        float[] ra0 = {-4 - aw, 12, -2}, ra1 = {-4, 24, 2}, la0 = {4, 12, -2}, la1 = {4 + aw, 24, 2};
        float[] rl0 = {-4, 0, -2}, rl1 = {0, 12, 2}, ll0 = {0, 0, -2}, ll1 = {4, 12, 2};
        d.add(new Def(BodyPart.TORSO, BodyPart.ROOT, new float[]{0, 12, 0}, tmin, tmax, 16, 16, 0, null));
        d.add(new Def(BodyPart.HEAD, BodyPart.TORSO, new float[]{0, 24, 0}, hmin, hmax, 0, 0, 0, null));
        d.add(new Def(BodyPart.HAT, BodyPart.HEAD, new float[]{0, 24, 0}, hmin, hmax, 32, 0, 0.5f, null));
        d.add(new Def(BodyPart.EAR_L, BodyPart.HEAD, new float[]{4.5f, 32, 0}, new float[]{1.5f, 32, -0.5f}, new float[]{7.5f, 38, 0.5f}, 0, 0, 0, new int[]{8, 0, 6, 6}));
        d.add(new Def(BodyPart.EAR_R, BodyPart.HEAD, new float[]{-4.5f, 32, 0}, new float[]{-7.5f, 32, -0.5f}, new float[]{-1.5f, 38, 0.5f}, 0, 0, 0, new int[]{8, 0, 6, 6}));
        d.add(new Def(BodyPart.JACKET, BodyPart.TORSO, new float[]{0, 12, 0}, tmin, tmax, 16, 32, 0.25f, null));
        d.add(new Def(BodyPart.RIGHT_ARM, BodyPart.TORSO, new float[]{-ap, 22, 0}, ra0, ra1, 40, 16, 0, null));
        d.add(new Def(BodyPart.RIGHT_SLEEVE, BodyPart.RIGHT_ARM, new float[]{-ap, 22, 0}, ra0, ra1, 40, 32, 0.25f, null));
        d.add(new Def(BodyPart.LEFT_ARM, BodyPart.TORSO, new float[]{ap, 22, 0}, la0, la1, 32, 48, 0, null));
        d.add(new Def(BodyPart.LEFT_SLEEVE, BodyPart.LEFT_ARM, new float[]{ap, 22, 0}, la0, la1, 48, 48, 0.25f, null));
        d.add(new Def(BodyPart.RIGHT_LEG, BodyPart.ROOT, new float[]{-2, 12, 0}, rl0, rl1, 0, 16, 0, null));
        d.add(new Def(BodyPart.RIGHT_PANTS, BodyPart.RIGHT_LEG, new float[]{-2, 12, 0}, rl0, rl1, 0, 32, 0.25f, null));
        d.add(new Def(BodyPart.LEFT_LEG, BodyPart.ROOT, new float[]{2, 12, 0}, ll0, ll1, 16, 48, 0, null));
        d.add(new Def(BodyPart.LEFT_PANTS, BodyPart.LEFT_LEG, new float[]{2, 12, 0}, ll0, ll1, 0, 48, 0.25f, null));
        return d;
    }

    private static Mat local(float[] p, float[] piv, double posMul) {
        return Mat.translate(p[0] * posMul, p[1] * posMul, p[2] * posMul).mul(Mat.translate(piv[0], piv[1], piv[2]))
                .mul(Mat.rot(p[3], p[4], p[5])).mul(Mat.scale(p[6], p[7], p[8])).mul(Mat.translate(-piv[0], -piv[1], -piv[2]));
    }

    /** @param pose from PoseEval  @param yaw/pitch camera orbit (deg)  @param zoom screen px per model px  @param target orbit centre (model px) */
    public static void render(int[] px, int W, int H, int[] skin, boolean slim, float[][] pose,
                              double yaw, double pitch, double zoom, double[] target, int gridColor, boolean grid) {
        Arrays.fill(px, 0); float[] z = new float[W * H]; Arrays.fill(z, -1e9f);
        Mat view = Mat.rot(pitch, 0, 0).mul(Mat.rot(0, yaw, 0)).mul(Mat.translate(-target[0], -target[1], -target[2]));
        if (grid) drawGrid(px, W, H, view, zoom, gridColor);
        Mat[] wm = new Mat[BodyPart.values().length]; boolean[] vis = new boolean[wm.length];
        wm[0] = local(pose[0], new float[]{0, 0, 0}, 16); vis[0] = true;
        for (Def d : defs(slim)) {
            int i = d.part().ordinal(), pi = d.parent().ordinal();
            wm[i] = wm[pi].mul(local(pose[i], d.pivot(), 1));
            vis[i] = vis[pi] && pose[i][9] > 0.5f;
            if (vis[i]) box(px, z, W, H, skin, view.mul(wm[i]), d, zoom);
        }
    }

    private static final float[] SHADE = {0.92f, 0.78f, 0.62f, 0.70f, 1.0f, 0.5f};
    private static void box(int[] px, float[] zb, int W, int H, int[] skin, Mat M, Def d, double zoom) {
        double inf = d.inflate();
        double x0 = d.min()[0] - inf, y0 = d.min()[1] - inf, z0 = d.min()[2] - inf, x1 = d.max()[0] + inf, y1 = d.max()[1] + inf, z1 = d.max()[2] + inf;
        double fw = x1 - x0, fh = y1 - y0, fd = z1 - z0;
        int w = Math.round(d.max()[0] - d.min()[0]), h = Math.round(d.max()[1] - d.min()[1]), dd = Math.round(d.max()[2] - d.min()[2]), u = d.u(), v = d.v();
        double[][] O = {{x0, y1, z1}, {x1, y1, z0}, {x0, y1, z0}, {x1, y1, z1}, {x0, y1, z0}, {x0, y0, z1}};
        double[][] U = {{fw, 0, 0}, {-fw, 0, 0}, {0, 0, fd}, {0, 0, -fd}, {fw, 0, 0}, {fw, 0, 0}};
        double[][] V = {{0, -fh, 0}, {0, -fh, 0}, {0, -fh, 0}, {0, -fh, 0}, {0, 0, fd}, {0, 0, -fd}};
        int[][] R = {{u + dd, v + dd, w, h}, {u + 2 * dd + w, v + dd, w, h}, {u, v + dd, dd, h}, {u + dd + w, v + dd, dd, h}, {u + dd, v, w, dd}, {u + dd + w, v, w, dd}};
        double[] a = new double[3], b = new double[3], c = new double[3];
        for (int f = 0; f < 6; f++) {
            int[] r = d.flat() != null ? d.flat() : R[f];
            M.point(O[f][0], O[f][1], O[f][2], a); M.vec(U[f][0], U[f][1], U[f][2], b); M.vec(V[f][0], V[f][1], V[f][2], c);
            // visible only if outward normal (V x U) points to the camera (+z in view space)
            if (c[0] * b[1] - c[1] * b[0] <= 0) continue;
            double ox = W / 2.0 + a[0] * zoom, oy = H / 2.0 - a[1] * zoom;
            double ux = b[0] * zoom, uy = -b[1] * zoom, vx = c[0] * zoom, vy = -c[1] * zoom;
            double det = ux * vy - uy * vx; if (Math.abs(det) < 1e-6) continue;
            double minX = Math.min(Math.min(ox, ox + ux), Math.min(ox + vx, ox + ux + vx)), maxX = Math.max(Math.max(ox, ox + ux), Math.max(ox + vx, ox + ux + vx));
            double minY = Math.min(Math.min(oy, oy + uy), Math.min(oy + vy, oy + uy + vy)), maxY = Math.max(Math.max(oy, oy + uy), Math.max(oy + vy, oy + uy + vy));
            int xa = Math.max(0, (int) Math.floor(minX)), xb = Math.min(W - 1, (int) Math.ceil(maxX)), ya = Math.max(0, (int) Math.floor(minY)), yb = Math.min(H - 1, (int) Math.ceil(maxY));
            float shade = SHADE[f];
            for (int y = ya; y <= yb; y++) for (int x = xa; x <= xb; x++) {
                double dx = x + 0.5 - ox, dy = y + 0.5 - oy;
                double s = (dx * vy - dy * vx) / det, t = (ux * dy - uy * dx) / det;
                if (s < -0.002 || s >= 1.002 || t < -0.002 || t >= 1.002) continue;
                float depth = (float) (a[2] + s * b[2] + t * c[2]); int idx = y * W + x;
                if (depth <= zb[idx]) continue;
                int tx = r[0] + Math.max(0, Math.min(r[2] - 1, (int) (s * r[2]))), ty = r[1] + Math.max(0, Math.min(r[3] - 1, (int) (t * r[3])));
                if (tx < 0 || ty < 0 || tx >= 64 || ty >= 64) continue;
                int col = skin[ty * 64 + tx]; if ((col >>> 24) < 128) continue;
                zb[idx] = depth;
                int rr = (int) (((col >> 16) & 255) * shade), gg = (int) (((col >> 8) & 255) * shade), bb = (int) ((col & 255) * shade);
                px[idx] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
    }
    private static void drawGrid(int[] px, int W, int H, Mat view, double zoom, int color) {
        double[] p = new double[3], q = new double[3];
        for (int i = -6; i <= 6; i++) {
            line(px, W, H, view, zoom, i * 16, -96, i * 16, 96, color, p, q);
            line(px, W, H, view, zoom, -96, i * 16, 96, i * 16, color, p, q);
        }
    }
    private static void line(int[] px, int W, int H, Mat v, double zoom, double x0, double z0, double x1, double z1, int color, double[] p, double[] q) {
        v.point(x0, 0, z0, p); v.point(x1, 0, z1, q);
        int ax = (int) (W / 2.0 + p[0] * zoom), ay = (int) (H / 2.0 - p[1] * zoom), bx = (int) (W / 2.0 + q[0] * zoom), by = (int) (H / 2.0 - q[1] * zoom);
        int dx = Math.abs(bx - ax), dy = -Math.abs(by - ay), sx = ax < bx ? 1 : -1, sy = ay < by ? 1 : -1, err = dx + dy;
        for (int guard = 0; guard < 4000; guard++) {
            if (ax >= 0 && ay >= 0 && ax < W && ay < H) px[ay * W + ax] = color;
            if (ax == bx && ay == by) break;
            int e2 = 2 * err; if (e2 >= dy) { err += dy; ax += sx; } if (e2 <= dx) { err += dx; ay += sy; }
        }
    }
}
