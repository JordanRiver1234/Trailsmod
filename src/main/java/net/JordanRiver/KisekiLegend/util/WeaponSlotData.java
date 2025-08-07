package net.JordanRiver.KisekiLegend.util;

import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotBakedModel;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles weapon/tool slot data for the Orbal Table system
 */
public class WeaponSlotData {
    public static final String NBT_KEY = "orbal_slots";
    public static final int MAX_SLOTS = 3;

    private final List<WeaponSlot> slots = new ArrayList<>();

    public static class WeaponSlot {
        public String elementType;
        public float posX, posY, posZ; // Position on weapon model
        public ItemStack quartzItem = ItemStack.EMPTY;
        public boolean isClosed = false; // If slot hole is closed

        public WeaponSlot(String elementType, float posX, float posY, float posZ) {
            this.elementType = elementType;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        public boolean hasQuartz() {
            return !quartzItem.isEmpty();
        }

        public boolean canInsertQuartz(ItemStack quartz) {


            if (isClosed || hasQuartz()) {
                System.out.println("Cannot insert: slot closed or already has quartz");
                return false;
            }

            if (!(quartz.getItem() instanceof QuartzItem quartzItem)) {
                System.out.println("Cannot insert: not a quartz item");
                return false;
            }

            String quartzElement = quartzItem.getElement();
            System.out.println("Quartz element: " + quartzElement);

            boolean compatible = quartzElement.equalsIgnoreCase(elementType);
            System.out.println("Elements compatible: " + compatible);

            return compatible;
        }

        public boolean insertQuartz(ItemStack quartz) {
            if (!canInsertQuartz(quartz)) return false;
            this.quartzItem = quartz.copy();
            return true;
        }

        public ItemStack removeQuartz() {
            ItemStack result = quartzItem.copy();
            quartzItem = ItemStack.EMPTY;
            return result;
        }

        public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putString("elementType", elementType);
            tag.putFloat("posX", posX);
            tag.putFloat("posY", posY);
            tag.putFloat("posZ", posZ);
            tag.putBoolean("isClosed", isClosed);

            if (!quartzItem.isEmpty()) {


                CompoundTag quartzTag = new CompoundTag();

                // FIXED: Always use manual NBT creation for reliability
                net.minecraft.resources.ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(quartzItem.getItem());
                if (itemId != null) {
                    quartzTag.putString("id", itemId.toString());
                    quartzTag.putInt("count", quartzItem.getCount());

                    if (quartzItem.getDamageValue() > 0) {
                        quartzTag.putInt("damage", quartzItem.getDamageValue());
                    }

                    // Save custom data components
                    net.minecraft.world.item.component.CustomData customData = quartzItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (customData != null && !customData.isEmpty()) {
                        quartzTag.put("custom_data", customData.copyTag());
                    }

                    System.out.println("Manual NBT keys: " + quartzTag.getAllKeys());
                    System.out.println("Final quartz NBT: " + quartzTag);

                    tag.put("quartz", quartzTag);
                    System.out.println("=== QUARTZ SAVED TO NBT SUCCESSFULLY ===");
                } else {
                    System.out.println("ERROR: Could not get item ID for quartz!");
                }
            } else {
                System.out.println("=== NO QUARTZ TO SAVE ===");
            }
        }

        public static WeaponSlot load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            String elementType = tag.getString("elementType");
            float posX = tag.getFloat("posX");
            float posY = tag.getFloat("posY");
            float posZ = tag.getFloat("posZ");

            WeaponSlot slot = new WeaponSlot(elementType, posX, posY, posZ);
            slot.isClosed = tag.getBoolean("isClosed");

            if (tag.contains("quartz")) {
                try {
                    CompoundTag quartzTag = tag.getCompound("quartz");


                    if (quartzTag.contains("id")) {
                        String itemIdString = quartzTag.getString("id");

                        net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.parse(itemIdString);
                        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);

                        if (item != null) {
                            int count = quartzTag.getInt("count");
                            slot.quartzItem = new ItemStack(item, count);

                            if (quartzTag.contains("damage")) {
                                slot.quartzItem.setDamageValue(quartzTag.getInt("damage"));
                            }

                            if (quartzTag.contains("custom_data")) {
                                slot.quartzItem.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                        net.minecraft.world.item.component.CustomData.of(quartzTag.getCompound("custom_data")));
                            }

                        } else {
                            System.out.println("ERROR: Could not find item for ID: " + itemIdString);
                            slot.quartzItem = ItemStack.EMPTY;
                        }
                    } else {
                        System.out.println("No 'id' tag found in quartz NBT");
                        slot.quartzItem = ItemStack.EMPTY;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to load quartz from NBT: " + e.getMessage());
                    e.printStackTrace();
                    slot.quartzItem = ItemStack.EMPTY;
                }
            } else {
            }

