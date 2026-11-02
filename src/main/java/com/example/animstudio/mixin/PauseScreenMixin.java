package com.example.animstudio.mixin;
import com.example.animstudio.gui.DashboardScreen;
import com.example.animstudio.record.RecordingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Record toggle + Studio shortcut to the pause menu of ANY world or server. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component c) { super(c); }
    @Inject(method = "init", at = @At("TAIL"))
    private void animstudio$addButtons(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(RecordingManager.label(), b -> {
            RecordingManager.toggle(); b.setMessage(RecordingManager.label());
        }).bounds(6, 6, 160, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Animation Studio"),
                b -> Minecraft.getInstance().setScreenAndShow(new DashboardScreen((Screen) (Object) this))).bounds(6, 30, 160, 20).build());
    }
}
