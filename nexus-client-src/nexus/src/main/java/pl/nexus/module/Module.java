package pl.nexus.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name, desc;
    public final Category category;
    public boolean enabled;
    public int key = -1;      // bind (GLFW), -1 = brak
    public boolean wasDown;
    public float anim; // animacja przelacznika w GUI

    public boolean hasValue;
    public String valueName = "";
    public double value, min, max;

    protected Module(String name, String desc, Category category) {
        this.name = name; this.desc = desc; this.category = category;
    }

    public Module withValue(String label, double def, double min, double max) {
        this.hasValue = true; this.valueName = label; this.value = def; this.min = min; this.max = max;
        return this;
    }

    /** wlaczony od startu (bez wywolywania onEnable) */
    public Module on() { this.enabled = true; this.anim = 1f; return this; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (pl.nexus.Config.notifications && mc.world != null && !name.equals("HUD") && !name.equals("ArrayList"))
            pl.nexus.gui.Notifications.push(name + (e ? " ON" : " OFF"), e ? 0xFF55FF55 : 0xFFFF5555);
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    protected void face(Entity target) {
        Vec3d t = target.getBoundingBox().getCenter();
        Vec3d f = mc.player.getEyePos();
        double dx = t.x - f.x, dy = t.y - f.y, dz = t.z - f.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        mc.player.setPitch((float) -Math.toDegrees(Math.atan2(dy, h)));
    }
}
