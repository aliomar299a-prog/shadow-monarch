package com.shadowmonarch;

import com.shadowmonarch.item.*;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = ShadowMonarch.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModItems {
    public static final ItemGroup TAB = new ItemGroup("shadowmonarch") {
        @Override
        public ItemStack createIcon() { return new ItemStack(MAGIC_STONE != null ? MAGIC_STONE : Items.DIAMOND); }
    };

    public static Item MAGIC_STONE, MANA_CRYSTAL, BOSS_KEY;
    public static final Item[] GATE_KEYS = new Item[6];
    public static Item KNIGHT_KILLER, KASAKA_FANG, BARUKA_DAGGER, DEMON_KING_DAGGER, IGRIS_SWORD, KAMISH_WRATH;

    private static Item.Properties props() { return new Item.Properties().group(TAB); }

    private static <T extends Item> T reg(RegistryEvent.Register<Item> e, String name, T item) {
        item.setRegistryName(ShadowMonarch.MODID, name);
        e.getRegistry().register(item);
        return item;
    }

    private static void armorSet(RegistryEvent.Register<Item> e, String set, IArmorMaterial mat) {
        reg(e, set + "_helmet", new ArmorItem(mat, EquipmentSlotType.HEAD, props()));
        reg(e, set + "_chestplate", new ArmorItem(mat, EquipmentSlotType.CHEST, props()));
        reg(e, set + "_leggings", new ArmorItem(mat, EquipmentSlotType.LEGS, props()));
        reg(e, set + "_boots", new ArmorItem(mat, EquipmentSlotType.FEET, props()));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> e) {
        MAGIC_STONE = reg(e, "magic_stone", new Item(props()));
        MANA_CRYSTAL = reg(e, "mana_crystal", new ManaCrystalItem(props().maxStackSize(16)));
        String[] letters = {"e", "d", "c", "b", "a", "s"};
        for (int i = 0; i < 6; i++) {
            GATE_KEYS[i] = reg(e, "gate_key_" + letters[i], new GateKeyItem(i, props().maxStackSize(16)));
        }
        BOSS_KEY = reg(e, "boss_gate_key", new GateKeyItem(-1, props().maxStackSize(4)));

        //                                                       dmg  speedDelta uses  effect
        KNIGHT_KILLER = reg(e, "knight_killer", new SMWeapon(5, -1.8F, 600, SMWeapon.Fx.NONE, props().maxStackSize(1)));
        KASAKA_FANG = reg(e, "kasaka_fang", new SMWeapon(7, -1.8F, 800, SMWeapon.Fx.POISON, props().maxStackSize(1)));
        BARUKA_DAGGER = reg(e, "baruka_dagger", new SMWeapon(9, -1.7F, 1000, SMWeapon.Fx.SLOW, props().maxStackSize(1)));
        DEMON_KING_DAGGER = reg(e, "demon_king_dagger", new SMWeapon(12, -1.4F, 1500, SMWeapon.Fx.WITHER, props().maxStackSize(1)));
        IGRIS_SWORD = reg(e, "igris_sword", new SMWeapon(16, -2.4F, 2000, SMWeapon.Fx.NONE, props().maxStackSize(1)));
        KAMISH_WRATH = reg(e, "kamish_wrath", new SMWeapon(20, -1.2F, 3000, SMWeapon.Fx.FIRE, props().maxStackSize(1)));

        armorSet(e, "hunter", SMArmorMaterial.HUNTER);
        armorSet(e, "knight", SMArmorMaterial.KNIGHT);
        armorSet(e, "monarch", SMArmorMaterial.MONARCH);

        // spawn eggs for every anime monster (EntityTypes are built statically in ModEntities)
        for (Map.Entry<String, EntityType<com.shadowmonarch.entity.SMMonster>> en : ModEntities.MOBS.entrySet()) {
            MobDefs.Def d = MobDefs.MOBS.get(en.getKey());
            reg(e, en.getKey() + "_spawn_egg", new SpawnEggItem(en.getValue(), d.egg1, d.egg2, props()));
        }
    }
}
