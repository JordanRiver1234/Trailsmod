package net.JordanRiver.KisekiLegend.item.enhancement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

public class ItemEnhancementSystem {

    // Quality system (1-999)
    public static class Quality {
        public static final int MIN_QUALITY = 1;
        public static final int MAX_QUALITY = 999;
        public static final int DEFAULT_QUALITY = 100;

        public static void setQuality(ItemStack item, int quality) {
            quality = Math.max(MIN_QUALITY, Math.min(MAX_QUALITY, quality));
            CompoundTag tag = getOrCreateCustomData(item);
            tag.putInt("Quality", quality);
            updateCustomData(item, tag);
        }

        public static int getQuality(ItemStack item) {
            CompoundTag tag = getCustomData(item);
            return tag.getInt("Quality"); // Returns 0 if not present
        }

        public static String getQualityRank(int quality) {
            if (quality >= 900) return "Legendary";
            if (quality >= 750) return "Epic";
            if (quality >= 600) return "Rare";
            if (quality >= 400) return "Good";
            if (quality >= 200) return "Common";
            return "Poor";
        }

        public static float getQualityMultiplier(int quality) {
            return 1.0f + (quality - DEFAULT_QUALITY) / 500.0f; // Quality affects effectiveness
        }
    }

    // Trait system
    public static class Traits {
        public static void addTrait(ItemStack item, String traitName, int level) {
            CompoundTag tag = getOrCreateCustomData(item);
            CompoundTag traits = tag.getCompound("Traits");
            traits.putInt(traitName, level);
            tag.put("Traits", traits);
            updateCustomData(item, tag);
        }

        public static void removeTrait(ItemStack item, String traitName) {
            CompoundTag tag = getCustomData(item);
            if (tag.contains("Traits")) {
                CompoundTag traits = tag.getCompound("Traits");
                traits.remove(traitName);
                tag.put("Traits", traits);
                updateCustomData(item, tag);
            }
        }

        public static int getTraitLevel(ItemStack item, String traitName) {
            CompoundTag tag = getCustomData(item);
            if (tag.contains("Traits")) {
                CompoundTag traits = tag.getCompound("Traits");
                return traits.getInt(traitName);
            }
            return 0;
        }

        public static Map<String, Integer> getAllTraits(ItemStack item) {
            Map<String, Integer> result = new HashMap<>();
            CompoundTag tag = getCustomData(item);
            if (tag.contains("Traits")) {
                CompoundTag traits = tag.getCompound("Traits");
                for (String key : traits.getAllKeys()) {
                    result.put(key, traits.getInt(key));
                }
            }
            return result;
        }

        public static boolean hasTrait(ItemStack item, String traitName) {
            return getTraitLevel(item, traitName) > 0;
        }
    }

    // Effect system
    public static class Effects {
        public static void addEffect(ItemStack item, String effectName, float value, int duration) {
            CompoundTag tag = getOrCreateCustomData(item);
            ListTag effects = tag.getList("Effects", 10); // 10 = CompoundTag type

            CompoundTag effectTag = new CompoundTag();
            effectTag.putString("Name", effectName);
            effectTag.putFloat("Value", value);
            effectTag.putInt("Duration", duration);

            effects.add(effectTag);
            tag.put("Effects", effects);
            updateCustomData(item, tag);
        }

        public static void removeEffect(ItemStack item, String effectName) {
            CompoundTag tag = getCustomData(item);
            if (tag.contains("Effects")) {
                ListTag effects = tag.getList("Effects", 10);
                for (int i = effects.size() - 1; i >= 0; i--) {
                    CompoundTag effectTag = effects.getCompound(i);
                    if (effectName.equals(effectTag.getString("Name"))) {
                        effects.remove(i);
                        break;
                    }
                }
                tag.put("Effects", effects);
                updateCustomData(item, tag);
            }
        }

