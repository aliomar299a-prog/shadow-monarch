package com.shadowmonarch.item;

import com.shadowmonarch.ShadowMonarch;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

public class SMArmorMaterial implements IArmorMaterial {
    public static final SMArmorMaterial HUNTER = new SMArmorMaterial("hunter", 22, new int[]{2, 5, 6, 2}, 0.0F, 14);
    public static final SMArmorMaterial KNIGHT = new SMArmorMaterial("knight", 33, new int[]{3, 6, 8, 3}, 1.0F, 12);
    public static final SMArmorMaterial MONARCH = new SMArmorMaterial("monarch", 45, new int[]{4, 7, 9, 4}, 3.0F, 20);

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] reduction; // boots, leggings, chestplate, helmet
    private final float toughness;
    private final int enchantability;

    private SMArmorMaterial(String name, int mult, int[] reduction, float toughness, int ench) {
        this.name = name; this.durabilityMultiplier = mult; this.reduction = reduction;
        this.toughness = toughness; this.enchantability = ench;
    }

    @Override public int getDurability(EquipmentSlotType slot) { return BASE_DURABILITY[slot.getIndex()] * durabilityMultiplier; }
    @Override public int getDamageReductionAmount(EquipmentSlotType slot) { return reduction[slot.getIndex()]; }
    @Override public int getEnchantability() { return enchantability; }
    @Override public SoundEvent getSoundEvent() { return SoundEvents.ITEM_ARMOR_EQUIP_IRON; }
    @Override public Ingredient getRepairMaterial() { return Ingredient.fromItems(Items.IRON_INGOT); }
    @Override public String getName() { return ShadowMonarch.MODID + ":" + name; }
    @Override public float getToughness() { return toughness; }
}
