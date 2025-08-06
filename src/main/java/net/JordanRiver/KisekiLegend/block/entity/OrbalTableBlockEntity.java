package net.JordanRiver.KisekiLegend.block.entity;

import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotRenderer;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.menu.OrbalTableMenu;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.OrbalTableRenderUpdatePacket;
import net.JordanRiver.KisekiLegend.network.OrbalTableSyncPacket;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotBakedModel;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrbalTableBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final net.minecraftforge.client.model.data.ModelProperty<ItemStack> WEAPON_PROPERTY =
            new net.minecraftforge.client.model.data.ModelProperty<>();
    public static final net.minecraftforge.client.model.data.ModelProperty<Boolean> MONITOR_OPEN_PROPERTY =
            new net.minecraftforge.client.model.data.ModelProperty<>();
    private ItemStack storedWeapon = ItemStack.EMPTY;
    private ItemStack clientWeapon = ItemStack.EMPTY; // Client-side weapon storage
    private int monitorOpenTicks = 0; // Track how long monitor has been open

    private final ItemStackHandler inventory = new ItemStackHandler(9) { // Changed from 8 to 9
        private boolean isUpdating = false;

        @Override
        protected void onContentsChanged(int slot) {
            if (isUpdating) return;

            isUpdating = true;
            try {
                setChanged();

                if (level != null && !level.isClientSide()) {
                    if (slot == 0) { // Weapon slot
                        ItemStack slotStack = getStackInSlot(slot);
                        boolean hasWeaponNow = !slotStack.isEmpty() && OrbalTableBlockEntity.this.isWeaponOrTool(slotStack);

                        if (hasWeaponNow) {
                            OrbalTableBlockEntity.this.storedWeapon = slotStack.copy();
                            OrbalTableBlockEntity.this.setMonitorOpen(true);
                            OrbalTableBlockEntity.this.syncToClients();
                        } else {
                            OrbalTableBlockEntity.this.onWeaponRemoved();
                        }
                    }
                }
            } finally {
                isUpdating = false;
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                // Only allow weapon/tool insertion if slot is empty
                ItemStack currentItem = getStackInSlot(0);
                if (!currentItem.isEmpty()) {
                    return false; // Prevent insertion if weapon already present
                }
                return OrbalTableBlockEntity.this.isWeaponOrTool(stack);
            } else if (slot >= 1 && slot <= 8) {
                return true;
            }
            return false;
        }


        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            System.out.println("=== TRYING TO INSERT ITEM ===");
            System.out.println("Slot: " + slot + ", Item: " + stack.getDisplayName().getString() + ", Simulate: " + simulate);

            if (slot == 0) {
                // Special handling for weapon slot - check if already occupied
                ItemStack currentWeapon = getStackInSlot(0);
                if (!currentWeapon.isEmpty()) {
                    System.out.println("Weapon slot already occupied! Cannot insert: " + stack.getDisplayName().getString());
                    return stack; // Return the entire stack - cannot insert
                }
            }

            if (!isItemValid(slot, stack)) {
                System.out.println("Item not valid for this slot!");
                return stack;
            }

            ItemStack result = super.insertItem(slot, stack, simulate);
            System.out.println("Insert result: " + (result.isEmpty() ? "SUCCESS" : "FAILED - " + result.getCount() + " remaining"));
            return result;
        }
    };

    // Animation states
    private boolean monitorOpen = false;
    private boolean chainsawActive = false;
    private int operationProgress = 0;
    private int maxOperationTime = 100; // 5 seconds at 20 ticks/second

    // Current operation data
    private WeaponSlotOperation currentOperation = null;

    public OrbalTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ORBAL_TABLE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack getWeaponItem() {
        if (level != null && level.isClientSide()) {
            return clientWeapon;
        } else {
            // SERVER: Check inventory first, then stored weapon
            ItemStack inventoryWeapon = inventory.getStackInSlot(0);
            if (!inventoryWeapon.isEmpty() && isWeaponOrTool(inventoryWeapon)) {
                return inventoryWeapon;
            } else {
                return storedWeapon;
            }
        }
    }
    // Update the hasWeapon method
    public boolean hasWeapon() {
        if (level != null && level.isClientSide()) {
            return !clientWeapon.isEmpty() && isWeaponOrTool(clientWeapon);
        } else {
            // SERVER: Check inventory first
            ItemStack inventoryWeapon = inventory.getStackInSlot(0);
            if (!inventoryWeapon.isEmpty() && isWeaponOrTool(inventoryWeapon)) {
                return true;
            }
            return !storedWeapon.isEmpty() && isWeaponOrTool(storedWeapon);
        }
    }
    public boolean insertWeapon(ItemStack weapon) {
        if (storedWeapon.isEmpty() && isWeaponOrTool(weapon)) {
            // Only set storedWeapon, let the inventory update happen separately
            storedWeapon = weapon.copy();

            // Use insertItem instead of setStackInSlot to avoid recursion
            ItemStack remainder = inventory.insertItem(0, weapon.copy(), false);
            if (!remainder.isEmpty()) {
                // If insertion failed, clear stored weapon
                storedWeapon = ItemStack.EMPTY;
                return false;
            }

            setChanged();
            onWeaponChanged();
            return true;
        }
        return false;
    }
    public void testInventory() {
        System.out.println("=== TESTING INVENTORY ===");
        System.out.println("Inventory: " + inventory);
        System.out.println("Slot count: " + inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            System.out.println("Slot " + i + ": " + (stack.isEmpty() ? "EMPTY" : stack.getDisplayName().getString()));
        }
    }
    public void debugWeaponStatus() {
        ItemStack weapon = getWeaponItem();
        System.out.println("=== WEAPON DEBUG ===");
        System.out.println("Weapon empty: " + weapon.isEmpty());
        if (!weapon.isEmpty()) {
            System.out.println("Weapon item: " + weapon.getItem());
            System.out.println("Weapon class: " + weapon.getItem().getClass());
            System.out.println("Is weapon/tool: " + isWeaponOrTool(weapon));
            System.out.println("Display name: " + weapon.getDisplayName().getString());
            System.out.println("Is damageable: " + weapon.isDamageableItem());
            System.out.println("Is SwordItem: " + (weapon.getItem() instanceof net.minecraft.world.item.SwordItem));
        }
        System.out.println("Inventory slot 0: " + inventory.getStackInSlot(0));
        System.out.println("Has weapon result: " + hasWeapon());
    }
    public boolean isWeaponOrTool(ItemStack stack) {
        // Use the same universal detection as the renderer
        return WeaponSlotRenderer.isWeaponOrTool(stack);
    }
    private static final Map<ItemDisplayContext, Float> SCALE_FACTORS = Map.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 1.0f,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 1.0f,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 0.8f,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND, 0.8f,
            ItemDisplayContext.GUI, 0.4f,
            ItemDisplayContext.GROUND, 0.8f
    );

    // Add weapon-specific scaling
    private static float getWeaponSpecificScale(ItemStack weapon, ItemDisplayContext context) {
        float baseScale = SCALE_FACTORS.getOrDefault(context, 1.0f);

        // Adjust scale based on weapon type
        Item item = weapon.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return baseScale * 1.2f; // Bows are larger
        } else if (item instanceof TridentItem) {
            return baseScale * 1.5f; // Tridents are much larger
        } else if (item instanceof SwordItem) {
            return baseScale * 1.0f; // Standard size
        } else if (item instanceof AxeItem) {
            return baseScale * 0.9f; // Slightly smaller
        }

        return baseScale;
    }
    public void forceVisualUpdate() {
        if (level != null && !level.isClientSide()) {
            setChanged();

            // Multiple approaches to ensure visual update
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

            // Force model data refresh
            requestModelDataUpdate();

            // Send custom sync packet
            syncToClients();

            if (level instanceof ServerLevel serverLevel) {
                // Force chunk update
                serverLevel.getChunkSource().blockChanged(worldPosition);

                // Force all nearby players to update
                List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(ServerPlayer.class,
                        new AABB(worldPosition).inflate(64));
                for (ServerPlayer player : nearbyPlayers) {
                    player.connection.send(ClientboundBlockEntityDataPacket.create(this));
                }
            }
        }
    }
    public void openScreen(ServerPlayer player) {
        System.out.println("=== OPENING SCREEN ===");
        System.out.println("Has weapon: " + hasWeapon());

        try {
            player.openMenu(this, buf -> {
                buf.writeBlockPos(worldPosition);
            });
            System.out.println("Menu opened successfully");
        } catch (Exception e) {
            System.out.println("ERROR opening menu: " + e.getMessage());
            e.printStackTrace();
        }

        // Trigger monitor animation if weapon is present
        if (hasWeapon()) {
            setMonitorOpen(true);
        }
    }
    public void setMonitorOpen(boolean open) {
        System.out.println("=== SET MONITOR OPEN CALLED ===");
        System.out.println("Current state: " + this.monitorOpen + ", New state: " + open);
        System.out.println("Level null: " + (level == null) + ", Is client: " + (level != null ? level.isClientSide() : "N/A"));

        if (this.monitorOpen != open) {
            this.monitorOpen = open;
            this.monitorOpenTicks = 0; // Reset timer when state changes
            if (level != null && !level.isClientSide()) {
                System.out.println("=== TRIGGERING ANIMATIONS: " + (open ? "monitor_open & wings_open" : "monitor_close & wings_close") + " ===");
                triggerAnim("monitor_controller", open ? "monitor_open" : "monitor_close");
                triggerAnim("wings_controller", open ? "wings_open" : "wings_close");
                syncToClients();

                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);

                System.out.println("=== ANIMATION TRIGGERED AND SYNCED ===");
            }
        } else {
            System.out.println("=== NO STATE CHANGE, ANIMATION NOT TRIGGERED ===");
        }
    }


    public boolean isMonitorOpen() {
        return monitorOpen;
    }
    public ItemStack removeWeapon() {
        if (!storedWeapon.isEmpty()) {
            ItemStack result = storedWeapon.copy();

            // Clear both stored weapon and inventory slot
            storedWeapon = ItemStack.EMPTY;
            inventory.setStackInSlot(0, ItemStack.EMPTY); // Clear inventory slot too

            setChanged();
            onWeaponRemoved(); // This will handle the visual updates

            System.out.println("=== WEAPON REMOVED DIRECTLY ===");
            return result;
        }
        return ItemStack.EMPTY;
    }
    private void onWeaponChanged() {
        if (level != null && !level.isClientSide()) {
            boolean hasWeaponNow = hasWeapon();
            System.out.println("=== WEAPON CHANGED ===");
            System.out.println("Has weapon now: " + hasWeaponNow);

            // Trigger animation
            setMonitorOpen(hasWeaponNow);

            // IMMEDIATE sync - don't wait
            syncToClients();

            // Force visual update
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
        }
    }
    public void startSlotOperation(WeaponSlotOperation operation) {
        System.out.println("=== START SLOT OPERATION ===");
        System.out.println("Operation type: " + operation.type);
        System.out.println("Element type: " + operation.elementType);

        // Validate materials before starting
        if (operation.type == WeaponSlotOperation.Type.ADD_SLOT ||
                operation.type == WeaponSlotOperation.Type.CHANGE_ELEMENT) {
            if (!hasRequiredMaterials(operation.elementType, 10)) {
                System.out.println("Not enough materials for operation!");
                return;
            }
        }

        this.currentOperation = operation;
        this.operationProgress = 0;
        this.chainsawActive = true;

        if (level != null && !level.isClientSide()) {
            System.out.println("Triggering chainsaw animation...");
            triggerAnim("chainsaw_controller", "chainsaw_start");
            syncToClients();
        }
    }
    public void setClientWeapon(ItemStack weapon) {
        this.clientWeapon = weapon.copy();
    }

    public boolean hasRequiredMaterials(String elementType, int amount) {
        if ("sepith".equals(elementType.toLowerCase())) {
            // Special handling for sepith mass
            ItemStack sepithStack = inventory.getStackInSlot(8);
            boolean hasEnough = !sepithStack.isEmpty() &&
                    sepithStack.getItem() == ModItems.SEPITH_MASS.get() &&
                    sepithStack.getCount() >= amount;
            System.out.println("Checking sepith materials: " +
                    (sepithStack.isEmpty() ? "EMPTY" : sepithStack.getDisplayName().getString() + " x" + sepithStack.getCount()) +
                    "/" + amount + " (has enough: " + hasEnough + ")");
            return hasEnough;
        }

        // Original element material checking
        int slot = getElementSlot(elementType);
        if (slot == -1) {
            System.out.println("Invalid element type: " + elementType);
            return false;
        }

        ItemStack stack = inventory.getStackInSlot(slot);
        boolean hasEnough = stack.getCount() >= amount;
        System.out.println("Checking materials for " + elementType + " in slot " + slot + ": " +
                (stack.isEmpty() ? "EMPTY" : stack.getDisplayName().getString() + " x" + stack.getCount()) +
                "/" + amount + " (has enough: " + hasEnough + ")");
        return hasEnough;
    }
    private Item getElementMassItem(String elementType) {
        return switch (elementType.toLowerCase()) {
            case "earth" -> ModItems.EARTH_MASS.get();
            case "water" -> ModItems.WATER_MASS.get();
            case "fire" -> ModItems.FIRE_MASS.get();
            case "wind" -> ModItems.WIND_MASS.get();
            case "time" -> ModItems.TIME_MASS.get();
            case "space" -> ModItems.SPACE_MASS.get();
            case "mirage" -> ModItems.MIRAGE_MASS.get();
            default -> null;
        };
    }

    private int getElementSlot(String elementType) {
        return switch (elementType.toLowerCase()) {
            case "earth" -> 1;
            case "water" -> 2;
            case "fire" -> 3;
            case "wind" -> 4;
            case "time" -> 5;
            case "space" -> 6;
            case "mirage" -> 7;
            default -> -1;
        };
    }


    public boolean consumeMaterials(String elementType, int amount) {
        int slot = getElementSlot(elementType);
        if (slot == -1 || !hasRequiredMaterials(elementType, amount)) {
            return false;
        }

        ItemStack stack = inventory.getStackInSlot(slot);
        stack.shrink(amount);
        System.out.println("Consumed " + amount + " " + elementType + " mass. Remaining: " + stack.getCount());
        return true;
    }
    public void triggerClientRenderUpdate() {
        if (level != null && !level.isClientSide()) {
            // FIXED: Force complete block update
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();

            // FIXED: Force chunk refresh
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);

                // FIXED: Send update to all nearby players
                serverLevel.getPlayers(player -> player.blockPosition().closerThan(worldPosition, 64))
                        .forEach(player -> {
                            player.connection.send(ClientboundBlockEntityDataPacket.create(this));
                        });
            }
        }
    }
    public void forceRendererUpdate() {
        if (level != null && level.isClientSide()) {
            // Force the renderer to re-check weapon state
            Minecraft.getInstance().levelRenderer.setBlocksDirty(
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()
            );
        }
    }
    public void tick() {
        if (level == null || level.isClientSide()) return;

        // FIX: Properly increment monitor open ticks
        if (monitorOpen) {
            monitorOpenTicks++;
            // Sync every 5 ticks to update clients
            if (monitorOpenTicks % 5 == 0) {
                syncToClients();
            }
        } else {
            monitorOpenTicks = 0;
        }
        // Debug: Print inventory contents every 20 ticks (1 second)
        if (level.getGameTime() % 20 == 0) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    System.out.println("Slot " + i + ": " + stack.getDisplayName().getString());

                }

            }
        }

        if (currentOperation != null && chainsawActive) {
            operationProgress++;
            if (operationProgress >= maxOperationTime) {
                completeOperation();
            }
        }
    }
    private boolean consumeClosingMaterials() {
        System.out.println("=== CHECKING FOR SEPITH MASS ===");

        // Check the sepith mass slot (slot 8) specifically
        ItemStack sepithStack = inventory.getStackInSlot(8);
        System.out.println("Sepith mass slot (8): " + (sepithStack.isEmpty() ? "EMPTY" : sepithStack.getDisplayName().getString() + " x" + sepithStack.getCount()));

        if (!sepithStack.isEmpty() && sepithStack.getItem() == ModItems.SEPITH_MASS.get()) {
            if (sepithStack.getCount() >= 5) {
                sepithStack.shrink(5);
                System.out.println("Consumed 5 sepith mass for slot closing. Remaining: " + sepithStack.getCount());
                setChanged();
                return true;
            } else {
                System.out.println("Not enough sepith mass: need 5, have " + sepithStack.getCount());
            }
        } else {
            System.out.println("No sepith mass found in sepith slot");
        }

        return false;
    }

    private boolean isSepithMass(Item item) {
        return item == ModItems.SEPITH_MASS.get(); // Only the singular sepith mass item
    }

    public void onWeaponRemoved() {
        System.out.println("=== WEAPON REMOVED - FORCING VISUAL UPDATE ===");

        // Store weapon before clearing for invalidation
        ItemStack weaponToInvalidate = storedWeapon.copy();

        storedWeapon = ItemStack.EMPTY;
        clientWeapon = ItemStack.EMPTY;

        // CRITICAL: Also clear inventory slot
        inventory.setStackInSlot(0, ItemStack.EMPTY);

        // ADD THIS LINE HERE:
        if (!weaponToInvalidate.isEmpty()) {
            WeaponSlotBakedModel.invalidateWeaponModel(weaponToInvalidate);
        }

        setMonitorOpen(false);


        // Cancel any ongoing operations
        if (currentOperation != null) {
            currentOperation = null;
            chainsawActive = false;
            operationProgress = 0;
        }

        if (level != null && !level.isClientSide()) {
            setChanged();

            OrbalTableSyncPacket packet = new OrbalTableSyncPacket(
                    worldPosition, false, false, 0, ItemStack.EMPTY, 0
            );
            NetworkHandler.sendToAllClients(packet);

            // Then force block updates
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
        }
    }

    private void completeOperation() {
        if (currentOperation == null) return;

        ItemStack weapon = getWeaponItem();
        if (weapon.isEmpty()) {
            cancelOperation();
            return;
        }

        WeaponSlotData slotData = WeaponSlotData.getOrCreate(weapon);

        switch (currentOperation.type) {
            case ADD_SLOT -> {
                if (slotData.getSlotCount() < 3 &&
                        consumeMaterials(currentOperation.elementType, 10)) {
                    slotData.addSlot(currentOperation.slotPosition, currentOperation.elementType);
                    System.out.println("Added slot with element: " + currentOperation.elementType);
                }
            }
            case REMOVE_SLOT -> {
                System.out.println("=== REMOVE_SLOT OPERATION ===");
                System.out.println("Slot index: " + currentOperation.slotIndex);
                System.out.println("Total slots: " + slotData.getSlots().size());

                WeaponSlotData.WeaponSlot slot = slotData.getSlot(currentOperation.slotIndex);
                if (slot != null && !slot.isClosed) {
                    System.out.println("Slot found and not closed");
                    // Check if slot has quartz first
                    if (slot.hasQuartz()) {
                        System.out.println("Slot has quartz, removing...");
                        ItemStack removedQuartz = slot.removeQuartz();
                        System.out.println("Removed quartz: " + (removedQuartz.isEmpty() ? "EMPTY" : removedQuartz.getDisplayName().getString()));

                        if (!removedQuartz.isEmpty() && removedQuartz.getItem() instanceof QuartzItem quartzItem) {
                            try {
                                removeQuartzEffectsFromWeapon(weapon, quartzItem);
                                System.out.println("Removed quartz effects from weapon");
                            } catch (Exception e) {
                                System.out.println("Error removing quartz effects: " + e.getMessage());
                            }
                        }

                        // CRITICAL: Return quartz to player
                        if (!removedQuartz.isEmpty()) {
                            returnItemToPlayer(removedQuartz);
                            System.out.println("Successfully returned quartz: " + removedQuartz.getDisplayName().getString());
                        }
                    } else {
                        System.out.println("No quartz in slot " + currentOperation.slotIndex + " to remove");
                        // Still complete the operation even if no quartz
                    }

                    System.out.println("Quartz removal operation completed for slot " + currentOperation.slotIndex);
                } else {
                    System.out.println("Cannot remove quartz - slot is closed or doesn't exist!");
                    System.out.println("Slot null: " + (slot == null));
                    if (slot != null) {
                        System.out.println("Slot closed: " + slot.isClosed);
                    }
                    cancelOperation();
                    return;
                }








            }
            case CHANGE_ELEMENT -> {
                if (consumeMaterials(currentOperation.elementType, 10)) {
                    WeaponSlotData.WeaponSlot slot = slotData.getSlot(currentOperation.slotIndex);
                    if (slot != null && !slot.hasQuartz()) {
                        slotData.changeSlotElement(currentOperation.slotIndex, currentOperation.elementType);
                        System.out.println("Changed slot " + currentOperation.slotIndex + " to element: " + currentOperation.elementType);
                    }
                }
            }
            case CLOSE_SLOT -> {
                WeaponSlotData.WeaponSlot slot = slotData.getSlot(currentOperation.slotIndex);
                if (slot != null && !slot.isClosed) {
                    // FIXED: Check for sepith mass first
                    if (consumeClosingMaterials()) {
                        // Return quartz if present (don't remove it from slot yet)
                        if (slot.hasQuartz()) {
                            ItemStack removedQuartz = slot.removeQuartz();

                            if (!removedQuartz.isEmpty() && removedQuartz.getItem() instanceof QuartzItem quartzItem) {
                                removeQuartzEffectsFromWeapon(weapon, quartzItem);
                            }

                            returnItemToPlayer(removedQuartz);
                            System.out.println("Returned quartz before closing: " + removedQuartz.getDisplayName().getString());
                        }

                        // NOW close the slot
                        slotData.closeSlot(currentOperation.slotIndex);
                        System.out.println("Slot " + currentOperation.slotIndex + " closed/sealed");
                    } else {
                        System.out.println("Not enough sepith mass to close slot!");
                        cancelOperation();
                        return;
                    }
                } else {
                    System.out.println("Slot is already closed or doesn't exist!");
                    cancelOperation();
                    return;
                }

            }
        }

        // FIXED: Better weapon validation that accounts for NBT changes
        ItemStack currentWeaponInSlot = inventory.getStackInSlot(0);
        if (!currentWeaponInSlot.isEmpty() && currentWeaponInSlot.getItem() == weapon.getItem()) {
            // Items match, safe to update (NBT differences are expected after slot operations)
            WeaponSlotData.save(weapon, slotData);
            inventory.setStackInSlot(0, weapon.copy());
            storedWeapon = weapon.copy();

            WeaponSlotBakedModel.invalidateWeaponModel(weapon);

            // Force comprehensive update
            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();
            syncToClients();

            System.out.println("Weapon updated successfully with new slot data");
        } else if (currentWeaponInSlot.isEmpty()) {
            System.out.println("WARNING: Weapon removed during operation, cancelling");
            cancelOperation();
            return;
        } else {
            System.out.println("WARNING: Different weapon type detected, operation cancelled");
            cancelOperation();
            return;
        }

        finishOperation();
    }
    public boolean insertQuartzIntoWeapon(ItemStack quartz, int slotIndex) {
        System.out.println("=== INSERTING QUARTZ INTO WEAPON VIA ORBAL TABLE ===");


        ItemStack weapon = getWeaponItem();
        if (weapon.isEmpty()) {
            System.out.println("No weapon in table!");
            return false;
        }

        WeaponSlotData slotData = WeaponSlotData.getOrCreate(weapon);
        WeaponSlotData.WeaponSlot slot = slotData.getSlot(slotIndex);

        if (slot == null || slot.isClosed) {
            System.out.println("Slot " + slotIndex + " is null or closed!");
            return false;
        }

        if (slot.canInsertQuartz(quartz)) {
            System.out.println("Slot can accept quartz, inserting...");
            boolean success = slot.insertQuartz(quartz);

            if (success) {
                // Apply quartz effects
                if (quartz.getItem() instanceof QuartzItem quartzItem) {
                    quartzItem.onInsertedIntoWeapon(weapon, quartz);
                }

                // Save the weapon data
                WeaponSlotData.save(weapon, slotData);

                // Update the weapon in inventory
                inventory.setStackInSlot(0, weapon.copy());
                storedWeapon = weapon.copy();

                // ADD THIS LINE HERE:
                WeaponSlotBakedModel.invalidateWeaponModel(weapon);

                setChanged();
                syncToClients();

                System.out.println("Quartz successfully inserted via Orbal Table!");
                return true;
            }
        } else {
            System.out.println("Slot cannot accept this quartz (wrong element or slot full)");
        }

        return false;
    }
    public void debugQuartzApplication(ItemStack weapon) {

        WeaponSlotData slotData = WeaponSlotData.getOrCreate(weapon);


        for (int i = 0; i < slotData.getSlots().size(); i++) {
            WeaponSlotData.WeaponSlot slot = slotData.getSlot(i);


            if (slot.hasQuartz()) {
                ItemStack quartz = slot.quartzItem;

                if (quartz.getItem() instanceof QuartzItem quartzItem) {

                }
            }
        }

        // Check weapon attributes
        ItemAttributeModifiers modifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            System.out.println("Current weapon modifiers: " + modifiers.modifiers().size());
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                System.out.println("  - " + entry.attribute().value() + ": " +
                        entry.modifier().amount() + " (ID: " + entry.modifier().id() + ")");
            }
        } else {
            System.out.println("No attribute modifiers on weapon");
        }
    }
    // FIXED: Enhanced returnItemToPlayer method
    private void returnItemToPlayer(ItemStack item) {
        if (level instanceof ServerLevel serverLevel) {
            List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(worldPosition).inflate(10));
            if (!nearbyPlayers.isEmpty()) {
                ServerPlayer player = nearbyPlayers.get(0);

                // Try to add to player inventory first
                if (player.getInventory().add(item)) {
                    player.sendSystemMessage(Component.literal("Returned: " + item.getDisplayName().getString()));
                    System.out.println("Returned item to player inventory: " + item.getDisplayName().getString());
                } else {
                    // Drop if inventory is full
                    Containers.dropItemStack(level, worldPosition.getX(),
                            worldPosition.getY() + 1, worldPosition.getZ(), item);
                    player.sendSystemMessage(Component.literal("Inventory full! Dropped: " + item.getDisplayName().getString()));
                    System.out.println("Dropped item on ground (inventory full): " + item.getDisplayName().getString());
                }
            } else {
                // No player nearby, drop the item
                Containers.dropItemStack(level, worldPosition.getX(),
                        worldPosition.getY() + 1, worldPosition.getZ(), item);
                System.out.println("Dropped item on ground (no player nearby): " + item.getDisplayName().getString());
            }
        }
    }
    private void removeQuartzEffectsFromWeapon(ItemStack weapon, QuartzItem quartzItem) {
        System.out.println("=== REMOVING QUARTZ EFFECTS FROM WEAPON ===");
        System.out.println("Quartz ID: " + quartzItem.getQuartzId());

        // Remove attribute modifiers added by this quartz
        ItemAttributeModifiers existing = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (existing != null) {
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
            String quartzId = quartzItem.getQuartzId();

            boolean removedAny = false;
            for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
                // Keep modifiers that aren't from this specific quartz
                if (!entry.modifier().id().getPath().contains("quartz_" + quartzId)) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                } else {
                    removedAny = true;
                    System.out.println("Removed modifier: " + entry.modifier().id().getPath());
                }
            }

            if (removedAny) {
                weapon.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
                System.out.println("Updated weapon attribute modifiers");
            }
        }

        // Remove MaterialQualitySystem data added by this quartz
        CompoundTag weaponTag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // Remove material traits from this quartz
        if (weaponTag.contains("MaterialTraits")) {
            // You'd need to track which traits came from which quartz for precise removal
            // For now, this removes all - you could improve this by storing source info
            weaponTag.remove("MaterialTraits");
            System.out.println("Removed material traits");
        }

        // Remove material effects from this specific quartz
        if (weaponTag.contains("MaterialEffects")) {
            ListTag effectsTag = weaponTag.getList("MaterialEffects", 10);
            String sourceId = "quartz_" + quartzItem.getQuartzId();

            for (int i = effectsTag.size() - 1; i >= 0; i--) {
                CompoundTag effectTag = effectsTag.getCompound(i);
                if (sourceId.equals(effectTag.getString("Source"))) {
                    effectsTag.remove(i);
                    System.out.println("Removed effect: " + effectTag.getString("Name"));
                }
            }
            weaponTag.put("MaterialEffects", effectsTag);
        }

        weaponTag.putLong("LastQuartzUpdate", System.currentTimeMillis());
        weaponTag.putString("LastQuartzRemoved", quartzItem.getQuartzId());
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag));

    }
    // Add these helper methods to OrbalTableBlockEntity:
    private Item getWeaponMaterial(ItemStack weapon) {
        if (weapon.getItem() instanceof SwordItem swordItem) {
            Tier tier = swordItem.getTier();
            if (tier == Tiers.DIAMOND) return Items.DIAMOND;
            if (tier == Tiers.IRON) return Items.IRON_INGOT;
            if (tier == Tiers.GOLD) return Items.GOLD_INGOT;
            if (tier == Tiers.NETHERITE) return Items.NETHERITE_INGOT;
        }
        // Add other weapon types as needed
        return Items.IRON_INGOT; // Default fallback
    }

    private boolean hasRequiredMaterials(Item material, int amount) {
        // Check all slots for the material
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.is(material) && stack.getCount() >= amount) {
                return true;
            }
        }
        return false;
    }

    public boolean isChainsawActive() {
        return chainsawActive;
    }

    public int getOperationProgress() {
        return operationProgress;
    }

    private boolean consumeWeaponMaterial(Item material, int amount) {
        // Find and consume from inventory slots
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.is(material) && stack.getCount() >= amount) {
                stack.shrink(amount);
                setChanged();
                return true;
            }
        }
        return false;
    }
    private void finishOperation() {
        this.chainsawActive = false;
        this.currentOperation = null;
        this.operationProgress = 0;

        if (level != null && !level.isClientSide()) {
            triggerAnim("chainsaw_controller", "chainsaw_stop");
            syncToClients();
        }
    }

    private void cancelOperation() {
        this.currentOperation = null;
        finishOperation();
    }
    public void debugSlotNumbering() {
        ItemStack weapon = getWeaponItem();
        if (!weapon.isEmpty()) {
            WeaponSlotData weaponSlotData = WeaponSlotData.getOrCreate(weapon);
            List<WeaponSlotData.WeaponSlot> slots = weaponSlotData.getSlots();
            for (int i = 0; i < slots.size(); i++) {
                WeaponSlotData.WeaponSlot slot = slots.get(i);

            }

            // Check what your screen is seeing
            List<WeaponSlotData.WeaponSlot> activeSlots = new ArrayList<>();
            for (WeaponSlotData.WeaponSlot slot : slots) {
                if (!slot.isClosed) {
                    activeSlots.add(slot);
                }
            }
            for (int i = 0; i < activeSlots.size(); i++) {
            }
        }
    }
    public int getMonitorOpenTime() {
        return monitorOpenTicks;
    }
    public void syncToClients() {
        if (level != null && !level.isClientSide()) {
            ItemStack weaponToSync = inventory.getStackInSlot(0);
            if (weaponToSync.isEmpty() || !isWeaponOrTool(weaponToSync)) {
                weaponToSync = storedWeapon;
            }

            // Create packet with monitorOpenTicks
            OrbalTableSyncPacket packet = new OrbalTableSyncPacket(
                    worldPosition, monitorOpen, chainsawActive, operationProgress, weaponToSync, monitorOpenTicks
            );
            NetworkHandler.sendToAllClients(packet);

            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
        }
    }
    public void updateClientState(boolean monitorOpen, boolean chainsawActive, int operationProgress, int monitorOpenTicks) {
        this.monitorOpen = monitorOpen;
        this.chainsawActive = chainsawActive;
        this.operationProgress = operationProgress;
        this.monitorOpenTicks = monitorOpenTicks;

        // Force visual update on client
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
        }
    }
    public void dropContents() {
        if (level != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(),
                            worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);

        // FIXED: Safe inventory serialization with validation
        try {
            // Use a temporary handler to avoid corruption
            ItemStackHandler tempInventory = new ItemStackHandler(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    try {
                        // Test if the stack can be serialized
                        CompoundTag testTag = new CompoundTag();
                        stack.save(registries, testTag);
                        tempInventory.setStackInSlot(i, stack);
                    } catch (Exception e) {
                        System.out.println("Corrupted stack in slot " + i + ", clearing: " + e.getMessage());
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                        tempInventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
            tag.put("inventory", tempInventory.serializeNBT(registries));
        } catch (Exception e) {
            System.out.println("Critical inventory serialization error: " + e.getMessage());
            // Create empty inventory
            ItemStackHandler emptyInventory = new ItemStackHandler(inventory.getSlots());
            tag.put("inventory", emptyInventory.serializeNBT(registries));
        }

        // FIXED: Safe weapon serialization
        if (!storedWeapon.isEmpty()) {
            try {
                CompoundTag weaponTag = new CompoundTag();
                storedWeapon.save(registries, weaponTag);
                tag.put("storedWeapon", weaponTag);
            } catch (Exception e) {
                System.out.println("Failed to save weapon, clearing: " + e.getMessage());
                storedWeapon = ItemStack.EMPTY;
            }
        }

        tag.putBoolean("monitorOpen", monitorOpen);
        return tag;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Monitor controller
        controllers.add(new AnimationController<>(this, "monitor_controller", 0, this::monitorPredicate)
                .triggerableAnim("monitor_open", RawAnimation.begin().thenPlay("monitor_open").thenPlayAndHold("monitor_open"))
                .triggerableAnim("monitor_close", RawAnimation.begin().thenPlay("monitor_close").thenPlayAndHold("monitor_close")));

        // Wings controller
        controllers.add(new AnimationController<>(this, "wings_controller", 0, this::wingsPredicate)
                .triggerableAnim("wings_open", RawAnimation.begin().thenPlay("wings_open").thenPlayAndHold("wings_open"))
                .triggerableAnim("wings_close", RawAnimation.begin().thenPlay("wings_close").thenPlayAndHold("wings_close")));

        // Chainsaw controller
        controllers.add(new AnimationController<>(this, "chainsaw_controller", 0, this::chainsawPredicate)
                .triggerableAnim("chainsaw_start", RawAnimation.begin().thenPlay("chainsaw_start").thenLoop("chainsaw_loop"))
                .triggerableAnim("chainsaw_stop", RawAnimation.begin().thenPlay("chainsaw_stop").thenPlayAndHold("idle")));
    }

    private PlayState monitorPredicate(AnimationState<OrbalTableBlockEntity> state) {
        return PlayState.CONTINUE;
    }

    private PlayState wingsPredicate(AnimationState<OrbalTableBlockEntity> state) {
        // Default to wings_close when not explicitly opened
        if (!monitorOpen && state.getController().getCurrentAnimation() == null) {
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("wings_close"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState chainsawPredicate(AnimationState<OrbalTableBlockEntity> state) {
        if (chainsawActive) {
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.kisekilegend.orbal_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new OrbalTableMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save inventory with individual slot handling
        CompoundTag inventoryTag = new CompoundTag();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                try {
                    CompoundTag slotTag = new CompoundTag();
                    // Manual item saving to avoid corruption
                    slotTag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    slotTag.putInt("count", stack.getCount());
                    if (stack.getDamageValue() > 0) {
                        slotTag.putInt("damage", stack.getDamageValue());
                    }

                    // Save custom data if present
                    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null && !customData.isEmpty()) {
                        slotTag.put("custom_data", customData.copyTag());
                    }

                    inventoryTag.put("slot_" + i, slotTag);
                } catch (Exception e) {
                    System.out.println("Error saving slot " + i + ": " + e.getMessage());
                }
            }
        }
        tag.put("inventory", inventoryTag);

        tag.putBoolean("monitorOpen", monitorOpen);
        tag.putBoolean("chainsawActive", chainsawActive);
        tag.putInt("operationProgress", operationProgress);

        // Save stored weapon with same manual approach
        if (!storedWeapon.isEmpty()) {
            try {
                CompoundTag weaponTag = new CompoundTag();
                weaponTag.putString("id", BuiltInRegistries.ITEM.getKey(storedWeapon.getItem()).toString());
                weaponTag.putInt("count", storedWeapon.getCount());
                if (storedWeapon.getDamageValue() > 0) {
                    weaponTag.putInt("damage", storedWeapon.getDamageValue());
                }

                CustomData customData = storedWeapon.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    weaponTag.put("custom_data", customData.copyTag());
                }

                tag.put("storedWeapon", weaponTag);
            } catch (Exception e) {
                System.out.println("Failed to save weapon: " + e.getMessage());
            }
        }

        if (currentOperation != null) {
            CompoundTag opTag = new CompoundTag();
            currentOperation.save(opTag);
            tag.put("currentOperation", opTag);
        }
    }



    // Replace the getModelData method with this:
    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
        return net.minecraftforge.client.model.data.ModelData.builder()
                .with(WEAPON_PROPERTY, getWeaponItem())
                .with(MONITOR_OPEN_PROPERTY, isMonitorOpen())
                .build();
    }
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Load inventory with error handling
        if (tag.contains("inventory")) {
            CompoundTag inventoryTag = tag.getCompound("inventory");
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventoryTag.contains("slot_" + i)) {
                    try {
                        CompoundTag slotTag = inventoryTag.getCompound("slot_" + i);
                        String itemId = slotTag.getString("id");
                        int count = slotTag.getInt("count");
                        int damage = slotTag.getInt("damage");

                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                        ItemStack stack = new ItemStack(item, count);
                        stack.setDamageValue(damage);

                        if (slotTag.contains("custom_data")) {
                            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(slotTag.getCompound("custom_data")));
                        }

                        inventory.setStackInSlot(i, stack);
                    } catch (Exception e) {
                        System.out.println("Failed to load slot " + i + ": " + e.getMessage());
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        }

        monitorOpen = tag.getBoolean("monitorOpen");
        chainsawActive = tag.getBoolean("chainsawActive");
        operationProgress = tag.getInt("operationProgress");

        // Load stored weapon with same manual approach
        if (tag.contains("storedWeapon")) {
            try {
                CompoundTag weaponTag = tag.getCompound("storedWeapon");
                String itemId = weaponTag.getString("id");
                int count = weaponTag.getInt("count");
                int damage = weaponTag.getInt("damage");

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                storedWeapon = new ItemStack(item, count);
                storedWeapon.setDamageValue(damage);

                if (weaponTag.contains("custom_data")) {
                    storedWeapon.set(DataComponents.CUSTOM_DATA, CustomData.of(weaponTag.getCompound("custom_data")));
                }

                System.out.println("Loaded stored weapon: " + storedWeapon.getDisplayName().getString());
            } catch (Exception e) {
                System.out.println("Failed to load weapon: " + e.getMessage());
                storedWeapon = ItemStack.EMPTY;
            }
        }

        // Sync weapon to inventory slot 0 after loading
        if (!storedWeapon.isEmpty() && inventory.getStackInSlot(0).isEmpty()) {
            inventory.setStackInSlot(0, storedWeapon.copy());
        }

        if (tag.contains("currentOperation")) {
            currentOperation = WeaponSlotOperation.load(tag.getCompound("currentOperation"));
        }
    }

    public static class Ticker implements BlockEntityTicker<OrbalTableBlockEntity> {
        @Override
        public void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, OrbalTableBlockEntity blockEntity) {
            blockEntity.tick();
        }
    }

    // Operation data class
    public static class WeaponSlotOperation {
        public enum Type {
            ADD_SLOT, REMOVE_SLOT, CHANGE_ELEMENT, CLOSE_SLOT
        }

        public Type type;
        public String elementType;
        public int slotIndex;
        public float[] slotPosition; // x, y, z position on weapon model

        public WeaponSlotOperation(Type type, String elementType, int slotIndex, float[] slotPosition) {
            this.type = type;
            this.elementType = elementType;
            this.slotIndex = slotIndex;
            this.slotPosition = slotPosition;
        }

        public void save(CompoundTag tag) {
            tag.putInt("type", type.ordinal());
            tag.putString("elementType", elementType);
            tag.putInt("slotIndex", slotIndex);
            if (slotPosition != null && slotPosition.length >= 3) {
                tag.putFloat("posX", slotPosition[0]);
                tag.putFloat("posY", slotPosition[1]);
                tag.putFloat("posZ", slotPosition[2]);

            }
        }

        public static WeaponSlotOperation load(CompoundTag tag) {
            Type type = Type.values()[tag.getInt("type")];
            String elementType = tag.getString("elementType");
            int slotIndex = tag.getInt("slotIndex");
            float[] slotPosition = null;
            if (tag.contains("posX")) {
                slotPosition = new float[]{tag.getFloat("posX"), tag.getFloat("posY"), tag.getFloat("posZ")};
            }
            return new WeaponSlotOperation(type, elementType, slotIndex, slotPosition);
        }
    }
    public void forceWeaponUpdate() {
        if (level != null && !level.isClientSide()) {
            // Simple but effective update
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            requestModelDataUpdate();

            // Force chunk update
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }

            // Custom sync packet
            syncToClients();

        }

    }
}
