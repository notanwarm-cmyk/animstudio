package com.example.animstudio.data;
import java.util.*;
public final class ActorData {
    public String id = UUID.randomUUID().toString();
    public String name = "Actor";
    public String skinFile;               // file name inside animstudio/skins, null = Steve
    public boolean slim, autoBlink = true;
    public long blinkSeed = new Random().nextLong();
    public double spawnX, spawnY, spawnZ; public float spawnYaw;
    public List<Track> tracks = new ArrayList<>();
    public List<FaceRegion> regions = new ArrayList<>();

    public Track track(BodyPart p, Channel c, boolean create) {
        for (Track t : tracks) if (t.part == p && t.channel == c) return t;
        if (!create) return null;
        Track t = new Track(p, c); tracks.add(t); return t;
    }
    public float sample(BodyPart p, Channel c, float frame) {
        Track t = track(p, c, false); return t == null ? Float.NaN : t.sample(frame);
    }
    public float sample(BodyPart p, Channel c, float frame, float def) {
        float v = sample(p, c, frame); return Float.isNaN(v) ? def : v;
    }
    public boolean hasRegion(FaceRegion.Kind k) { return regions.stream().anyMatch(r -> r.kind == k); }
}
