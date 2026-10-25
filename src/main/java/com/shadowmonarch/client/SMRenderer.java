package com.shadowmonarch.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.shadowmonarch.MobDefs;
import com.shadowmonarch.ShadowMonarch;
import com.shadowmonarch.entity.IVariant;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** One renderer for all mod mobs: texture file = variant id, size = MobDefs scale. */
public class SMRenderer<T extends MobEntity & IVariant, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SMRenderer(EntityRendererManager manager, M model, boolean heldItems) {
        super(manager, model, 0.6F);
        if (heldItems) this.addLayer(new HeldItemLayer(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        String id = entity.variantId();
        ResourceLocation rl = TEX.get(id);
        if (rl == null) {
            rl = new ResourceLocation(ShadowMonarch.MODID, "textures/entity/" + id + ".png");
            TEX.put(id, rl);
        }
        return rl;
    }

    @Override
    protected void preRenderCallback(T entity, float partialTickTime) {
        MobDefs.Def d = MobDefs.get(entity.variantId());
        float s = d == null ? 1.0F : d.scale;
        if (entity.variantId().startsWith("shadow_")) s *= 0.95F;
        GlStateManager.scalef(s, s, s);
    }
}
