package com.example.animstudio.actor;
/** Duck interface mixed into AvatarRenderState: carries per-part rotation overrides (degrees, NaN = leave vanilla). */
public interface ActorPoseHolder {
    float[] animstudio$getPose();
    void animstudio$setPose(float[] pose);
}
