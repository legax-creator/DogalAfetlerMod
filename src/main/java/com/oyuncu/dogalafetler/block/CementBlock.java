package com.oyuncu.dogalafetler.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class CementBlock extends Block {

    public CementBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.STONE));
    }
}
