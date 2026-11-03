package com.example.animstudio.render;
/** 3x4 affine matrix, row-major: [r00 r01 r02 tx | r10 r11 r12 ty | r20 r21 r22 tz]. */
public final class Mat {
    public final double[] m = new double[12];
    public static Mat id() { Mat r = new Mat(); r.m[0] = r.m[5] = r.m[10] = 1; return r; }
    public static Mat translate(double x, double y, double z) { Mat r = id(); r.m[3] = x; r.m[7] = y; r.m[11] = z; return r; }
    public static Mat scale(double x, double y, double z) { Mat r = new Mat(); r.m[0] = x; r.m[5] = y; r.m[10] = z; return r; }
    /** Rx, then Ry, then Rz (degrees). */
    public static Mat rot(double rx, double ry, double rz) {
        return rz(rz).mul(ry(ry)).mul(rx(rx));
    }
    private static Mat rx(double d) { double c = Math.cos(Math.toRadians(d)), s = Math.sin(Math.toRadians(d)); Mat r = id(); r.m[5] = c; r.m[6] = -s; r.m[9] = s; r.m[10] = c; return r; }
    private static Mat ry(double d) { double c = Math.cos(Math.toRadians(d)), s = Math.sin(Math.toRadians(d)); Mat r = id(); r.m[0] = c; r.m[2] = s; r.m[8] = -s; r.m[10] = c; return r; }
    private static Mat rz(double d) { double c = Math.cos(Math.toRadians(d)), s = Math.sin(Math.toRadians(d)); Mat r = id(); r.m[0] = c; r.m[1] = -s; r.m[4] = s; r.m[5] = c; return r; }
    public Mat mul(Mat o) {
        Mat r = new Mat(); double[] a = m, b = o.m;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) r.m[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j];
            r.m[i * 4 + 3] = a[i * 4] * b[3] + a[i * 4 + 1] * b[7] + a[i * 4 + 2] * b[11] + a[i * 4 + 3];
        }
        return r;
    }
    public void point(double x, double y, double z, double[] o) {
        o[0] = m[0] * x + m[1] * y + m[2] * z + m[3]; o[1] = m[4] * x + m[5] * y + m[6] * z + m[7]; o[2] = m[8] * x + m[9] * y + m[10] * z + m[11];
    }
    public void vec(double x, double y, double z, double[] o) {
        o[0] = m[0] * x + m[1] * y + m[2] * z; o[1] = m[4] * x + m[5] * y + m[6] * z; o[2] = m[8] * x + m[9] * y + m[10] * z;
    }
}
