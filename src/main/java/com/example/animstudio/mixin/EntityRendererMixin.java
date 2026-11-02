package com.example.animstudio.mixin;
import com.example.animstudio.world.LightRig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** "Dark zones": darken entities/actors inside a zone by scaling packed light (block<<4 | sky<<20). */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private void animstudio$dark(Entity e, float partial, CallbackInfoReturnable<Integer> cir) {
        float k = LightRig.darkFactor(e); if (k <= 0) return;
        int packed = cir.getReturnValue(), b = (packed >> 4) & 15, s = (packed >> 20) & 15;
        cir.setReturnValue((Math.round(b * (1 - k)) << 4) | (Math.round(s * (1 - k)) << 20));
    }
}
