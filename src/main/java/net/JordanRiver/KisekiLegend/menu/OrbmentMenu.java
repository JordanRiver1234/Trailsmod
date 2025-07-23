package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class OrbmentMenu extends AbstractContainerMenu {
    public static final int ORBMENT_SLOT_COUNT = 6;

    private final SizedItemStackHandler orbmentHandler;
    private final OrbmentComponent orbmentComponent;
    private final ItemStack orbmentStack;

    public OrbmentMenu(int id, Inventory inv) {
        super(ModMenuTypes.ORBMENT_MENU.get(), id);

        Player player = inv.player;
        this.orbmentStack = player.getItemInHand(player.getUsedItemHand()); // Remove .copy()

// Load component and ensure it's initialized
        this.orbmentComponent = OrbmentItem.loadComponent(orbmentStack, player.level(), player.level().isClientSide() ? null : (ServerPlayer) player);
        if (!this.orbmentComponent.areLinesInitialized() && !player.level().isClientSide()) {
            this.orbmentComponent.initializeLines();
            OrbmentItem.saveComponent(orbmentStack, this.orbmentComponent, player.level(), (ServerPlayer) player);
        }
        // Don't save here - just use what was loaded
        this.orbmentHandler = orbmentComponent.getInventory();

        // Rest of the constructor remains the same...
        int cx = 255;
        int cy = 95;
        int r = 40;
        for (int i = 0; i < ORBMENT_SLOT_COUNT; i++) {
            final int idx = i;
            double ang = 2 * Math.PI * i / ORBMENT_SLOT_COUNT - Math.PI / 2;
            int x = (int) (cx + r * Math.cos(ang));
            int y = (int) (cy + r * Math.sin(ang));
            addSlot(new SlotItemHandler(orbmentHandler, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    if (!orbmentComponent.isSlotUnlocked(idx)) {
                        return false;
                    }
                    return orbmentComponent.isQuartzValidForSlot(idx, s);
                }

                @Override
                public boolean isActive() {
                    return orbmentComponent.isSlotUnlocked(idx);
                }
            });
        }

        int invX = 16;
        int invY = 32;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, invX + col * 18, invY + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < ORBMENT_SLOT_COUNT) { // From Orbment to Player
                if (!this.moveItemStackTo(itemstack1, ORBMENT_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemstack1.getItem() instanceof QuartzItem) { // From Player to Orbment
                if (!this.moveItemStackTo(itemstack1, 0, ORBMENT_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= ORBMENT_SLOT_COUNT && index < this.slots.size() - 9) { // From Player main inventory to hotbar
                if (!this.moveItemStackTo(itemstack1, this.slots.size() - 9, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= this.slots.size() - 9) { // From Player hotbar to main inventory
                if (!this.moveItemStackTo(itemstack1, ORBMENT_SLOT_COUNT, this.slots.size() - 9, false)) {
                    return ItemStack.EMPTY;
                }
            }


            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }


    @Override
    public void removed(Player player) {
        super.removed(player);
        // --- ADDED: Play close sound ---
        if (player.level().isClientSide) {
            player.playSound(ModSoundEvents.ORBMENT_MENU_CLOSE.get(), 0.7F, 1.0F);
        }
        orbmentComponent.recalculate();
        if (!player.level().isClientSide()) {
            // Find the actual orbment in the player's inventory and save to that
            ItemStack actualOrbment = player.getItemInHand(player.getUsedItemHand());
            if (actualOrbment.getItem() instanceof OrbmentItem) {
                OrbmentItem.saveComponent(actualOrbment, this.orbmentComponent, player.level(), (ServerPlayer) player);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack currentStack = player.getItemInHand(player.getUsedItemHand());
        return !currentStack.isEmpty() && currentStack.getItem() instanceof OrbmentItem;
    }

    public OrbmentComponent getOrbmentComponent() {
        return orbmentComponent;
    }
}
