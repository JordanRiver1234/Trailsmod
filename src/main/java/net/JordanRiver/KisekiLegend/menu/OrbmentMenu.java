package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;

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
        this.orbmentStack = player.getItemInHand(player.getUsedItemHand());
        this.orbmentComponent = OrbmentItem.loadComponent(orbmentStack, player.level());
        this.orbmentHandler = orbmentComponent.getInventory();

        // --- Orbment Core slots (Center X coordinate adjusted for centering) ---
        int cx = 255; // MOVED RIGHT to center the panel and text over the slots
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

        // --- Player inventory (UNCHANGED from your file) ---
        int invX = 8;
        int invY = 20;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        // Hotbar
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
        orbmentComponent.recalculate();
        OrbmentItem.saveComponent(orbmentStack, orbmentComponent, player.level());
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(player.getUsedItemHand()) == this.orbmentStack;
    }

    public OrbmentComponent getOrbmentComponent() {
        return orbmentComponent;
    }
}
