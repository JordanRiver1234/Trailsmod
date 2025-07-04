package net.JordanRiver.KisekiLegend.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ElementalMassItem extends Item {
    private final String elementKey;

    public ElementalMassItem(String elementKey, Properties props) {
        super(props);
        this.elementKey = elementKey;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        applyDefaultTag(stack);
        return stack;
    }

    public static void setAmount(ItemStack stack, String key, int amount) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void applyDefaultTag(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(this.elementKey, 1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public void ensureNBT(ItemStack stack) {
        CompoundTag tag;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        } else {
            tag = new CompoundTag();
        }
        if (!tag.contains(this.elementKey) || tag.getInt(this.elementKey) <= 0) {
            tag.putInt(this.elementKey, 1);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ensureNBT(stack);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        ensureNBT(stack);
    }
}
