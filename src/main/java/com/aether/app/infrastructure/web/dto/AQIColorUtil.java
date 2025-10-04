package com.aether.app.infrastructure.web.dto;

/**
 * Utility class to calculate AQI color with smooth gradients between ranges.
 * Based on EPA AQI scale used in Spain and many countries.
 */
public class AQIColorUtil {

    private static class RGB {
        int r, g, b;
        RGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        static RGB fromHex(String hex) {
            hex = hex.replace("#", "");
            return new RGB(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
            );
        }
        
        String toHex() {
            return String.format("#%02X%02X%02X", r, g, b);
        }
    }

    // Color definitions based on EPA AQI scale
    private static final String COLOR_GOOD = "#00E400";           // Verde brillante (0-50)
    private static final String COLOR_MODERATE = "#FFFF00";       // Amarillo (51-100)
    private static final String COLOR_SENSITIVE = "#FF7E00";      // Naranja (101-150)
    private static final String COLOR_UNHEALTHY = "#FF0000";      // Rojo (151-200)
    private static final String COLOR_VERY_UNHEALTHY = "#8F3F97"; // Morado (201-300)
    private static final String COLOR_HAZARDOUS = "#7E0023";      // Marrón (301-500)
    private static final String COLOR_UNKNOWN = "#808080";        // Gris para valores desconocidos

    /**
     * Calculates the hex color for a given AQI value with smooth gradient transitions.
     * 
     * @param aqi The Air Quality Index value
     * @return Hex color string (e.g., "#00E400")
     */
    public static String getAQIColor(Integer aqi) {
        if (aqi == null || aqi < 0) {
            return COLOR_UNKNOWN;
        }

        // Define color ranges with interpolation
        if (aqi <= 50) {
            // Good range: interpolate from bright green to yellow-green
            return interpolateColor(COLOR_GOOD, COLOR_MODERATE, aqi, 0, 50);
        } else if (aqi <= 100) {
            // Moderate range: interpolate from yellow to orange
            return interpolateColor(COLOR_MODERATE, COLOR_SENSITIVE, aqi, 51, 100);
        } else if (aqi <= 150) {
            // Unhealthy for Sensitive Groups: interpolate from orange to red
            return interpolateColor(COLOR_SENSITIVE, COLOR_UNHEALTHY, aqi, 101, 150);
        } else if (aqi <= 200) {
            // Unhealthy: interpolate from red to purple
            return interpolateColor(COLOR_UNHEALTHY, COLOR_VERY_UNHEALTHY, aqi, 151, 200);
        } else if (aqi <= 300) {
            // Very Unhealthy: interpolate from purple to brown
            return interpolateColor(COLOR_VERY_UNHEALTHY, COLOR_HAZARDOUS, aqi, 201, 300);
        } else {
            // Hazardous: stay at brown, but could darken further if needed
            if (aqi <= 500) {
                return COLOR_HAZARDOUS;
            } else {
                // Beyond 500, return even darker brown
                return "#4C0013";
            }
        }
    }

    /**
     * Interpolates between two hex colors based on a value within a range.
     * 
     * @param startColor Starting hex color
     * @param endColor Ending hex color
     * @param value Current value
     * @param rangeStart Start of the range
     * @param rangeEnd End of the range
     * @return Interpolated hex color
     */
    private static String interpolateColor(String startColor, String endColor, 
                                          int value, int rangeStart, int rangeEnd) {
        RGB start = RGB.fromHex(startColor);
        RGB end = RGB.fromHex(endColor);
        
        // Calculate interpolation factor (0.0 to 1.0)
        double factor = (double) (value - rangeStart) / (rangeEnd - rangeStart);
        factor = Math.max(0.0, Math.min(1.0, factor)); // Clamp between 0 and 1
        
        // Interpolate each color component
        int r = (int) (start.r + (end.r - start.r) * factor);
        int g = (int) (start.g + (end.g - start.g) * factor);
        int b = (int) (start.b + (end.b - start.b) * factor);
        
        RGB result = new RGB(r, g, b);
        return result.toHex();
    }
}

