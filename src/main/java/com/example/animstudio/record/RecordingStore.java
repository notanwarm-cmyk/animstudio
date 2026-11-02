package com.example.animstudio.record;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
public final class RecordingStore {
    private static final Gson GSON = new Gson();
    public static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("animstudio/recordings"); }
    public static void save(Recording r) {
        try { Files.createDirectories(dir()); Files.writeString(dir().resolve(r.name + ".json"), GSON.toJson(r)); }
        catch (IOException e) { e.printStackTrace(); }
    }
    public static Recording load(String name) {
        try { return GSON.fromJson(Files.readString(dir().resolve(name + ".json")), Recording.class); }
        catch (IOException e) { return null; }
    }
    public static List<String> list() {
        try (Stream<Path> s = Files.list(dir())) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted(Comparator.reverseOrder()).toList();
        } catch (IOException e) { return List.of(); }
    }
    public static void delete(String name) { try { Files.deleteIfExists(dir().resolve(name + ".json")); } catch (IOException ignored) {} }
}
