package com.example.animstudio.actor;
import com.example.animstudio.data.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import java.util.*;
public final class ActorManager {
    private static final Map<String, ActorEntity> ACTORS = new HashMap<>();
    private static int nextId = -50_000;

    public static ActorEntity get(ActorData d) { return ACTORS.get(d.id); }
    /** Make the world match the project: spawn missing actors, remove deleted ones. */
    public static void sync(StudioProject p) {
        ClientLevel lvl = Minecraft.getInstance().level; if (lvl == null) return;
        Set<String> keep = new HashSet<>();
        for (ActorData d : p.actors) {
            keep.add(d.id);
            ActorEntity e = ACTORS.get(d.id);
            if (e == null || e.level() != lvl) {
                e = new ActorEntity(lvl, d); e.setId(nextId--);
                e.setPos(d.spawnX, d.spawnY, d.spawnZ); lvl.addEntity(e); ACTORS.put(d.id, e);
            }
        }
        for (var it = ACTORS.entrySet().iterator(); it.hasNext();) {
            var en = it.next();
            if (!keep.contains(en.getKey())) { lvl.removeEntity(en.getValue().getId(), Entity.RemovalReason.DISCARDED); it.remove(); }
        }
    }
    public static void clear() { ACTORS.clear(); }
    public static void tick() { for (ActorEntity e : ACTORS.values()) e.tickRoot(); }
}
