package com.oyuncu.dogalafetler.init;

import com.oyuncu.dogalafetler.block.CementBlock;
import com.oyuncu.dogalafetler.block.ReinforcedIronBarBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "dogalafetler");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "dogalafetler");

    // Blokların kendileri
    public static final RegistryObject<Block> REINFORCED_IRON_BAR = BLOCKS.register("reinforced_iron_bar", ReinforcedIronBarBlock::new);
    public static final RegistryObject<Block> CEMENT = BLOCKS.register("cement", CementBlock::new);

    // Blokların envanterde eşya (Item) olarak görünebilmesi için kayıtları
    public static final RegistryObject<Item> REINFORCED_IRON_BAR_ITEM = ITEMS.register("reinforced_iron_bar", 
            () -> new BlockItem(REINFORCED_IRON_BAR.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> CEMENT_ITEM = ITEMS.register("cement", 
            () -> new BlockItem(CEMENT.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
