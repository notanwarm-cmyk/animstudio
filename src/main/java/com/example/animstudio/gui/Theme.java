package com.example.animstudio.gui;
public final class Theme {
    public final int bg, panel, panelAlt, field, text, dim, accent, line;
    private Theme(int bg, int panel, int panelAlt, int field, int text, int dim, int accent, int line) {
        this.bg = bg; this.panel = panel; this.panelAlt = panelAlt; this.field = field;
        this.text = text; this.dim = dim; this.accent = accent; this.line = line;
    }
    public static final Theme LIGHT = new Theme(0xFFE9EAEE, 0xFFF6F6F8, 0xFFECECF0, 0xFFFFFFFF, 0xFF1E1F24, 0xFF6B6D78, 0xFF2F7DF6, 0xFFCFD0D8);
    public static final Theme DARK  = new Theme(0xFF16171B, 0xFF1F2025, 0xFF26272D, 0xFF2E3037, 0xFFE6E7EB, 0xFF8D909C, 0xFF4C9AFF, 0xFF3A3C45);
}
