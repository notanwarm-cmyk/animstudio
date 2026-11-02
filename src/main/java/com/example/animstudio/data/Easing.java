package com.example.animstudio.data;
public enum Easing {
    LINEAR, STEP, SMOOTH, EASE_IN, EASE_OUT;
    public float apply(float t) {
        return switch (this) {
            case LINEAR -> t;
            case STEP -> 0f;
            case SMOOTH -> t * t * (3 - 2 * t);
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
        };
    }
    public Easing next() { return values()[(ordinal() + 1) % values().length]; }
}
