package com.shadowmonarch.client;

import com.shadowmonarch.entity.*;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.QuadrupedModel;
import net.minecraft.client.renderer.entity.model.SpiderModel;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static KeyBinding SUMMON;
    public static KeyBinding MENU;

    public static void init() {
        SUMMON = new KeyBinding("key.shadowmonarch.summon", GLFW.GLFW_KEY_K, "key.categories.shadowmonarch");
        MENU = new KeyBinding("key.shadowmonarch.army", GLFW.GLFW_KEY_L, "key.categories.shadowmonarch");
        ClientRegistry.registerKeyBinding(SUMMON);
        ClientRegistry.registerKeyBinding(MENU);

        RenderingRegistry.registerEntityRenderingHandler(SMBiped.class,
                m -> new SMRenderer<SMBiped, BipedModel<SMBiped>>(m, new BipedModel<SMBiped>(), true));
        RenderingRegistry.registerEntityRenderingHandler(ShadowBiped.class,
                m -> new SMRenderer<ShadowBiped, BipedModel<ShadowBiped>>(m, new BipedModel<ShadowBiped>(), true));
        RenderingRegistry.registerEntityRenderingHandler(SMQuad.class,
                m -> new SMRenderer<SMQuad, QuadrupedModel<SMQuad>>(m, new QuadrupedModel<SMQuad>(6, 0.0F), false));
        RenderingRegistry.registerEntityRenderingHandler(ShadowQuad.class,
                m -> new SMRenderer<ShadowQuad, QuadrupedModel<ShadowQuad>>(m, new QuadrupedModel<ShadowQuad>(6, 0.0F), false));
        RenderingRegistry.registerEntityRenderingHandler(SMInsect.class,
                m -> new SMRenderer<SMInsect, SpiderModel<SMInsect>>(m, new SpiderModel<SMInsect>(), false));
        RenderingRegistry.registerEntityRenderingHandler(ShadowInsect.class,
                m -> new SMRenderer<ShadowInsect, SpiderModel<ShadowInsect>>(m, new SpiderModel<ShadowInsect>(), false));

        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    private ClientSetup() {}
}
