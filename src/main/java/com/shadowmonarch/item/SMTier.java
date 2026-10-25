package com.shadowmonarch.item;

import net.minecraft.item.IItemTier;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;

public class SMTier implements IItemTier {
    private final int uses;

    public SMTier(int uses) { this.uses = uses; }

    @Override public int getMaxUses() { return uses; }
    @Override public float getEfficiency() { return 8.0F; }
    @Override public float getAttackDamage() { return 0.0F; }
    @Override public int getHarvestLevel() { return 3; }
    @Override public int getEnchantability() { return 20; }
    @Override public Ingredient getRepairMaterial() { return Ingredient.fromItems(Items.DIAMOND); }
}
