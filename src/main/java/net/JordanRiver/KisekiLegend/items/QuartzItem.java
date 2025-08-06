package net.JordanRiver.KisekiLegend.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.item.enhancement.ItemEnhancementSystem;
import net.JordanRiver.KisekiLegend.item.enhancement.MaterialQualitySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuartzItem extends Item {
    private final String element;
    private final Map<String, Integer> sepith;

    public QuartzItem(String element, Map<String, Integer> sepith, Properties properties) {
        super(properties.stacksTo(1));
        this.element = element;
        this.sepith = sepith;

        // Initialize with default quality if not set
        ItemStack defaultStack = new ItemStack(this);
        if (ItemEnhancementSystem.Quality.getQuality(defaultStack) == 0) {
            ItemEnhancementSystem.Quality.setQuality(defaultStack, ItemEnhancementSystem.Quality.DEFAULT_QUALITY);
        }
    }


    /**
     * Called when this quartz is removed from a weapon slot
     */
    public void onRemovedFromWeapon(ItemStack weapon, ItemStack quartz) {
        // Override in implementations if needed
    }
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // Add default modifiers from parent
        ItemAttributeModifiers defaultModifiers = super.getDefaultAttributeModifiers(stack);
        for (ItemAttributeModifiers.Entry entry : defaultModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag quartzData = data.copyTag();
            if (quartzData.contains("Attributes", 9)) {
                ListTag attributeList = quartzData.getList("Attributes", 10);
                for (int i = 0; i < attributeList.size(); i++) {
                    CompoundTag attrTag = attributeList.getCompound(i);
                    Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse(attrTag.getString("Name")));
                    if (attribute != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                ResourceLocation.parse(attrTag.getString("UUID")),
                                attrTag.getDouble("Amount"),
                                AttributeModifier.Operation.values()[attrTag.getInt("Operation")]
                        );
                        // Apply to both hands
                        builder.add(Holder.direct(attribute), modifier, EquipmentSlotGroup.MAINHAND);
                        builder.add(Holder.direct(attribute), modifier, EquipmentSlotGroup.OFFHAND);
                    }
                }
            }
        }
        return builder.build();
    }

    // --- The rest of the file is correct as-is ---
    public String getElement() { return element; }
    public Map<String, Integer> getSepith() { return sepith; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Element: " + element).withStyle(ChatFormatting.GOLD));

        if (!sepith.isEmpty()) {
            tooltip.add(Component.literal("Sepith Value:").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, Integer> entry : sepith.entrySet()) {
                tooltip.add(Component.literal(" - " + entry.getKey() + ": " + entry.getValue()).withStyle(ChatFormatting.DARK_GREEN));
            }
        }

        // FIXED: Show weapon buffs in tooltip
        Map<String, Float> buffs = getWeaponBuffs();
        if (!buffs.isEmpty()) {
            tooltip.add(Component.literal("Weapon Buffs:").withStyle(ChatFormatting.AQUA));
            for (Map.Entry<String, Float> buff : buffs.entrySet()) {
                String buffName = buff.getKey().replace("_", " ");
                String sign = buff.getValue() > 0 ? "+" : "";
                tooltip.add(Component.literal(" " + sign + buff.getValue() + " " + buffName)
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        // Show existing custom data
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag quartzData = data.copyTag();
            if (quartzData.contains("Quality")) {
                tooltip.add(Component.literal("Quality: " + quartzData.getInt("Quality")).withStyle(ChatFormatting.YELLOW));
            }
        }
    }
    public Map<String, Float> getWeaponBuffs(ItemStack quartzStack) {
        Map<String, Float> buffs = new HashMap<>();

        // Get base buffs from quartz type
        String quartzId = getQuartzId();
        Map<String, Float> baseBuffs = getBaseBuffsForQuartzType(quartzId);

        // Apply quality and material modifiers from the actual quartz stack
        int quality = ItemEnhancementSystem.Quality.getQuality(quartzStack);
        if (quality == 0) quality = ItemEnhancementSystem.Quality.DEFAULT_QUALITY;

        float qualityMultiplier = ItemEnhancementSystem.Quality.getQualityMultiplier(quality);

        // Apply base buffs with quality multiplier
        for (Map.Entry<String, Float> buff : baseBuffs.entrySet()) {
            float enhancedValue = buff.getValue() * qualityMultiplier;
            buffs.put(buff.getKey(), enhancedValue);
        }

        // Add trait-based buffs
        Map<String, Integer> traits = ItemEnhancementSystem.Traits.getAllTraits(quartzStack);
        for (Map.Entry<String, Integer> trait : traits.entrySet()) {
            addTraitBuffs(buffs, trait.getKey(), trait.getValue());
            System.out.println("Processing trait: " + trait.getKey() + " level " + trait.getValue());
        }

        // ADD THIS: Convert effects to weapon buffs too
        List<ItemEnhancementSystem.ItemEffect> effects = ItemEnhancementSystem.Effects.getAllEffects(quartzStack);
        for (ItemEnhancementSystem.ItemEffect effect : effects) {
            addEffectBuffs(buffs, effect.name(), effect.value());
            System.out.println("Processing effect: " + effect.name() + " value " + effect.value());
        }

        System.out.println("Final weapon buffs: " + buffs);
        return buffs;
    }
    private void addEffectBuffs(Map<String, Float> buffs, String effectName, float value) {
        String lowerName = effectName.toLowerCase();

        switch (effectName) {
            // HP Effects
            case "HP Gain XS" -> buffs.merge("max_health", value * 0.1f, Float::sum);
            case "HP Gain S" -> buffs.merge("max_health", value * 0.25f, Float::sum);
            case "HP Gain M" -> buffs.merge("max_health", value * 0.5f, Float::sum);
            case "HP Gain L" -> buffs.merge("max_health", value * 1.0f, Float::sum);
            case "HP Gain XL" -> buffs.merge("max_health", value * 2.0f, Float::sum);
            case "HP Regen S" -> buffs.merge("max_health", value * 0.5f, Float::sum);
            case "HP Regen M" -> buffs.merge("max_health", value * 1.0f, Float::sum);
            case "HP Regen L" -> buffs.merge("max_health", value * 1.5f, Float::sum);

            // Attack/Damage Effects
            case "ATK Up S" -> buffs.merge("attack_damage", value * 0.5f, Float::sum);
            case "ATK Up M" -> buffs.merge("attack_damage", value * 1.0f, Float::sum);
            case "ATK Down S" -> buffs.merge("attack_damage", -value * 0.5f, Float::sum);
            case "Physical Damage XS" -> buffs.merge("attack_damage", value * 0.25f, Float::sum);
            case "Physical Damage M" -> buffs.merge("attack_damage", value * 0.5f, Float::sum);
            case "Fire Damage L" -> buffs.merge("attack_damage", value * 0.75f, Float::sum);
            case "Fire Damage XL" -> buffs.merge("attack_damage", value * 1.0f, Float::sum);
            case "Ice Damage S" -> buffs.merge("attack_damage", value * 0.4f, Float::sum);
            case "Lightning Damage S" -> buffs.merge("attack_damage", value * 0.6f, Float::sum);
            case "Poison Damage XS" -> buffs.merge("attack_damage", value * 0.15f, Float::sum);

            // Defense Effects
            case "DEF Up S" -> buffs.merge("armor", value * 0.5f, Float::sum);
            case "Guardian Mirror S" -> buffs.merge("armor", value * 1.0f, Float::sum);
            case "Defense Veil" -> buffs.merge("armor", value * 0.8f, Float::sum);
            case "Reduce Damage -3%" -> buffs.merge("armor", value * 0.3f, Float::sum);
            case "Reduce Damage -10%" -> buffs.merge("armor", value * 1.0f, Float::sum);
            case "Fire Resist Up" -> buffs.merge("armor", value * 0.4f, Float::sum);
            case "Fire Resist Up+" -> buffs.merge("armor", value * 0.6f, Float::sum);

            // Speed Effects
            case "SPD Up S" -> buffs.merge("movement_speed", value * 0.02f, Float::sum);
            case "SPD Up M" -> buffs.merge("movement_speed", value * 0.03f, Float::sum);
            case "SPD Down S" -> buffs.merge("movement_speed", -value * 0.02f, Float::sum);
            case "Wind Rider" -> {
                buffs.merge("movement_speed", value * 0.04f, Float::sum);
                buffs.merge("luck", value * 0.5f, Float::sum);
            }

            // Special Resistances & Protections
            case "Water Breathing" -> buffs.merge("luck", value * 0.3f, Float::sum);
            case "Night Vision" -> buffs.merge("luck", value * 0.2f, Float::sum);
            case "Fire Resistance" -> buffs.merge("armor", value * 0.5f, Float::sum);
            case "Levitation" -> buffs.merge("movement_speed", value * 0.01f, Float::sum);
            case "Slow Falling" -> buffs.merge("armor_toughness", value * 0.3f, Float::sum);

            // Enhancement Effects
            case "Enhance Skills +3%" -> buffs.merge("luck", value * 0.3f, Float::sum);
            case "Enhance Skills +7%" -> buffs.merge("luck", value * 0.7f, Float::sum);
            case "Enhance Skills +10%" -> buffs.merge("luck", value * 1.0f, Float::sum);
            case "Enhance Critical +20%" -> buffs.merge("luck", value * 2.0f, Float::sum);
            case "Critical Rate Up S" -> buffs.merge("luck", value * 1.5f, Float::sum);
            case "Enhance Items +5%" -> buffs.merge("attack_damage", value * 0.05f, Float::sum);
            case "Weaken Items +3%" -> buffs.merge("attack_damage", -value * 0.03f, Float::sum);

            // Utility Effects
            case "Magic Veil" -> buffs.merge("armor", value * 0.6f, Float::sum);
            case "Money Magnet" -> buffs.merge("luck", value * 1.0f, Float::sum);
            case "Eye for Materials" -> buffs.merge("luck", value * 0.8f, Float::sum);
            case "Treasure Hunter" -> buffs.merge("luck", value * 1.2f, Float::sum);
            case "Dragon Slayer" -> buffs.merge("attack_damage", value * 2.0f, Float::sum);

            // Food & Saturation Effects
            case "Healing Taste S" -> buffs.merge("max_health", value * 0.2f, Float::sum);
            case "Healing Taste M" -> buffs.merge("max_health", value * 0.4f, Float::sum);
            case "Healing Taste L" -> buffs.merge("max_health", value * 0.6f, Float::sum);
            case "Feeling Full S" -> buffs.merge("max_health", value * 0.3f, Float::sum);
            case "Mild Sweetness" -> buffs.merge("luck", value * 0.1f, Float::sum);
            case "Sweetness" -> buffs.merge("luck", value * 0.2f, Float::sum);

            // Powerful Combination Effects
            case "All Stats Up L" -> {
                buffs.merge("attack_damage", value * 1.0f, Float::sum);
                buffs.merge("armor", value * 0.8f, Float::sum);
                buffs.merge("movement_speed", value * 0.02f, Float::sum);
            }
            case "Energy Surge L" -> {
                buffs.merge("max_health", value * 1.5f, Float::sum);
                buffs.merge("luck", value * 1.0f, Float::sum);
            }
            case "Light Blessing S" -> buffs.merge("luck", value * 0.5f, Float::sum);
            case "Thunderclap S" -> buffs.merge("attack_damage", value * 0.8f, Float::sum);

            // Recovery Effects
            case "KO Recovery S" -> buffs.merge("max_health", value * 1.0f, Float::sum);
            case "KO Recovery M" -> buffs.merge("max_health", value * 2.0f, Float::sum);
            case "Resist KO +10%" -> buffs.merge("max_health", value * 0.5f, Float::sum);

            // Harmful/Risky Effects (negative buffs)
            case "Self Harm" -> buffs.merge("max_health", -value * 0.2f, Float::sum);
            case "Surprise! S" -> buffs.merge("luck", -value * 0.1f, Float::sum);
            case "Fire Vulnerability" -> buffs.merge("armor", -value * 0.5f, Float::sum);
            case "Random Effect" -> buffs.merge("luck", value * 0.1f, Float::sum);
            case "Random Teleport" -> buffs.merge("movement_speed", value * 0.01f, Float::sum);
            case "Explosive" -> buffs.merge("attack_damage", value * 0.5f, Float::sum);

            // Debuff Infliction Effects (converted to offensive power)
            case "Inflict Burn S" -> buffs.merge("attack_damage", value * 0.3f, Float::sum);
            case "Inflict Burn M" -> buffs.merge("attack_damage", value * 0.5f, Float::sum);
            case "Inflict Burn L" -> buffs.merge("attack_damage", value * 0.7f, Float::sum);
            case "Inflict Poison S" -> buffs.merge("attack_damage", value * 0.2f, Float::sum);
            case "Inflict Poison M" -> buffs.merge("attack_damage", value * 0.4f, Float::sum);
            case "Inflict Poison L" -> buffs.merge("attack_damage", value * 0.6f, Float::sum);
            case "Inflict Frostbite S" -> buffs.merge("attack_damage", value * 0.25f, Float::sum);
            case "Inflict Frostbite M" -> buffs.merge("attack_damage", value * 0.4f, Float::sum);
            case "Inflict Thorn S" -> buffs.merge("armor_toughness", value * 0.3f, Float::sum);
            case "Inflict Curse S" -> buffs.merge("attack_damage", value * 0.2f, Float::sum);
            case "Inflict Curse M" -> buffs.merge("attack_damage", value * 0.3f, Float::sum);
            case "Inflict Curse L" -> buffs.merge("attack_damage", value * 0.4f, Float::sum);
            case "Inflict Slow S" -> buffs.merge("attack_damage", value * 0.15f, Float::sum);
            case "All Stats Down S" -> buffs.merge("attack_damage", -value * 0.3f, Float::sum);

            // Utility & Cleansing Effects
            case "Remove Debuffs" -> buffs.merge("luck", value * 1.5f, Float::sum);
            case "Remove Ailments" -> buffs.merge("max_health", value * 0.5f, Float::sum);
            case "Poison Cure" -> buffs.merge("armor", value * 0.3f, Float::sum);

            // XP & Learning Effects
            case "XP Gain" -> buffs.merge("luck", value * 0.5f, Float::sum);

            // Fallback for unrecognized effects
            default -> {
                if (!lowerName.contains("inflict") && !lowerName.contains("harm") &&
                        !lowerName.contains("vulnerability") && !lowerName.contains("down")) {
                    buffs.merge("luck", value * 0.1f, Float::sum);
                }
            }
        }
    }

    private Map<String, Float> getBaseBuffsForQuartzType(String quartzId) {
        Map<String, Float> buffs = new HashMap<>();
        switch (quartzId) {
            // Attack Quartz (Fire type)
            case "attack_1" -> buffs.put("attack_damage", 2.0f);
            case "attack_2" -> buffs.put("attack_damage", 4.0f);
            case "attack_3" -> buffs.put("attack_damage", 6.0f);
            case "seal" -> buffs.put("attack_damage", 3.0f);
            case "confuse" -> buffs.put("attack_damage", 2.0f);
            case "strike" -> buffs.put("attack_damage", 4.0f);

            // Defense Quartz (Earth type)
            case "defense_1" -> buffs.put("armor", 1.0f);
            case "defense_2" -> buffs.put("armor", 2.0f);
            case "defense_3" -> buffs.put("armor", 3.0f);
            case "poison" -> buffs.put("armor", 1.5f);
            case "mute" -> buffs.put("armor", 1.5f);
            case "petrify" -> buffs.put("armor", 2.0f);

            // Health Quartz (Water type)
            case "hp_1" -> buffs.put("max_health", 2.0f);
            case "hp_2" -> buffs.put("max_health", 4.0f);
            case "hp_3" -> buffs.put("max_health", 6.0f);
            case "heal" -> buffs.put("max_health", 3.0f);
            case "freeze" -> buffs.put("max_health", 2.0f);

            // Mind/Mana Quartz (Water type)
            case "mind_1" -> buffs.put("max_mana", 10.0f);
            case "mind_2" -> buffs.put("max_mana", 20.0f);
            case "mind_3" -> buffs.put("max_mana", 30.0f);

            // Shield Quartz (Wind type)
            case "shield_1" -> buffs.put("armor_toughness", 1.0f);
            case "shield_2" -> buffs.put("armor_toughness", 2.0f);
            case "shield_3" -> buffs.put("armor_toughness", 3.0f);

            // Evasion Quartz (Wind type)
            case "evade_1" -> buffs.put("luck", 1.0f);
            case "evade_2" -> buffs.put("luck", 2.0f);
            case "evade_3" -> buffs.put("luck", 3.0f);

            // Movement Impediment Quartz (Wind type)
            case "impede_1" -> buffs.put("knockback_resistance", 0.2f);
            case "impede_2" -> buffs.put("knockback_resistance", 0.4f);
            case "impede_3" -> buffs.put("knockback_resistance", 0.6f);
            case "sleep" -> buffs.put("knockback_resistance", 0.3f);
            case "scent" -> buffs.put("knockback_resistance", 0.25f);

            // Action Speed Quartz (Time type)
            case "action_1" -> buffs.put("movement_speed", 0.1f);
            case "action_2" -> buffs.put("movement_speed", 0.2f);
            case "action_3" -> buffs.put("movement_speed", 0.3f);
            case "blind" -> buffs.put("movement_speed", 0.15f);

            // Casting Speed Quartz (Time type)
            case "cast_1" -> buffs.put("attack_speed", 0.15f);
            case "cast_2" -> buffs.put("attack_speed", 0.3f);

            // Critical Hit Quartz (Time type)
            case "deathblow_1" -> buffs.put("luck", 2.0f);
            case "deathblow_2" -> buffs.put("luck", 4.0f);

            // Movement Range Quartz (Space type)
            case "move_1" -> buffs.put("movement_speed", 0.05f);
            case "move_2" -> buffs.put("movement_speed", 0.1f);
            case "move_3" -> buffs.put("movement_speed", 0.15f);

            // EP Cut (Mana Efficiency) Quartz (Space/Time/Mirage type)
            case "ep_cut_1" -> buffs.put("max_mana", 5.0f);
            case "ep_cut_2" -> buffs.put("max_mana", 10.0f);
            case "ep_cut_3" -> buffs.put("max_mana", 15.0f);

            // Range Quartz (Space type)
            case "range_1" -> buffs.put("reach_distance", 1.0f);
            case "eagle_eye" -> buffs.put("reach_distance", 2.0f);

            // EP (Energy Points) Quartz (Mirage/Time/Space type)
            case "ep_1" -> buffs.put("max_mana", 8.0f);
            case "ep_2" -> buffs.put("max_mana", 16.0f);
            case "ep_3" -> buffs.put("max_mana", 24.0f);

            // Hit Rate Quartz (Mirage type)
            case "hit_1" -> buffs.put("luck", 0.5f);
            case "hit_2" -> buffs.put("luck", 1.0f);
            case "hit_3" -> buffs.put("luck", 1.5f);

            // Utility Quartz (Mirage type)
            case "information" -> buffs.put("luck", 1.0f);
            case "haze" -> buffs.put("armor", 1.0f);
            case "cloak" -> buffs.put("movement_speed", 0.1f);
        }

        return buffs;
    }
    private void addTraitBuffs(Map<String, Float> buffs, String traitName, int level) {
        switch (traitName) {
            // Healing & Recovery Traits (POSITIVE)
            case "Healing" -> buffs.merge("max_health", level * 1.0f, Float::sum);
            case "Healing+" -> buffs.merge("max_health", level * 1.5f, Float::sum);
            case "Healing++" -> buffs.merge("max_health", level * 2.0f, Float::sum);
            case "Terrific Healing" -> {
                buffs.merge("max_health", level * 2.5f, Float::sum);
                buffs.merge("armor", level * 0.5f, Float::sum);
            }
            case "Natural Medicine" -> {
                buffs.merge("max_health", level * 1.0f, Float::sum);
                buffs.merge("armor", level * 0.3f, Float::sum);
            }

            // Defense & Protection Traits (POSITIVE)
            case "Defense Charge" -> buffs.merge("armor", level * 1.2f, Float::sum);
            case "Steel Protection" -> buffs.merge("armor", level * 1.5f, Float::sum);
            case "Dragonscale Protection" -> buffs.merge("armor", level * 2.0f, Float::sum);
            case "Indestructible Shield" -> buffs.merge("armor", level * 2.5f, Float::sum);

            // Quality Traits (POSITIVE)
            case "Quality" -> {
                buffs.merge("attack_damage", level * 0.3f, Float::sum);
                buffs.merge("armor", level * 0.2f, Float::sum);
            }
            case "Quality+" -> {
                buffs.merge("attack_damage", level * 0.5f, Float::sum);
                buffs.merge("armor", level * 0.3f, Float::sum);
            }
            case "Quality++" -> {
                buffs.merge("attack_damage", level * 0.7f, Float::sum);
                buffs.merge("armor", level * 0.4f, Float::sum);
            }
            case "High Quality" -> {
                buffs.merge("attack_damage", level * 0.6f, Float::sum);
                buffs.merge("armor", level * 0.4f, Float::sum);
                buffs.merge("luck", level * 0.3f, Float::sum);
            }
            case "Best Quality" -> {
                buffs.merge("attack_damage", level * 1.0f, Float::sum);
                buffs.merge("armor", level * 0.6f, Float::sum);
                buffs.merge("luck", level * 0.5f, Float::sum);
            }

            // Attack Enhancement Traits (POSITIVE)
            case "Attack Charge" -> buffs.merge("attack_damage", level * 1.5f, Float::sum);
            case "Sharp Edge S" -> buffs.merge("attack_damage", level * 2.0f, Float::sum);
            case "Destructive" -> buffs.merge("attack_damage", level * 1.2f, Float::sum);
            case "Destructive+" -> buffs.merge("attack_damage", level * 1.8f, Float::sum);
            case "Destructive++" -> buffs.merge("attack_damage", level * 2.4f, Float::sum);
            case "War God's Power" -> {
                buffs.merge("attack_damage", level * 2.5f, Float::sum);
                buffs.merge("armor_toughness", level * 1.0f, Float::sum);
            }

            // Critical Hit Traits (POSITIVE)
            case "Critical" -> buffs.merge("luck", level * 1.5f, Float::sum);
            case "Critical+" -> buffs.merge("luck", level * 2.0f, Float::sum);
            case "Critical++" -> buffs.merge("luck", level * 2.5f, Float::sum);

            // Speed & Movement Traits (POSITIVE)
            case "Speed Charge" -> buffs.merge("movement_speed", level * 0.05f, Float::sum);
            case "Speed of Light" -> {
                buffs.merge("movement_speed", level * 0.08f, Float::sum);
                buffs.merge("attack_speed", level * 0.1f, Float::sum);
            }

            // Legendary & Ultimate Traits (POSITIVE)
            case "Primordial Power" -> {
                buffs.merge("attack_damage", level * 2.0f, Float::sum);
                buffs.merge("armor", level * 1.5f, Float::sum);
                buffs.merge("max_health", level * 2.0f, Float::sum);
            }
            case "Glorious Soul" -> {
                buffs.merge("attack_damage", level * 1.5f, Float::sum);
                buffs.merge("armor", level * 1.2f, Float::sum);
                buffs.merge("luck", level * 1.0f, Float::sum);
            }
            case "Divine Petal" -> {
                buffs.merge("max_health", level * 3.0f, Float::sum);
                buffs.merge("armor", level * 1.0f, Float::sum);
                buffs.merge("luck", level * 0.8f, Float::sum);
            }
            case "Stats Power" -> {
                buffs.merge("attack_damage", level * 1.0f, Float::sum);
                buffs.merge("armor", level * 0.8f, Float::sum);
                buffs.merge("movement_speed", level * 0.03f, Float::sum);
            }
            case "Rarest" -> {
                buffs.merge("attack_damage", level * 2.5f, Float::sum);
                buffs.merge("armor", level * 2.0f, Float::sum);
                buffs.merge("luck", level * 2.0f, Float::sum);
            }

            // Elemental & Special Traits (NEUTRAL)
            case "Sponge" -> buffs.merge("armor", level * 0.3f, Float::sum);
            case "Icy Echo" -> buffs.merge("armor", level * 0.4f, Float::sum);
            case "Perpetual Ice S" -> buffs.merge("armor", level * 0.6f, Float::sum);
            case "Secret Rainbow" -> buffs.merge("luck", level * 0.5f, Float::sum);
            case "Soft Texture" -> buffs.merge("armor_toughness", level * 0.5f, Float::sum);
            case "Mystic Life" -> buffs.merge("max_health", level * 1.0f, Float::sum);
            case "Resonant" -> buffs.merge("luck", level * 0.4f, Float::sum);
            case "Fantasy Spore" -> buffs.merge("luck", level * 1.0f, Float::sum);
            case "Infinite Energy" -> buffs.merge("movement_speed", level * 0.02f, Float::sum);
            case "Glittering Darkness" -> buffs.merge("armor", level * 0.5f, Float::sum);

            // Reactive Traits (POSITIVE)
            case "Light Glow" -> buffs.merge("armor_toughness", level * 0.8f, Float::sum);
            case "Smoldering Lunacy" -> {
                buffs.merge("attack_damage", level * 1.5f, Float::sum);
                buffs.merge("armor_toughness", level * 0.6f, Float::sum);
            }
            case "Dissolving Heat S" -> {
                buffs.merge("attack_damage", level * 1.8f, Float::sum);
                buffs.merge("armor_toughness", level * 0.8f, Float::sum);
            }
            case "Thunder Burn" -> buffs.merge("attack_damage", level * 1.3f, Float::sum);
            case "Thunder Current S" -> buffs.merge("attack_damage", level * 2.0f, Float::sum);

            // Charge & Boost Traits (POSITIVE)
            case "HP Charge" -> buffs.merge("max_health", level * 1.5f, Float::sum);
            case "Stats Charge+" -> {
                buffs.merge("attack_damage", level * 0.8f, Float::sum);
                buffs.merge("armor", level * 0.6f, Float::sum);
                buffs.merge("movement_speed", level * 0.02f, Float::sum);
            }
            case "Skill Charge" -> buffs.merge("luck", level * 0.8f, Float::sum);
            case "Skill Charge+" -> buffs.merge("luck", level * 1.2f, Float::sum);

            // Special & Utility Traits (POSITIVE)
            case "Flowing Wisdom" -> {
                buffs.merge("luck", level * 1.0f, Float::sum);
                buffs.merge("armor", level * 0.3f, Float::sum);
            }
            case "Rich Flavor" -> buffs.merge("max_health", level * 0.8f, Float::sum);
            case "Area Bonus" -> buffs.merge("attack_damage", level * 1.0f, Float::sum);
            case "Free Soul" -> buffs.merge("movement_speed", level * 0.04f, Float::sum);
            case "Overflowing Courage" -> {
                buffs.merge("attack_damage", level * 1.2f, Float::sum);
                buffs.merge("max_health", level * 1.0f, Float::sum);
            }
            case "Healing Taste S" -> buffs.merge("max_health", level * 0.5f, Float::sum);
            case "Healing Taste M" -> buffs.merge("max_health", level * 1.0f, Float::sum);
            case "Clear Head S" -> buffs.merge("luck", level * 0.6f, Float::sum);
            case "Reverse Hour Hand" -> buffs.merge("movement_speed", level * 0.06f, Float::sum);

            // Negative Traits (NEGATIVE)
            case "Sticky Goo S" -> buffs.merge("movement_speed", -level * 0.02f, Float::sum);
            case "Sticky Goo M" -> buffs.merge("movement_speed", -level * 0.04f, Float::sum);
            case "Power Throw" -> {
                buffs.merge("attack_damage", level * 0.8f, Float::sum);
                buffs.merge("armor", -level * 0.3f, Float::sum);
            }
            case "Power Throw+" -> {
                buffs.merge("attack_damage", level * 1.2f, Float::sum);
                buffs.merge("armor", -level * 0.5f, Float::sum);
            }
            case "Explosive" -> {
                buffs.merge("attack_damage", level * 1.5f, Float::sum);
                buffs.merge("max_health", -level * 0.5f, Float::sum);
            }
            case "Rapid" -> {
                buffs.merge("attack_speed", level * 0.1f, Float::sum);
                buffs.merge("armor", -level * 0.2f, Float::sum);
            }
            case "Rapid+" -> {
                buffs.merge("attack_speed", level * 0.15f, Float::sum);
                buffs.merge("armor", -level * 0.3f, Float::sum);
            }
            case "Slowdown S" -> buffs.merge("movement_speed", -level * 0.03f, Float::sum);
            case "Slowdown M" -> buffs.merge("movement_speed", -level * 0.05f, Float::sum);
            case "Curse Strength" -> buffs.merge("attack_damage", -level * 0.5f, Float::sum);
            case "Curse Protection" -> buffs.merge("armor", -level * 0.5f, Float::sum);
            case "Assassin Poison S" -> {
                buffs.merge("attack_damage", level * 0.5f, Float::sum);
                buffs.merge("max_health", -level * 0.3f, Float::sum);
            }
            case "Grievous Wound S" -> {
                buffs.merge("attack_damage", level * 1.0f, Float::sum);
                buffs.merge("max_health", -level * 0.5f, Float::sum);
            }
            case "Twilight Invitation S" -> buffs.merge("luck", -level * 0.3f, Float::sum);
            case "Hazy Outline S" -> buffs.merge("luck", -level * 0.2f, Float::sum);
            case "Expensive" -> buffs.merge("attack_damage", -level * 0.3f, Float::sum);
            case "Expensive+" -> buffs.merge("attack_damage", -level * 0.5f, Float::sum);
            case "Expensive++" -> buffs.merge("attack_damage", -level * 0.7f, Float::sum);

            // Default fallback
            default -> {
                // Check for common patterns in trait names
                String lowerName = traitName.toLowerCase();
                if (lowerName.contains("healing") || lowerName.contains("hp")) {
                    buffs.merge("max_health", level * 1.0f, Float::sum);
                } else if (lowerName.contains("attack") || lowerName.contains("destructive") ||
                        lowerName.contains("sharp") || lowerName.contains("critical")) {
                    buffs.merge("attack_damage", level * 1.0f, Float::sum);
                } else if (lowerName.contains("defense") || lowerName.contains("protection") ||
                        lowerName.contains("shield")) {
                    buffs.merge("armor", level * 0.8f, Float::sum);
                } else if (lowerName.contains("speed") || lowerName.contains("charge")) {
                    buffs.merge("movement_speed", level * 0.03f, Float::sum);
                } else if (lowerName.contains("curse") || lowerName.contains("expensive") ||
                        lowerName.contains("sticky")) {
                    buffs.merge("luck", -level * 0.2f, Float::sum);
                } else {
                    // Generic positive boost for unrecognized traits
                    buffs.merge("luck", level * 0.1f, Float::sum);
                }
            }
        }

        System.out.println("Converted trait '" + traitName + "' level " + level + " to weapon buffs");
    }
    public static void debugWeaponData(ItemStack weapon) {
        System.out.println("=== WEAPON DATA DEBUG ===");
        CustomData customData = weapon.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            System.out.println("Weapon NBT keys: " + tag.getAllKeys());

            if (tag.contains("MaterialTraits")) {
                System.out.println("Material traits: " + tag.getCompound("MaterialTraits"));
            }

            if (tag.contains("MaterialEffects")) {
                System.out.println("Material effects: " + tag.getList("MaterialEffects", 10));
            }

            System.out.println("Quality: " + ItemEnhancementSystem.Quality.getQuality(weapon));
            System.out.println("Traits: " + ItemEnhancementSystem.Traits.getAllTraits(weapon));
            System.out.println("Effects: " + ItemEnhancementSystem.Effects.getAllEffects(weapon));
        } else {
            System.out.println("No custom data on weapon");
        }
    }

    public void onInsertedIntoWeapon(ItemStack weapon, ItemStack quartz) {
        System.out.println("=== APPLYING QUARTZ TO WEAPON - ENHANCED DEBUG ===");
        System.out.println("Weapon: " + weapon.getDisplayName().getString());
        System.out.println("Quartz: " + quartz.getDisplayName().getString());
        System.out.println("Quartz ID: " + getQuartzId());

        if (weapon.isEmpty() || quartz.isEmpty()) {
            System.out.println("ERROR: Weapon or quartz is empty!");
            return;
        }

        // Apply enhanced attribute buffs
        System.out.println("--- APPLYING ENHANCED ATTRIBUTE BUFFS ---");
        Map<String, Float> buffs = getWeaponBuffs(quartz);
        System.out.println("Enhanced buffs: " + buffs);

        if (!buffs.isEmpty()) {
            applyQuartzAttributesToWeapon(weapon, quartz);
            System.out.println("Enhanced attribute buffs applied successfully");
        }

        // Apply MaterialQualitySystem traits and effects to weapon NBT
        System.out.println("--- APPLYING MATERIAL QUALITY SYSTEM DATA ---");
        applyMaterialQualityDataToWeapon(weapon, quartz);

        // Apply material traits
        applyMaterialTraitsToWeapon(weapon, quartz);

        // Update weapon metadata
        CompoundTag weaponTag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        weaponTag.putLong("LastQuartzUpdate", System.currentTimeMillis());
        weaponTag.putString("LastQuartzApplied", getQuartzId());
        weaponTag.putInt("QuartzQuality", ItemEnhancementSystem.Quality.getQuality(quartz));
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag));

        System.out.println("=== ENHANCED QUARTZ EFFECTS APPLIED SUCCESSFULLY ===");
    }

    private void applyMaterialQualityDataToWeapon(ItemStack weapon, ItemStack quartz) {
        CompoundTag weaponTag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // Apply traits from MaterialQualitySystem
        Map<String, MaterialQualitySystem.TraitData> materialTraits = MaterialQualitySystem.getMaterialTraits(quartz.getItem());
        if (!materialTraits.isEmpty()) {
            CompoundTag traitsTag = weaponTag.getCompound("MaterialTraits");
            for (MaterialQualitySystem.TraitData trait : materialTraits.values()) {
                traitsTag.putInt(trait.getName(), trait.getLevel());
                System.out.println("Applied material trait: " + trait.getName() + " level " + trait.getLevel());
            }
            weaponTag.put("MaterialTraits", traitsTag);
        }

        // Apply effects from MaterialQualitySystem
        Map<String, MaterialQualitySystem.EffectData> materialEffects = MaterialQualitySystem.getMaterialEffects(quartz.getItem());
        if (!materialEffects.isEmpty()) {
            ListTag effectsTag = weaponTag.getList("MaterialEffects", 10);
            for (MaterialQualitySystem.EffectData effect : materialEffects.values()) {
                CompoundTag effectTag = new CompoundTag();
                effectTag.putString("Name", effect.getName());
                effectTag.putFloat("Value", effect.getValue());
                effectTag.putInt("Duration", effect.getDuration());
                effectTag.putString("Type", effect.getType().name());
                effectTag.putString("Source", "quartz_" + getQuartzId());
                effectsTag.add(effectTag);
                System.out.println("Applied material effect: " + effect.getName() + " = " + effect.getValue());
            }
            weaponTag.put("MaterialEffects", effectsTag);
        }

        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag));
    }

    private void applyMaterialTraitsToWeapon(ItemStack weapon, ItemStack quartz) {
        try {
            System.out.println("=== APPLYING MATERIAL TRAITS ===");

            CompoundTag weaponTag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            // Store quartz-specific data safely
            CompoundTag quartzData = new CompoundTag();
            quartzData.putString("quartz_id", getQuartzId());
            quartzData.putString("element", getElement());
            quartzData.putLong("applied_time", System.currentTimeMillis());

            // Store sepith values
            if (!getSepith().isEmpty()) {
                CompoundTag sepithTag = new CompoundTag();
                for (Map.Entry<String, Integer> entry : getSepith().entrySet()) {
                    sepithTag.putInt(entry.getKey(), entry.getValue());
                }
                quartzData.put("sepith", sepithTag);
            }

            // Store custom traits
            quartzData.putString("custom_trait_1", "enhanced_damage");
            quartzData.putDouble("custom_value_1", 1.5);

            weaponTag.put("applied_quartz_" + getQuartzId(), quartzData);
            weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag));

            System.out.println("Material traits applied successfully");

        } catch (Exception e) {
            System.out.println("Error applying material traits: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void applyQuartzAttributesToWeapon(ItemStack weapon, ItemStack quartz) {
        Map<String, Float> buffs = getWeaponBuffs(quartz); // Pass the actual quartz stack
           if (buffs.isEmpty()) {
            System.out.println("No buffs to apply for " + getQuartzId());
            return;
        }

        System.out.println("Applying buffs: " + buffs);

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // Keep existing modifiers (but remove old quartz modifiers from this specific quartz)
        ItemAttributeModifiers existing = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (existing != null) {
            System.out.println("Existing modifiers count: " + existing.modifiers().size());
            for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
                // Only keep non-quartz modifiers OR quartz modifiers from different quartz
                String modifierId = entry.modifier().id().getPath();
                String currentQuartzId = "quartz_" + getQuartzId();

                if (!modifierId.contains(currentQuartzId)) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                    System.out.println("Kept existing modifier: " + modifierId);
                } else {
                    System.out.println("Removed old modifier from this quartz: " + modifierId);
                }
            }
        }
