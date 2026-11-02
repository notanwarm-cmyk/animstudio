package com.example.animstudio.actor;
import com.example.animstudio.anim.AnimationPlayer;
import com.example.animstudio.anim.BlinkScheduler;
import com.example.animstudio.data.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import java.util.UUID;
/** Client-side-only fake player. Never sent to a server, so it is safe in any world. */
public final class ActorEntity extends RemotePlayer {
    public final ActorData data; public SkinManager.Skin skin;
    private static final BodyPart[] LIMBS = {BodyPart.HEAD, BodyPart.TORSO, BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG};

    public ActorEntity(ClientLevel level, ActorData d) {
        super(level, new GameProfile(UUID.nameUUIDFromBytes(d.id.getBytes()), d.name));
        this.data = d; this.skin = SkinManager.load(d);
    }
    public void reloadSkin() { skin = SkinManager.load(data); }
    @Override public PlayerSkin getSkin() { return skin.playerSkin(); }
    @Override public boolean shouldShowName() { return false; }

    /** Tick-rate root motion; limbs/face are evaluated at render rate in samplePose(). */
    public void tickRoot() {
        float f = AnimationPlayer.frame;
        double x = data.sample(BodyPart.ROOT, Channel.POS_X, f, (float) data.spawnX), y = data.sample(BodyPart.ROOT, Channel.POS_Y, f, (float) data.spawnY),
               z = data.sample(BodyPart.ROOT, Channel.POS_Z, f, (float) data.spawnZ);
        float yaw = data.sample(BodyPart.ROOT, Channel.ROT_Y, f, data.spawnYaw);
        setOldPosAndRot();                       // keeps vanilla walk-cycle + interpolation working
        setPos(x, y, z); setYRot(yaw); setYBodyRot(yaw); setYHeadRot(yaw);
    }
    /** @return degrees: [part][x,y,z] flattened for HEAD,TORSO,R_ARM,L_ARM,R_LEG,L_LEG; NaN = not keyed. Also drives the face. */
    public float[] samplePose(float partial) {
        float f = AnimationPlayer.renderFrame(partial);
        float[] out = new float[LIMBS.length * 3];
        for (int i = 0; i < LIMBS.length; i++) {
            out[i * 3] = data.sample(LIMBS[i], Channel.ROT_X, f); out[i * 3 + 1] = data.sample(LIMBS[i], Channel.ROT_Y, f);
            out[i * 3 + 2] = data.sample(LIMBS[i], Channel.ROT_Z, f);
        }
        float eyes = data.sample(BodyPart.FACE, Channel.EYES_CLOSED, f, 0f);
        if (data.autoBlink && data.hasRegion(FaceRegion.Kind.EYE))
            eyes = Math.max(eyes, BlinkScheduler.closed(data.blinkSeed, f / AnimationPlayer.project.tempo));
        skin.face().update(data.regions, eyes, data.sample(BodyPart.FACE, Channel.MOUTH_OPEN, f, 0f));
        return out;
    }
}
