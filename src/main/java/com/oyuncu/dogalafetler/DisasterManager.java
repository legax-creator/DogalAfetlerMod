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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DisasterManager {

    // AFET 1: TSUNAMİ
    public static void triggerTsunami(ServerLevel level, BlockPos strikePos) {
        level.players().forEach(p -> p.sendSystemMessage(Component.literal("§c§lUYARI: Dev Tsunami dalgaları kıyıya yaklaşıyor!")));
        
        int radius = 40;
        int height = 15;

        for (int x = -radius; x <= radius; x++) {
            for (int z = 0; z < 60; z++) {
                for (int y = 0; y < height; y++) {
                    BlockPos currentPos = strikePos.offset(x, y, z);
                    BlockState state = level.getBlockState(currentPos);

                    if (!StructureValidator.isSafeStructure(level, currentPos)) {
                        if (!state.is(Blocks.BEDROCK) && !state.is(Blocks.AIR)) {
                            level.setBlockAndUpdate(currentPos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    // AFET 2: DEPREM
    public static void triggerEarthquake(ServerLevel level, BlockPos centerPos, int intensity) {
        String msg = intensity == 3 ? "§4§lŞİDDETLİ DEPREM OLUYOR! GÜVENLİ BİR YERE GEÇİN!" : "§eYer sarsılıyor...";
        level.players().forEach(p -> p.sendSystemMessage(Component.literal(msg)));

        int damageRadius = intensity * 15;

        for (int x = -damageRadius; x <= damageRadius; x++) {
            for (int z = -damageRadius; z <= damageRadius; z++) {
                if (level.random.nextInt(100) < (intensity * 20)) {
                    BlockPos surfacePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, centerPos.offset(x, 0, z)).below();
                    
                    if (!StructureValidator.isSafeStructure(level, surfacePos)) {
                        level.setBlockAndUpdate(surfacePos, Blocks.AIR.defaultBlockState());
                        if (intensity == 3) {
                            level.setBlockAndUpdate(surfacePos.below(), Blocks.GRAVEL.defaultBlockState());
                        }
                    }
                }
            }
        }

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(centerPos).inflate(damageRadius));
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));
            if (intensity == 3 && player.onGround()) {
                player.hurt(level.damageSources().fall(), 5.0F);
            }
        }
    }

    // AFET 3: HORTUM (Görsel Netleştirildi)
    public static void tickTornado(ServerLevel level, BlockPos tornadoPos) {
        if (WeatherSystem.getWindSpeed() > 65.0f) {
            int pullRadius = 20;
            List<Entity> entities = level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(tornadoPos).inflate(pullRadius));

            for (Entity entity : entities) {
                if (!(entity instanceof ServerPlayer player && player.isCreative())) {
                    Vec3 motion = new Vec3(0, 1.2, 0);
                    entity.setDeltaMovement(motion);
                    entity.hasImpulse = true;
                }
            }
            
            // GÖRSEL DÜZELTME: Sunucudan tüm clientlara (override) zorunlu partikül basıyoruz.
            // Yüksekliği spiral şeklinde tarayarak devasa bir hortum sütunu oluşturuyoruz.
            for (int height = 0; height < 25; height += 2) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, 
                        tornadoPos.getX() + (Math.sin(height) * 1.5), 
                        tornadoPos.getY() + height, 
                        tornadoPos.getZ() + (Math.cos(height) * 1.5), 
                        15, 0.5, 0.5, 0.5, 0.02);
                
                // Ekstra bulut partikülleri ile içini dolduruyoruz
                level.sendParticles(ParticleTypes.CLOUD, tornadoPos.getX(), tornadoPos.getY() + height, tornadoPos.getZ(), 5, 1.0, 0.5, 1.0, 0.01);
            }
        }
    }

    // AFET 4: GİRDAP (Görsel Netleştirildi)
    public static void tickWhirlpool(ServerLevel level, BlockPos whirlpoolPos, int elapsedSeconds) {
        if (elapsedSeconds <= 10) {
            // İlk 10 saniye başlangıç efekti (büyük partiküllerle)
            level.sendParticles(ParticleTypes.SPLASH, whirlpoolPos.getX(), whirlpoolPos.getY() + 0.5, whirlpoolPos.getZ(), 30, 1, 0.1, 1, 0.1);
            return;
        }

        int pullRadius = 35;
        List<Entity> entities = level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(whirlpoolPos).inflate(pullRadius));

        for (Entity entity : entities) {
            double dx = whirlpoolPos.getX() - entity.getX();
            double dz = whirlpoolPos.getZ() - entity.getZ();
            
            double angle = Math.atan2(dz, dx) + 0.2;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            double targetX = whirlpoolPos.getX() - Math.cos(angle) * (distance - 0.3);
            double targetZ = whirlpoolPos.getZ() - Math.sin(angle) * (distance - 0.3);
            
            double motionX = (targetX - entity.getX()) * 0.2;
            double motionZ = (targetZ - entity.getZ()) * 0.2;
            double motionY = -0.15;

            entity.setDeltaMovement(new Vec3(motionX, motionY, motionZ));
            entity.hasImpulse = true;
        }

        // GÖRSEL DÜZELTME: Su yüzeyinde dönen dev bir girdap çemberi oluşturuyoruz
        for (int r = 2; r < 8; r += 2) {
            for (double i = 0; i < Math.PI * 2; i += 0.8) {
                double pX = whirlpoolPos.getX() + (Math.cos(i) * r);
                double pZ = whirlpoolPos.getZ() + (Math.sin(i) * r);
                // Kabarcık ve su damlası partiküllerini zorla basıyoruz
                level.sendParticles(ParticleTypes.BUBBLE, pX, whirlpoolPos.getY() + 0.2, pZ, 5, 0.1, 0.1, 0.1, 0.0);
                level.sendParticles(ParticleTypes.POOF, pX, whirlpoolPos.getY() + 0.5, pZ, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    // AFET 5: ÇIĞ VE KAR FIRTINASI (YENİ EKLEME VE GÖRSEL)
    public static void tickBlizzard(ServerLevel level, BlockPos centerPos) {
        // Kar fırtınasının vurduğu alandaki oyuncuları yavaşlat ve görüşü kapat
        int radius = 30;
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(centerPos).inflate(radius));
        
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)); // Görüşü karartır fırtına hissi verir
        }

        // GÖRSEL: Havadan yoğun kar yağıyormuş gibi bembeyaz kar ve poof partikülleri fırlatıyoruz
        for (int i = 0; i < 5; i++) {
            int rx = level.random.nextInt(radius * 2) - radius;
            int rz = level.random.nextInt(radius * 2) - radius;
            BlockPos airPos = centerPos.offset(rx, 15, rz); // Yukarıdan aşağı yağış
            
            level.sendParticles(ParticleTypes.SNOWFLAKE, airPos.getX(), airPos.getY(), airPos.getZ(), 40, 5, 5, 5, 0.1);
        }
    }
}
