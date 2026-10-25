package pl.nexus.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), WORLD("World"), VISUAL("Wygląd"), SETTINGS("Ustawienia");
    public final String label;
    Category(String label) { this.label = label; }
}
