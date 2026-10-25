package com.shadowmonarch.data;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.UUID;

/** Levels, ranks, gate (promotion) bosses and the stats they give. */
public final class Progression {
    public static final int MAX_LEVEL = 50;
    /** The level at which you must beat a Gate Master before you can level further. */
    public static final int[] GATE_LEVELS = {5, 10, 15, 20, 25, 30, 40};
    public static final String[] GATE_BOSSES = {"goblin_chief", "fang", "cerberus", "tank", "iron", "igris", "beru"};
    public static final String[] RANK_NAMES = {"E", "D", "C", "B", "A", "S", "SS", "National"};

    private static final UUID HP_ID = UUID.fromString("5b7d4c2e-0a11-4f6a-9c01-aa0000000001");
    private static final UUID DMG_ID = UUID.fromString("5b7d4c2e-0a11-4f6a-9c01-aa0000000002");
    private static final UUID SPD_ID = UUID.fromString("5b7d4c2e-0a11-4f6a-9c01-aa0000000003");

    public static int levelCap(int rank) {
        return rank < GATE_LEVELS.length ? GATE_LEVELS[rank] : MAX_LEVEL;
    }

    public static boolean atGate(SMData d) {
        return d.rank() < GATE_LEVELS.length && d.level() >= GATE_LEVELS[d.rank()];
    }

    public static String gateBoss(SMData d) {
        return d.rank() < GATE_BOSSES.length ? GATE_BOSSES[d.rank()] : null;
    }

    public static int xpNeeded(int level) {
        return 40 + 20 * level + 3 * level * level;
    }

    public static String rankName(int rank) {
        return RANK_NAMES[Math.min(rank, RANK_NAMES.length - 1)];
    }

    public static void say(PlayerEntity p, String msg) {
        p.sendMessage(new StringTextComponent("§3[System] §f" + msg));
    }

    // ------------------------------------------------------------------ xp / levels
    public static void addXp(ServerPlayerEntity p, int amount) {
        SMData d = SMData.get(p);
        int cap = levelCap(d.rank());
        if (d.level() >= cap) {
            if (d.level() < MAX_LEVEL) notifyGate(p, d, false);
            ModNetwork.sync(p);
            return;
        }
        d.setXp(d.xp() + amount);
        boolean up = false;
        while (d.level() < cap && d.xp() >= xpNeeded(d.level())) {
            d.setXp(d.xp() - xpNeeded(d.level()));
            d.setLevel(d.level() + 1);
            up = true;
            say(p, "§eLEVEL UP!§f You are now level §a" + d.level() + "§f.");
        }
        if (d.level() >= cap) d.setXp(0);
        if (up) {
            applyStats(p);
            p.world.playSound(null, p.getPosition(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1f);
            if (d.level() >= cap && d.level() < MAX_LEVEL) notifyGate(p, d, true);
            Quests.onLevel(p);
        }
        ModNetwork.sync(p);
    }

    private static void notifyGate(ServerPlayerEntity p, SMData d, boolean first) {
        String boss = gateBoss(d);
        if (boss == null) return;
        MobDefs.Def def = MobDefs.MOBS.get(boss);
        if (first) {
            say(p, "§cYour growth is blocked! §fDefeat the Gate Master §e" + def.name
                    + "§f to rank up. Use the §cGate Master's Key§f (or /sm key).");
        } else if (p.ticksExisted % 200 == 0) {
            say(p, "§cDefeat §e" + def.name + "§c to continue growing.");
        }
        if (d.keyRank() != d.rank() + 1) {
            d.setKeyRank(d.rank() + 1);
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModItems.BOSS_KEY));
        }
    }

    /** Called when any boss monster dies by the player's hand (or their shadows'). */
    public static void onBossKilled(ServerPlayerEntity p, MobDefs.Def def) {
        SMData d = SMData.get(p);
        if (def.shadow != null && d.unlockShadow(def.shadow)) {
            say(p, "§5Shadow Extraction successful: §d" + def.name.split(",")[0]
                    + "§f joined your army! Open the Shadow Army menu (L) to equip it.");
            p.world.playSound(null, p.getPosition(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.4f, 1.5f);
        }
        String gate = gateBoss(d);
        if (gate != null && gate.equals(def.id) && d.level() >= GATE_LEVELS[d.rank()]) {
            rankUp(p, d);
        }
        Quests.advance(p, Quests.Type.BOSS, def.id, 1);
        ModNetwork.sync(p);
    }

    public static void rankUp(ServerPlayerEntity p, SMData d) {
        d.setRank(d.rank() + 1);
        applyStats(p);
        p.setHealth(p.getMaxHealth());
        d.setMp(d.maxMp());
        say(p, "§6§lRANK UP! §r§fYou are now a §6" + rankName(d.rank())
                + "-Rank§f hunter. Your power has surged!");
        p.world.playSound(null, p.getPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
        if (d.level() < levelCap(d.rank())) say(p, "You can level up again. Next Gate Master at level "
                + levelCap(d.rank()) + (d.rank() < GATE_LEVELS.length ? "." : " (max)."));
    }

    // ------------------------------------------------------------------ stats
    public static void applyStats(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        double hp = (d.level() - 1) * 1.5 + d.rank() * 10.0;
        double dmg = (d.level() - 1) * 0.35 + d.rank() * 3.0;
        double spd = d.rank() * 0.006 + (d.level() - 1) * 0.0005;
        set(p, SharedMonsterAttributes.MAX_HEALTH, HP_ID, "sm_hp", hp);
        set(p, SharedMonsterAttributes.ATTACK_DAMAGE, DMG_ID, "sm_dmg", dmg);
        set(p, SharedMonsterAttributes.MOVEMENT_SPEED, SPD_ID, "sm_spd", spd);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(ServerPlayerEntity p, net.minecraft.entity.ai.attributes.IAttribute attr, UUID id,
                            String name, double amount) {
        IAttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) {
            inst.applyModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private Progression() {}
}
