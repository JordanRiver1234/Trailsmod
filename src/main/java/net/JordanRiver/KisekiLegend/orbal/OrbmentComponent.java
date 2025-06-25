package net.JordanRiver.KisekiLegend.orbal;

import com.mojang.serialization.DataResult;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

public class OrbmentComponent implements INBTSerializable<CompoundTag> {
    public static final int MAX_SLOTS = 6;

    private int unlockedSlots = 1;
    private final int[] sepith = new int[7];
    private final SizedItemStackHandler inventory = new SizedItemStackHandler(MAX_SLOTS);

    private static final Map<String, Integer> ELEMENT_INDEX = Map.of(
            "earth", 0, "water", 1, "wind", 2,
            "fire", 3,  "space", 4, "mirage", 5, "time", 6
    );

    public boolean insertQuartz(int slot, ItemStack stack) {
        if (slot < unlockedSlots && slot < MAX_SLOTS && !stack.isEmpty()) {
            inventory.setStackInSlot(slot, stack.copy());
            updateSepithCounts();
            return true;
        }
        return false;
    }

    public void removeQuartz(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            updateSepithCounts();
        }
    }

    public void unlockSlot() {
        if (unlockedSlots < MAX_SLOTS) unlockedSlots++;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int count) {
        this.unlockedSlots = Math.min(count, MAX_SLOTS);
    }

    public SizedItemStackHandler getInventory() {
        return inventory;
    }

    public int[] getSepithCounts() {
        return sepith;
    }

    public void recalculate() {
        updateSepithCounts();
    }

    private void updateSepithCounts() {
        for (int i = 0; i < sepith.length; i++) sepith[i] = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null) {
                    CompoundTag tag = data.copyTag();
                    for (var e : ELEMENT_INDEX.entrySet()) {
                        if (tag.contains(e.getKey(), Tag.TAG_INT)) {
                            sepith[e.getValue()] += tag.getInt(e.getKey());
                        }
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("UnlockedSlots", unlockedSlots);

        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // encode with CODEC, throwing if it fails
                CompoundTag encoded = (CompoundTag) ItemStack.CODEC
                        .encodeStart(NbtOps.INSTANCE, stack)
                        .getOrThrow(err -> new RuntimeException("Failed to encode ItemStack: " + err));

                CompoundTag wrapper = new CompoundTag();
                wrapper.put("stack", encoded);
                slotsTag.put("slot" + i, wrapper);
            }
        }

        root.put("QuartzSlots", slotsTag);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        this.unlockedSlots = root.getInt("UnlockedSlots");

        if (root.contains("QuartzSlots", Tag.TAG_COMPOUND)) {
            CompoundTag slotsTag = root.getCompound("QuartzSlots");
            for (String key : slotsTag.getAllKeys()) {
                if (key.startsWith("slot")) {
                    try {
                        int idx = Integer.parseInt(key.substring(4));
                        CompoundTag wrapper = slotsTag.getCompound(key);
                        CompoundTag encoded = wrapper.getCompound("stack");

                        ItemStack stack = ItemStack.CODEC
                                .parse(NbtOps.INSTANCE, encoded)
                                .getOrThrow(err -> new RuntimeException("Failed to decode ItemStack: " + err));

                        inventory.setStackInSlot(idx, stack);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        updateSepithCounts();
    }

    /**
     * Completely replace this component’s internal handler with another one.
     */
    public void setInventory(SizedItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            this.inventory.setStackInSlot(i, handler.getStackInSlot(i).copy());
        }
        updateSepithCounts();
    }
}