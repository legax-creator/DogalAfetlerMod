package com.oyuncu.dogalafetler;

import com.oyuncu.dogalafetler.init.ModBlocks;
import com.oyuncu.dogalafetler.util.StructureValidator;
import com.oyuncu.dogalafetler.weather.WeatherSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DisasterManager {

    // AFET 1: TSUNAMİ MECHANIC
    public static void triggerTsunami(ServerLevel level, BlockPos strikePos) {
        level.players().forEach(p -> p.sendSystemMessage(Component.literal("§c§lUYARI: Dev Tsunami dalgaları kıyıya yaklaşıyor!")));
        
        int radius = 40; // 35-45 blok kalınlığında dalga devasa yapısı
        int height = 15; // Yükseklik

        for (int x = -radius; x <= radius; x++) {
            for (int z = 0; z < 60; z++) { // 50-75 blok ilerleme
                for (int y = 0; y < height; y++) {
                    BlockPos currentPos = strikePos.offset(x, y, z);
                    BlockState state = level.getBlockState(currentPos);

                    // Betonarme temel kontrolü: Güvenliyse YIKMA, değilse su yap
                    if (!StructureValidator.isSafeStructure(level, currentPos)) {
                        if (!state.is(Blocks.BEDROCK) && !state.is(Blocks.AIR)) {
                            level.setBlockAndUpdate(currentPos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    // AFET 2: 3 KADEMELİ DEPREM MECHANIC
    public static void triggerEarthquake(ServerLevel level, BlockPos centerPos, int intensity) {
        // intensity: 1 (Az), 2 (Orta), 3 (Yüksek)
        String msg = intensity == 3 ? "§4§lŞİDDETLİ DEPREM OLUYOR! GÜVENLİ BİR YERE GEÇİN!" : "§eYer sarsılıyor...";
        level.players().forEach(p -> p.sendSystemMessage(Component.literal(msg)));

        int durationTicks = intensity * 400; // Şiddetine göre süre artar (15-60 sn arası)
        int damageRadius = intensity * 15;

        for (int x = -damageRadius; x <= damageRadius; x++) {
            for (int z = -damageRadius; z <= damageRadius; z++) {
                if (level.random.nextInt(100) < (intensity * 20)) {
                    BlockPos surfacePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, centerPos.offset(x, 0, z)).below();
                    
                    // Betonarme değilse yarık aç veya bloğu kır
                    if (!StructureValidator.isSafeStructure(level, surfacePos)) {
                        level.setBlockAndUpdate(surfacePos, Blocks.AIR.defaultBlockState());
                        // Altındaki blokları da mağara göçüğü simülasyonu için sars
                        if (intensity == 3) {
                            level.setBlockAndUpdate(surfacePos.below(), Blocks.GRAVEL.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Oyunculara sarsıntı ve hasar verme efekti
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(centerPos).inflate(damageRadius));
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));
            if (intensity == 3 && player.onGround()) {
                player.hurt(level.damageSources().fall(), 5.0F); // 4-6 kalp arası hasar
            }
        }
    }

    // AFET 3: FİZİKSEL HORTUM MECHANIC (Rüzgar Hızına Bağlı)
    public static void tickTornado(ServerLevel level, BlockPos tornadoPos) {
        // Rüzgar hızı hortum için yeterliyse hortum nesneleri çeker ve fırlatır
        if (WeatherSystem.getWindSpeed() > 65.0f) {
            int pullRadius = 20;
            List<Entity> entities = level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(tornadoPos).inflate(pullRadius));

            for (Entity entity : entities) {
                if (!(entity instanceof ServerPlayer player && player.isCreative())) {
                    // Yukarı ve merkeze doğru fırlatma fiziği
                    Vec3 motion = new Vec3(0, 1.2, 0); // ~20 blok yukarı fırlatma
                    entity.setDeltaMovement(motion);
                    entity.hasImpulse = true;
                }
            }
            // Partikül efektleri ile hortum görüntüsü
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, tornadoPos.getX(), tornadoPos.getY(), tornadoPos.getZ(), 50, 2, 10, 2, 0.1);
        }
    }

    // AFET 4: OKYANUS GİRDABI MECHANIC (YENİ!)
    public static void tickWhirlpool(ServerLevel level, BlockPos whirlpoolPos, int elapsedSeconds) {
        // Sadece su biyomlarında tetiklenir kontrolü çağrıldığı yerde yapılacak
        
        if (elapsedSeconds <= 10) {
            // İlk 10 saniye: Girdap küçük, tehlikesiz. Sadece su partikülü döner.
            level.sendParticles(ParticleTypes.BUBBLE, whirlpoolPos.getX(), whirlpoolPos.getY(), whirlpoolPos.getZ(), 20, 2, 0.5, 2, 0.05);
            return;
        }

        // 10 saniyeden sonra devasa aşamaya geçer (Toplam 2 dakika sürecek)
        int pullRadius = 35;
        List<Entity> entities = level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(whirlpoolPos).inflate(pullRadius));

        for (Entity entity : entities) {
            BlockPos entityPos = entity.blockPosition();
            
            // Merkez ile varlık arasındaki mesafe vektörü
            double dx = whirlpoolPos.getX() - entity.getX();
            double dz = whirlpoolPos.getZ() - entity.getZ();
            
            // Kendi etrafında döndürme (Merkezkaç) ve İçine Çekme (Spiral) fiziği
            double angle = Math.atan2(dz, dx) + 0.2; // Dönme açısı artışı
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            // Merkeze yaklaştıkça çekim gücü artar, tekneler batar veya zorlanır
            double targetX = whirlpoolPos.getX() - Math.cos(angle) * (distance - 0.3);
            double targetZ = whirlpoolPos.getZ() - Math.sin(angle) * (distance - 0.3);
            
            double motionX = (targetX - entity.getX()) * 0.2;
            double motionZ = (targetZ - entity.getZ()) * 0.2;
            double motionY = -0.15; // Aşağı, girdabın dibine doğru çekim

            entity.setDeltaMovement(new Vec3(motionX, motionY, motionZ));
            entity.hasImpulse = true;
        }

        // Görsel dev anafor efekti
        level.sendParticles(ParticleTypes.CURRENT_DOWNPOUR, whirlpoolPos.getX(), whirlpoolPos.getY(), whirlpoolPos.getZ(), 100, 5, 1, 5, 0.2);
    }
}
