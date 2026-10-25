package com.shadowmonarch.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

/** Daggers / swords from the anime. Total damage = {@code damage} (the player's base 1 is subtracted). */
public class SMWeapon extends SwordItem {
    public enum Fx { NONE, POISON, SLOW, WITHER, FIRE }

    private final Fx fx;

    public SMWeapon(int damage, float speedDelta, int uses, Fx fx, Properties props) {
        super(new SMTier(uses), damage - 1, speedDelta, props);
        this.fx = fx;
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (fx) {
            case POISON: target.addPotionEffect(new EffectInstance(Effects.POISON, 120, 1)); break;
            case SLOW: target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 100, 2)); break;
            case WITHER: target.addPotionEffect(new EffectInstance(Effects.WITHER, 80, 1)); break;
            case FIRE: target.setFire(8); break;
            default: break;
        }
        return super.hitEntity(stack, target, attacker);
    }
}
