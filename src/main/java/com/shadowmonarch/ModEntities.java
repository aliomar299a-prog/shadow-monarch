package com.shadowmonarch;

import com.shadowmonarch.entity.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All EntityTypes are built in the static block (ModItems needs them for the spawn eggs, and Items register first).
 */
@Mod.EventBusSubscriber(modid = ShadowMonarch.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final Map<String, EntityType<SMMonster>> MOBS = new LinkedHashMap<>();
    public static final Map<String, EntityType<ShadowEntity>> SHADOWS = new LinkedHashMap<>();

    static {
        for (MobDefs.Def d : MobDefs.MOBS.values()) {
            EntityType.IFactory<SMMonster> f;
            switch (d.body) {
                case QUAD: f = SMQuad::new; break;
                case INSECT: f = SMInsect::new; break;
                default: f = SMBiped::new; break;
            }
            EntityType<SMMonster> t = EntityType.Builder.create(f, EntityClassification.MONSTER)
                    .size(d.width, d.height).build(ShadowMonarch.MODID + ":" + d.id);
            t.setRegistryName(ShadowMonarch.MODID, d.id);
            MOBS.put(d.id, t);
        }
        for (Map.Entry<String, MobDefs.Def> en : MobDefs.SHADOWS.entrySet()) {
            MobDefs.Def d = en.getValue();
            EntityType.IFactory<ShadowEntity> f;
            switch (d.body) {
                case QUAD: f = ShadowQuad::new; break;
                case INSECT: f = ShadowInsect::new; break;
                default: f = ShadowBiped::new; break;
            }
            String name = "shadow_" + en.getKey();
            EntityType<ShadowEntity> t = EntityType.Builder.create(f, EntityClassification.CREATURE)
                    .size(d.width * 0.9f, d.height * 0.9f).build(ShadowMonarch.MODID + ":" + name);
            t.setRegistryName(ShadowMonarch.MODID, name);
            SHADOWS.put(en.getKey(), t);
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> e) {
        for (EntityType<SMMonster> t : MOBS.values()) e.getRegistry().register(t);
        for (EntityType<ShadowEntity> t : SHADOWS.values()) e.getRegistry().register(t);
    }

    private ModEntities() {}
}
