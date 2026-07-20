package com.oyuncu.dogalafetler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "dogalafetler", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DisasterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("afet")
            .requires(source -> source.hasPermission(2)) // OP yetkisi gerekir
            .then(Commands.argument("tur", StringArgumentType.word())
                .suggests((context, builder) -> builder.suggest("tsunami").suggest("deprem").suggest("hortum").suggest("girdap").suggest("kar").buildFuture())
                .executes(context -> {
                    String tur = StringArgumentType.getString(context, "tur");
                    CommandSourceStack source = context.getSource();
                    ServerLevel level = source.getLevel();
                    BlockPos pos = BlockPos.containing(source.getPosition());

                    switch (tur.toLowerCase()) {
                        case "tsunami" -> {
                            DisasterManager.triggerTsunami(level, pos);
                            source.sendSuccess(() -> Component.literal("§4[KOMUT] Tsunami dalgaları çağrıldı!"), true);
                        }
                        case "deprem" -> {
                            // Varsayılan olarak en yüksek şiddette (3) tetikler
                            DisasterManager.triggerEarthquake(level, pos, 3);
                            source.sendSuccess(() -> Component.literal("§4[KOMUT] Yıkıcı deprem tetiklendi!"), true);
                        }
                        default -> source.sendFailure(Component.literal("§cGeçersiz afet türü! Süreli afetler otomatik rüzgar/dünya döngüsü ile çalışır."));
                    }
                    return 1;
                })
                // Deprem için özel şiddet argümanı (/afet deprem <şiddet>)
                .then(Commands.argument("siddet", IntegerArgumentType.integer(1, 3))
                    .executes(context -> {
                        String tur = StringArgumentType.getString(context, "tur");
                        int siddet = IntegerArgumentType.getInteger(context, "siddet");
                        CommandSourceStack source = context.getSource();
                        ServerLevel level = source.getLevel();
                        BlockPos pos = BlockPos.containing(source.getPosition());

                        if (tur.equalsIgnoreCase("deprem")) {
                            DisasterManager.triggerEarthquake(level, pos, siddet);
                            source.sendSuccess(() -> Component.literal("§4[KOMUT] " + siddet + " şiddetinde deprem tetiklendi!"), true);
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
