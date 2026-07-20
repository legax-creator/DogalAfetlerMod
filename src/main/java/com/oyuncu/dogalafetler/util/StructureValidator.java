package com.oyuncu.dogalafetler.util;

import com.oyuncu.dogalafetler.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StructureValidator {

    /**
     * Verilen bir koordinattaki bloğun afete dayanıklı (betonarme) olup olmadığını kontrol eder.
     * Kural: Bloğun komşu yönlerinde (Sağ, Sol, Ön, Arka, Üst, Alt) en az 3 adet İnşaat Demiri bulunmalı
     * ve bu yapının içinde en az 1 adet Çimento bloğu temas etmelidir.
     */
    public static boolean isSafeStructure(Level level, BlockPos pos) {
        int ironBarCount = 0;
        boolean hasCement = false;

        // Bloğun etrafındaki 6 yönü de (Kuzey, Güney, Doğu, Batı, Üst, Alt) kontrol ediyoruz
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            // İnşaat demiri mi?
            if (neighborState.is(ModBlocks.REINFORCED_IRON_BAR.get())) {
                ironBarCount++;
            }
            
            // Çimento mu?
            if (neighborState.is(ModBlocks.CEMENT.get())) {
                hasCement = true;
            }

            // Eğer bu komşu blokların da etrafına bakmak istersek derinleşebiliriz, 
            // ama temel kuralımız için doğrudan temas eden 3 demir ve çimento yeterli.
        }

        // Kural kontrolü: En az 3 demir VE çimento varlığı
        return ironBarCount >= 3 && hasCement;
    }
}

