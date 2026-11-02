package com.example.animstudio.record;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
public final class RecordingManager {
    private static Recording current;
    public static boolean active() { return current != null; }
    public static Component label() { return Component.literal(active() ? "\u25CF Recording... (click to stop)" : "\u25CB Start Recording"); }

    public static void toggle() { if (active()) stopAndSave(); else start(); }
    public static void start() {
        current = new Recording();
        current.name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }
    public static void stopAndSave() {
        if (current == null) return;
        if (!current.frames.isEmpty()) RecordingStore.save(current);
        current = null;
    }
    /** END_CLIENT_TICK. Records the local player's motion and actions. */
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (current == null || p == null || mc.level == null) return;
        Recording.Frame f = new Recording.Frame();
        f.x = p.getX(); f.y = p.getY(); f.z = p.getZ();
        f.yRot = p.getYRot(); f.xRot = p.getXRot(); f.headYaw = p.getYHeadRot();
        f.pose = p.getPose().name();
        if (p.isCrouching()) f.flags |= Recording.SNEAK;
        if (p.isSprinting()) f.flags |= Recording.SPRINT;
        if (p.swinging) f.flags |= Recording.SWING;
        if (p.isUsingItem()) f.flags |= Recording.USING;
        if (p.onGround()) f.flags |= Recording.GROUND;
        f.mainHand = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
        current.frames.add(f);
    }
}
