package pl.nexus.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pl.nexus.Config;
import pl.nexus.Config.Theme;
import pl.nexus.module.Category;
import pl.nexus.module.Module;
import pl.nexus.module.ModuleManager;

import java.util.List;
import java.util.Locale;

public class ClickGuiScreen extends Screen {
    private static final int W = 440, H = 270, SB = 96, RH = 26, RG = 4;
    private static final Category[] CATS = Category.values();
    private static final float[] SWATCHES = {0.00f, 0.07f, 0.14f, 0.33f, 0.48f, 0.58f, 0.75f, 0.90f};

    private static Category sel = Category.COMBAT;

    private float open, tiltY, tiltX, selAnim = sel.ordinal(), fade = 1f;
    private float lx, ly;
    private int ox, oy;
    private long last;
    private float dt, k;
    private int dragKind;           // 1 modul, 2 odcien, 3 przechyl, 4 rozmiar poswiaty, 5 przezroczystosc
    private Module dragMod, listening;
    private int dragX, dragW;
    private final float[] swAnim = new float[8];
    private static final int VIS = 7;
    private final int[] scroll = new int[8];

    private int off(List<Module> l) {
        int o = MathHelper.clamp(scroll[sel.ordinal()], 0, Math.max(0, l.size() - VIS));
        scroll[sel.ordinal()] = o;
        return o;
    }

    public ClickGuiScreen() {
        super(Text.literal("Nexus"));
        for (int i = 0; i < swAnim.length; i++) swAnim[i] = switchState(i) ? 1f : 0f;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) { }

    // ------------------------------------------------------------ helpers
    private int f(int col) {
        int a = (int) ((col >>> 24) * fade);
        return (a << 24) | (col & 0xFFFFFF);
    }
    private static int withAlpha(int col, int a) { return (MathHelper.clamp(a, 0, 255) << 24) | (col & 0xFFFFFF); }
    private static int op(int col) { return withAlpha(col, (int) ((col >>> 24) * Config.opacity)); }
    private static int lerpCol(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
    private void fill(DrawContext c, int x1, int y1, int x2, int y2, int col) { c.fill(x1, y1, x2, y2, f(col)); }
    private void rrect(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        int[] in = {3, 2, 1, 1};
        for (int i = 0; i < 4; i++) {
            fill(c, x1 + in[i], y1 + i, x2 - in[i], y1 + i + 1, col);
            fill(c, x1 + in[i], y2 - i - 1, x2 - in[i], y2 - i, col);
        }
        fill(c, x1, y1 + 4, x2, y2 - 4, col);
    }
    private void outline(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        fill(c, x1 + 2, y1, x2 - 2, y1 + 1, col); fill(c, x1 + 2, y2 - 1, x2 - 2, y2, col);
        fill(c, x1, y1 + 2, x1 + 1, y2 - 2, col); fill(c, x2 - 1, y1 + 2, x2, y2 - 2, col);
    }
    private void text(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, s, x, y, f(col), false); }
    private void bigText(DrawContext c, String s, int x, int y, float scale, int col) {
        MatrixStack m = c.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(scale, scale, 1f);
        text(c, s, 0, 0, col);
        m.pop();
    }
    private static boolean hit(float px, float py, int x, int y, int w, int h) { return px >= x && px < x + w && py >= y && py < y + h; }
    private void layer(DrawContext c) { c.getMatrices().translate(0, 0, 1); }
    private void click() {
        if (Config.sounds) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
    private static String keyName(int key) { return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT); }

