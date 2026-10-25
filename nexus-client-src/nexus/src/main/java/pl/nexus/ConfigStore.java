package pl.nexus;

import net.fabricmc.loader.api.FabricLoader;
import pl.nexus.module.Module;
import pl.nexus.module.ModuleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigStore {
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("nexus.properties"); }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("theme", Config.theme.name());
        p.setProperty("hue", String.valueOf(Config.hue));
        p.setProperty("rainbow", String.valueOf(Config.rainbow));
        p.setProperty("glow", String.valueOf(Config.glow));
        p.setProperty("tilt", String.valueOf(Config.tilt));
        p.setProperty("glowSize", String.valueOf(Config.glowSize));
        p.setProperty("opacity", String.valueOf(Config.opacity));
        p.setProperty("dim", String.valueOf(Config.dim));
        p.setProperty("watermark", String.valueOf(Config.watermark));
        p.setProperty("sounds", String.valueOf(Config.sounds));
        p.setProperty("notifications", String.valueOf(Config.notifications));
        p.setProperty("friends", String.join(",", Config.friends));
        for (Module m : ModuleManager.ALL) {
            String k = "mod." + m.name + ".";
            p.setProperty(k + "enabled", String.valueOf(m.enabled));
            p.setProperty(k + "key", String.valueOf(m.key));
            if (m.hasValue) p.setProperty(k + "value", String.valueOf(m.value));
        }
        try (var w = Files.newBufferedWriter(file())) { p.store(w, "Nexus Client"); } catch (IOException ignored) { }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (var r = Files.newBufferedReader(f)) { p.load(r); } catch (IOException e) { return; }
        try {
            if (p.getProperty("theme") != null) Config.theme = Config.Theme.valueOf(p.getProperty("theme"));
            Config.hue = Float.parseFloat(p.getProperty("hue", String.valueOf(Config.hue)));
            Config.rainbow = Boolean.parseBoolean(p.getProperty("rainbow", "false"));
            Config.glow = Boolean.parseBoolean(p.getProperty("glow", "true"));
            Config.tilt = Float.parseFloat(p.getProperty("tilt", "1"));
            Config.glowSize = Float.parseFloat(p.getProperty("glowSize", "1"));
            Config.opacity = Float.parseFloat(p.getProperty("opacity", "1"));
            Config.dim = Boolean.parseBoolean(p.getProperty("dim", "true"));
            Config.watermark = Boolean.parseBoolean(p.getProperty("watermark", "true"));
            Config.sounds = Boolean.parseBoolean(p.getProperty("sounds", "true"));
            Config.notifications = Boolean.parseBoolean(p.getProperty("notifications", "true"));
            Config.friends.clear();
            for (String s : p.getProperty("friends", "").split(",")) if (!s.isBlank()) Config.friends.add(s.trim().toLowerCase());
            for (Module m : ModuleManager.ALL) {
                String k = "mod." + m.name + ".";
                if (p.getProperty(k + "enabled") != null) { m.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled")); m.anim = m.enabled ? 1f : 0f; }
                if (p.getProperty(k + "key") != null) m.key = Integer.parseInt(p.getProperty(k + "key"));
                if (m.hasValue && p.getProperty(k + "value") != null) m.value = Double.parseDouble(p.getProperty(k + "value"));
            }
        } catch (RuntimeException ignored) { /* uszkodzony plik - zostaja domyslne */ }
    }

    private ConfigStore() {}
}
