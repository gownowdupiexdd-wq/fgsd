package pl.nexus.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import pl.nexus.Config;
import pl.nexus.module.Module;
import pl.nexus.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Hud {
    private static double lastX, lastZ, bps;
    private static boolean hasLast;

    /** wolane co tick */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) { hasLast = false; return; }
        if (hasLast) {
            double d = Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ) * 20.0;
            bps += (d - bps) * 0.3;
        }
        lastX = mc.player.getX(); lastZ = mc.player.getZ(); hasLast = true;
    }

    public static void render(DrawContext c, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null || mc.world == null) return;
        TextRenderer tr = mc.textRenderer;

        if (Config.watermark) c.drawTextWithShadow(tr, "Nexus", 5, 5, Config.accent());

        if (ModuleManager.isOn("HUD")) {
            String xyz = String.format(Locale.ROOT, "XYZ %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            c.drawTextWithShadow(tr, xyz, 5, 17, 0xFFDDDDDD);
            var entry = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            int ping = entry == null ? 0 : entry.getLatency();
            String info = String.format(Locale.ROOT, "FPS %d  |  %.1f b/s  |  %d ms  |  %s", mc.getCurrentFps(), bps, ping,
                    mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT));
            c.drawTextWithShadow(tr, info, 5, 28, 0xFFDDDDDD);
        }

        if (ModuleManager.isOn("ArrayList")) {
            List<String> names = new ArrayList<>();
            for (Module m : ModuleManager.ALL)
                if (m.enabled && !m.name.equals("HUD") && !m.name.equals("ArrayList")) names.add(m.name);
            names.sort((a, b) -> tr.getWidth(b) - tr.getWidth(a));
            int sw = c.getScaledWindowWidth();
            int y = 4;
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i);
                int x = sw - tr.getWidth(n) - 7;
                int col = Config.accentAt(i * 0.05f);
                c.fill(x - 3, y - 1, sw, y + 10, 0x90000000);
                c.fill(sw - 2, y - 1, sw, y + 10, col);
                c.drawTextWithShadow(tr, n, x, y + 1, col);
                y += 11;
            }
        }
        Notifications.render(c, tr);
    }

    private Hud() {}
}
