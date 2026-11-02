package com.example.animstudio.mixin;
import com.example.animstudio.StudioConfig;
import com.example.animstudio.actor.ActorEntity;
import com.example.animstudio.anim.FrameLimiter;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * 15-FPS mode: entity render state is only re-extracted when the 1/15 s wall-clock bucket changes; in between,
 * the cached state is reused -> entities/actors stop-motion while the world + camera render at full FPS.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique private final Int2ObjectOpenHashMap<EntityRenderState> animstudio$cache = new Int2ObjectOpenHashMap<>();
    @Unique private final Int2ObjectOpenHashMap<Long> animstudio$stamp = new Int2ObjectOpenHashMap<>();

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void animstudio$hold(E e, float partial, CallbackInfoReturnable<EntityRenderState> cir) {
        if (!FrameLimiter.active() || e == Minecraft.getInstance().getCameraEntity()) return;
        if (!StudioConfig.I.limitAllEntities && !(e instanceof ActorEntity)) return;
        EntityRenderState s = animstudio$cache.get(e.getId());
        if (s != null && animstudio$stamp.get(e.getId()) == FrameLimiter.bucket()) cir.setReturnValue(s);
    }
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void animstudio$store(E e, float partial, CallbackInfoReturnable<EntityRenderState> cir) {
        if (!FrameLimiter.active()) { if (!animstudio$cache.isEmpty()) { animstudio$cache.clear(); animstudio$stamp.clear(); } return; }
        if (animstudio$cache.size() > 1024) { animstudio$cache.clear(); animstudio$stamp.clear(); }
        animstudio$cache.put(e.getId(), cir.getReturnValue()); animstudio$stamp.put(e.getId(), FrameLimiter.bucket());
    }
}
