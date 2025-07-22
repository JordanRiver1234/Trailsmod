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
    public static final int MAX_FAVORITES = 6;

    public static final int SEPITH_MATCH_BONUS = 3;

    private static final int BASE_MAX_EP = 270;
    private int currentEP = BASE_MAX_EP;

    public int getCurrentEP() { return this.currentEP; }
    public int getMaxEP() { return BASE_MAX_EP + getUnlockedSlots() * 30; }
    public boolean useEP(int amount) {
        if (amount <= this.currentEP) {
            this.currentEP -= amount;
            return true;
        }
        return false;
    }
    public void regenerateEP() { this.currentEP = Math.min(getMaxEP(), this.currentEP + 1); }
    public void fillToMaxEP() { this.currentEP = getMaxEP(); }
    public void setCurrentEP(int ep) { this.currentEP = Math.min(ep, getMaxEP()); }


    private final boolean[] unlockedStatus = new boolean[MAX_SLOTS];
    private final Element[] sepithLines = new Element[MAX_SLOTS];
    private boolean linesInitialized = false;

    private final String[] favoriteArts = new String[MAX_FAVORITES];
    private String lastSelectedArtName = "";

    private final int[] sepith = new int[7];
    private final SizedItemStackHandler inventory = new SizedItemStackHandler(MAX_SLOTS);

    public static final Map<String, Integer> ELEMENT_INDEX = Map.of(
            "earth", 0, "water", 1, "wind", 2,
            "fire", 3, "space", 4, "mirage", 5, "time", 6
    );

    public OrbmentComponent() {
        Arrays.fill(sepithLines, Element.NONE);
        Arrays.fill(unlockedStatus, false);
        Arrays.fill(favoriteArts, "");
        unlockedStatus[0] = true;
    }

    public String getFavorite(int index) {
        if (index >= 0 && index < MAX_FAVORITES) {
            return favoriteArts[index];
        }
        return "";
    }
    public void setFavorite(int index, String artName) {
        if (index >= 0 && index < MAX_FAVORITES) {
            favoriteArts[index] = artName == null ? "" : artName;
            markDirty(); // Ensure persistence
        }
    }

    public String getLastSelectedArtName() {
        return this.lastSelectedArtName == null ? "" : this.lastSelectedArtName;
    }

    public void setLastSelectedArtName(String artName) {
        this.lastSelectedArtName = artName == null ? "" : artName;
        // Force save when setting last selected art
        this.markDirty();
    }
    public void setSelectedArt(String artName) {
        setLastSelectedArtName(artName);
    }

    public String getSelectedArt() {
        return getLastSelectedArtName();
    }

    private boolean isDirty = false;
    public void markDirty() { this.isDirty = true; }
    public boolean isDirty() { return this.isDirty; }
    public void clearDirty() { this.isDirty = false; }
    public boolean isArtAvailable(String artName) {
        ArtsRegistry.ArtDefinition artDef = ArtsRegistry.ALL_ARTS.stream()
                .filter(art -> art.name().equals(artName))
                .findFirst()
                .orElse(null);        if (artDef == null) return false;

        return artDef.elementCost().entrySet().stream()
                .allMatch(e -> getSepithCounts()[ELEMENT_INDEX.get(e.getKey())] >= e.getValue());
    }

    public List<String> getAvailableFavorites() {
        return Arrays.stream(favoriteArts)
                .filter(art -> !art.isEmpty() && isArtAvailable(art))
                .collect(Collectors.toList());
    }
    public boolean hasQuartz(String quartzId) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof QuartzItem qi && quartzId.equals(qi.getQuartzId())) {
                return true;
            }
        }
        return false;
    }
    public void syncArtSelection(String newArtName) {
        if (isArtAvailable(newArtName)) {
            setLastSelectedArtName(newArtName);
            markDirty();
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

    public boolean[] getUnlockedStatus() { return unlockedStatus; }
    public SizedItemStackHandler getInventory() { return inventory; }
    public int[] getSepithCounts() { return sepith; }
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
    public float getArtDamageMultiplier(Element artElement) {
        if (artElement == Element.NONE) return 1.0f;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (sepithLines[i] == artElement) {
                if (!stack.isEmpty() && stack.getItem() instanceof QuartzItem) {
                    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                    if (data != null && data.copyTag().getInt(artElement.getName()) > 0) {
                        return 1.2f;
                    }
                }
            }
        }
        return 1.0f;
    }
    public void tickBuffs(Player player) {
        for (QuartzDefinition def : QuartzRegistry.all()) {
            Holder<MobEffect> h = def.getSelfBuffHolder();
            if (h != null) player.removeEffect(h);
        }
        Set<String> slotted = new HashSet<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() instanceof QuartzItem qi) slotted.add(qi.getQuartzId());
        }
        for (String id : slotted) {
            QuartzDefinition def = QuartzRegistry.get(id);
            if (def != null) def.applySelfBuff(player);
        }
    }
    public Element[] getSepithLines() { return sepithLines; }
    public boolean areLinesInitialized() { return linesInitialized; }

    public void initializeLines() {
        if (linesInitialized) return;
        Arrays.fill(sepithLines, Element.NONE);
        int lineCount = ThreadLocalRandom.current().nextInt(1, 4);
        List<Integer> availableSlots = IntStream.range(0, MAX_SLOTS).boxed().collect(Collectors.toList());
        Collections.shuffle(availableSlots);
        List<Element> elements = new ArrayList<>(Arrays.asList(Element.values()));
        elements.remove(Element.NONE);
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
            if (!inventory.getStackInSlot(slot).isEmpty()) return;
            this.sepithLines[slot] = element;
            updateSepithCounts();
        }
    }
    public void removeSepithLine(int slot) { setSepithLine(slot, Element.NONE); }
    public boolean isQuartzValidForSlot(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof QuartzItem quartzItem)) return false;
        Element lineElement = this.sepithLines[slot];
        if (lineElement == Element.NONE) return true;
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
        for (Element line : sepithLines) linesTag.add(StringTag.valueOf(line.name()));
        root.put("SepithLines", linesTag);

        ListTag favsTag = new ListTag();
        for (String fav : favoriteArts) favsTag.add(StringTag.valueOf(fav));
        root.put("FavoriteArts", favsTag);
        root.putString("LastSelectedArt", this.lastSelectedArtName);

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
        }

        if (root.contains("SepithLines", Tag.TAG_LIST)) {
            ListTag linesTag = root.getList("SepithLines", Tag.TAG_STRING);
            // First fill all slots with NONE
            Arrays.fill(sepithLines, Element.NONE);
            // Then restore the saved values
            for (int i = 0; i < Math.min(linesTag.size(), MAX_SLOTS); i++) {
                try {
                    sepithLines[i] = Element.valueOf(linesTag.getString(i));
                } catch (IllegalArgumentException e) {
                    sepithLines[i] = Element.NONE;
                }
            }
        }

        if (root.contains("FavoriteArts", Tag.TAG_LIST)) {
            ListTag favsTag = root.getList("FavoriteArts", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(favsTag.size(), MAX_FAVORITES); i++) {
                favoriteArts[i] = favsTag.getString(i);
            }
        }
        if (root.contains("LastSelectedArt", Tag.TAG_STRING)) {
            this.lastSelectedArtName = root.getString("LastSelectedArt");
        }

        if (root.contains("QuartzSlots", Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(provider, root.getCompound("QuartzSlots"));
        }
        updateSepithCounts();
    }
}