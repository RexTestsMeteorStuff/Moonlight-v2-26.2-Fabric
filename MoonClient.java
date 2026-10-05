package com.moonclient;

import com.moonclient.gui.MoonScreen;
import com.moonclient.modules.MoonGuiModule;
import com.moonclient.modules.MoonPresenceModule;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screens.Screen;

import java.lang.invoke.MethodHandles;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MoonClient extends MeteorAddon {
    public static final Category CATEGORY = new Category("Moon");

    @Override
    public void onInitialize() {
        MeteorClient.EVENT_BUS.registerLambdaFactory("com.moonclient",
            (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        Modules.get().add(new MoonGuiModule());
        Modules.get().add(new MoonPresenceModule());
        MeteorClient.EVENT_BUS.subscribe(new GuiRedirect());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.moonclient";
    }

    public static void openScreen(Screen screen) {
        mc.gui.setScreen(screen);
    }

    /** Replaces Meteor's hotkey GUI with the Moon menu. */
    public static class GuiRedirect {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onOpenScreen(OpenScreenEvent event) {
            if (event.screen instanceof ModulesScreen) {
                event.cancel();
                mc.execute(() -> openScreen(new MoonScreen()));
            }
        }
    }
}
