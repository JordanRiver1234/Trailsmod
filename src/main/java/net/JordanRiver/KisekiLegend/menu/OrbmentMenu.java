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
    private final OrbmentComponent   orbmentComponent;
    private final ItemStack          orbmentStack;

    // ← ONLY this 2-arg constructor
    public OrbmentMenu(int id, Inventory inv) {
        super(ModMenuTypes.ORBMENT_MENU.get(), id);

        Player player = inv.player;
        // read your stack + NBT
        this.orbmentStack     = player.getItemInHand(player.getUsedItemHand());
        this.orbmentComponent = OrbmentItem.loadComponent(orbmentStack, player.level());
        this.orbmentHandler   = orbmentComponent.getInventory();

        // radial slots
        int cx = 240, cy = 100, r = 40;
        for (int i = 0; i < ORBMENT_SLOT_COUNT; i++) {
            final int idx = i;
            double ang = 2*Math.PI*i/ORBMENT_SLOT_COUNT - Math.PI/2;
            int x = (int)(cx + r*Math.cos(ang)) - 9;
            int y = (int)(cy + r*Math.sin(ang)) - 9;
            addSlot(new SlotItemHandler(orbmentHandler, i, x, y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return idx < orbmentComponent.getUnlockedSlots()
                            && s.getItem() instanceof QuartzItem;
                }
                @Override public boolean isActive() {
                    return idx < orbmentComponent.getUnlockedSlots();
                }
            });
        }

        // player inventory
        int invX = 32, invY = 32;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row*9 + 9, invX + col*18, invY + row*18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, invX + col*18, invY + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        orbmentComponent.recalculate();
        OrbmentItem.saveInventory(
                orbmentStack,
                orbmentHandler,
                orbmentComponent.getUnlockedSlots(),
                player.level()
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Expose for your screen to read sepith + unlocked slots */
    public OrbmentComponent getOrbmentComponent() {
        return orbmentComponent;
    }
}