    // ------------------------------------------------------------ render
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.nanoTime();
        dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1.0e9f);
        last = now;
        k = 1f - (float) Math.exp(-dt * 14f);
        float kSlow = 1f - (float) Math.exp(-dt * 9f);

        open = Math.min(1f, open + dt * 4f);
        float e = 1f - (float) Math.pow(1f - open, 3);
        fade = e;
        if (Config.dim) c.fill(0, 0, width, height, ((int) (0x66 * e)) << 24);

        ox = (width - W) / 2; oy = (height - H) / 2;
        float cx = width / 2f, cy = height / 2f;

        tiltY += (((mx - cx) / cx) * 7f * Config.tilt - tiltY) * kSlow;
        tiltX += (-((my - cy) / cy) * 5f * Config.tilt - tiltX) * kSlow;
        float sc = 0.9f + 0.1f * e;

        // plaska transformacja 2D (bez osi Z => zero z-fightingu), przechyl + lekkie scinanie
        float ry = (float) Math.toRadians(tiltY), rx = (float) Math.toRadians(tiltX);
        Matrix4f tilt = new Matrix4f();
        tilt.m00((float) Math.cos(ry));
        tilt.m11((float) Math.cos(rx));
        tilt.m01((float) Math.sin(ry) * 0.10f);
        tilt.m10((float) Math.sin(rx) * 0.10f);
        Matrix4f full = new Matrix4f().translate(cx, cy, 0).mul(tilt).scale(sc, sc, 1f).translate(-cx, -cy, 0);

        // dokladna odwrotnosc => myszka trafia dokladnie w to co widac
        Vector3f v = new Matrix4f(full).invert().transformPosition(mx, my, 0, new Vector3f());
        lx = v.x; ly = v.y;

        MatrixStack m = c.getMatrices();
        m.push();
        m.peek().getPositionMatrix().mul(full);

        Theme th = Config.theme;
        int acc = Config.accent();

        for (int i = 8; i >= 1; i--) rrect(c, ox - i, oy - i, ox + W + i, oy + H + i, 0x0C000000);
        rrect(c, ox, oy, ox + W, oy + H, op(th.bg));
        fill(c, ox + 6, oy, ox + W - 6, oy + 1, acc);

        rrect(c, ox, oy, ox + SB, oy + H, op(th.side));
        fill(c, ox + SB - 4, oy + 4, ox + SB, oy + H - 4, op(th.side));
        bigText(c, "NEXUS", ox + 12, oy + 12, 1.6f, acc);
        text(c, "CLIENT  v1.1", ox + 13, oy + 28, th.sub);

        selAnim += (sel.ordinal() - selAnim) * k;
        int iy = oy + 42 + Math.round(selAnim * 24);
        rrect(c, ox + 8, iy, ox + SB - 8, iy + 21, withAlpha(acc, 0x38));
        fill(c, ox + 8, iy + 4, ox + 10, iy + 17, acc);
        for (int i = 0; i < CATS.length; i++) {
            int y = oy + 42 + i * 24;
            boolean s = CATS[i] == sel, hov = hit(lx, ly, ox + 8, y, SB - 16, 21);
            text(c, CATS[i].label, ox + 18, y + 7, s ? acc : (hov ? th.text : th.sub));
        }
        text(c, "PPM na module = bind", ox + 8, oy + H - 24, th.sub);
        text(c, "RShift = zamknij", ox + 8, oy + H - 14, th.sub);

        layer(c);
        if (Config.glow && hit(lx, ly, ox, oy, W, H)) glow(c, lx, ly, acc);

        layer(c);
        bigText(c, sel.label, ox + SB + 12, oy + 12, 1.4f, acc);
        String sub = switch (sel) {
            case VISUAL -> "motyw i kolor";
            case SETTINGS -> "efekty i opcje";
            default -> ModuleManager.in(sel).size() + " modułów";
        };
        text(c, sub, ox + SB + 12, oy + 27, th.sub);

        layer(c);
        switch (sel) {
            case VISUAL -> renderVisual(c);
            case SETTINGS -> renderSettings(c);
            default -> renderModules(c);
        }
        m.pop();
    }

    private void glow(DrawContext c, float gx, float gy, int color) {
        final int N = 10;
        float R = 72f * Config.glowSize;
        for (int i = 0; i < N; i++) {
            float r = R * (1f - i / (float) N);
            int col = withAlpha(color, 9);
            for (int dy = -(int) r; dy <= r; dy += 2) {
                int y = (int) gy + dy;
                if (y < oy + 2 || y > oy + H - 4) continue;
                float hw = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
                int x1 = Math.max((int) (gx - hw), ox + 2), x2 = Math.min((int) (gx + hw), ox + W - 2);
                if (x2 > x1) fill(c, x1, y, x2, y + 2, col);
            }
        }
    }

    private void renderModules(DrawContext c) {
        List<Module> list = ModuleManager.in(sel);
        Theme th = Config.theme;
        int acc = Config.accent();
        int rx = ox + SB + 12, ry = oy + 40, rw = W - SB - 24;

        int off = off(list);
        if (list.size() > VIS) {
            int trackH = VIS * (RH + RG) - RG;
            int th2 = Math.max(16, trackH * VIS / list.size());
            int ty = ry + (trackH - th2) * off / Math.max(1, list.size() - VIS);
            fill(c, ox + W - 8, ry, ox + W - 5, ry + trackH, withAlpha(th.sub, 0x30));
            fill(c, ox + W - 8, ty, ox + W - 5, ty + th2, acc);
        }
        for (int i = off; i < Math.min(list.size(), off + VIS); i++) {
            Module mod = list.get(i);
            int y = ry + (i - off) * (RH + RG);
            boolean hov = hit(lx, ly, rx, y, rw, RH);
            mod.anim += ((mod.enabled ? 1f : 0f) - mod.anim) * k;

            rrect(c, rx, y, rx + rw, y + RH, th.row);
            if (mod.anim > 0.02f) {
                rrect(c, rx, y, rx + rw, y + RH, withAlpha(acc, (int) (0x1C * mod.anim)));
                fill(c, rx, y + 4, rx + 2, y + RH - 4, withAlpha(acc, (int) (255 * mod.anim)));
            }
            if (hov) {
                rrect(c, rx, y, rx + rw, y + RH, withAlpha(acc, 0x1A));
                outline(c, rx, y, rx + rw, y + RH, withAlpha(acc, 0xA0));
            }
            int nameCol = lerpCol(th.text, acc, mod.anim);
            text(c, mod.name, rx + 10, y + 5, nameCol);
            String bind = listening == mod ? "[...]" : (mod.key > 0 ? "[" + keyName(mod.key) + "]" : "");
            if (!bind.isEmpty()) text(c, bind, rx + 14 + textRenderer.getWidth(mod.name), y + 5, listening == mod ? acc : th.sub);
            text(c, textRenderer.trimToWidth(mod.desc, 158), rx + 10, y + 15, th.sub);

            if (mod.hasValue) {
                int sx = rx + rw - 150, sw = 90;
                text(c, String.format(Locale.ROOT, "%s %.1f", mod.valueName, mod.value), sx, y + 3, th.sub);
                float t = (float) ((mod.value - mod.min) / (mod.max - mod.min));
                fill(c, sx, y + 16, sx + sw, y + 19, withAlpha(th.sub, 0x55));
                fill(c, sx, y + 16, sx + (int) (sw * t), y + 19, acc);
                fill(c, sx + (int) (sw * t) - 2, y + 13, sx + (int) (sw * t) + 2, y + 22, th.text);
            }
            drawSwitch(c, rx + rw - 40, y + 7, mod.anim);
        }
    }

    private void drawSwitch(DrawContext c, int x, int y, float a) {
        Theme th = Config.theme;
        rrect(c, x, y, x + 28, y + 12, lerpCol(th.sub | 0xFF000000, Config.accent(), a));
        int kx = x + 2 + (int) (a * 16);
        rrect(c, kx, y + 2, kx + 10, y + 10, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------ strony ustawien
    private int sx() { return ox + SB + 12; }
    private int sy() { return oy + 40; }
    private int sw() { return W - SB - 24; }
    private int sliderX() { return sx() + sw() - 180; }

    private void renderVisual(DrawContext c) {
        Theme th = Config.theme; int acc = Config.accent();
        int x = sx(), y0 = sy(), w = sw();

        text(c, "Motyw", x, y0, th.sub);
        int bw = (w - 18) / 4;
        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++) {
            int bx = x + i * (bw + 6), by = y0 + 12;
            boolean s = themes[i] == Config.theme, hov = hit(lx, ly, bx, by, bw, 20);
            rrect(c, bx, by, bx + bw, by + 20, themes[i].bg | 0xFF000000);
            if (s) outline(c, bx, by, bx + bw, by + 20, acc);
            else if (hov) outline(c, bx, by, bx + bw, by + 20, withAlpha(acc, 0x90));
            String n = themes[i].label;
            text(c, n, bx + (bw - textRenderer.getWidth(n)) / 2, by + 6, themes[i].text);
        }

        text(c, "Kolor interfejsu", x, y0 + 46, th.sub);
        for (int i = 0; i < w; i += 2)
            fill(c, x + i, y0 + 58, x + i + 2, y0 + 68, 0xFF000000 | MathHelper.hsvToRgb(i / (float) w, 0.65f, 1f));
        int kx = x + (int) (Config.hue * (w - 2));
        fill(c, kx - 1, y0 + 55, kx + 3, y0 + 71, 0xFFFFFFFF);
        for (int i = 0; i < SWATCHES.length; i++) {
            int bx = x + i * 28, by = y0 + 76;
            rrect(c, bx, by, bx + 22, by + 12, 0xFF000000 | MathHelper.hsvToRgb(SWATCHES[i], 0.65f, 1f));
            if (hit(lx, ly, bx, by, 22, 12)) outline(c, bx - 1, by - 1, bx + 23, by + 13, th.text);
        }

        switchRow(c, x, y0 + 98, w, "Tęcza (animowany kolor)", 0);
        sliderRow(c, x, y0 + 126, w, "Przezroczystość okna", String.format(Locale.ROOT, "%.0f%%", Config.opacity * 100), (Config.opacity - 0.5f) / 0.5f);
    }

    private void renderSettings(DrawContext c) {
        int x = sx(), y0 = sy(), w = sw();
        switchRow(c, x, y0, w, "Poświata przy kursorze", 1);
        sliderRow(c, x, y0 + 28, w, "Rozmiar poświaty", String.format(Locale.ROOT, "%.1fx", Config.glowSize), (Config.glowSize - 0.5f) / 1.5f);
        sliderRow(c, x, y0 + 56, w, "Przechył 3D", String.format(Locale.ROOT, "%.1f", Config.tilt), Config.tilt / 2f);
        switchRow(c, x, y0 + 84, w, "Przyciemnienie tła", 2);
        switchRow(c, x, y0 + 112, w, "Watermark na ekranie", 3);
        switchRow(c, x, y0 + 140, w, "Dźwięki GUI", 4);
        buttonRow(c, x, y0 + 168, w, "Usuń wszystkie bindy");
        buttonRow(c, x, y0 + 196, w, "PANIC - wyłącz wszystkie moduły");
    }

    private boolean switchState(int i) {
        return switch (i) {
            case 0 -> Config.rainbow; case 1 -> Config.glow; case 2 -> Config.dim;
            case 3 -> Config.watermark; case 4 -> Config.sounds; default -> false;
        };
    }
    private void toggleSwitch(int i) {
        switch (i) {
            case 0 -> Config.rainbow = !Config.rainbow; case 1 -> Config.glow = !Config.glow;
            case 2 -> Config.dim = !Config.dim; case 3 -> Config.watermark = !Config.watermark;
            case 4 -> Config.sounds = !Config.sounds; default -> { }
        }
    }

    private void rowBg(DrawContext c, int x, int y, int w) {
        rrect(c, x, y, x + w, y + RH - 2, Config.theme.row);
        if (hit(lx, ly, x, y, w, RH - 2)) outline(c, x, y, x + w, y + RH - 2, withAlpha(Config.accent(), 0xA0));
    }
    private void switchRow(DrawContext c, int x, int y, int w, String label, int id) {
        swAnim[id] += ((switchState(id) ? 1f : 0f) - swAnim[id]) * k;
        rowBg(c, x, y, w);
        text(c, label, x + 10, y + 8, Config.theme.text);
        drawSwitch(c, x + w - 40, y + 6, swAnim[id]);
    }
    private void sliderRow(DrawContext c, int x, int y, int w, String label, String val, float t) {
        Theme th = Config.theme;
        rowBg(c, x, y, w);
        text(c, label, x + 10, y + 8, th.text);
        int sx = x + w - 180, sw = 170;
        text(c, val, sx - 4 - textRenderer.getWidth(val), y + 8, th.sub);
        sx += 0;
        // suwak ssx..sx+sw, wartosc obok etykiety zeby sie nie nakladala
        fill(c, sx, y + 11, sx + sw, y + 14, withAlpha(th.sub, 0x55));
        fill(c, sx, y + 11, sx + (int) (sw * t), y + 14, Config.accent());
        fill(c, sx + (int) (sw * t) - 2, y + 8, sx + (int) (sw * t) + 2, y + 17, th.text);
    }
    private void buttonRow(DrawContext c, int x, int y, int w, String label) {
        rowBg(c, x, y, w);
        text(c, label, x + 10, y + 8, Config.theme.text);
    }

    // ------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < CATS.length; i++) {
            if (hit(lx, ly, ox + 8, oy + 42 + i * 24, SB - 16, 21)) { sel = CATS[i]; listening = null; click(); return true; }
        }
        if (sel == Category.VISUAL) return button == 0 && visualClick();
        if (sel == Category.SETTINGS) return button == 0 && settingsClick();

        List<Module> list = ModuleManager.in(sel);
        int rx = ox + SB + 12, ry = oy + 40, rw = W - SB - 24;
        int off = off(list);
        for (int i = off; i < Math.min(list.size(), off + VIS); i++) {
            Module mod = list.get(i);
            int y = ry + (i - off) * (RH + RG);
            if (!hit(lx, ly, rx, y, rw, RH)) continue;
            if (button == 1) { listening = (listening == mod) ? null : mod; click(); return true; }
            if (button != 0) return true;
            int sx = rx + rw - 150, sw = 90;
            if (mod.hasValue && hit(lx, ly, sx - 3, y + 9, sw + 6, 16)) {
                dragKind = 1; dragMod = mod; dragX = sx; dragW = sw; applyDrag();
            } else { mod.toggle(); click(); }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean visualClick() {
        int x = sx(), y0 = sy(), w = sw();
        int bw = (w - 18) / 4;
        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++)
            if (hit(lx, ly, x + i * (bw + 6), y0 + 12, bw, 20)) { Config.theme = themes[i]; click(); return true; }
        if (hit(lx, ly, x, y0 + 55, w, 16)) { dragKind = 2; dragX = x; dragW = w - 2; applyDrag(); return true; }
        for (int i = 0; i < SWATCHES.length; i++)
            if (hit(lx, ly, x + i * 28, y0 + 76, 22, 12)) { Config.hue = SWATCHES[i]; Config.rainbow = false; click(); return true; }
        if (hit(lx, ly, x, y0 + 98, w, RH - 2)) { toggleSwitch(0); click(); return true; }
        if (hit(lx, ly, x, y0 + 126, w, RH - 2)) { dragKind = 5; dragX = sliderX(); dragW = 170; applyDrag(); return true; }
        return false;
    }

    private boolean settingsClick() {
        int x = sx(), y0 = sy(), w = sw();
        if (hit(lx, ly, x, y0, w, RH - 2)) { toggleSwitch(1); click(); return true; }
        if (hit(lx, ly, x, y0 + 28, w, RH - 2)) { dragKind = 4; dragX = sliderX(); dragW = 170; applyDrag(); return true; }
        if (hit(lx, ly, x, y0 + 56, w, RH - 2)) { dragKind = 3; dragX = sliderX(); dragW = 170; applyDrag(); return true; }
        if (hit(lx, ly, x, y0 + 84, w, RH - 2)) { toggleSwitch(2); click(); return true; }
        if (hit(lx, ly, x, y0 + 112, w, RH - 2)) { toggleSwitch(3); click(); return true; }
        if (hit(lx, ly, x, y0 + 140, w, RH - 2)) { toggleSwitch(4); click(); return true; }
        if (hit(lx, ly, x, y0 + 168, w, RH - 2)) { for (Module m : ModuleManager.ALL) m.key = -1; click(); return true; }
        if (hit(lx, ly, x, y0 + 196, w, RH - 2)) {
            for (Module m : ModuleManager.ALL)
                if (m.enabled && !m.name.equals("HUD") && !m.name.equals("ArrayList")) m.setEnabled(false);
            click(); return true;
        }
        return false;
    }

    private void applyDrag() {
        float t = MathHelper.clamp((lx - dragX) / dragW, 0f, 1f);
        switch (dragKind) {
            case 1 -> dragMod.value = Math.round((dragMod.min + t * (dragMod.max - dragMod.min)) * 10.0) / 10.0;
            case 2 -> { Config.hue = t; Config.rainbow = false; }
            case 3 -> Config.tilt = Math.round(t * 20f) / 10f;
            case 4 -> Config.glowSize = Math.round((0.5f + t * 1.5f) * 10f) / 10f;
            case 5 -> Config.opacity = Math.round((0.5f + t * 0.5f) * 20f) / 20f;
            default -> { }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (sel.ordinal() < Category.VISUAL.ordinal()) {
            scroll[sel.ordinal()] -= (int) Math.signum(vAmount);
            off(ModuleManager.in(sel));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public void removed() {
        pl.nexus.ConfigStore.save();
        super.removed();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragKind != 0) { applyDrag(); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragKind = 0; dragMod = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { /* anuluj */ }
            else if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) listening.key = -1;
            else listening.key = key;
            listening = null;
            click();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
