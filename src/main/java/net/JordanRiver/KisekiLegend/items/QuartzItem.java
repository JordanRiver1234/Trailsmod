package net.JordanRiver.KisekiLegend.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

public class QuartzItem extends Item {
    private final String element;
    private final Map<String, Integer> sepith;
    private final String bonus;
    private final String effect;



    public QuartzItem(String element, Map<String, Integer> sepith, String bonus, String effect, Properties properties) {
        super(properties.stacksTo(10));
        this.element = element;
        this.sepith = sepith;
        this.bonus = bonus;
        this.effect = effect;
    }

    public String getElement() {
        return element;
    }

    public Map<String, Integer> getSepith() {
        return sepith;
    }

    public String getBonus() {
        return bonus;
    }

    public String getEffect() {
        return effect;
    }


    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Element: " + element).withStyle(ChatFormatting.GOLD));
        if (!bonus.isEmpty()) {
            tooltip.add(Component.literal("Bonus: " + bonus).withStyle(ChatFormatting.AQUA));
        }
        if (!effect.isEmpty()) {
            tooltip.add(Component.literal("Effect: " + effect).withStyle(ChatFormatting.BLUE));
        }
        if (!sepith.isEmpty()) {
            tooltip.add(Component.literal("Sepith Cost:").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, Integer> entry : sepith.entrySet()) {
                tooltip.add(Component.literal(" - " + entry.getKey() + ": " + entry.getValue()).withStyle(ChatFormatting.DARK_GREEN));
            }
        }
    }


    public String getQuartzId() {
        var key = ForgeRegistries.ITEMS.getKey(this);
        return key == null ? "" : key.getPath();
    }
}
