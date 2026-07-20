package com.oyuncu.dogalafetler.weather;

import net.minecraft.world.level.Level;

public class WeatherSystem {
    private static int currentMonth = 7; // Temmuz (7. ay) ile başlıyoruz
    private static float windSpeed = 15.0f; // km/s
    private static WindDirection windDirection = WindDirection.NORTH;

    public enum WindDirection {
        NORTH("↑"), SOUTH("↓"), EAST("→"), WEST("←"),
        NORTH_EAST("↗"), NORTH_WEST("↖"), SOUTH_EAST("↘"), SOUTH_WEST("↙");

        private final String arrow;
        WindDirection(String arrow) { this.arrow = arrow; }
        public String getArrow() { return arrow; }
    }

    public static void updateWeather(Level level) {
        if (level.isClientSide()) return;

        long totalDays = level.getDayTime() / 24000L;
        currentMonth = (int)((totalDays / 10) % 12) + 1; // Her 10 günde bir ay değişir

        // Mevsimsel rüzgarlar
        if (currentMonth >= 6 && currentMonth <= 8) {
            windSpeed = 10.0f + (level.random.nextFloat() * 20.0f); // Yaz rüzgarı
        } else if (currentMonth == 12 || currentMonth <= 2) {
            windSpeed = 45.0f + (level.random.nextFloat() * 45.0f); // Kış rüzgarı
        } else {
            windSpeed = 25.0f + (level.random.nextFloat() * 35.0f); // Bahar rüzgarı
        }

        if (level.random.nextInt(200) == 0) {
            WindDirection[] directions = WindDirection.values();
            windDirection = directions[level.random.nextInt(directions.length)];
        }
    }

    public static float getWindSpeed() { return windSpeed; }
    public static WindDirection getWindDirection() { return windDirection; }
    public static int getCurrentMonth() { return currentMonth; }
    
    public static boolean canFormTornado() {
        return windSpeed > 65.0f && ((currentMonth >= 4 && currentMonth <= 6) || (currentMonth >= 9 && currentMonth <= 11));
    }
}
