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
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
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
    private static int activeDisaster = 0; // 0:Yok, 1:Yağmur, 2:Hortum, 3:Tsunami, 4:Kar, 5:Kum, 6:Deprem, 7:Girdap
    
    // Afet Koordinatları/Verileri
    private static BlockPos disasterCenter = BlockPos.ZERO;
    private static final Map<UUID, Integer> customTimerMap = new HashMap<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && event.level instanceof ServerLevel level) {
            
            // 1. ZAMANLAYICI VE AKTİF AFET YÖNETİMİ
            if (disasterTimer > 0) {
                disasterTimer--;
                runActiveDisasterLogic(level);
            } else if (activeDisaster != 0) {
                activeDisaster = 0;
                customTimerMap.clear();
            }

            // 2. RASTGELE AFET TETİKLEME (Her 45 saniyede bir - 900 Tick)
            tickCounter++;
            if (tickCounter >= 900) {
                tickCounter = 0;
                tryTriggerRandomDisaster(level);
            }
        }
    }

    private static void tryTriggerRandomDisaster(ServerLevel level) {
        if (disasterTimer > 0 || level.players().isEmpty()) return;

        // Rastgele bir oyuncuyu merkez seç
        ServerPlayer randomPlayer = level.players().get(RANDOM.nextInt(level.players().size()));
        disasterCenter = randomPlayer.blockPosition();

        int dice = RANDOM.nextInt(100);

        if (dice < 12) { // %12 Sağanak Yağmur
            activeDisaster = 1;
            disasterTimer = 2400; // 2 dk
            level.setWeatherParameters(0, 2400, true, true);
            broadcastMessage(level, "§c[UYARI] Şiddetli sağanak yağmur fırtınası başladı! Yıldırımlara dikkat edin!");
        } 
        else if (dice >= 12 && dice < 24) { // %12 Hortum
            activeDisaster = 2;
            disasterTimer = 1200; // 1 dk
            broadcastMessage(level, "§4[TEHLİKE] Gökyüzü kararıyor... Dev bir HORTUM yaklaşıyor!");
        } 
        else if (dice >= 24 && dice < 36) { // %12 Tsunami (Depremsiz tetiklenme)
            startTsunami(level);
        }
        else if (dice >= 36 && dice < 48) { // %12 Kar Fırtınası
            activeDisaster = 4;
            disasterTimer = 2000;
            level.setWeatherParameters(0, 2000, true, false);
            broadcastMessage(level, "§b[UYARI] Sıcaklık hızla düşüyor, şiddetli Kar Fırtınası kapıda!");
        }
        else if (dice >= 48 && dice < 60) { // %12 Kum Fırtınası
            activeDisaster = 5;
            disasterTimer = 1800; // 1.5 dk
            broadcastMessage(level, "§6[UYARI] Çöllerden yükselen dev bir Kum Fırtınası yaklaşıyor!");
        }
        else if (dice >= 60 && dice < 72) { // %12 Deprem
            activeDisaster = 6;
            disasterTimer = 600; // 30 sn
            broadcastMessage(level, "§4[DEHŞET] Yeryüzü sarsılıyor! DEPREM oluyor!");
            
            // %40 Deprem sonrası Tsunami şansı
            if (RANDOM.nextInt(100) < 40) {
                disasterTimer += 1200; // Süreyi tsunami için uzat
                activeDisaster = 3; // Deprem biter bitmez Tsunami devreye girsin
            }
        }
        else if (dice >= 72 && dice < 84) { // %12 Girdap
            activeDisaster = 7;
            disasterTimer = 2400; // 2 dk
            broadcastMessage(level, "§9[TEHLİKE] Okyanusta devasa bir GİRDAP oluştu!");
        }
    }

    private static void startTsunami(ServerLevel level) {
        activeDisaster = 3;
        disasterTimer = 1600;
        broadcastMessage(level, "§4[DEHŞET] Denizler kabarıyor, dev TSUNAMİ dalgaları geliyor!");
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
        }
    }

    // ================= AFET KODLARI =================

    // 1. SAĞANAK YAĞMUR
    private static void runHeavyRain(ServerLevel level) {
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
        // Hortumu her tick rastgele 1-2 blok hareket ettir
        disasterCenter = disasterCenter.east(RANDOM.nextInt(3)-1).south(RANDOM.nextInt(3)-1);
        int radius = 15;
        
        AABB area = new AABB(disasterCenter).inflate(radius, 20, radius);
        List<Entity> entities = level.getEntities((Entity)null, area, e -> e instanceof LivingEntity);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 direction = Vec3.atCenterOf(disasterCenter).subtract(living.position());
                living.setDeltaMovement(direction.normalize().scale(0.3).add(0, 0.4, 0)); // Merkeze ve yukarı çek
                living.hurtMarked = true;

                if (living instanceof ServerPlayer player) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0)); // 3 sn bulantı
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));  // Görüş azalması
                }
            }
        }

        // Blok çekme mantığı
        for (int i = 0; i < 5; i++) {
            BlockPos breakPos = disasterCenter.east(RANDOM.nextInt(radius*2)-radius).south(RANDOM.nextInt(radius*2)-radius).above(RANDOM.nextInt(10));
            if (!level.getBlockState(breakPos).isAir() && level.getBlockState(breakPos).getDestroySpeed(level, breakPos) >= 0) {
                level.destroyBlock(breakPos, false);
            }
        }
    }

    // 3. TSUNAMİ
    private static void runTsunamiLogic(ServerLevel level) {
        // Dalgaları oyuncuların etrafına doğru yükselt
        for (ServerPlayer player : level.players()) {
            BlockPos pPos = player.blockPosition();
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos target = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos.east(x).south(z));
                    if (target.getY() < 75) { // Deniz seviyesinin biraz üstüne kadar su bassın
                        if (level.getBlockState(target).isAir()) {
                            level.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
                            // Etraftaki zayıf blokları kır
                            BlockPos wallPos = target.east().south();
                            if(level.getBlockState(wallPos).getBlock() == Blocks.GRASS || level.getBlockState(wallPos).getBlock() == Blocks.POPPY) {
                                level.destroyBlock(wallPos, false);
                            }
                        }
                        // Taşları yosunlu taşa çevir
                        BlockPos below = target.below();
                        if (level.getBlockState(below).getBlock() == Blocks.STONE) {
                            level.setBlockAndUpdate(below, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                            
                    }
                }
            }
            // Deniz canlılarını karaya fırlat
            if (RANDOM.nextInt(50) == 0) {
                EntityType.SQUID.spawn(level, player.blockPosition().above(5), net.minecraft.world.entity.MobSpawnType.EVENT);
            }
        }
    }

    // 4. ÇIĞ MEKANİĞİ (Tetiklenme Eventleri)
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
            if (pos.getY() > 90 && RANDOM.nextInt(100) < 25) { // %25 çığ ihtimali
                broadcastMessage(level, "§b[TEHLİKE] Dağda kar kütleleri kırıldı! ÇIĞ DÜŞÜYOR!");
                for (int i = 0; i < 15; i++) {
                    BlockPos spawnPos = pos.above(10).east(RANDOM.nextInt(9)-4).south(RANDOM.nextInt(9)-4);
                    FallingBlockEntity.fall(level, spawnPos, Blocks.SNOW_BLOCK.defaultBlockState());
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                player.hurt(level.damageSources().freeze(), 4.0F); // Donma hasarı
            }
        }
    }

    // 5. KAR FIRTINASI
    private static void runBlizzard(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            if (level.canSeeSky(pos)) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                player.setTicksFrozen(player.getTicksFrozen() + 15); // Hızlıca donmaya başlar
                
                // Kar yükselmesi
                if (RANDOM.nextInt(10) == 0) {
                    BlockPos snowPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.east(RANDOM.nextInt(7)-3).south(RANDOM.nextInt(7)-3));
                    if (level.getBlockState(snowPos).getBlock() == Blocks.SNOW) {
                        // Kar katmanını tamamen kar bloğuna çevirerek sıkıştır
                        level.setBlockAndUpdate(snowPos, Blocks.SNOW_BLOCK.defaultBlockState());
                    } else if (level.getBlockState(snowPos).isAir()) {
                        level.setBlockAndUpdate(snowPos, Blocks.SNOW.defaultBlockState());
                    }
                }
            }
            // Kar içinde sıkışırsa hasar yesin
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

                    if (time >= 300) { // 15 saniye (300 tick) açıkta kaldıysa
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                        player.setSecondsOnFire(3); // Yanma hasarı
                    }
                } else {
                    // Siperdeyse zamanlayıcı düşsün
                    UUID id = player.getUUID();
                    if (customTimerMap.containsKey(id)) customTimerMap.put(id, Math.max(0, customTimerMap.get(id) - 2));
                }

                // Kum seviyesi artışı
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
            // Ekran sarsıntısı efekti (Bulantı taklidi)
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false));

            // Yarıklar açılması ve blok düşmesi
            if (RANDOM.nextInt(10) == 0) {
                BlockPos pPos = player.blockPosition();
                BlockPos crackPos = pPos.east(RANDOM.nextInt(13)-6).south(RANDOM.nextInt(13)-6);
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, crackPos).below();
                
                if (!level.getBlockState(surface).isAir() && surface.getY() > 50) {
                    // Bloğu kırıp düşen blok entity'sine çeviriyoruz
                    FallingBlockEntity.fall(level, surface.above(15), level.getBlockState(surface));
                    level.setBlockAndUpdate(surface, Blocks.AIR.defaultBlockState()); // Yarık hissi
                }
            }
        }
    }

    // 8. GİRDAP
    private static void runWhirlpool(ServerLevel level) {
        // Girdap merkezini yavaşça kaydır
        if (level.getGameTime() % 4 == 0) {
            disasterCenter = disasterCenter.east(RANDOM.nextInt(3)-1).south(RANDOM.nextInt(3)-1);
        }
        
        int r = 30;
        AABB waterArea = new AABB(disasterCenter).inflate(r, 15, r);
        List<Entity> aquaticEntities = level.getEntities((Entity)null, waterArea, e -> e instanceof LivingEntity);

        for (Entity entity : aquaticEntities) {
            if (entity instanceof LivingEntity living && living.isInWater()) {
                Vec3 center = Vec3.atCenterOf(disasterCenter);
                Vec3 pullDirection = center.subtract(living.position());
                
                // Aşağıya doğru batırma ve merkeze çekme gücü
                living.setDeltaMovement(pullDirection.normalize().scale(0.25).add(0, -0.15, 0));
                living.hurtMarked = true;

                // Boğulmayı tetikle ve yavaşça hasar ver
                living.setAirSupply(Math.max(0, living.getAirSupply() - 4));
                if (level.getGameTime() % 20 == 0) {
                    living.hurt(level.damageSources().drown(), 1.5F);
                }
            }
        }
    }

    private static void broadcastMessage(ServerLevel level, String text) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
