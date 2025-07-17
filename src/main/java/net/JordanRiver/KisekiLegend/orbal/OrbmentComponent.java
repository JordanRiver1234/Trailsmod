package net.JordanRiver.KisekiLegend.orbal;

import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;
import net.JordanRiver.KisekiLegend.quartz.QuartzDefinition;
import net.JordanRiver.KisekiLegend.quartz.QuartzRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrbmentComponent implements INBTSerializable<CompoundTag> {
    public static final Capability<OrbmentComponent> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final int MAX_SLOTS = 6;
    public static final int SEPITH_MATCH_BONUS = 3; // The flat bonus for matching a quartz to a line

    // ---- EP system ----
    private static final int BASE_MAX_EP = 270;
    private int currentEP = BASE_MAX_EP;

    public int getCurrentEP() {
        return this.currentEP;
    }

    public int getMaxEP() {
        return BASE_MAX_EP + getUnlockedSlots() * 30;
    }

    public boolean useEP(int amount) {
        if (amount <= this.currentEP) {
            this.currentEP -= amount;
            return true;
        }
        return false;
    }

    public void regenerateEP() {
        this.currentEP = Math.min(getMaxEP(), this.currentEP + 1);
    }
    public void fillToMaxEP() {
        this.currentEP = getMaxEP();
    }
    public void setCurrentEP(int ep) {
        this.currentEP = Math.min(ep, getMaxEP());
    }

    // --------------------

    private final boolean[] unlockedStatus = new boolean[MAX_SLOTS];
    private final Element[] sepithLines = new Element[MAX_SLOTS];
    private boolean linesInitialized = false;

    private final int[] sepith = new int[7];
    private final SizedItemStackHandler inventory = new SizedItemStackHandler(MAX_SLOTS);

    public static final Map<String, Integer> ELEMENT_INDEX = Map.of(
            "earth", 0, "water", 1, "wind", 2,
            "fire", 3, "space", 4, "mirage", 5, "time", 6
    );

    public OrbmentComponent() {
        Arrays.fill(sepithLines, Element.NONE);
        Arrays.fill(unlockedStatus, false);
        unlockedStatus[0] = true; // First slot is always unlocked by default
    }

    public boolean hasQuartz(String quartzId) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof QuartzItem qi
                    && quartzId.equals(qi.getQuartzId())) {
                return true;
            }
        }
        return false;
    }

    public boolean insertQuartz(int slot, ItemStack stack) {
        if (isSlotUnlocked(slot) && slot < MAX_SLOTS && !stack.isEmpty() && isQuartzValidForSlot(slot, stack)) {
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

    public void unlockSlot(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            unlockedStatus[slot] = true;
        }
    }

    public int getUnlockedSlots() {
        int count = 0;
        for (boolean b : unlockedStatus) {
            if (b) count++;
        }
        return count;
    }

    public boolean isSlotUnlocked(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) return false;
        return unlockedStatus[slot];
    }

    public boolean[] getUnlockedStatus() {
        return unlockedStatus;
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
            if (!stack.isEmpty() && stack.getItem() instanceof QuartzItem) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null) {
                    CompoundTag tag = data.copyTag();
                    Element lineElement = sepithLines[i];

                    for (var e : ELEMENT_INDEX.entrySet()) {
                        String elementName = e.getKey();
                        if (tag.contains(elementName, Tag.TAG_INT)) {
                            int value = tag.getInt(elementName);

                            // --- SEPITH BONUS LOGIC ---
                            // If the slot's line color matches the element being checked, add a flat bonus.
                            if (lineElement != Element.NONE && lineElement.getName().equalsIgnoreCase(elementName)) {
                                value += SEPITH_MATCH_BONUS;
                            }
                            sepith[e.getValue()] += value;
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates the damage multiplier for a given art based on slotted quartz.
     * @param artElement The element of the art being cast.
     * @return 1.2f for a 20% bonus, otherwise 1.0f for normal damage.
     */
    public float getArtDamageMultiplier(Element artElement) {
        if (artElement == Element.NONE) {
            return 1.0f; // No bonus for non-elemental arts
        }

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            // Check if the slot line matches the art's element
            if (sepithLines[i] == artElement) {
                if (!stack.isEmpty() && stack.getItem() instanceof QuartzItem) {
                    // Check if the quartz itself provides the matching element
                    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                    if (data != null && data.copyTag().getInt(artElement.getName()) > 0) {
                        return 1.2f; // 20% damage bonus!
                    }
                }
            }
        }

        return 1.0f; // No matching quartz found in a matching line
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

    // --- Sepith Line Methods ---

    public Element[] getSepithLines() {
        return sepithLines;
    }

    public boolean areLinesInitialized() {
        return linesInitialized;
    }

    public void initializeLines() {
        if (linesInitialized) return;

        Arrays.fill(sepithLines, Element.NONE);
        int lineCount = ThreadLocalRandom.current().nextInt(1, 4); // 1-3 lines
        List<Integer> availableSlots = IntStream.range(0, MAX_SLOTS).boxed().collect(Collectors.toList());
        Collections.shuffle(availableSlots);

        List<Element> elements = new ArrayList<>(Arrays.asList(Element.values()));
        elements.remove(Element.NONE); // Don't assign NONE randomly
        Collections.shuffle(elements);

        for (int i = 0; i < lineCount; i++) {
            int slot = availableSlots.get(i);
            Element element = elements.get(i % elements.size());
            this.sepithLines[slot] = element;
        }
        this.linesInitialized = true;
    }

    public void setSepithLine(int slot, Element element) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return;
            }
            this.sepithLines[slot] = element;
            updateSepithCounts();
        }
    }

    public void removeSepithLine(int slot) {
        setSepithLine(slot, Element.NONE);
    }

    public boolean isQuartzValidForSlot(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof QuartzItem quartzItem)) {
            return false;
        }

        Element lineElement = this.sepithLines[slot];
        if (lineElement == Element.NONE) {
            return true; // Neutral slots accept any quartz
        }

        // Check if the quartz's primary element matches the line's element
        String quartzElement = quartzItem.getElement().toLowerCase();
        String lineElementName = lineElement.getName().toLowerCase();

        return quartzElement.equals(lineElementName);
    }


    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("CurrentEP", this.currentEP);
        root.putBoolean("LinesInitialized", this.linesInitialized);

        byte[] unlockedBytes = new byte[MAX_SLOTS];
        for(int i=0; i<MAX_SLOTS; i++) unlockedBytes[i] = (byte) (unlockedStatus[i] ? 1 : 0);
        root.putByteArray("UnlockedStatus", unlockedBytes);

        ListTag linesTag = new ListTag();
        for (Element line : sepithLines) {
            linesTag.add(StringTag.valueOf(line.name()));
        }
        root.put("SepithLines", linesTag);

        root.put("QuartzSlots", inventory.serializeNBT(provider));
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        this.currentEP = root.getInt("CurrentEP");
        this.linesInitialized = root.getBoolean("LinesInitialized");

        if (root.contains("UnlockedStatus", Tag.TAG_BYTE_ARRAY)) {
            byte[] unlockedBytes = root.getByteArray("UnlockedStatus");
            for(int i=0; i<Math.min(unlockedBytes.length, MAX_SLOTS); i++) {
                unlockedStatus[i] = unlockedBytes[i] == 1;
            }
        } else if (root.contains("UnlockedSlots", Tag.TAG_INT)) { // Backwards compatibility
            int unlockedCount = root.getInt("UnlockedSlots");
            Arrays.fill(unlockedStatus, false);
            for (int i = 0; i < Math.min(unlockedCount, MAX_SLOTS); i++) {
                unlockedStatus[i] = true;
            }
        }


        if (root.contains("SepithLines", Tag.TAG_LIST)) {
            ListTag linesTag = root.getList("SepithLines", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(linesTag.size(), MAX_SLOTS); i++) {
                try {
                    sepithLines[i] = Element.valueOf(linesTag.getString(i));
                } catch (IllegalArgumentException e) {
                    sepithLines[i] = Element.NONE; // Fallback for invalid names
                }
            }
        }

        if (root.contains("QuartzSlots", Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(provider, root.getCompound("QuartzSlots"));
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