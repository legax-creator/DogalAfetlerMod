package com.oyuncu.dogalafetler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = DogalAfetler.MODID)
public class DisasterManager {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static int disasterTimer = 0;

    // Afet Durum Belirteçleri
    // 0:Yok, 1:Yağmur, 2:Hortum, 3:Tsunami, 4:Kar, 5:Kum, 6:Deprem, 7:Girdap, 8:Sel
    private static int activeDisaster = 0; 
    
    // Afet Koordinatları/Verileri
    private static BlockPos disasterCenter = BlockPos.ZERO;
    private static final Map<UUID, Integer> customTimerMap = new HashMap<>();
    private static final List<BlockPos> floodPlacedWaters = new ArrayList<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && event.level instanceof ServerLevel level) {
            
            // 1. ZAMANLAYICI VE AKTİF AFET YÖNETİMİ
            if (disasterTimer > 0) {
                disasterTimer--;
                runActiveDisasterLogic(level);
            } else if (activeDisaster != 0) {
                int previousDisaster = activeDisaster;
                
                // Sel bittiyse suları temizle
                if (activeDisaster == 8) {
                    cleanupFlood(level);
                }
                
                activeDisaster = 0;
                customTimerMap.clear();

                // TSUNAMİDEN SONRA OTOMATİK SEL BAŞLATMA
                if (previousDisaster == 3) {
                    forceStartDisaster(level, 8, 1800, "§4[TEHLİKE] Tsunami dalgaları çekildi ama geriye büyük bir SEL BASKINI kaldı! Yükseklere kaçın!");
                }
            }

            // 2. RASTGELE AFET TETİKLEME (Her 45 saniyede bir - 900 Tick)
            tickCounter++;
            if (tickCounter >= 900) {
                tickCounter = 0;
                tryTriggerRandomDisaster(level);
            }
        }
    }

    public static void forceStartDisaster(ServerLevel level, int disasterId, int durationInTicks, String alertMessage) {
        if (!level.players().isEmpty()) {
            if (activeDisaster == 8) cleanupFlood(level); // Eski sel varsa temizle
            
            ServerPlayer randomPlayer = level.players().get(RANDOM.nextInt(level.players().size()));
            disasterCenter = randomPlayer.blockPosition();
            activeDisaster = disasterId;
            disasterTimer = durationInTicks;
            
            if (disasterId == 1) level.setWeatherParameters(0, durationInTicks, true, true);
            if (disasterId == 4) level.setWeatherParameters(0, durationInTicks, true, false);
            
            broadcastMessage(level, alertMessage);
        }
    }

    private static void tryTriggerRandomDisaster(ServerLevel level) {
        if (disasterTimer > 0 || level.players().isEmpty()) return;

        int dice = RANDOM.nextInt(100);

        if (dice < 11) { // %11 Sağanak Yağmur
            forceStartDisaster(level, 1, 2400, "§c[UYARI] Şiddetli sağanak yağmur fırtınası başladı! Yıldırımlara dikkat edin!");
        } 
        else if (dice >= 11 && dice < 22) { // %11 Hortum
            forceStartDisaster(level, 2, 1200, "§4[TEHLİKE] Gökyüzü kararıyor... Dev bir HORTUM yaklaşıyor!");
        } 
        else if (dice >= 22 && dice < 33) { // %11 Tsunami
            forceStartDisaster(level, 3, 1600, "§4[DEHŞET] Denizler kabarıyor, dev TSUNAMİ dalgaları geliyor!");
        }
        else if (dice >= 33 && dice < 44) { // %11 Kar Fırtınası
            forceStartDisaster(level, 4, 2000, "§b[UYARI] Sıcaklık hızla düşüyor, şiddetli Kar Fırtınası kapıda!");
        }
        else if (dice >= 44 && dice < 55) { // %11 Kum Fırtınası
            forceStartDisaster(level, 5, 1800, "§6[UYARI] Çöllerden yükselen dev bir Kum Fırtınası yaklaşıyor!");
        }
        else if (dice >= 55 && dice < 66) { // %11 Deprem
            forceStartDisaster(level, 6, 600, "§4[DEHŞET] Yeryüzü sarsılıyor! DEPREM oluyor!");
        }
        else if (dice >= 66 && dice < 77) { // %11 Girdap
            forceStartDisaster(level, 7, 2400, "§9[TEHLİKE] Okyanusta devasa bir GİRDAP oluştu!");
        }
        else if (dice >= 77 && dice < 88) { // %11 Sel
            forceStartDisaster(level, 8, 1800, "§4[TEHLİKE] Şiddetli yağışlar yüzünden SEL BASKINI başlıyor! Yükseklere kaçın!");
        }
    }

    private static void runActiveDisasterLogic(ServerLevel level) {
        switch (activeDisaster) {
            case 1 -> runHeavyRain(level);
            case 2 -> runTornado(level);
            case 3 -> runTsunamiLogic(level);
            case 4 -> runBlizzard(level);
            case 5 -> runSandstorm(level);
            case 6 -> runEarthquake(level);
            case 7 -> runWhirlpool(level);
            case 8 -> runFloodLogic(level);
        }
    }

    // ================= AFET KODLARI =================

    // 1. SAĞANAK YAĞMUR
    private static void runHeavyRain(ServerLevel level) {
        // YAĞMUR YAĞARKEN RASTGELE SEL TETİKLENME ŞANSI (%0.1 her tick)
        if (RANDOM.nextInt(1000) == 0 && disasterTimer > 400) {
            activeDisaster = 8;
            disasterTimer = 1800;
            broadcastMessage(level, "§4[TEHLİKE] Sağanak yağış nehirleri taşırdı! SEL BASKINI BAŞLIYOR!");
            return;
        }

        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            if (RANDOM.nextInt(40) == 0) {
                BlockPos strikePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.east(RANDOM.nextInt(31)-15).south(RANDOM.nextInt(31)-15));
                if (level.canSeeSky(strikePos)) {
                    LightningBolt lb = EntityType.LIGHTNING_BOLT.create(level);
                    if (lb != null) { lb.moveTo(Vec3.atBottomCenterOf(strikePos)); level.addFreshEntity(lb); }
                }
            }
            if (RANDOM.nextInt(10) == 0) {
                BlockPos targetPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.east(RANDOM.nextInt(11)-5).south(RANDOM.nextInt(11)-5));
                if (level.getBlockState(targetPos).isAir() && level.canSeeSky(targetPos)) {
                    BlockPos below = targetPos.below();
                    if (!level.getBlockState(below).isAir() && level.getBlockState(below).getBlock() != Blocks.WATER) {
                        level.setBlockAndUpdate(targetPos, Blocks.WATER.defaultBlockState());
                    }
                }
            }
        }
    }

    // 2. HORTUM
    private static void runTornado(ServerLevel level) {
        disasterCenter = disasterCenter.east(RANDOM.nextInt(3)-1).south(RANDOM.nextInt(3)-1);
        int radius = 15;
        AABB area = new AABB(disasterCenter).inflate(radius, 20, radius);
        List<Entity> entities = level.getEntities((Entity)null, area, e -> e instanceof LivingEntity);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 direction = Vec3.atCenterOf(disasterCenter).subtract(living.position());
                living.setDeltaMovement(direction.normalize().scale(0.3).add(0, 0.4, 0));
                living.hurtMarked = true;
                if (living instanceof ServerPlayer player) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            BlockPos breakPos = disasterCenter.east(RANDOM.nextInt(radius*2)-radius).south(RANDOM.nextInt(radius*2)-radius).above(RANDOM.nextInt(10));
            if (!level.getBlockState(breakPos).isAir() && level.getBlockState(breakPos).getDestroySpeed(level, breakPos) >= 0) {
                level.destroyBlock(breakPos, false);
            }
        }
    }

    // 3. TSUNAMİ
    private static void runTsunamiLogic(ServerLevel level) {
        // Tsunami Dalgası Çarpma Anı (Afet başladıktan 5 saniye sonra - 1500 Tick kaldığında)
        if (disasterTimer == 1500) {
            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().getY() < 75) {
                    if (player.getArmorValue() == 0) { // Zırhsız kişilere 5 kalp hasar
                        player.hurt(level.damageSources().generic(), 10.0F);
                        broadcastMessage(level, "§4[DEHŞET] " + player.getName().getString() + " tsunami dalgasına zırhsız yakalandı ve ağır yaralandı!");
                    }
                }
            }
        }

        for (ServerPlayer player : level.players()) {
            BlockPos pPos = player.blockPosition();
            
            // TSUNAMİDE YUKARI ÇIKMAYI ZORLAŞTIRMA (Aşağı çekim)
            if (player.isInWater()) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, -0.15, 0));
                player.hurtMarked = true;
            }

            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos target = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos.east(x).south(z));
                    if (target.getY() < 75) {
                        if (level.getBlockState(target).isAir()) {
                            level.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
                        }
                        BlockPos below = target.below();
                        if (level.getBlockState(below).getBlock() == Blocks.STONE) {
                            level.setBlockAndUpdate(below, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                        }
                    }
                }
            }
            if (RANDOM.nextInt(50) == 0) {
                EntityType.SQUID.spawn(level, player.blockPosition().above(5), net.minecraft.world.entity.MobSpawnType.EVENT);
            }
        }
    }

    // 4. ÇIĞ MEKANİĞİ
    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player && event.getDistance() >= 3) {
            checkAndTriggerAvalanche(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getPlayer() instanceof ServerPlayer player) {
            checkAndTriggerAvalanche(player);
        }
    }

    private static void checkAndTriggerAvalanche(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        if (level.getBiome(pos).is(Biomes.SNOWY_TAIGA) || level.getBiome(pos).is(Biomes.JAGGED_PEAKS) || level.getBiome(pos).is(Biomes.FROZEN_PEAKS)) {
            if (pos.getY() > 90 && RANDOM.nextInt(100) < 25) {
                broadcastMessage(level, "§b[TEHLİKE] Dağda kar kütleleri kırıldı! ÇIĞ DÜŞÜYOR!");
                for (int i = 0; i < 15; i++) {
                    BlockPos spawnPos = pos.above(10).east(RANDOM.nextInt(9)-4).south(RANDOM.nextInt(9)-4);
                    FallingBlockEntity.fall(level, spawnPos, Blocks.SNOW_BLOCK.defaultBlockState());
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                player.hurt(level.damageSources().freeze(), 4.0F);
            }
        }
    }

    // 5. KAR FIRTINASI
    private static void runBlizzard(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            if (level.canSeeSky(pos)) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                player.setTicksFrozen(player.getTicksFrozen() + 15);
                if (RANDOM.nextInt(10) == 0) {
                    BlockPos snowPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.east(RANDOM.nextInt(7)-3).south(RANDOM.nextInt(7)-3));
                    if (level.getBlockState(snowPos).getBlock() == Blocks.SNOW) {
                        level.setBlockAndUpdate(snowPos, Blocks.SNOW_BLOCK.defaultBlockState());
                    } else if (level.getBlockState(snowPos).isAir()) {
                        level.setBlockAndUpdate(snowPos, Blocks.SNOW.defaultBlockState());
                    }
                }
            }
            if (level.getBlockState(pos.above()).getBlock() == Blocks.SNOW_BLOCK || level.getBlockState(pos).getBlock() == Blocks.SNOW_BLOCK) {
                player.hurt(level.damageSources().inWall(), 2.0F);
            }
        }
    }

    // 6. KUM FIRTINASI
    private static void runSandstorm(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            if (level.getBiome(pos).is(Biomes.DESERT)) {
                if (level.canSeeSky(pos)) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                    UUID id = player.getUUID();
                    int time = customTimerMap.getOrDefault(id, 0) + 1;
                    customTimerMap.put(id, time);
                    if (time >= 300) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                        player.setSecondsOnFire(3);
                    }
                } else {
                    UUID id = player.getUUID();
                    if (customTimerMap.containsKey(id)) customTimerMap.put(id, Math.max(0, customTimerMap.get(id) - 2));
                }
                if (RANDOM.nextInt(15) == 0) {
                    BlockPos sandPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.east(RANDOM.nextInt(11)-5).south(RANDOM.nextInt(11)-5));
                    if (level.getBlockState(sandPos).isAir() && !level.getBlockState(sandPos.below()).isAir()) {
                        level.setBlockAndUpdate(sandPos, Blocks.SAND.defaultBlockState());
                    }
                }
            }
        }
    }

    // 7. DEPREM
    private static void runEarthquake(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false));
            if (RANDOM.nextInt(10) == 0) {
                BlockPos pPos = player.blockPosition();
                BlockPos crackPos = pPos.east(RANDOM.nextInt(13)-6).south(RANDOM.nextInt(13)-6);
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, crackPos).below();
                if (!level.getBlockState(surface).isAir() && surface.getY() > 50) {
                    FallingBlockEntity.fall(level, surface.above(15), level.getBlockState(surface));
                    level.setBlockAndUpdate(surface, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    // 8. GİRDAP
    private static void runWhirlpool(ServerLevel level) {
        if (level.getGameTime() % 4 == 0) disasterCenter = disasterCenter.east(RANDOM.nextInt(3)-1).south(RANDOM.nextInt(3)-1);
        int r = 30;
        AABB waterArea = new AABB(disasterCenter).inflate(r, 15, r);
        List<Entity> aquaticEntities = level.getEntities((Entity)null, waterArea, e -> e instanceof LivingEntity);
        for (Entity entity : aquaticEntities) {
            if (entity instanceof LivingEntity living && living.isInWater()) {
                Vec3 center = Vec3.atCenterOf(disasterCenter);
                Vec3 pullDirection = center.subtract(living.position());
                living.setDeltaMovement(pullDirection.normalize().scale(0.25).add(0, -0.15, 0));
                living.hurtMarked = true;
                living.setAirSupply(Math.max(0, living.getAirSupply() - 4));
                if (level.getGameTime() % 20 == 0) living.hurt(level.damageSources().drown(), 1.5F);
            }
        }
    }

    // 9. SEL MEKANİĞİ
    private static void runFloodLogic(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos pPos = player.blockPosition();
            AABB floodArea = new AABB(pPos).inflate(20, 12, 20);

            // A. Su Yükseltme ve Toprağı Çamura Çevirme (Her saniye)
            if (level.getGameTime() % 20 == 0) {
                int currentFloodHeight = pPos.getY() + (RANDOM.nextInt(2)); 
                
                for (int x = -10; x <= 10; x++) {
                    for (int z = -10; z <= 10; z++) {
                        BlockPos target = new BlockPos(pPos.getX() + x, currentFloodHeight, pPos.getZ() + z);
                        if (level.getBlockState(target).isAir()) {
                            level.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
                            floodPlacedWaters.add(target);
                        }

                        // Toprak ve çimenleri çamura çevir
                        BlockPos ground = target.below();
                        BlockState state = level.getBlockState(ground);
                        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlockAndUpdate(ground, Blocks.MUD.defaultBlockState());
                        }
                    }
                }
            }

            // B. Hızlı Akıntı Etkisi & Yukarı Çıkmayı Zorlaştırma (Aşağı Çekim)
            List<LivingEntity> entitiesInWater = level.getEntitiesOfClass(LivingEntity.class, floodArea, LivingEntity::isInWater);
            for (LivingEntity living : entitiesInWater) {
                // Rastgele X ve Z itmesi (Hızlı ve düzensiz akıntı hissi)
                double streamX = (RANDOM.nextDouble() - 0.5) * 0.25;
                double streamZ = (RANDOM.nextDouble() - 0.5) * 0.25;
                // Aşağı doğru güçlü çekim (Yukarı çıkmayı zorlaştırır)
                living.setDeltaMovement(living.getDeltaMovement().add(streamX, -0.18, streamZ));
                living.hurtMarked = true;
            }

            // C. Tekneleri Batırma Mekaniği
            List<Boat> boats = level.getEntitiesOfClass(Boat.class, floodArea);
            for (Boat boat : boats) {
                // Tekneleri suyun dibine doğru sertçe batırır
                boat.setDeltaMovement(boat.getDeltaMovement().add(0, -0.35, 0));
                boat.hurtMarked = true;
            }
        }
    }

    private static void cleanupFlood(ServerLevel level) {
        for (BlockPos pos : floodPlacedWaters) {
            if (level.getBlockState(pos).getBlock() == Blocks.WATER) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        floodPlacedWaters.clear();
    }

    private static void broadcastMessage(ServerLevel level, String text) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
