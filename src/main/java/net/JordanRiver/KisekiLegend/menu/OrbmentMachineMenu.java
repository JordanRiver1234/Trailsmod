package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public class OrbmentMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final OrbmentMachineBlockEntity machine;

    public OrbmentMachineMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv,
                (OrbmentMachineBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos())
        );
    }

    public OrbmentMachineMenu(int id, Inventory playerInv, OrbmentMachineBlockEntity machine) {
        super(ModMenuTypes.ORBMENT_MACHINE.get(), id);
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        this.machine = machine;

        // Expose the machine's orbment as slot 0 at (8,8)
        IItemHandlerModifiable handler = new IItemHandlerModifiable() {
            @Override public int getSlots() { return 1; }
            @Override public ItemStack getStackInSlot(int slot) { return machine.getOrbment(); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!simulate) machine.setOrbment(stack);
                return ItemStack.EMPTY;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack out = machine.getOrbment().copy();
                if (!simulate) machine.setOrbment(ItemStack.EMPTY);
                return out;
            }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof OrbmentItem;
            }
            @Override public void setStackInSlot(int slot, ItemStack stack) {
                machine.setOrbment(stack);
            }
        };
        this.addSlot(new SlotItemHandler(handler, 0, 8, 8));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 214 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 272));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                        level.getBlockEntity(pos) == machine && player.distanceToSqr(pos.getCenter()) <= 64.0,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Use 1 Sepith Mass per unlock instead of 5
            machine.tryUnlockSlotWithSepith(player, 1);
            return true;
        } else if (id == 1) {
            machine.tryConvertFullSetToSepith(player);
            return true;
        }
        return false;
    }

    /**
     * Returns the underlying BlockEntity.
     */
    public OrbmentMachineBlockEntity getMachine() {
        return machine;
    }

    /**
     * Convenience: get the current orbment stack.
     */
    public ItemStack getOrbmentStack() {
        return machine.getOrbment();
    }
}