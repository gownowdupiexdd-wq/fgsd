package pl.nexus.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import pl.nexus.Config;

import java.util.ArrayList;
import java.util.List;

public final class Notifications {
    private record N(String text, int color, long time) {}
    private static final List<N> LIST = new ArrayList<>();

    public static void push(String text, int color) {
        LIST.add(0, new N(text, color, System.currentTimeMillis()));
        while (LIST.size() > 5) LIST.remove(LIST.size() - 1);
    }

    public static void render(DrawContext c, TextRenderer tr) {
        long now = System.currentTimeMillis();
        LIST.removeIf(n -> now - n.time > 2600);
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        for (int i = 0; i < LIST.size(); i++) {
            N n = LIST.get(i);
            long age = now - n.time;
            int w = tr.getWidth(n.text) + 14;
            float in = Math.min(1f, age / 200f), out = age > 2200 ? (age - 2200) / 400f : 0f;
            int slide = (int) ((1f - in + out) * (w + 10));
            int x = sw - w - 6 + slide, y = sh - 52 - i * 18;
            c.fill(x, y, x + w, y + 15, 0xB0101216);
            c.fill(x, y, x + 2, y + 15, Config.accent());
            c.drawTextWithShadow(tr, n.text, x + 8, y + 4, n.color);
        }
    }

    private Notifications() {}
}
