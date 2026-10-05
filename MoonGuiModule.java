package com.moonclient.modules;

import com.moonclient.MoonClient;
import com.moonclient.gui.MoonScreen;
import meteordevelopment.meteorclient.systems.modules.Module;

/** Bind this module to a key (Meteor GUI, or the .bind command) to open the Moon menu. */
public class MoonGuiModule extends Module {
    public MoonGuiModule() {
        super(MoonClient.CATEGORY, "moon-gui", "Opens the Moon Client menu.");
    }

    @Override
    public void onActivate() {
        MoonClient.openScreen(new MoonScreen());
        toggle();
    }
}
