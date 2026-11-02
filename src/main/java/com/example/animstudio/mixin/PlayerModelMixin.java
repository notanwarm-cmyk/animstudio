package com.example.animstudio.mixin;
import com.example.animstudio.actor.ActorPoseHolder;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Keyframed parts REPLACE vanilla rotation; un-keyed parts keep the vanilla walk/idle animation. Sleeves/pants/hat are child parts. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void animstudio$apply(AvatarRenderState state, CallbackInfo ci) {
        float[] pose = ((ActorPoseHolder) state).animstudio$getPose();
        if (pose == null) return;
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        ModelPart[] parts = {m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        for (int i = 0; i < parts.length; i++) {
            if (!Float.isNaN(pose[i * 3]))     parts[i].xRot = (float) Math.toRadians(pose[i * 3]);
            if (!Float.isNaN(pose[i * 3 + 1])) parts[i].yRot = (float) Math.toRadians(pose[i * 3 + 1]);
            if (!Float.isNaN(pose[i * 3 + 2])) parts[i].zRot = (float) Math.toRadians(pose[i * 3 + 2]);
        }
    }
}
