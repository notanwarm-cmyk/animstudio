package com.example.animstudio.mixin;
import com.example.animstudio.actor.ActorEntity;
import com.example.animstudio.actor.ActorPoseHolder;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Copies the sampled keyframe pose from the ActorEntity into the render state (the model reads state, not the entity). */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void animstudio$pose(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
        ((ActorPoseHolder) state).animstudio$setPose(avatar instanceof ActorEntity a ? a.samplePose(partial) : null);
    }
}
