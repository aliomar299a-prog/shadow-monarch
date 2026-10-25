package com.shadowmonarch.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin typed wrapper around the player's persistent NBT ("PlayerPersisted"/"sm").
 * Server side it wraps the live tag, client side it wraps the last synced copy.
 */
public class SMData {
    public static final int MAX_EQUIPPED = 2;
    private final CompoundNBT t;

    public SMData(CompoundNBT t) { this.t = t; }

    public static SMData get(PlayerEntity p) {
        CompoundNBT pd = p.getPersistentData();
        if (!pd.contains(PlayerEntity.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            pd.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
        }
        CompoundNBT per = pd.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        if (!per.contains("sm", Constants.NBT.TAG_COMPOUND)) per.put("sm", new CompoundNBT());
        return new SMData(per.getCompound("sm"));
    }

    public CompoundNBT raw() { return t; }

    public CompoundNBT copy() { return t.copy(); }

    // ------------------------------------------------------------------ basics
    public boolean isInit() { return t.getBoolean("init"); }

    public void init() {
        t.putBoolean("init", true);
        t.putInt("lvl", 1);
        t.putInt("xp", 0);
        t.putInt("rank", 0);
        t.putInt("quest", 0);
        t.putInt("prog", 0);
        t.putInt("mp", maxMp());
    }

    public int level() { return Math.max(1, t.getInt("lvl")); }
    public void setLevel(int v) { t.putInt("lvl", v); }
    public int xp() { return t.getInt("xp"); }
    public void setXp(int v) { t.putInt("xp", v); }
    public int rank() { return t.getInt("rank"); }
    public void setRank(int v) { t.putInt("rank", v); }
    public int quest() { return t.getInt("quest"); }
    public void setQuest(int v) { t.putInt("quest", v); }
    public int prog() { return t.getInt("prog"); }
    public void setProg(int v) { t.putInt("prog", v); }
    public long sprintBase() { return t.getLong("sbase"); }
    public void setSprintBase(long v) { t.putLong("sbase", v); }
    public int keyRank() { return t.getInt("keyrank"); }
    public void setKeyRank(int v) { t.putInt("keyrank", v); }

    public int maxMp() { return 30 + level() * 8 + rank() * 20; }
    public int mp() { return Math.min(t.getInt("mp"), maxMp()); }
    public void setMp(int v) { t.putInt("mp", Math.max(0, Math.min(v, maxMp()))); }
    /** fractional regeneration accumulator (hundredths of an MP point) */
    public int mpFrac() { return t.getInt("mpf"); }
    public void setMpFrac(int v) { t.putInt("mpf", v); }

    // ------------------------------------------------------------------ dungeon
    public boolean inDungeon() { return t.getBoolean("indg"); }
    public void setInDungeon(boolean v) { t.putBoolean("indg", v); }
    public void setReturn(double x, double y, double z) {
        t.putDouble("rx", x); t.putDouble("ry", y); t.putDouble("rz", z);
    }
    public double[] returnPos() { return new double[]{t.getDouble("rx"), t.getDouble("ry"), t.getDouble("rz")}; }

    // ------------------------------------------------------------------ shadows
    private CompoundNBT shadowTag() {
        if (!t.contains("sh", Constants.NBT.TAG_COMPOUND)) t.put("sh", new CompoundNBT());
        return t.getCompound("sh");
    }

    public boolean hasShadow(String id) { return shadowTag().contains(id); }
    public int shadowLevel(String id) { return Math.max(1, shadowTag().getInt(id)); }
    public void setShadowLevel(String id, int lvl) { shadowTag().putInt(id, lvl); }
    public boolean unlockShadow(String id) {
        if (hasShadow(id)) return false;
        shadowTag().putInt(id, 1);
        return true;
    }
    public List<String> unlockedShadows() { return new ArrayList<>(shadowTag().keySet()); }

    public List<String> equipped() {
        List<String> out = new ArrayList<>();
        ListNBT l = t.getList("eq", Constants.NBT.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    public void setEquipped(List<String> ids) {
        ListNBT l = new ListNBT();
        for (String s : ids) l.add(new StringNBT(s));
        t.put("eq", l);
    }
}
