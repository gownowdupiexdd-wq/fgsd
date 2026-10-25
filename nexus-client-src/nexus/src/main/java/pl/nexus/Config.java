package pl.nexus;

import net.minecraft.util.math.MathHelper;

public final class Config {
    public static Theme theme = Theme.DARK;
    public static float hue = 0.58f;
    public static boolean rainbow = false;
    public static boolean glow = true;
    public static float tilt = 1f;        // 0 = wylaczone, 2 = maksimum
    public static float glowSize = 1f;    // 0.5 - 2.0
    public static float opacity = 1f;     // 0.5 - 1.0
    public static boolean dim = true;     // przyciemnienie tla za GUI
    public static boolean watermark = true;
    public static boolean sounds = true;
    public static boolean notifications = true;
    public static final java.util.Set<String> friends = new java.util.LinkedHashSet<>();

    public static int accent() { return accentAt(0f); }

    public static int accentAt(float offset) {
        float h = hue;
        if (rainbow) h = ((System.currentTimeMillis() % 6000L) / 6000f + offset) % 1f;
        return 0xFF000000 | MathHelper.hsvToRgb(h, 0.65f, 1f);
    }

    public enum Theme {
        //            nazwa      tlo         sidebar     wiersz      tekst       podtekst
        DARK    ("Czarny",   0xF2141519, 0xF20E0F12, 0x18FFFFFF, 0xFFE8EAF0, 0xFF8A8F9C),
        LIGHT   ("Biały",    0xF2F3F4F8, 0xF2E3E5EC, 0x14000000, 0xFF1A1C23, 0xFF6A6F7E),
        MIDNIGHT("Midnight", 0xF20B1022, 0xF2070B18, 0x1CAACCFF, 0xFFE4EBFF, 0xFF7C89B0),
        MOCHA   ("Mocha",    0xF22A211C, 0xF2201915, 0x1CFFE0C0, 0xFFF2E6DA, 0xFFA8978A);

        public final String label;
        public final int bg, side, row, text, sub;

        Theme(String label, int bg, int side, int row, int text, int sub) {
            this.label = label; this.bg = bg; this.side = side;
            this.row = row; this.text = text; this.sub = sub;
        }
    }
}
