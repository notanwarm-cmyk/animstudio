package com.example.animstudio.data;
public enum BodyPart { ROOT, HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, FACE;
    public String label() { return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' '); } }
