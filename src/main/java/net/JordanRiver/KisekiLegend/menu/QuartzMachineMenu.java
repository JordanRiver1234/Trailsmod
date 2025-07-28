package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class QuartzMachineMenu extends AbstractContainerMenu {
    public final QuartzMachineBlockEntity blockEntity;
    private final Level level;
    private final BlockPos blockPos; // Add this field

    // This constructor is called on the CLIENT. It receives the data packet.
    public QuartzMachineMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.QUARTZ_MACHINE_MENU.get(), pContainerId);
        this.level = inv.player.level();

        System.out.println("=== CLIENT MENU CONSTRUCTOR ===");

        if (extraData != null) {
            this.blockPos = extraData.readBlockPos();
            System.out.println("Client menu - received blockPos: " + this.blockPos);
        } else {
            this.blockPos = null;
            System.out.println("Client menu - extraData is null");
        }

        // Don't try to get block entity immediately on client - it may not be synced yet
        this.blockEntity = null;
        this.setupSlots(inv);
    }
    private QuartzMachineBlockEntity cachedBlockEntity;

    public QuartzMachineBlockEntity getBlockEntity() {
        // Server-side: use direct reference
        if (blockEntity != null) {
            return blockEntity;
        }

        // Client-side: use cached lookup with validation
        if (blockPos != null && level != null) {
            // Check if cached entity is still valid
            if (cachedBlockEntity != null &&
                    cachedBlockEntity.getLevel() == level &&
                    cachedBlockEntity.getBlockPos().equals(blockPos) &&
                    !cachedBlockEntity.isRemoved()) {
                return cachedBlockEntity;
            }

            // Fresh lookup and cache
            BlockEntity entity = level.getBlockEntity(blockPos);
            if (entity instanceof QuartzMachineBlockEntity quartzEntity) {
                cachedBlockEntity = quartzEntity;
                return cachedBlockEntity;
            }
        }

        return null;
    }
    public QuartzMachineMenu(int pContainerId, Inventory inv, QuartzMachineBlockEntity entity) {
        super(ModMenuTypes.QUARTZ_MACHINE_MENU.get(), pContainerId);
        this.blockEntity = entity;
        this.level = inv.player.level();
        this.blockPos = entity != null ? entity.getBlockPos() : null; // Add this line

        this.setupSlots(inv);
    }

    // Helper method to avoid code duplication
    private void setupSlots(Inventory inv) {
        // No machine slots - we'll handle the result slot through custom networking
        // Only player inventory slots (36 total)
        int inventoryX = 8;
        int inventoryY = 84;
        int hotbarY = 142;

        // Player main inventory (27 slots)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, inventoryX + col * 18, inventoryY + row * 18));
            }
        }

        // Player hotbar (9 slots)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inv, i, inventoryX + i * 18, hotbarY));
        }


        // Debug: Print total slot count
        System.out.println("QuartzMachineMenu total slots: " + this.slots.size());
    }
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Only sync from server side and only if block entity exists
        if (!this.level.isClientSide() && this.blockEntity != null) {
            // Throttled sync - only every 5 ticks
            if ((this.level.getGameTime() % 5) == 0) {
                this.blockEntity.syncToClient();
            }
        }
    }
    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        // Only call onMenuClosed on server side and only if player is still valid
        if (!this.level.isClientSide() && pPlayer instanceof ServerPlayer) {
            this.blockEntity.onMenuClosed(pPlayer);
        }
    }
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }
    private boolean hasChanged = false;

    public boolean hasChanged() {
        boolean result = this.hasChanged;
        this.hasChanged = false; // Reset after checking
        return result;
    }
    @Override
    public boolean stillValid(Player pPlayer) {
        if (blockEntity == null) {
            // Try to re-acquire the block entity
            // This requires storing the BlockPos, so add this field to your class:
            // private BlockPos blockPos;
            return true; // Keep menu open while we try to sync
        }
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.QUARTZ_MACHINE.get());
    }
}