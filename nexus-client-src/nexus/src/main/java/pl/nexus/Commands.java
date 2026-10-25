package pl.nexus;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import pl.nexus.module.Module;
import pl.nexus.module.ModuleManager;

import java.util.Locale;

/** Komendy w czacie zaczynajace sie od kropki - nie sa wysylane na serwer. */
public final class Commands {
    public static void init() { ClientSendMessageEvents.ALLOW_CHAT.register(Commands::handle); }

    private static void say(String s) {
        var p = MinecraftClient.getInstance().player;
        if (p != null) p.sendMessage(Text.literal("§b[Nexus] §f" + s), false);
    }

    private static Module find(String n) {
        for (Module m : ModuleManager.ALL) if (m.name.equalsIgnoreCase(n)) return m;
        return null;
    }

    private static boolean handle(String msg) {
        if (!msg.startsWith(".")) return true;
        String[] a = msg.substring(1).trim().split("\\s+");
        try {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "help" -> say(".t <moduł> | .bind <moduł> <klawisz|none> | .set <moduł> <liczba> | .list | .friend add/remove/list <nick> | .notify | .panic | .save");
                case "t", "toggle" -> {
                    Module m = a.length > 1 ? find(a[1]) : null;
                    if (m == null) say("Nie ma takiego modułu. Użyj .list"); else m.toggle();
                }
                case "bind" -> {
                    Module m = a.length > 2 ? find(a[1]) : null;
                    if (m == null) { say("Użycie: .bind <moduł> <klawisz|none>"); break; }
                    if (a[2].equalsIgnoreCase("none")) { m.key = -1; say(m.name + ": bind usunięty"); break; }
                    try {
                        m.key = InputUtil.fromTranslationKey("key.keyboard." + a[2].toLowerCase(Locale.ROOT)).getCode();
                        say(m.name + " -> " + a[2].toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) { say("Nieznany klawisz: " + a[2]); }
                }
                case "set" -> {
                    Module m = a.length > 2 ? find(a[1]) : null;
                    if (m == null || !m.hasValue) { say("Użycie: .set <moduł> <liczba> (moduł musi mieć suwak)"); break; }
                    m.value = Math.max(m.min, Math.min(m.max, Double.parseDouble(a[2])));
                    say(m.name + ": " + m.valueName + " = " + m.value);
                }
                case "list" -> {
                    StringBuilder sb = new StringBuilder();
                    for (Module m : ModuleManager.ALL) sb.append(m.enabled ? "§a" : "§7").append(m.name).append(" ");
                    say(sb.toString());
                }
                case "friend" -> {
                    String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
                    if (sub.equals("add") && a.length > 2) { Config.friends.add(a[2].toLowerCase(Locale.ROOT)); say("Dodano znajomego: " + a[2]); }
                    else if (sub.equals("remove") && a.length > 2) { Config.friends.remove(a[2].toLowerCase(Locale.ROOT)); say("Usunięto: " + a[2]); }
                    else say("Znajomi: " + (Config.friends.isEmpty() ? "brak" : String.join(", ", Config.friends)));
                }
                case "notify" -> { Config.notifications = !Config.notifications; say("Powiadomienia: " + (Config.notifications ? "ON" : "OFF")); }
                case "panic" -> {
                    for (Module m : ModuleManager.ALL)
                        if (m.enabled && !m.name.equals("HUD") && !m.name.equals("ArrayList")) m.setEnabled(false);
                    say("Wszystkie moduły wyłączone");
                }
                case "save" -> { ConfigStore.save(); say("Zapisano konfigurację"); }
                default -> say("Nieznana komenda. Wpisz .help");
            }
        } catch (NumberFormatException e) { say("To nie jest liczba"); }
        ConfigStore.save();
        return false; // nie wysylaj na serwer
    }

    private Commands() {}
}
