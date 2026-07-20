package com.oyuncu.dogalafetler.client;

import com.oyuncu.dogalafetler.weather.WeatherSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class WindHudOverlay {

    public static final IGuiOverlay HUD_WIND = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int screenWidth = width;
        int screenHeight = height;

        int hotbarRightX = (screenWidth / 2) + 95; // Hotbar'ın hemen sağı
        int yPos = screenHeight - 22; // Hotbar hizası

        String windText = String.format("§b%.0f km/s %s", 
                WeatherSystem.getWindSpeed(), 
                WeatherSystem.getWindDirection().getArrow());

        guiGraphics.drawString(mc.font, windText, hotbarRightX, yPos, 0xFFFFFFFF, true);
    };
}

