package com.example.animstudio.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
public final class StudioWorld {
    public static final String LEVEL_ID = "AnimStudio";
    private static Runnable pending;
    private static boolean fkeyHeld;
    private static boolean prefillNext;

    public static void runAfterJoin(Runnable r) { pending = r; }
    public static void onJoin() { if (pending != null) { Runnable r = pending; pending = null; r.run(); } }

    /** Launch (or create) the dedicated flat creative studio world. */
    public static void launch(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) { if (pending != null) onJoin(); return; }
        try {
            if (mc.getLevelSource().levelExists(LEVEL_ID)) { mc.createWorldOpenFlows().openWorld(LEVEL_ID, () -> mc.setScreenAndShow(parent)); return; }
        } catch (Exception ignored) {}
        prefillNext = true;   // first run: vanilla Create World screen, pre-filled by prefill() via ScreenEvents.AFTER_INIT
        CreateWorldScreen.openFresh(mc, () -> mc.setScreenAndShow(parent));
    }

    /** Called from AnimStudioClient when a CreateWorldScreen finishes init. */
    public static void prefill(CreateWorldScreen cws) {
        if (!prefillNext) return;
        prefillNext = false;
        WorldCreationUiState ui = cws.getUiState();
        ui.setName(LEVEL_ID); ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); ui.setAllowCommands(true);
        ui.getNormalPresetList().stream()
          .filter(e -> e.preset() != null && e.preset().unwrapKey().map(k -> k.identifier().getPath().equals("flat")).orElse(false))
          .findFirst().ifPresent(ui::setWorldType);
    }

    public static boolean inStudioWorld() {
        var srv = Minecraft.getInstance().getSingleplayerServer();
        return srv != null && LEVEL_ID.equals(srv.getWorldData().getLevelName());
    }

    /** F3+F4 -> /gamemode creative, only inside the studio world. */
    public static void pollShortcut(Minecraft mc) {
        if (mc.player == null || !inStudioWorld()) return;
        boolean f3 = com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F3);
        boolean f4 = com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F4);
        boolean now = f3 && f4;
        if (now && !fkeyHeld) mc.player.connection.sendCommand("gamemode creative");
        fkeyHeld = now;
    }
}
