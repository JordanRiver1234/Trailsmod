package net.JordanRiver.KisekiLegend.client.renderer;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class WeaponSlotRenderer {

    /**
     * Checks if an item can have weapon slots
     */
    public static boolean isWeaponOrTool(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        // Check vanilla weapons and tools
        if (item instanceof SwordItem || item instanceof AxeItem ||
                item instanceof PickaxeItem || item instanceof ShovelItem ||
                item instanceof HoeItem || item instanceof BowItem ||
                item instanceof CrossbowItem || item instanceof TridentItem) {
            return true;
        }

        // Check for attack damage attribute (covers most modded weapons) - Fixed for 1.21.1
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return !modifiers.modifiers().isEmpty();
    }

    /**
     * Checks if a weapon has slots (uses WeaponSlotData)
     */
    public static boolean hasWeaponSlots(ItemStack weapon) {
        if (!isWeaponOrTool(weapon)) {
            return false;
        }

        try {
            WeaponSlotData slotData = WeaponSlotData.getOrCreate(weapon);
            boolean hasSlots = slotData.getActiveSlotCount() > 0;

            if (hasSlots) {
            }

            return hasSlots;
        } catch (Exception e) {
            System.err.println("Error checking weapon slots for " + weapon.getItem() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the texture for a slot based on element type
     */
    public static ResourceLocation getSlotTexture(String elementType) {
        return switch (elementType.toLowerCase()) {
            case "earth" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/earth_slot");
            case "water" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/water_slot");
            case "fire" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/fire_slot");
            case "wind" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/wind_slot");
            case "time" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/time_slot");
            case "space" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/space_slot");
            case "mirage" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/mirage_slot");
            default -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/earth_slot");
        };
    }

    /**
     * Gets the texture for quartz based on quartz ID
     */
    /**
     * Gets the texture for quartz based on quartz ID
     */
    public static ResourceLocation getQuartzTexture(String quartzId) {
        // FIXED: Return the correct path for your quartz textures
        // Since your textures are at textures/item/defense_1.png etc.
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/" + quartzId);
    }
    public static ResourceLocation getQuartzTextureDebug(String quartzId) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/" + quartzId);

        return texture;
    }
    /**
     * Check if an item should use the weapon slot model system
     */
    public static boolean shouldUseSlotModel(ItemStack stack) {
        // Only use slot model for weapons/tools that have been configured with slots
        return isWeaponOrTool(stack) && hasWeaponSlots(stack);
    }

    /**
     * Debugging method to print slot information
     */
    public static void debugSlotInfo(ItemStack weapon) {
        if (!hasWeaponSlots(weapon)) {
            return;
        }

        WeaponSlotData slotData = WeaponSlotData.getOrCreate(weapon);


        for (int i = 0; i < slotData.getSlots().size(); i++) {
            WeaponSlotData.WeaponSlot slot = slotData.getSlots().get(i);
            if (!slot.isClosed) {

            }
        }
    }

    /**
     * Gets the element color for UI display
     */
    public static int getElementColor(String element) {
        return switch (element.toLowerCase()) {
            case "earth" -> 0x8B4513; // Brown
            case "water" -> 0x0066CC; // Blue
            case "fire" -> 0xFFD92222;  // Red-Orange
            case "wind" -> 0x90EE90;  // Light Green
            case "time" -> 0x9370DB;  // Purple
            case "space" -> 0xFFD9D522; // Yellow
            case "mirage" -> 0xFF888888; // Grey
            default -> 0xFF404040;      // Gray
        };
    }
}