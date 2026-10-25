package com.shadowmonarch.event;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.Quests;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.dungeon.DungeonManager;
import com.shadowmonarch.entity.ShadowEntity;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ServerEvents {
    private static final Random RNG = new Random();
    private final Map<UUID, double[]> lastPos = new HashMap<>();

    // ------------------------------------------------------------------ join / world creation
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        SMData d = SMData.get(p);
        if (!d.isInit()) {
            // brand-new world (or first time with the mod): the System awakens automatically, no shadows yet
            d.init();
            Progression.applyStats(p);
            p.setHealth(p.getMaxHealth());
            Progression.say(p, "§b§lYou have been chosen as a Player.§r§f The System has awakened.");
            Progression.say(p, "You have no shadows yet. Complete quests, clear Gates and defeat Gate Masters to grow stronger.");
            Quests.start(p, d);
        } else {
            Progression.applyStats(p);
        }
        DungeonManager.onLogin(p);
        ModNetwork.sync(p);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        DungeonManager.end(p, true);
        ShadowManager.dismissAll(p);
        lastPos.remove(p.getUniqueID());
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone e) {
        CompoundNBT old = SMData.get(e.getOriginal()).raw();
        CompoundNBT now = SMData.get(e.getPlayer()).raw();
        for (String k : new HashSet<>(now.keySet())) now.remove(k);
        now.merge(old.copy());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        Progression.applyStats(p);
        p.setHealth(p.getMaxHealth());
        ModNetwork.sync(p);
    }

    // ------------------------------------------------------------------ tick: MP regen, quests, gates
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) return;
        if (!(e.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.player;
        if (p.ticksExisted % 20 != 0) return;
        SMData d = SMData.get(p);
        if (!d.isInit()) return;

        double[] last = lastPos.get(p.getUniqueID());
        boolean resting = last != null && p.getDistanceSq(last[0], last[1], last[2]) < 0.09;
        lastPos.put(p.getUniqueID(), new double[]{p.posX, p.posY, p.posZ});

        if (d.mp() < d.maxMp()) {
            double rate = 40 + d.level() * 2; // hundredths of MP per second
            if (resting) rate *= 3;
            if (p.isSleeping()) rate *= 10;
            int food = p.getFoodStats().getFoodLevel();
            if (food >= 18) rate *= 1.5; else if (food <= 6) rate *= 0.3;
            int frac = d.mpFrac() + (int) rate;
            int before = d.mp();
            d.setMp(before + frac / 100);
            d.setMpFrac(frac % 100);
            if (d.mp() != before) ModNetwork.sync(p);
        }
        Quests.tick(p);
        DungeonManager.tick(p);
    }

    // ------------------------------------------------------------------ eating restores MP
    @SubscribeEvent
    public void onEat(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ItemStack stack = e.getItem();
        if (!stack.getItem().isFood()) return;
        Food f = stack.getItem().getFood();
        if (f == null) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getEntityLiving();
        SMData d = SMData.get(p);
        if (!d.isInit()) return;
        d.setMp(d.mp() + f.getHealing() * 4);
        ModNetwork.sync(p);
    }

    // ------------------------------------------------------------------ kills
    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntityLiving();
        if (dead.world.isRemote) return;
        if (dead instanceof ServerPlayerEntity) {
            ServerPlayerEntity pl = (ServerPlayerEntity) dead;
            DungeonManager.end(pl, false);
            ShadowManager.dismissAll(pl);
            return;
        }
        Entity src = e.getSource().getTrueSource();
        ServerPlayerEntity p = null;
        if (src instanceof ServerPlayerEntity) {
            p = (ServerPlayerEntity) src;
        } else if (src instanceof ShadowEntity) {
            PlayerEntity o = ((ShadowEntity) src).getOwner();
            if (o instanceof ServerPlayerEntity) p = (ServerPlayerEntity) o;
        }
        if (p == null || !(dead instanceof IMob) || dead instanceof ShadowEntity) return;
        if (!SMData.get(p).isInit()) return;

        String path = dead.getType().getRegistryName().getPath();
        MobDefs.Def def = "shadowmonarch".equals(dead.getType().getRegistryName().getNamespace()) ? MobDefs.get(path) : null;
        int xp = def != null ? def.xp : Math.max(2, (int) (dead.getMaxHealth() / 4));
        int stones = 0;
        if (def != null) {
            stones = def.boss ? def.stones : (RNG.nextInt(2) == 0 ? 1 : 0);
        } else if (RNG.nextInt(7) == 0) {
            stones = 1;
        }
        if (stones > 0) dead.entityDropItem(new ItemStack(ModItems.MAGIC_STONE, stones), 0.5F);

        Progression.addXp(p, xp);
        Quests.onKill(p, path);
        if (def != null && def.boss) Progression.onBossKilled(p, def);
    }

    // ------------------------------------------------------------------ owner can't hurt own shadows
    @SubscribeEvent
    public void onAttack(LivingAttackEvent e) {
        if (e.getEntityLiving() instanceof ShadowEntity) {
            Entity src = e.getSource().getTrueSource();
            ShadowEntity s = (ShadowEntity) e.getEntityLiving();
            if (src instanceof PlayerEntity && src.getUniqueID().equals(s.getOwnerId())) e.setCanceled(true);
        }
    }
}
