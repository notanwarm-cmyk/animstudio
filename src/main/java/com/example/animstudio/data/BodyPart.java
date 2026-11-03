package com.example.animstudio.data;
/** Order matters: parents always come before their children. */
public enum BodyPart {
    ROOT("Whole character"), TORSO("Torso / Belly"), HEAD("Head"), HAT("Hat layer"), EAR_L("Left ear"), EAR_R("Right ear"),
    JACKET("Jacket layer"), RIGHT_ARM("Right arm"), RIGHT_SLEEVE("Right sleeve"), LEFT_ARM("Left arm"), LEFT_SLEEVE("Left sleeve"),
    RIGHT_LEG("Right leg"), RIGHT_PANTS("Right pants"), LEFT_LEG("Left leg"), LEFT_PANTS("Left pants"), FACE("Face (eyes / mouth)");
    private final String label;
    BodyPart(String l) { label = l; }
    public String label() { return label; }
}
