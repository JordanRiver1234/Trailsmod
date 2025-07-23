package net.JordanRiver.KisekiLegend.items;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SizedItemStackHandler extends ItemStackHandler {

    public SizedItemStackHandler(int size) {
        super(size);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag nbt) {
        ContainerHelper.loadAllItems(nbt, this.stacks, lookup);
        onLoad();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.stacks, lookup);
        return tag;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return stack.getItem() instanceof QuartzItem;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        // Don't modify the stack here - let OrbmentComponent handle sepith data
        super.setStackInSlot(slot, stack);
    }
}