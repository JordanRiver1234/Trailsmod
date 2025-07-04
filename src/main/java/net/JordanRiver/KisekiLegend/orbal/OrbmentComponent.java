package net.JordanRiver.KisekiLegend.orbal;

import com.mojang.serialization.DataResult;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;
import net.JordanRiver.KisekiLegend.quartz.QuartzDefinition;
import net.JordanRiver.KisekiLegend.quartz.QuartzRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

public class OrbmentComponent implements INBTSerializable<CompoundTag> {
    public static final Capability<OrbmentComponent> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final int MAX_SLOTS = 6;

    private int unlockedSlots = 1;
    private final int[] sepith = new int[7];
    private final SizedItemStackHandler inventory = new SizedItemStackHandler(MAX_SLOTS);

    public static final Map<String, Integer> ELEMENT_INDEX = Map.of(
            "earth", 0, "water", 1, "wind", 2,
            "fire", 3, "space", 4, "mirage", 5, "time", 6
    );

    public boolean hasQuartz(String quartzId) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof QuartzItem qi &&
                    quartzId.equals(qi.getQuartzId())) {
                return true;
            }
        }
        return false;
    }

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

    public void setUnlockedSlots(int c) {
        unlockedSlots = Math.min(c, MAX_SLOTS);
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
        Arrays.fill(sepith, 0);
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

    public void tickBuffs(Player player) {
        for (QuartzDefinition def : QuartzRegistry.all()) {
            Holder<MobEffect> h = def.getSelfBuffHolder();
            if (h != null) {
                player.removeEffect(h);
            }
        }

        Set<String> slotted = new HashSet<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof QuartzItem qi) {
                slotted.add(qi.getQuartzId());
            }
        }

        for (String id : slotted) {
            QuartzDefinition def = QuartzRegistry.get(id);
            if (def != null) {
                def.applySelfBuff(player);
            }
        }
    }


    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("UnlockedSlots", unlockedSlots);
        CompoundTag slots = new CompoundTag();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) {
                Tag rawTag = ItemStack.CODEC
                        .encodeStart(NbtOps.INSTANCE, s)
                        .getOrThrow(err -> new RuntimeException(err));
                CompoundTag enc = (CompoundTag) rawTag;
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("stack", enc);
                slots.put("slot" + i, wrapper);
            }
        }
        root.put("QuartzSlots", slots);
        return root;
    }


    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        unlockedSlots = root.getInt("UnlockedSlots");
        if (root.contains("QuartzSlots", Tag.TAG_COMPOUND)) {
            CompoundTag slots = root.getCompound("QuartzSlots");
            for (String k : slots.getAllKeys()) {
                if (k.startsWith("slot")) {
                    int idx = Integer.parseInt(k.substring(4));
                    CompoundTag wrapper = slots.getCompound(k);
                    CompoundTag enc = wrapper.getCompound("stack");
                    ItemStack s = ItemStack.CODEC
                            .parse(NbtOps.INSTANCE, enc)
                            .getOrThrow(err -> new RuntimeException(err));
                    inventory.setStackInSlot(idx, s);
                }
            }
        }
        updateSepithCounts();
    }

    public void setInventory(SizedItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            inventory.setStackInSlot(i, handler.getStackInSlot(i).copy());
        }
        updateSepithCounts();
    }
}
