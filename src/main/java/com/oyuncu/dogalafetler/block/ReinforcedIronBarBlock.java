package com.oyuncu.dogalafetler.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ReinforcedIronBarBlock extends Block {

    public ReinforcedIronBarBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)
                .noOcclusion()); // Blok saydam/ızgara gibi görünebilsin diye
    }

    // Oyuncuların ve mobların bloğun içinden minder gibi geçebilmesini sağlar
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
