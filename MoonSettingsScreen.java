package com.moonclient.gui;

import com.moonclient.MoonClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Moon-styled settings page for one module.
 *
 * Handles: on/off switch, true/false, whole numbers and decimals (click the bar to set the value),
 * and option lists (click to cycle, right-click to go back). Any other kind of setting (block lists,
 * colors, text...) shows its value, and clicking it opens Meteor's own editor for that module.
 */
public class MoonSettingsScreen extends MoonBaseScreen {
    private static final int CONTROL_W = 130;

    private record Row(String label, Setting<?> setting, boolean header, boolean moduleToggle) {}

    private final Screen parent;
    private final Module module;
    private final List<Row> rows = new ArrayList<>();
    private int scrollRows = 0;

    public MoonSettingsScreen(Screen parent, Module module) {
        super();
        this.parent = parent;
        this.module = module;

        rows.add(new Row("Enabled", null, false, true));
        for (SettingGroup group : module.settings) {
            rows.add(new Row(group.name, null, true, false));
            for (Setting<?> s : group) rows.add(new Row(pretty(s.name), s, false, false));
        }
    }

    private static String pretty(String name) {
        String s = name.replace('-', ' ').replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------------------------------------------------------------- layout

    private int listTop()     { return py + TITLE_H + GAP; }
    private int listBottom()  { return py + ph - FOOTER_H; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_H); }
    private int rowX()        { return px + PAD; }
    private int rowW()        { return pw - PAD * 2 - 6; }
    private int controlX()    { return rowX() + rowW() - 10 - CONTROL_W; }

    private void clampScroll() {
        int max = Math.max(0, rows.size() - visibleRows());
        scrollRows = Math.max(0, Math.min(max, scrollRows));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout();
        clampScroll();
        drawSky(g);
        drawPanel(g, "\u263E  " + module.title.toUpperCase());

        boolean backHover = inside(mouseX, mouseY, px + PAD, py, 50, TITLE_H);
        text(g, "< Back", px + PAD, py + (TITLE_H - 8) / 2 + 1, backHover ? TEXT : TEXT_DIM);

        int count = visibleRows();
        for (int i = 0; i < count && scrollRows + i < rows.size(); i++) {
            Row row = rows.get(scrollRows + i);
            int rx = rowX(), ry = listTop() + i * ROW_H, rw = rowW(), rh = ROW_H - 2;
            int ty = ry + (rh - 8) / 2;

            if (row.header()) {
                text(g, row.label().toUpperCase(), rx + 4, ty, ACCENT);
                fill(g, rx, ry + rh - 1, rx + rw, ry + rh, 0x44AFC4FF);
                continue;
            }

            boolean hov = inside(mouseX, mouseY, rx, ry, rw, rh);
            fill(g, rx, ry, rx + rw, ry + rh, hov ? 0x40FFFFFF : 0x22FFFFFF);
            text(g, row.label(), rx + 8, ty, hov ? TEXT : 0xFFC4CEEA);
            drawControl(g, row, ry, rh);
        }

        int total = rows.size();
        if (total > count) {
            int trackH = count * ROW_H;
            int barH = Math.max(12, trackH * count / total);
            int barY = listTop() + (trackH - barH) * scrollRows / (total - count);
            fill(g, px + pw - PAD + 1, barY, px + pw - PAD + 4, barY + barH, 0xAAFFFFFF);
        }

        text(g, trim(module.description, pw - PAD * 2), px + PAD, py + ph - FOOTER_H + 5, TEXT_FAINT);
    }

