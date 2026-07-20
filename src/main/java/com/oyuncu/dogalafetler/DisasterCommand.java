package com.oyuncu.dogalafetler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DogalAfetler.MODID)
public class DisasterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("afet")
            .requires(source -> source.hasPermission(2)) // OP yetkisi ister
            .then(Commands.argument("tur", StringArgumentType.string())
                .executes(context -> {
                    String tur = StringArgumentType.getString(context, "tur").toLowerCase();
                    ServerLevel level = context.getSource().getLevel();

                    switch (tur) {
                        case "yagmur" -> DisasterManager.forceStartDisaster(level, 1, 2400, "§c[KOMUT] Şiddetli yağmur fırtınası tetiklendi!");
                        case "hortum" -> DisasterManager.forceStartDisaster(level, 2, 1200, "§4[KOMUT] Dev hortum tetiklendi!");
                        case "tsunami" -> DisasterManager.forceStartDisaster(level, 3, 1600, "§4[KOMUT] Tsunami dalgaları çağrıldı!");
                        case "kar" -> DisasterManager.forceStartDisaster(level, 4, 2000, "§b[KOMUT] Tipi ve Kar fırtınası tetiklendi!");
                        case "kum" -> DisasterManager.forceStartDisaster(level, 5, 1800, "§6[KOMUT] Kum fırtınası tetiklendi!");
                        case "deprem" -> DisasterManager.forceStartDisaster(level, 6, 600, "§4[KOMUT] Yıkıcı deprem tetiklendi!");
                        case "girdap" -> DisasterManager.forceStartDisaster(level, 7, 2400, "§9[KOMUT] Okyanus girdabı tetiklendi!");
                        case "sel" -> DisasterManager.forceStartDisaster(level, 8, 1800, "§4[KOMUT] Büyük sel baskını tetiklendi!");
                        default -> context.getSource().sendFailure(Component.literal("Geçersiz afet türü! Kullanım: /afet <yagmur|hortum|tsunami|kar|kum|deprem|girdap|sel>"));
                    }
                    return 1;
                })
            )
        );
    }
}
