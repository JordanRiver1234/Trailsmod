package net.JordanRiver.KisekiLegend.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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

import java.util.List;
import java.util.Map;

public class QuartzItem extends Item {
    private final String element;
    private final Map<String, Integer> sepith;

    public QuartzItem(String element, Map<String, Integer> sepith, Properties properties) {
        super(properties.stacksTo(1));
        this.element = element;
        this.sepith = sepith;
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
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag quartzData = data.copyTag();
            if (quartzData.contains("Quality")) {
                tooltip.add(Component.literal("Quality: " + quartzData.getInt("Quality")).withStyle(ChatFormatting.YELLOW));
            }
            ListTag effects = quartzData.getList("Effects", 8);
            if (!effects.isEmpty()) {
                tooltip.add(Component.literal("Effects:").withStyle(ChatFormatting.AQUA));
                for (int i = 0; i < effects.size(); i++) {
                    tooltip.add(Component.literal(" - " + effects.getString(i)).withStyle(ChatFormatting.BLUE));
                }
            }
        }
    }

    public String getQuartzId() {
        var key = ForgeRegistries.ITEMS.getKey(this);
        return key == null ? "" : key.getPath();
    }
}