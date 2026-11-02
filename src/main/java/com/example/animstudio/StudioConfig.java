package com.example.animstudio;
import com.example.animstudio.gui.Theme;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class StudioConfig {
    public boolean dark = true;
    public boolean fps15 = false;            // 15-FPS stop-motion mode
    public boolean limitAllEntities = true;  // false = actors only
    public int limitFps = 15;
    public String irisPack = "";
    public static StudioConfig I = load();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("animstudio.json"); }
    private static StudioConfig load() {
        try { StudioConfig c = new Gson().fromJson(Files.readString(file()), StudioConfig.class); if (c != null) return c; }
        catch (Exception ignored) {}
        return new StudioConfig();
    }
    public void save() { try { Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(this)); } catch (Exception ignored) {} }
    public Theme theme() { return dark ? Theme.DARK : Theme.LIGHT; }
}
