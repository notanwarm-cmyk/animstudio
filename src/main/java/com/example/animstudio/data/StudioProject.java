package com.example.animstudio.data;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
public final class StudioProject {
    public String name = "Untitled";
    public float tempo = 24f;          // frames per second of the project timeline
    public int lengthFrames = 240;
    public boolean loop = true;
    public List<ActorData> actors = new ArrayList<>();
    public List<LightPoint> lights = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("animstudio/projects"); }
    public void save() {
        try { Files.createDirectories(dir()); Files.writeString(dir().resolve(name + ".json"), GSON.toJson(this)); }
        catch (IOException e) { e.printStackTrace(); }
    }
    public static StudioProject load(String name) {
        try { return GSON.fromJson(Files.readString(dir().resolve(name + ".json")), StudioProject.class); }
        catch (Exception e) { return new StudioProject(); }
    }
    public static List<String> list() {
        try (Stream<Path> s = Files.list(dir())) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted().toList();
        } catch (IOException e) { return List.of(); }
    }
}
