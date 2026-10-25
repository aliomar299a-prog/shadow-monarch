package com.shadowmonarch.entity;

/** Implemented by every mod entity so one renderer class can pick texture/scale per variant. */
public interface IVariant {
    /** e.g. "goblin" or "shadow_igris" (also the texture file name). */
    String variantId();
}
