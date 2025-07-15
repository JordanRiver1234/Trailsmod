package net.JordanRiver.KisekiLegend.orbal;

import java.util.List;
import java.util.Map;


public class ArtsRegistry {

    public record ArtDefinition(
        String name,
        Map<String, Integer> elementCost,
        String epCost,
        String castTime,
        String power,
        SpawnStyle style,               // ← NEW parameter
        String effectDescription


    ) {
        public int getCastDelayTicks() {
            try {
                int at = Integer.parseInt(castTime.split(" ")[0]);
                return at * 20;
            } catch (Exception e) {
                return 20; // fallback to 1 second
            }
        }
        public boolean shouldLoop() {
            return false; //  Force all spells to NOT loop unless special exception
        }
    }

    public static final List<ArtDefinition> ALL_ARTS = List.of(
            // Earth Attack Arts
            new ArtDefinition("Stone Hammer", Map.of("earth", 1), "10 EP", "1 AT", "10", SpawnStyle.PROJECTILE,"Earth - Single - Attack"),
            new ArtDefinition("Earth Lance", Map.of("earth", 5), "20 EP", "1 AT", "50", SpawnStyle.GROUND, "Earth - Line - Attack"),
            new ArtDefinition("Petrify Breath", Map.of("earth", 3), "30 EP", "1 AT", "20", SpawnStyle.GROUND,"Earth - Single - Attack [Petrify 20%]"),
            new ArtDefinition("Stone Impact", Map.of("earth", 3, "space", 2), "250 EP", "5 AT", "10", SpawnStyle.BOUNCING_PROJECTILE,"Earth - Area (M) - Attack"),
            new ArtDefinition("Titanic Roar", Map.of("earth", 8, "space", 4), "450 EP", "10 AT", "30", SpawnStyle.AOE_CENTERED,"Earth - Area (M) - Attack"),

            // Water Attack Arts
            new ArtDefinition("Aqua Bleed", Map.of("water", 1), "10 EP", "1 AT", "10", SpawnStyle.PROJECTILE,"Water - Single - Attack"),
            new ArtDefinition("Blue Impact", Map.of("water", 5), "20 EP", "1 AT", "50", SpawnStyle.PROJECTILE,"Water - Single - Attack"),
            new ArtDefinition("Diamond Dust", Map.of("water", 4, "wind", 2, "space", 1), "140 EP", "5 AT", "20", SpawnStyle.PROJECTILE,"Water - Area (S) - Attack [Freeze 20%]"),

            // Fire Attack Arts
            new ArtDefinition("Fire Bolt", Map.of("fire", 1), "10 EP", "1 AT", "10", SpawnStyle.PROJECTILE,"Fire - Single - Attack"),
            new ArtDefinition("Flare Arrow", Map.of("fire", 3), "20 EP", "1 AT", "40", SpawnStyle.PROJECTILE,"Fire - Single - Attack"),
            new ArtDefinition("Napalm Breath", Map.of("fire", 6), "40 EP", "5 AT", "80", SpawnStyle.PROJECTILE,"Fire - Single - Attack"),
            new ArtDefinition("Fire Bolt EX", Map.of("fire", 3, "wind", 1, "space", 1), "120 EP", "1 AT", "20", SpawnStyle.PROJECTILE,"Fire - Area (M) - Attack"),
            new ArtDefinition("Spiral Flare", Map.of("fire", 5, "wind", 2, "space", 2), "230 EP", "5 AT", "50", SpawnStyle.PROJECTILE,"Fire - Area (M) - Attack"),
            new ArtDefinition("Volcanic Rave", Map.of("fire", 8, "earth", 4, "space", 2), "250 EP", "10 AT", "90", SpawnStyle.PROJECTILE,"Fire - Area (M) - Attack"),

            // Wind Attack Arts
            new ArtDefinition("Air Strike", Map.of("wind", 1), "10 EP", "1 AT", "10", SpawnStyle.PROJECTILE,"Wind - Single - Attack"),
            new ArtDefinition("Aerial", Map.of("wind", 4), "20 EP", "1 AT", "20", SpawnStyle.PROJECTILE,"Wind - Area (M) - Attack"),
            new ArtDefinition("Aero Storm", Map.of("wind", 8), "50 EP", "5 AT", "50", SpawnStyle.PROJECTILE,"Wind - Area (L) - Attack"),
            new ArtDefinition("Lightning", Map.of("wind", 4, "space", 2), "30 EP", "1 AT", "30", SpawnStyle.PROJECTILE,"Wind - Line - Attack [Seal 20%]"),
            new ArtDefinition("Plasma Wave", Map.of("wind", 8, "space", 4), "40 EP", "5 AT", "40", SpawnStyle.PROJECTILE,"Wind - Line - Attack [Seal 20%]"),

            // Time Attack Arts
            new ArtDefinition("Shadow Spear", Map.of("time", 5), "20 EP", "1 AT", "20", SpawnStyle.PROJECTILE,"Time - Single - Attack [Deathblow 20%]"),
            new ArtDefinition("Hell Gate", Map.of("time", 4, "space", 2, "mirage", 1), "50 EP", "1 AT", "50", SpawnStyle.PROJECTILE,"Time - Area (S) - Attack [Faint 20%]"),
            new ArtDefinition("White Gehenna", Map.of("time", 8, "space", 4, "mirage", 2), "80 EP", "10 AT", "80", SpawnStyle.PROJECTILE,"Time - Area (M) - Attack [Faint 20%]"),
            new ArtDefinition("Soul Blur", Map.of("time", 1), "10 EP", "1 AT", "10", SpawnStyle.PROJECTILE,"Time - Single - Attack [Faint 20%]"),

            // Earth Support Arts
            new ArtDefinition("Earth Guard", Map.of("earth", 2), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Earth - Single - Support"),
            new ArtDefinition("Earth Wall", Map.of("earth", 4), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Earth - Area (S) - Support"),
            new ArtDefinition("Crest", Map.of("earth", 4, "water", 3, "space", 2, "mirage", 1), "120 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Earth - Single - Support"),

            // Water Recovery Arts
            new ArtDefinition("Tear", Map.of("water", 1), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Single - Recovery"),
            new ArtDefinition("Teara", Map.of("water", 4), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Single - Recovery"),
            new ArtDefinition("Tearal", Map.of("water", 6), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Single - Recovery"),
            new ArtDefinition("La Tear", Map.of("water", 2, "space", 1), "200 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Area (S) - Recovery"),
            new ArtDefinition("La Teara", Map.of("water", 5, "space", 2), "1000 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Area (M) - Recovery"),
            new ArtDefinition("Thelas", Map.of("water", 4, "earth", 2, "mirage", 1), "100 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Single - Recovery"),
            new ArtDefinition("Curia", Map.of("earth", 4, "mirage", 2), "100 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Single - Recovery"),
            new ArtDefinition("La Curia", Map.of("water", 8, "mirage", 4, "space", 2), "100 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Water - Area (L) - Recovery"),

            // Fire Support Arts
            new ArtDefinition("Forte", Map.of("fire", 4, "wind", 3, "space", 2, "mirage", 1), "120 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Fire - Single - Support"),

            // Wind Support Arts
            new ArtDefinition("Sylphen Guard", Map.of("wind", 2), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Wind - Single - Support"),
            new ArtDefinition("Sylphen Wing", Map.of("wind", 6), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Wind - Single - Support"),

            // Time Support Arts
            new ArtDefinition("Clock Up", Map.of("time", 1), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Time - Single - Support"),
            new ArtDefinition("Clock Up EX", Map.of("time", 9), "30 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Time - Single - Support"),
            new ArtDefinition("Anti-Sept", Map.of("time", 3), "20 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Time - Single - Debilitate"),
            new ArtDefinition("Anti-Sept All", Map.of("time", 11), "40 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Time - Area (M) - Debilitate"),

            // Mirage Arts
            new ArtDefinition("Saint", Map.of("mirage", 4, "fire", 3, "earth", 3, "water", 2, "wind", 2, "space", 2), "240 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Mirage - Single - Support"),
            new ArtDefinition("Chaos Brand", Map.of("mirage", 5), "10 EP", "1 AT", "-", SpawnStyle.PROJECTILE,"Mirage - Single - Debilitate")
    );
}

