package com.moonclient.gui;

import com.moonclient.MoonClient;
import com.moonclient.modules.MoonGuiModule;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/** Moon Client main menu: category tabs + module list. Module settings open in MoonSettingsScreen. */
public class MoonScreen extends MoonBaseScreen {
    private static final int TAB_H = 24;

    private final List<Category> categories = new ArrayList<>();
    private int tab = 0;
    private int scrollRows = 0;

    public MoonScreen() {
        super();

        for (Category c : Modules.loopCategories()) categories.add(c);
        // Start on the first non-Moon category so you see real modules right away
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i) != MoonClient.CATEGORY) { tab = i; break; }
        }
    }

    // ---------------------------------------------------------------- layout

    private int listTop()     { return py + TITLE_H + TAB_H + GAP * 2; }
    private int listBottom()  { return py + ph - FOOTER_H; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_H); }
    private int rowX()        { return px + PAD; }
    private int rowW()        { return pw - PAD * 2 - 6; }
    private int toggleX()     { return rowX() + rowW() - 34; }
    private int gearX()       { return toggleX() - 20; }
    private int tabY()        { return py + TITLE_H + GAP; }

    private int tabW() {
        int n = Math.max(1, categories.size());
        return (pw - PAD * 2 - (n - 1) * GAP) / n;
    }

    private int tabX(int i) { return px + PAD + i * (tabW() + GAP); }

    private List<Module> modulesInTab() {
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.get().getGroup(categories.get(tab))) {
            if (m instanceof MoonGuiModule) continue;
            if (m.name.equals("discord-presence")) continue; // Meteor's own presence; Moon has moon-presence
            out.add(m);
        }
        return out;
    }

    private void clampScroll(int total) {
        int max = Math.max(0, total - visibleRows());
        scrollRows = Math.max(0, Math.min(max, scrollRows));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout();
        drawSky(g);
        drawPanel(g, "\u263E  MOON CLIENT");

        // category tabs
        for (int i = 0; i < categories.size(); i++) {
            int tx = tabX(i), ty = tabY(), tw = tabW();
            boolean sel = i == tab;
            boolean hov = inside(mouseX, mouseY, tx, ty, tw, TAB_H);
            fill(g, tx, ty, tx + tw, ty + TAB_H, sel ? 0xFFEAF0FF : hov ? 0x40FFFFFF : 0x22FFFFFF);
            String name = categories.get(i).name;
            text(g, name, tx + (tw - textWidth(name)) / 2, ty + (TAB_H - 8) / 2, sel ? 0xFF0E1428 : TEXT_DIM);
        }

        // module rows
        List<Module> mods = modulesInTab();
        clampScroll(mods.size());
        int rows = visibleRows();
        for (int i = 0; i < rows && scrollRows + i < mods.size(); i++) {
            Module m = mods.get(scrollRows + i);
            int rx = rowX(), ry = listTop() + i * ROW_H, rw = rowW(), rh = ROW_H - 2;
            boolean hov = inside(mouseX, mouseY, rx, ry, rw, rh);
            boolean on = m.isActive();

            fill(g, rx, ry, rx + rw, ry + rh, hov ? 0x40FFFFFF : 0x22FFFFFF);
            if (on) fill(g, rx, ry, rx + 2, ry + rh, ACCENT);

            text(g, m.title, rx + 8, ry + (rh - 8) / 2, on ? TEXT : TEXT_DIM);

            int descX = rx + 8 + textWidth(m.title) + 10;
            int avail = gearX() - 8 - descX;
            if (avail > 40) text(g, trim(m.description, avail), descX, ry + (rh - 8) / 2, TEXT_FAINT);

            text(g, "\u2699", gearX(), ry + (rh - 8) / 2, hov ? TEXT : TEXT_DIM);
            drawToggle(g, toggleX(), ry + (rh - 12) / 2, on);
        }

        // scrollbar
        int total = mods.size();
        if (total > rows) {
            int trackH = rows * ROW_H;
            int barH = Math.max(12, trackH * rows / total);
            int barY = listTop() + (trackH - barH) * scrollRows / (total - rows);
            fill(g, px + pw - PAD + 1, barY, px + pw - PAD + 4, barY + barH, 0xAAFFFFFF);
        }

        text(g, total + " modules  |  click to toggle  |  right-click or \u2699 for settings",
            px + PAD, py + ph - FOOTER_H + 5, TEXT_FAINT);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        double mx = event.x(), my = event.y();
        int button = event.button();

        for (int i = 0; i < categories.size(); i++) {
            if (inside(mx, my, tabX(i), tabY(), tabW(), TAB_H)) {
                tab = i;
                scrollRows = 0;
                return true;
            }
        }

        List<Module> mods = modulesInTab();
        if (my >= listTop() && my < listTop() + visibleRows() * ROW_H) {
            int idx = scrollRows + (int) ((my - listTop()) / ROW_H);
            if (idx < mods.size() && inside(mx, my, rowX(), listTop() + (idx - scrollRows) * ROW_H, rowW(), ROW_H - 2)) {
                Module m = mods.get(idx);
                boolean onGear = mx >= gearX() - 2 && mx < gearX() + 14;
                if (button == 1 || (button == 0 && onGear)) {
                    MoonClient.openScreen(new MoonSettingsScreen(this, m)); // Moon-styled settings
                } else if (button == 0) {
                    m.toggle();
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollRows -= (int) Math.signum(scrollY);
        clampScroll(modulesInTab().size());
        return true;
    }
}
