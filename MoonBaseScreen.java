package com.moonclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * Shared look for every Moon Client screen: night sky, stars, moon, glass panel, helpers.
 * All 26.2-specific drawing calls (fill / text / textWidth) live at the bottom of this class.
 *
 * Do NOT call super.extractRenderState(...) in subclasses: vanilla's menu blur is applied before
 * the screen draws, and asking for it twice in one frame crashes the game.
 */
public abstract class MoonBaseScreen extends Screen {
    protected static final int MAX_W = 520, MAX_H = 330;
    protected static final int TITLE_H = 26, FOOTER_H = 18, ROW_H = 24, PAD = 8, GAP = 4;

    // ARGB palette
    protected static final int PANEL     = 0xC00E1428;
    protected static final int TITLE_BAR = 0xD0182244;
    protected static final int BORDER    = 0x55AFC4FF;
    protected static final int ACCENT    = 0xFF9DB4FF;
    protected static final int TEXT      = 0xFFE8EEFF;
    protected static final int TEXT_DIM  = 0xFF8C9AC0;
    protected static final int TEXT_FAINT = 0xFF59658A;
    protected static final int KNOB      = 0xFFFFFFFF;
    protected static final int TRACK_OFF = 0xFF38425E;

    private static final int STAR_COUNT = 150;
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starPhase = new float[STAR_COUNT];
    private final int[] starSize = new int[STAR_COUNT];
    private final long openedAt = System.currentTimeMillis();

    protected int px, py, pw, ph;

    protected MoonBaseScreen() {
        super(Component.literal("Moon Client"));
        Random rng = new Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rng.nextFloat();
            starY[i] = rng.nextFloat();
            starPhase[i] = rng.nextFloat() * 6.2831f;
            starSize[i] = rng.nextInt(10) == 0 ? 2 : 1;
        }
    }

    protected void layout() {
        pw = Math.min(MAX_W, width - 24);
        ph = Math.min(MAX_H, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------------------------------------------------------- shared drawing

    protected void drawSky(GuiGraphicsExtractor g) {
        // Night tint over the blurred world: deep navy at the top, indigo near the horizon
        int steps = 24;
        int h = (height + steps - 1) / steps;
        for (int i = 0; i < steps; i++) {
            int c = lerp(0xB0070B1E, 0xA01D2B55, i / (float) (steps - 1));
            fill(g, 0, i * h, width, Math.min(height, (i + 1) * h), c);
        }

        // Twinkling stars
        double t = (System.currentTimeMillis() - openedAt) / 1000.0;
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = (int) (starX[i] * width), sy = (int) (starY[i] * height);
            float tw = 0.55f + 0.45f * (float) Math.sin(t * 1.6 + starPhase[i]);
            int color = ((int) (255 * tw) << 24) | 0xFFFFFF;
            int s = starSize[i];
            fill(g, sx, sy, sx + s, sy + s, color);
            if (s == 2) {
                int soft = ((int) (110 * tw) << 24) | 0xFFFFFF;
                fill(g, sx - 1, sy, sx, sy + s, soft);
                fill(g, sx + s, sy, sx + s + 1, sy + s, soft);
                fill(g, sx, sy - 1, sx + s, sy, soft);
                fill(g, sx, sy + s, sx + s, sy + s + 1, soft);
            }
        }

        drawMoon(g, (int) (width * 0.80), (int) (height * 0.22), Math.max(18, Math.min(width, height) / 9));
    }

    protected void drawPanel(GuiGraphicsExtractor g, String title) {
        fill(g, px, py, px + pw, py + ph, PANEL);
        outline(g, px, py, pw, ph, BORDER);
        fill(g, px + 1, py + 1, px + pw - 1, py + TITLE_H, TITLE_BAR);
        text(g, title, px + (pw - textWidth(title)) / 2, py + (TITLE_H - 8) / 2 + 1, ACCENT);
    }

    protected void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean on) {
        fill(g, x, y, x + 24, y + 12, on ? ACCENT : TRACK_OFF);
        int kx = on ? x + 14 : x + 2;
        fill(g, kx, y + 2, kx + 8, y + 10, KNOB);
    }

    private void drawMoon(GuiGraphicsExtractor g, int cx, int cy, int r) {
        for (int i = 6; i >= 1; i--) disc(g, cx, cy, r + i * 5, 0x10BFD0FF); // soft glow
        disc(g, cx, cy, r, 0xFFEFF2FF);                                       // body
        disc(g, cx - r / 3, cy - r / 4, Math.max(2, r / 5), 0x40A9B4D8);     // craters
        disc(g, cx + r / 4, cy + r / 5, Math.max(2, r / 4), 0x40A9B4D8);
        disc(g, cx - r / 6, cy + r / 2, Math.max(2, r / 7), 0x40A9B4D8);
        disc(g, cx + r / 2, cy - r / 3, Math.max(1, r / 8), 0x40A9B4D8);
    }

    private void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            fill(g, cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    protected void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        fill(g, x, y, x + w, y + 1, color);
        fill(g, x, y + h - 1, x + w, y + h, color);
        fill(g, x, y, x + 1, y + h, color);
        fill(g, x + w - 1, y, x + w, y + h, color);
    }

    protected String trim(String s, int maxWidth) {
        if (s == null) return "";
        if (textWidth(s) <= maxWidth) return s;
        while (s.length() > 1 && textWidth(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    protected static int lerp(int a, int b, float t) {
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 255, cb = (b >> shift) & 255;
            out |= (((int) (ca + (cb - ca) * t)) & 255) << shift;
        }
        return out;
    }

    // ---------------------------------------------------------------- 26.2 drawing helpers

    protected void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    protected void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(this.font, Component.literal(s), x, y, color);
    }

    protected int textWidth(String s) {
        return this.font.width(s);
    }
}