    private void drawControl(GuiGraphicsExtractor g, Row row, int ry, int rh) {
        int x0 = controlX();
        int cy = ry + (rh - 12) / 2;

        if (row.moduleToggle()) {
            drawToggle(g, x0 + CONTROL_W - 24, cy, module.isActive());
            return;
        }

        Setting<?> s = row.setting();
        if (s instanceof BoolSetting b) {
            drawToggle(g, x0 + CONTROL_W - 24, cy, b.get());
        } else if (s instanceof IntSetting i) {
            drawSlider(g, x0, cy, i.get(), i.sliderMin, i.sliderMax, String.valueOf(i.get()));
        } else if (s instanceof DoubleSetting d) {
            drawSlider(g, x0, cy, d.get(), d.sliderMin, d.sliderMax, String.format("%." + d.decimalPlaces + "f", d.get()));
        } else if (s instanceof EnumSetting<?> e) {
            drawPill(g, x0, cy, String.valueOf(e.get()), false);
        } else {
            drawPill(g, x0, cy, String.valueOf(s.get()), true);
        }
    }

    private void drawSlider(GuiGraphicsExtractor g, int x, int y, double value, double min, double max, String label) {
        double ratio = max > min ? Math.max(0, Math.min(1, (value - min) / (max - min))) : 0;
        fill(g, x, y, x + CONTROL_W, y + 12, TRACK_OFF);
        fill(g, x, y, x + (int) (CONTROL_W * ratio), y + 12, 0xFF5E78C9);
        text(g, label, x + (CONTROL_W - textWidth(label)) / 2, y + 2, TEXT);
    }

    private void drawPill(GuiGraphicsExtractor g, int x, int y, String label, boolean external) {
        fill(g, x, y, x + CONTROL_W, y + 12, TRACK_OFF);
        String shown = trim(label, CONTROL_W - (external ? 22 : 8));
        text(g, shown, x + 4, y + 2, TEXT);
        if (external) text(g, "\u2197", x + CONTROL_W - 12, y + 2, TEXT_DIM);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        double mx = event.x(), my = event.y();
        int button = event.button();

        if (inside(mx, my, px + PAD, py, 50, TITLE_H)) {
            onClose();
            return true;
        }

        if (my >= listTop() && my < listTop() + visibleRows() * ROW_H) {
            int idx = scrollRows + (int) ((my - listTop()) / ROW_H);
            if (idx < rows.size() && inside(mx, my, rowX(), listTop() + (idx - scrollRows) * ROW_H, rowW(), ROW_H - 2)) {
                Row row = rows.get(idx);
                if (row.header()) return true;

                if (row.moduleToggle()) {
                    module.toggle();
                    return true;
                }

                Setting<?> s = row.setting();
                if (s instanceof BoolSetting b) {
                    b.set(!b.get());
                } else if (s instanceof IntSetting || s instanceof DoubleSetting) {
                    if (mx >= controlX() - 4 && mx <= controlX() + CONTROL_W + 4) setFromSlider(s, mx);
                } else if (s instanceof EnumSetting<?>) {
                    cycleEnum(s, button != 1);
                } else {
                    MoonClient.openScreen(GuiThemes.get().moduleScreen(module)); // Meteor editor for odd types
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void setFromSlider(Setting<?> s, double mx) {
        double ratio = Math.max(0, Math.min(1, (mx - controlX()) / CONTROL_W));
        if (s instanceof IntSetting i) {
            int v = (int) Math.round(i.sliderMin + ratio * (i.sliderMax - i.sliderMin));
            i.set(Math.max(i.min, Math.min(i.max, v)));
        } else if (s instanceof DoubleSetting d) {
            double p = Math.pow(10, d.decimalPlaces);
            double v = Math.round((d.sliderMin + ratio * (d.sliderMax - d.sliderMin)) * p) / p;
            d.set(Math.max(d.min, Math.min(d.max, v)));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void cycleEnum(Setting setting, boolean forward) {
        Object v = setting.get();
        if (v instanceof Enum<?> e) {
            Object[] all = e.getDeclaringClass().getEnumConstants();
            int n = all.length;
            int i = (e.ordinal() + (forward ? 1 : n - 1)) % n;
            setting.set(all[i]);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollRows -= (int) Math.signum(scrollY);
        clampScroll();
        return true;
    }

    /** Esc and the Back button return to the module list. */
    @Override
    public void onClose() {
        MoonClient.openScreen(parent);
    }
}
