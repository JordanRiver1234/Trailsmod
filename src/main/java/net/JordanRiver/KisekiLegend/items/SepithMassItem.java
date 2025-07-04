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

public class SepithMassItem extends Item {
    public SepithMassItem(Properties props) {
        super(props);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        applyDefaultTag(stack);
        return stack;
    }

    public static void setAmount(ItemStack stack, int amount, String element) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putInt("amount", amount);
        tag.putString("element", element);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void applyDefaultTag(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("amount", 1);
        tag.putString("element", "mixed");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void ensureNBT(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        boolean dirty = false;

        if (!tag.contains("amount") || tag.getInt("amount") <= 0) {
            tag.putInt("amount", 1);
            dirty = true;
        }
        if (!tag.contains("element")) {
            tag.putString("element", "mixed");
            dirty = true;
        }

        if (dirty || data == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
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