        public static List<ItemEffect> getAllEffects(ItemStack item) {
            List<ItemEffect> result = new ArrayList<>();
            CompoundTag tag = getCustomData(item);
            if (tag.contains("Effects")) {
                ListTag effects = tag.getList("Effects", 10);
                for (int i = 0; i < effects.size(); i++) {
                    CompoundTag effectTag = effects.getCompound(i);
                    result.add(new ItemEffect(
                            effectTag.getString("Name"),
                            effectTag.getFloat("Value"),
                            effectTag.getInt("Duration")
                    ));
                }
            }
            return result;
        }

        public static boolean hasEffect(ItemStack item, String effectName) {
            return getAllEffects(item).stream()
                    .anyMatch(effect -> effect.name().equals(effectName));
        }
    }

    // Helper methods for CustomData component (1.21.1)
    public static CompoundTag getCustomData(ItemStack item) {
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private static CompoundTag getOrCreateCustomData(ItemStack item) {
        return getCustomData(item);
    }

    private static void updateCustomData(ItemStack item, CompoundTag tag) {
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // Utility methods for combining enhancements
    public static void applyNodeEnhancements(ItemStack item, String nodeType, Map<String, Integer> materialContributions) {
        switch (nodeType.toLowerCase()) {
            case "quality" -> {
                int qualityBonus = materialContributions.values().stream().mapToInt(Integer::intValue).sum() * 50;
                int currentQuality = Quality.getQuality(item);
                Quality.setQuality(item, Math.max(Quality.DEFAULT_QUALITY, currentQuality + qualityBonus));
            }
            case "trait" -> {
                // Add random trait based on materials used
                String[] possibleTraits = {"Sharpness", "Durability", "Efficiency", "Luck", "Resistance"};
                String selectedTrait = possibleTraits[item.hashCode() % possibleTraits.length];
                int traitLevel = materialContributions.size();
                Traits.addTrait(item, selectedTrait, traitLevel);
            }
            case "effect" -> {
                // Add effect based on node completion
                String[] possibleEffects = {"Healing", "Strength", "Speed", "Protection", "Regeneration" };
                String selectedEffect = possibleEffects[item.hashCode() % possibleEffects.length];
                float effectValue = materialContributions.values().stream().mapToInt(Integer::intValue).sum() * 0.1f;
                Effects.addEffect(item, selectedEffect, effectValue, 600); // 30 seconds
            }
        }
    }

    // Get tooltip information
    public static List<Component> getEnhancementTooltip(ItemStack item) {
        List<Component> tooltip = new ArrayList<>();

        // Quality info
        int quality = Quality.getQuality(item);
        if (quality > 0) {
            String rank = Quality.getQualityRank(quality);
            tooltip.add(Component.literal("Quality: " + quality + " (" + rank + ")").withStyle(style ->
                    style.withColor(getQualityColor(quality))));
        }

        // Traits info
        Map<String, Integer> traits = Traits.getAllTraits(item);
        if (!traits.isEmpty()) {
            tooltip.add(Component.literal("Traits:").withStyle(style -> style.withColor(0x00AA00)));
            for (Map.Entry<String, Integer> trait : traits.entrySet()) {
                tooltip.add(Component.literal("  " + trait.getKey() + " " + trait.getValue())
                        .withStyle(style -> style.withColor(0x55FF55)));
            }
        }

        // Effects info
        List<ItemEffect> effects = Effects.getAllEffects(item);
        if (!effects.isEmpty()) {
            tooltip.add(Component.literal("Effects:").withStyle(style -> style.withColor(0x0000AA)));
            for (ItemEffect effect : effects) {
                tooltip.add(Component.literal("  " + effect.name() + ": " +
                                String.format("%.1f", effect.value()) + " (" + effect.duration() + "s)")
                        .withStyle(style -> style.withColor(0x5555FF)));
            }
        }

        return tooltip;
    }

    private static int getQualityColor(int quality) {
        if (quality >= 900) return 0xFF6600; // Orange (Legendary)
        if (quality >= 750) return 0xAA00AA; // Purple (Epic)
        if (quality >= 600) return 0x0055FF; // Blue (Rare)
        if (quality >= 400) return 0x00AA00; // Green (Good)
        if (quality >= 200) return 0xFFFFFF; // White (Common)
        return 0x808080; // Gray (Poor)
    }

    // Data record for effects
    public record ItemEffect(String name, float value, int duration) {}
}