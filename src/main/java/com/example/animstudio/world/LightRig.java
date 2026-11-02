package com.example.animstudio.world;
import com.example.animstudio.anim.AnimationPlayer;
import com.example.animstudio.data.LightPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
public final class LightRig {
    /** Emitter: a vanilla `light` block (works with Iris/OptiFine-style shader lighting and every vanilla renderer). */
    public static void placeEmitter(int level) {
        Minecraft mc = Minecraft.getInstance(); if (mc.player == null) return;
        LightPoint p = new LightPoint(); p.x = mc.player.getBlockX(); p.y = mc.player.getBlockY() + 1; p.z = mc.player.getBlockZ(); p.level = level;
        AnimationPlayer.project.lights.add(p);
        mc.player.connection.sendCommand("setblock " + p.x + " " + p.y + " " + p.z + " minecraft:light[level=" + level + "]");
    }
    public static void placeDark(float radius) {
        Minecraft mc = Minecraft.getInstance(); if (mc.player == null) return;
        LightPoint p = new LightPoint(); p.dark = true; p.radius = radius;
        p.x = mc.player.getBlockX(); p.y = mc.player.getBlockY(); p.z = mc.player.getBlockZ();
        AnimationPlayer.project.lights.add(p);
    }
    /** 0..1: how dark an entity at this position should be (used by EntityRendererMixin). */
    public static float darkFactor(Entity e) {
        float best = 0;
        for (LightPoint p : AnimationPlayer.project.lights) {
            if (!p.dark) continue;
            double d = Math.sqrt(e.distanceToSqr(p.x + .5, p.y + .5, p.z + .5));
            if (d < p.radius) best = Math.max(best, (float) (1 - d / p.radius) * 2f > 1 ? 1 : (float) (1 - d / p.radius) * 2f);
        }
        return best;
    }
}
