package com.shadowmonarch.client;

import com.shadowmonarch.data.SMData;
import net.minecraft.nbt.CompoundNBT;

/** Last System data received from the server. */
public final class ClientData {
    private static CompoundNBT tag = new CompoundNBT();
    public static int version = 0;

    public static void set(CompoundNBT t) {
        tag = t == null ? new CompoundNBT() : t;
        version++;
    }

    public static SMData get() { return new SMData(tag); }

    public static boolean ready() { return tag.getBoolean("init"); }

    private ClientData() {}
}
