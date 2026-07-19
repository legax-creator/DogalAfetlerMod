package com.oyuncu.dogalafetler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = DogalAfetler.MODID)
public class DisasterManager {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    
    // Afet durum kontrolü
    private static boolean isHeavyRainActive = false;
    private static int disasterTimer = 0;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Sadece sunucu tarafında ve ana dünyada (Overworld) çalışmasını sağlıyoruz
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && event.level instanceof ServerLevel level) {
            
            // Aktif afet süresini azalt
            if (disasterTimer > 0) {
                disasterTimer--;
                applyDisasterEffects(level);
            } else if (isHeavyRainActive) {
                isHeavyRainActive = false; // Süre bittiyse afeti sonlandır
            }

            // Her 30 saniyede bir (600 tick) rastgele afet şansını kontrol et
            tickCounter++;
            if (tickCounter >= 600) {
                tickCounter = 0;
                tryStartDisaster(level);
            }
        }
    }

    private static void tryStartDisaster(ServerLevel level) {
        if (disasterTimer > 0) return; // Zaten aktif bir afet varsa yenisini başlatma

        int chance = RANDOM.nextInt(100); // 0-99 arası sayı

        if (chance < 20) { // %20 şansla Sağanak Yağmur başlasın
            isHeavyRainActive = true;
            disasterTimer = 2400; // Afet 2 dakika sürsün (2 * 60 saniye * 20 tick)
            
            // Dünyada fırtınayı başlat
            level.setWeatherParameters(0, 2400, true, true);
            
            // Oyunculara chat üzerinden uyarı gönder
            broadcastMessage(level, "§c[UYARI] Şiddetli fırtına ve sağanak yağmur başlıyor! Sığınacak bir yer bulun!");
        }
    }

    private static void applyDisasterEffects(ServerLevel level) {
        if (isHeavyRainActive && level.isRaining()) {
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();

                // 1. MEKANİK: Oyuncunun yakınına yıldırımlar düşürme (%2 şans)
                if (RANDOM.nextInt(50) == 0) {
                    int offsetX = RANDOM.nextInt(31) - 15; // -15 ile +15 blok arası mesafe
                    int offsetZ = RANDOM.nextInt(31) - 15;
                    
                    // Gökten düşeceği için o koordinattaki en yüksek bloğu bul
                    BlockPos strikePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, playerPos.east(offsetX).south(offsetZ));
                    
                    if (level.canSeeSky(strikePos)) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightning != null) {
                            lightning.moveTo(Vec3.atBottomCenterOf(strikePos));
                            level.addFreshEntity(lightning);
                        }
                    }
                }

                // 2. MEKANİK: Küçük çukurların suyla dolması (%10 şans)
                if (RANDOM.nextInt(10) == 0) {
                    int offsetX = RANDOM.nextInt(11) - 5; // -5 ile +5 arası dar bir alan
                    int offsetZ = RANDOM.nextInt(11) - 5;
                    BlockPos targetPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, playerPos.east(offsetX).south(offsetZ));

                    // Bulduğumuz en üst blok hava ise (yani çukur boşluğuysa) ve üstü açıksa
                    if (level.getBlockState(targetPos).isAir() && level.canSeeSky(targetPos)) {
                        BlockPos belowPos = targetPos.below();
                        // Altındaki blok hava veya su değilse (sağlam bir zeminse) oraya su koy
                        if (!level.getBlockState(belowPos).isAir() && level.getBlockState(belowPos).getBlock() != Blocks.WATER) {
                            level.setBlockAndUpdate(targetPos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void broadcastMessage(ServerLevel level, String text) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
        }
    }
}
