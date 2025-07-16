package net.JordanRiver.KisekiLegend.orbal;

import java.util.Map;

/**
 * Represents the seven elemental types for Sepith Lines and Quartz.
 * Includes color data for UI rendering.
 */
public enum Element {
    NONE("none", 0xFF404040),     // Neutral/Default - Greyish
    EARTH("earth", 0xFFD98622),   // Orange
    WATER("water", 0xFF2276D9),   // Blue
    FIRE("fire", 0xFFD92222),     // Red
    WIND("wind", 0xFF22D945),     // Green
    TIME("time", 0xFF8A2BE2),     // BlueViolet (as a distinct color for Time)
    SPACE("space", 0xFFD9D522),   // Yellow
    MIRAGE("mirage",  0xFF888888); // Dark grey (changed from 0xFF808080)0xFF404040

    private final String name;
    private final int color;

    Element(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    /**
     * Gets an Element enum from its string name.
     * @param text The string identifier (e.g., "fire").
     * @return The corresponding Element, or NONE if not found.
     */
    public static Element fromString(String text) {
        for (Element b : Element.values()) {
            if (b.name.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return NONE;
    }

    /**
     * A map for quick lookup of elements by their string name.
     */
    public static final Map<String, Element> NAME_MAP = Map.of(
            "earth", EARTH, "water", WATER, "wind", WIND,
            "fire", FIRE, "space", SPACE, "mirage", MIRAGE, "time", TIME
    );
}