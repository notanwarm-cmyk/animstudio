package com.example.animstudio.mixin;
import com.example.animstudio.gui.DashboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component c) { super(c); }
    @Inject(method = "init", at = @At("TAIL"))
    private void animstudio$button(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("Animation Studio"),
                b -> Minecraft.getInstance().setScreenAndShow(new DashboardScreen((Screen) (Object) this))).bounds(6, 6, 110, 20).build());
    }
}
