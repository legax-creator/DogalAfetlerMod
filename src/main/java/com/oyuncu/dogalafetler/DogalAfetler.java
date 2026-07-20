package com.oyuncu.dogalafetler;

import com.oyuncu.dogalafetler.client.WindHudOverlay;
import com.oyuncu.dogalafetler.init.ModBlocks;
import com.oyuncu.dogalafetler.weather.WeatherSystem;
import net.minecraft.core.BlockPos; // Eksik olan import satırımız buraya eklendi
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DogalAfetler.MODID)
public class DogalAfetler {
    public static final String MODID = "dogalafetler";

    public DogalAfetler() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Blok ve Item kayıtları
        ModBlocks.register(modEventBus);

        // Forge event bus yapısı
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        private static int whirlpoolTicks = 0;
        private static int blizzardTicks = 0;

        @SubscribeEvent
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Rüzgar ve mevsim sistemi döngüsü
                WeatherSystem.updateWeather(serverLevel);

                // Süreli afetlerin oyun içindeki döngüsel tetikleyicileri
                BlockPos spawnPos = serverLevel.getSharedSpawnPos();

                // Rüzgar hızı yüksekse hortumu otomatik tetikle
                if (WeatherSystem.getWindSpeed() > 65.0f) {
                    DisasterManager.tickTornado(serverLevel, spawnPos);
                }

                // Girdap döngüsü (Örnek simülasyon: Sürekli aktif ticklenir, süreye göre evrilir)
                whirlpoolTicks++;
                DisasterManager.tickWhirlpool(serverLevel, spawnPos, whirlpoolTicks / 20);

                // Kar fırtınası döngüsü (Kış aylarında otomatik tetiklenir)
                if (WeatherSystem.getCurrentMonth() == 12 || WeatherSystem.getCurrentMonth() <= 2) {
                    blizzardTicks++;
                    if (blizzardTicks % 20 == 0) { // Her saniye
                        DisasterManager.tickBlizzard(serverLevel, spawnPos);
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("wind_hud", WindHudOverlay.HUD_WIND);
        }
    }
}
