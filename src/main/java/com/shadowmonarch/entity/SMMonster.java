package com.shadowmonarch.entity;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.server.ServerBossInfo;
import net.minecraftforge.fml.network.NetworkHooks;

/** Base class of every hostile anime monster. Stats come from {@link MobDefs}. */
public abstract class SMMonster extends MonsterEntity implements IVariant {
    private ServerBossInfo bossInfo;

    protected SMMonster(EntityType<? extends SMMonster> type, World world) {
        super(type, world);
        MobDefs.Def d = def();
        if (d != null) {
            if (d.boss) {
                bossInfo = new ServerBossInfo(new StringTextComponent(d.name), BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
                experienceValue = 50;
            }
            switch (d.id) {
                case "igris": hold(new ItemStack(ModItems.IGRIS_SWORD)); break;
                case "iron": hold(new ItemStack(Items.IRON_SWORD)); break;
                case "goblin_chief": hold(new ItemStack(Items.IRON_AXE)); break;
                case "goblin": hold(new ItemStack(Items.STONE_SWORD)); break;
                default: break;
            }
        }
        this.setHealth(this.getMaxHealth());
    }

    private void hold(ItemStack s) {
        if (s.isEmpty()) return;
        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, s);
        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
    }

    public MobDefs.Def def() {
        return MobDefs.get(getType().getRegistryName().getPath());
    }

    @Override
    public String variantId() {
        return getType().getRegistryName().getPath();
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        MobDefs.Def d = def();
        if (d == null) return;
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(d.health);
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(d.speed);
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(d.damage);
        this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
        this.getAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(d.boss ? 0.6D : 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 0.9D));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.entity.ai.goal.NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new net.minecraft.entity.ai.goal.NearestAttackableTargetGoal<>(this, ShadowEntity.class, true));
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (bossInfo != null) bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void addTrackingPlayer(ServerPlayerEntity player) {
        super.addTrackingPlayer(player);
        if (bossInfo != null) bossInfo.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(ServerPlayerEntity player) {
        super.removeTrackingPlayer(player);
        if (bossInfo != null) bossInfo.removePlayer(player);
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
