package com.example.animstudio;
import com.example.animstudio.actor.ActorManager;
import com.example.animstudio.anim.AnimationPlayer;
import com.example.animstudio.gui.DashboardScreen;
import com.example.animstudio.record.RecordingManager;
import com.example.animstudio.world.StudioWorld;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
public final class AnimStudioClient implements ClientModInitializer {
    public static KeyMapping OPEN;
    @Override public void onInitializeClient() {
        OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.animstudio.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("animstudio", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.consumeClick()) mc.setScreenAndShow(new DashboardScreen(mc.screen));
            RecordingManager.tick(mc);
            StudioWorld.pollShortcut(mc);
            AnimationPlayer.tick();
            ActorManager.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((h, s, mc) -> { ActorManager.clear(); StudioWorld.onJoin(); });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { RecordingManager.stopAndSave(); ActorManager.clear(); });
    }
}
