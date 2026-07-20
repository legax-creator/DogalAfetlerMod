package com.oyuncu.dogalafetler;

import com.oyuncu.dogalafetler.client.WindHudOverlay;
import com.oyuncu.dogalafetler.weather.WeatherSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("dogalafetler")
public class DogalAfetler {

    public DogalAfetler() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Forge event bus yapısını kaydediyoruz
        MinecraftForge.EVENT_BUS.register(this);
    }

    // 1. DÜNYA DÖNGÜSÜ: Rüzgar ve mevsimlerin her gün arka planda güncellenmesi
    @Mod.EventBusSubscriber(modid = "dogalafetler", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            // Sadece sunucu tarafında ve tick sonlandığında rüzgarı günceller
            if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
                WeatherSystem.updateWeather(event.level);
            }
        }
    }

    // 2. HUD KAYDI: Rüzgar göstergesinin Hotbar sağına çizilmesi (Sadece Client/İstemci tarafında çalışır)
    @Mod.EventBusSubscriber(modid = "dogalafetler", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            // Arayüzümüzü en üste katman olarak ekliyoruz
            event.registerAboveAll("wind_hud", WindHudOverlay.HUD_WIND);
        }
    }
}
