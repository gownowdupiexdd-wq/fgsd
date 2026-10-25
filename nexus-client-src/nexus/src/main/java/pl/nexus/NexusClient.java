package pl.nexus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.nexus.gui.ClickGuiScreen;
import pl.nexus.gui.Hud;
import pl.nexus.module.Module;
import pl.nexus.module.ModuleManager;

public class NexusClient implements ClientModInitializer {
    public static KeyBinding openKey;
    private static boolean loaded;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nexus.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nexus"));
        Commands.init();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!loaded) { loaded = true; ConfigStore.load(); }   // dopiero gdy gra jest gotowa
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
            }
            if (mc.currentScreen == null && mc.player != null) {
                long h = mc.getWindow().getHandle();
                for (Module m : ModuleManager.ALL) {
                    if (m.key <= 0) continue;
                    boolean down = InputUtil.isKeyPressed(h, m.key);
                    if (down && !m.wasDown) m.toggle();
                    m.wasDown = down;
                }
            }
            Hud.tick(mc);
            ModuleManager.tick();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigStore.save());
        HudRenderCallback.EVENT.register(Hud::render);
    }
}
