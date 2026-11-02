package com.example.animstudio.data;
/** ROOT uses POS_* (blocks) + ROT_Y (yaw). Limbs use ROT_* (degrees). FACE uses EYES_CLOSED / MOUTH_OPEN (0..1). */
public enum Channel { POS_X, POS_Y, POS_Z, ROT_X, ROT_Y, ROT_Z, EYES_CLOSED, MOUTH_OPEN }
