package com.shadowmonarch.event;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModEntities;
import com.shadowmonarch.ModItems;
import com.shadowmonarch.data.Progression;
import com.shadowmonarch.data.Quests;
import com.shadowmonarch.data.SMData;
import com.shadowmonarch.entity.ShadowEntity;
import com.shadowmonarch.net.ModNetwork;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/** Summon / dismiss / equip / upgrade logic (server side). */
public final class ShadowManager {

    public static List<ShadowEntity> findShadows(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        AxisAlignedBB box = new AxisAlignedBB(p.posX - 200, 0, p.posZ - 200, p.posX + 200, 256, p.posZ + 200);
        return new ArrayList<>(w.getEntitiesWithinAABB(ShadowEntity.class, box,
                s -> s.getOwnerId() != null && s.getOwnerId().equals(p.getUniqueID())));
    }

    public static void dismissAll(ServerPlayerEntity p) {
        for (ShadowEntity s : findShadows(p)) s.remove();
    }

    public static void toggleSummon(ServerPlayerEntity p) {
        SMData d = SMData.get(p);
        List<ShadowEntity> active = findShadows(p);
        if (!active.isEmpty()) {
            for (ShadowEntity s : active) {
                p.getServerWorld().spawnParticle(ParticleTypes.SMOKE, s.posX, s.posY + 1, s.posZ, 12, 0.3, 0.5, 0.3, 0.02);
                s.remove();
            }
            Progression.say(p, "Your shadows return to the dark.");
            return;
        }
        List<String> eq = d.equipped();
        if (eq.isEmpty()) {
            Progression.say(p, "No shadows equipped. Open the Shadow Army menu (L) to equip them"
                    + (d.unlockedShadows().isEmpty() ? " (you have none yet - defeat their original monsters)." : "."));
            return;
        }
        int cost = 0;
        for (String id : eq) cost += MobDefs.SHADOWS.get(id).summonCost();
        if (d.mp() < cost) {
            Progression.say(p, "§cNot enough MP. §fNeed " + cost + ", you have " + d.mp() + ".");
            return;
        }
        d.setMp(d.mp() - cost);
        ServerWorld w = p.getServerWorld();
        int i = 0;
        for (String id : eq) {
            EntityType<ShadowEntity> type = ModEntities.SHADOWS.get(id);
            if (type == null) continue;
            ShadowEntity s = type.create(w);
            if (s == null) continue;
            double ang = Math.toRadians(p.rotationYaw + 90 + 120 * i);
            double x = p.posX + Math.cos(ang) * 2.0, z = p.posZ + Math.sin(ang) * 2.0;
            s.setLocationAndAngles(x, p.posY, z, p.rotationYaw, 0f);
            s.init(p, id, d.shadowLevel(id));
            w.addEntity(s);
            w.spawnParticle(ParticleTypes.PORTAL, x, p.posY + 1, z, 30, 0.4, 0.8, 0.4, 0.2);
            i++;
        }
        w.playSound(null, p.getPosition(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 0.5f);
        Progression.say(p, "§5Arise!§f Shadows summoned (-" + cost + " MP).");
        Quests.advance(p, Quests.Type.SUMMON, null, 1);
        ModNetwork.sync(p);
    }

    public static void equip(ServerPlayerEntity p, String id) {
        SMData d = SMData.get(p);
        if (!d.hasShadow(id)) return;
        List<String> eq = d.equipped();
        if (eq.contains(id)) return;
        if (eq.size() >= SMData.MAX_EQUIPPED) {
            Progression.say(p, "You can only equip " + SMData.MAX_EQUIPPED + " shadows. Unequip one first.");
            return;
        }
        eq.add(id);
        d.setEquipped(eq);
        ModNetwork.sync(p);
    }

    public static void unequip(ServerPlayerEntity p, String id) {
        SMData d = SMData.get(p);
        List<String> eq = d.equipped();
        if (eq.remove(id)) {
            d.setEquipped(eq);
            ModNetwork.sync(p);
        }
    }

    public static int upgradeCost(int level) { return level * 2; }

    public static void upgrade(ServerPlayerEntity p, String id) {
        SMData d = SMData.get(p);
        if (!d.hasShadow(id)) return;
        int lvl = d.shadowLevel(id);
        if (lvl >= MobDefs.MAX_SHADOW_LEVEL) {
            Progression.say(p, "That shadow is already at its maximum level.");
            return;
        }
        int cost = upgradeCost(lvl);
        int have = 0;
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s.getItem() == ModItems.MAGIC_STONE) have += s.getCount();
        }
        if (have < cost) {
            Progression.say(p, "§cNeed " + cost + " Magic Stones §f(you have " + have + ").");
            return;
        }
        int left = cost;
        for (int i = 0; i < p.inventory.getSizeInventory() && left > 0; i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s.getItem() == ModItems.MAGIC_STONE) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        d.setShadowLevel(id, lvl + 1);
        for (ShadowEntity s : findShadows(p)) if (s.getShadowId().equals(id)) s.setShadowLevel(lvl + 1);
        Progression.say(p, "§d" + MobDefs.SHADOWS.get(id).name.split(",")[0] + "§f is now level §a" + (lvl + 1) + "§f.");
        Quests.advance(p, Quests.Type.UPGRADE, null, 1);
        ModNetwork.sync(p);
    }

    private ShadowManager() {}
}
