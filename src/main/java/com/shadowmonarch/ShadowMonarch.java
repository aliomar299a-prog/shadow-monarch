package com.shadowmonarch;

import com.shadowmonarch.client.ClientSetup;
import com.shadowmonarch.net.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ShadowMonarch.MODID)
public class ShadowMonarch {
    public static final String MODID = "shadowmonarch";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public ShadowMonarch() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(new com.shadowmonarch.event.ServerEvents());
        MinecraftForge.EVENT_BUS.register(new com.shadowmonarch.event.CommandEvents());
    }

    private void setup(final FMLCommonSetupEvent event) {
        ModNetwork.init();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ClientSetup.init();
    }
}
