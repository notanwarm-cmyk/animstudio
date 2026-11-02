package com.example.animstudio.mixin;
import com.example.animstudio.actor.ActorPoseHolder;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements ActorPoseHolder {
    @Unique private float[] animstudio$pose;
    @Override public float[] animstudio$getPose() { return animstudio$pose; }
    @Override public void animstudio$setPose(float[] p) { animstudio$pose = p; }
}
