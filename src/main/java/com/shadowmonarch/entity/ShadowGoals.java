package com.shadowmonarch.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public final class ShadowGoals {
    /** Follow the owner, teleport to them if too far. */
    public static class Follow extends Goal {
        private final ShadowEntity shadow;
        private final double speed;
        private final float minDist, maxDist;
        private PlayerEntity owner;
        private int timer;

        public Follow(ShadowEntity shadow, double speed, float minDist, float maxDist) {
            this.shadow = shadow; this.speed = speed; this.minDist = minDist; this.maxDist = maxDist;
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            PlayerEntity o = shadow.getOwner();
            if (o == null || o.isSpectator()) return false;
            if (shadow.getDistanceSq(o) < (double) (minDist * minDist)) return false;
            owner = o;
            return true;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return owner != null && owner.isAlive() && shadow.getDistanceSq(owner) > (double) (minDist * minDist);
        }

        @Override
        public void startExecuting() { timer = 0; }

        @Override
        public void resetTask() {
            owner = null;
            shadow.getNavigator().clearPath();
        }

        @Override
        public void tick() {
            shadow.getLookController().setLookPositionWithEntity(owner, 10.0F, (float) shadow.getVerticalFaceSpeed());
            if (--timer <= 0) {
                timer = 10;
                if (shadow.getDistanceSq(owner) >= (double) (maxDist * maxDist)) {
                    shadow.setLocationAndAngles(owner.posX + 1.5, owner.posY, owner.posZ + 1.5,
                            shadow.rotationYaw, shadow.rotationPitch);
                    shadow.getNavigator().clearPath();
                } else {
                    shadow.getNavigator().tryMoveToEntityLiving(owner, speed);
                }
            }
        }
    }

    /**
     * Attack what the owner attacked (this is how animals become targets: only after the owner hit them)
     * or whatever hurt the owner.
     */
    public static class OwnerTarget extends TargetGoal {
        private final ShadowEntity shadow;
        private LivingEntity wanted;

        public OwnerTarget(ShadowEntity shadow) {
            super(shadow, false);
            this.shadow = shadow;
            this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        private boolean valid(PlayerEntity owner, LivingEntity t) {
            return t != null && t.isAlive() && t != shadow && t != owner
                    && !(t instanceof PlayerEntity) && !(t instanceof ShadowEntity);
        }

        @Override
        public boolean shouldExecute() {
            PlayerEntity owner = shadow.getOwner();
            if (owner == null) return false;
            LivingEntity t = owner.getLastAttackedEntity();
            if (valid(owner, t) && owner.ticksExisted - owner.getLastAttackedEntityTime() < 120) {
                wanted = t;
                return true;
            }
            t = owner.getRevengeTarget();
            if (valid(owner, t) && owner.ticksExisted - owner.getRevengeTimer() < 120) {
                wanted = t;
                return true;
            }
            return false;
        }

        @Override
        public void startExecuting() {
            shadow.setAttackTarget(wanted);
            super.startExecuting();
        }
    }

    private ShadowGoals() {}
}