            return slot;
        }
    }

    public static WeaponSlotData getOrCreate(ItemStack weapon) {
        CustomData customData = weapon.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains(NBT_KEY)) {
            // We need to get registry access from somewhere - for now use a fallback
            net.minecraft.core.HolderLookup.Provider registries = net.minecraft.client.Minecraft.getInstance().level != null
                    ? net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                    : net.minecraft.core.RegistryAccess.EMPTY;
            return load(customData.copyTag().getCompound(NBT_KEY), registries);
        }
        return new WeaponSlotData();
    }
    public static void save(ItemStack weapon, WeaponSlotData data) {
        CompoundTag weaponTag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag slotTag = new CompoundTag();

        // Get proper registry access
        net.minecraft.core.HolderLookup.Provider registries;
        if (Minecraft.getInstance().level != null) {
            registries = Minecraft.getInstance().level.registryAccess();
        } else {
            // Fallback for server-side
            registries = net.minecraft.core.RegistryAccess.EMPTY;
        }

        data.save(slotTag, registries);
        weaponTag.put(NBT_KEY, slotTag);
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag));

        // FIXED: Force cache invalidation when data changes
        WeaponSlotBakedModel.invalidateWeaponModel(weapon);
    }

    public boolean addSlot(float[] position, String elementType) {
        if (getActiveSlotCount() >= MAX_SLOTS) return false;

        // Ensure proper 3D positioning with slight randomization to avoid overlaps
        float adjustedZ = position[2];
        if (Math.abs(adjustedZ) < 0.001f) {
            // Add slight Z variation if position is too flat
            adjustedZ = 0.05f + ((float)Math.random() - 0.5f) * 0.02f;
        }

        // Check for reusable closed slots first
        for (int i = 0; i < slots.size(); i++) {
            WeaponSlot slot = slots.get(i);
            if (slot.isClosed) {
                slot.elementType = elementType;
                slot.posX = position[0];
                slot.posY = position[1];
                slot.posZ = adjustedZ;
                slot.isClosed = false;
                slot.quartzItem = ItemStack.EMPTY;
                System.out.println("Reused closed slot at 3D position: " + position[0] + ", " + position[1] + ", " + adjustedZ);
                return true;
            }
        }

        // Add new slot with proper 3D positioning
        if (slots.size() < MAX_SLOTS) {
            slots.add(new WeaponSlot(elementType, position[0], position[1], adjustedZ));
            System.out.println("Added new 3D slot at position: " + position[0] + ", " + position[1] + ", " + adjustedZ);
            return true;
        }

        return false;
    }
    public void debugSlots() {


        for (int i = 0; i < slots.size(); i++) {
            WeaponSlot slot = slots.get(i);

        }
    }

    public List<Integer> getActiveSlotIndices() {
        List<Integer> activeIndices = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).isClosed) {
                activeIndices.add(i);
            }
        }
        return activeIndices;
    }

    public boolean removeSlot(int index) {
        if (index < 0 || index >= slots.size()) return false;

        // FIXED: Don't actually remove from list - just mark as closed
        // This preserves slot indices and prevents numbering issues
        WeaponSlot slot = slots.get(index);
        slot.isClosed = true;
        slot.quartzItem = ItemStack.EMPTY; // Remove any quartz

        System.out.println("Slot " + index + " marked as closed (not removed from list)");
        return true;
    }
    public void compactSlots() {
        // Only call this when you want to permanently remove closed slots
        slots.removeIf(slot -> slot.isClosed);
        System.out.println("Compacted slots - removed all closed slots");
    }

    public boolean changeSlotElement(int index, String newElementType) {
        if (index < 0 || index >= slots.size()) return false;

        WeaponSlot slot = slots.get(index);
        if (slot.hasQuartz()) return false; // Can't change element with quartz inserted

        slot.elementType = newElementType;
        return true;
    }

    public boolean closeSlot(int index) {
        if (index < 0 || index >= slots.size()) return false;

        WeaponSlot slot = slots.get(index);
        slot.isClosed = true;
        slot.quartzItem = ItemStack.EMPTY; // Remove any quartz when closing
        return true;
    }

    public boolean openSlot(int index) {
        if (index < 0 || index >= slots.size()) return false;

        slots.get(index).isClosed = false;
        return true;
    }

    public WeaponSlot getSlot(int index) {
        if (index < 0 || index >= slots.size()) return null;
        return slots.get(index);
    }

    public List<WeaponSlot> getSlots() {
        return new ArrayList<>(slots);
    }

    public int getSlotCount() {
        // FIXED: Count only non-closed slots for display purposes
        return getActiveSlotCount();
    }
    public int getTotalSlotCount() {
        // If you need the actual list size including closed slots
        return slots.size();
    }
    public int getNextAvailableSlotIndex() {
        // Find first closed slot that can be reopened, or return next new index
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isClosed) {
                return i; // Reuse closed slot
            }
        }
        return slots.size(); // Add new slot at end
    }
    public int getActiveSlotCount() {
        return (int) slots.stream().filter(slot -> !slot.isClosed).count();
    }

    public boolean canInsertQuartz(ItemStack quartz) {
        return slots.stream().anyMatch(slot -> slot.canInsertQuartz(quartz));
    }

    public boolean insertQuartz(ItemStack quartz) {
        System.out.println("=== INSERTING QUARTZ INTO WEAPON SLOTS ===");
        System.out.println("Quartz: " + (quartz.isEmpty() ? "EMPTY" : quartz.getDisplayName().getString()));
        System.out.println("Available slots: " + slots.size());

        for (int i = 0; i < slots.size(); i++) {
            WeaponSlot slot = slots.get(i);
            System.out.println("Checking slot " + i + ": element=" + slot.elementType + ", closed=" + slot.isClosed + ", hasQuartz=" + slot.hasQuartz());

            if (slot.canInsertQuartz(quartz)) {
                System.out.println("Slot " + i + " can accept quartz, inserting...");
                boolean success = slot.insertQuartz(quartz);
                System.out.println("Insert result: " + success);
                if (success) {
                    System.out.println("Quartz successfully inserted into slot " + i);
                    // FIXED: Invalidate cache immediately after insertion
                    System.out.println("=== FORCING MODEL REFRESH AFTER QUARTZ INSERTION ===");
                    return true;
                }
            }
        }

        System.out.println("No suitable slot found for quartz");
        return false;
    }

    public ItemStack removeQuartzFromSlot(int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        return slots.get(index).removeQuartz();
    }

    // Get combined sepith values from all inserted quartz (for buffs)
    public java.util.Map<String, Integer> getCombinedSepith() {
        java.util.Map<String, Integer> combined = new java.util.HashMap<>();

        for (WeaponSlot slot : slots) {
            if (slot.hasQuartz() && slot.quartzItem.getItem() instanceof QuartzItem quartzItem) {
                quartzItem.getSepith().forEach((element, value) ->
                        combined.merge(element, value, Integer::sum));
            }
        }

        return combined;
    }

    // Get positions for rendering slot holes
    public List<float[]> getSlotPositions() {
        List<float[]> positions = new ArrayList<>();
        for (WeaponSlot slot : slots) {
            if (!slot.isClosed) {
                positions.add(new float[]{slot.posX, slot.posY, slot.posZ});
            }
        }
        return positions;
    }
    public static void clearAllWeaponCaches() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getItemRenderer() != null) {
                // Force a complete model refresh
                mc.getItemRenderer().getItemModelShaper().getModelManager();
            }
        } catch (Exception e) {
            System.out.println("Error clearing caches: " + e.getMessage());
        }
    }
    public List<RenderSlotInfo> getRenderInfo() {
        List<RenderSlotInfo> info = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            WeaponSlot slot = slots.get(i);
            if (!slot.isClosed) {
                info.add(new RenderSlotInfo(
                        slot.posX, slot.posY, slot.posZ,
                        slot.elementType, slot.quartzItem, i // Use actual list index, not visual index
                ));
            }
        }
        return info;
    }
    public static class RenderSlotInfo {
        public final float posX, posY, posZ;
        public final String elementType;
        public final ItemStack quartzItem;
        public final int slotIndex;

        public RenderSlotInfo(float posX, float posY, float posZ, String elementType, ItemStack quartzItem, int slotIndex) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.elementType = elementType;
            this.quartzItem = quartzItem;
            this.slotIndex = slotIndex;
        }

        public boolean hasQuartz() {
            return !quartzItem.isEmpty();
        }
    }


    private void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag slotsTag = new ListTag();
        for (WeaponSlot slot : slots) {
            CompoundTag slotTag = new CompoundTag();
            slot.save(slotTag, registries);
            slotsTag.add(slotTag);
        }
        tag.put("slots", slotsTag);
    }
    public static void forceModelRefresh(ItemStack weapon) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            // Force the item model to be re-evaluated
            Minecraft.getInstance().getItemRenderer().getModel(weapon,
                    Minecraft.getInstance().level,
                    Minecraft.getInstance().player, 0);
        }
    }
    private static WeaponSlotData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        WeaponSlotData data = new WeaponSlotData();

        if (tag.contains("slots")) {
            ListTag slotsTag = tag.getList("slots", 10); // 10 = CompoundTag
            for (int i = 0; i < slotsTag.size(); i++) {
                CompoundTag slotTag = slotsTag.getCompound(i);
                data.slots.add(WeaponSlot.load(slotTag, registries));
            }
        }

        return data;
    }
}