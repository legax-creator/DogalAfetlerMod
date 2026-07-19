package com.oyuncu.dogalafetler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DogalAfetler.MODID)
public class DogalAfetler {
    // build.gradle ve mods.toml ile birebir aynı olmalı
    public static final String MODID = "dogalafetler"; 
    public static final Logger LOGGER = LogManager.getLogger();

    public DogalAfetler() {
        // Modun başlangıç kurulum etkinliklerini dinliyoruz
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // İleride yazacağımız afet yöneticisini Forge'un ana etkinlik otobüsüne kaydediyoruz
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Dinamik Dogal Afetler Modu Aktif Edildi!");
    }
}
