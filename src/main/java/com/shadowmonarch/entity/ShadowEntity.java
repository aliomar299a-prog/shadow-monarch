package com.shadowmonarch.entity;

import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.UUID;
import java.util.function.Predicate;

/** A summoned shadow soldier. Fights monsters and whatever its owner fights; never hurts the owner. */
public abstract class ShadowEntity extends CreatureEntity implements IVariant {
    private UUID ownerId;
    private String shadowId = "igris";
    private int shadowLevel = 1;

    protected ShadowEntity(EntityType<? extends ShadowEntity> type, World world) {
        super(type, world);
        String path = type.getRegistryName() == null ? null : type.getRegistryName().getPath();
        if (path != null && path.startsWith("shadow_")) shadowId = path.substring(7);
    }

    public UUID getOwnerId() { return ownerId; }
    public String getShadowId() { return shadowId; }
    public int getShadowLevel() { return shadowLevel; }

    public PlayerEntity getOwner() {
        return ownerId == null ? null : this.world.getPlayerByUuid(ownerId);
    }

    @Override
    public String variantId() { return "shadow_" + shadowId; }

    /** Called right after creation by the summoning code. */
    public void init(PlayerEntity owner, String id, int level) {
        this.ownerId = owner.getUniqueID();
        this.shadowId = id;
        this.shadowLevel = level;
        applyLevel(true);
        MobDefs.Def d = MobDefs.get(id);
        if (d != null) {
            this.setCustomName(new StringTextComponent("§5" + d.name.split(",")[0] + " §7Lv." + level));
            this.setCustomNameVisible(true);
        }
    }

    public void setShadowLevel(int level) {
        this.shadowLevel = level;
        applyLevel(false);
        MobDefs.Def d = MobDefs.get(shadowId);
        if (d != null) this.setCustomName(new StringTextComponent("§5" + d.name.split(",")[0] + " §7Lv." + level));
    }

    private void applyLevel(boolean fullHeal) {
        MobDefs.Def d = MobDefs.get(shadowId);
        if (d == null) return;
        double factor = 0.3 + 0.12 * (shadowLevel - 1);
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.max(20.0, d.health * factor));
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(Math.max(3.0, d.damage * factor));
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(Math.max(0.32, d.speed));
        if (fullHeal) this.setHealth(this.getMaxHealth());
        if (shadowId.equals("igris")) {
            this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(ModItems.IGRIS_SWORD));
        } else if (shadowId.equals("iron")) {
            this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        if (this.getAttributes().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE) == null) {
            this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        }
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32D);
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override
    protected void registerGoals() {
        Predicate<LivingEntity> hostile = e -> e instanceof IMob && !(e instanceof ShadowEntity);
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new ShadowGoals.Follow(this, 1.2D, 4.0F, 24.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new ShadowGoals.OwnerTarget(this));
        this.targetSelector.addGoal(2, new net.minecraft.entity.ai.goal.NearestAttackableTargetGoal<MobEntity>(
                this, MobEntity.class, 10, true, false, hostile));
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (!this.world.isRemote && this.ticksExisted % 20 == 0) {
            PlayerEntity o = getOwner();
            if (o == null || !o.isAlive() || o.dimension != this.dimension) {
                this.remove();
                return;
            }
            if (this.ticksExisted % 60 == 0) this.heal(1.0F + shadowLevel * 0.3F);
        }
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) { return false; }

    @Override
    public void writeAdditional(CompoundNBT c) {
        super.writeAdditional(c);
        if (ownerId != null) c.putUniqueId("Owner", ownerId);
        c.putString("ShadowId", shadowId);
        c.putInt("ShadowLevel", shadowLevel);
    }

    @Override
    public void readAdditional(CompoundNBT c) {
        super.readAdditional(c);
        if (c.hasUniqueId("Owner")) ownerId = c.getUniqueId("Owner");
        if (c.contains("ShadowId")) shadowId = c.getString("ShadowId");
        shadowLevel = Math.max(1, c.getInt("ShadowLevel"));
        applyLevel(false);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