// Combine all buffs by attribute type first
        Map<String, Float> combinedBuffs = new HashMap<>();
        for (Map.Entry<String, Float> buff : buffs.entrySet()) {
            combinedBuffs.merge(buff.getKey(), buff.getValue(), Float::sum);
        }

// Add new quartz buffs
        int appliedCount = 0;
        for (Map.Entry<String, Float> buff : combinedBuffs.entrySet()) {
            // FIXED: Try multiple attribute locations
            Attribute attribute = null;

            // Try minecraft namespace first
            ResourceLocation minecraftAttr = ResourceLocation.fromNamespaceAndPath("minecraft", buff.getKey());
            attribute = ForgeRegistries.ATTRIBUTES.getValue(minecraftAttr);

            // If not found, try forge namespace
            if (attribute == null) {
                ResourceLocation forgeAttr = ResourceLocation.fromNamespaceAndPath("forge", buff.getKey());
                attribute = ForgeRegistries.ATTRIBUTES.getValue(forgeAttr);
            }

            // Try some common alternative names
            if (attribute == null) {
                String altName = switch (buff.getKey()) {
                    case "attack_damage" -> "generic.attack_damage";
                    case "max_health" -> "generic.max_health";
                    case "armor" -> "generic.armor";
                    case "armor_toughness" -> "generic.armor_toughness";
                    case "knockback_resistance" -> "generic.knockback_resistance";
                    case "movement_speed" -> "generic.movement_speed";
                    case "attack_speed" -> "generic.attack_speed";
                    case "luck" -> "generic.luck";
                    default -> buff.getKey();
                };

                ResourceLocation altAttr = ResourceLocation.fromNamespaceAndPath("minecraft", altName);
                attribute = ForgeRegistries.ATTRIBUTES.getValue(altAttr);
            }

            if (attribute != null) {
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                        KisekiLegend.MOD_ID, "quartz_" + getQuartzId() + "_" + buff.getKey());

                AttributeModifier modifier = new AttributeModifier(
                        modifierId,
                        buff.getValue(),
                        AttributeModifier.Operation.ADD_VALUE
                );

                builder.add(Holder.direct(attribute), modifier, EquipmentSlotGroup.MAINHAND);
                appliedCount++;
                System.out.println("Applied buff: " + buff.getKey() + " = " + buff.getValue() + " (Attribute: " + attribute + ")");
            } else {
                System.out.println("WARNING: Could not find attribute for: " + buff.getKey());

                // List available attributes for debugging
                System.out.println("Available attributes:");
                ForgeRegistries.ATTRIBUTES.getValues().stream()
                        .limit(10) // Limit output
                        .forEach(attr -> System.out.println("  - " + ForgeRegistries.ATTRIBUTES.getKey(attr)));
            }
        }

        weapon.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        System.out.println("Successfully applied " + appliedCount + "/" + buffs.size() + " buffs to weapon");
    }

    public Map<String, Float> getWeaponBuffs() {
        return getBaseBuffsForQuartzType(getQuartzId());
    }

    public String getQuartzId() {
        try {
            var key = ForgeRegistries.ITEMS.getKey(this);
            if (key != null) {
                return key.getPath();
            }
        } catch (Exception e) {
            System.out.println("Error getting quartz ID: " + e.getMessage());
        }

        // Fallback: use class name
        String className = this.getClass().getSimpleName();
        return className.toLowerCase().replace("quartzitem", "").replace("item", "");
    }
}