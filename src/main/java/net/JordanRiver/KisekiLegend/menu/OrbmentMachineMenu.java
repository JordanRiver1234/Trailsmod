package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class OrbmentMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final OrbmentMachineBlockEntity machine;
    private int selectedSlot = -1; // -1 for no selection

    public OrbmentMachineMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, (OrbmentMachineBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public OrbmentMachineMenu(int id, Inventory playerInv, OrbmentMachineBlockEntity machine) {
        super(ModMenuTypes.ORBMENT_MACHINE.get(), id);
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        this.machine = machine;

        // Dummy slot for orbment data
        NonNullList<ItemStack> orbmentList = NonNullList.withSize(1, machine.getOrbment());
        addSlot(new SlotItemHandler(new ItemStackHandler(orbmentList), 0, -100, -100) {
            @Override public boolean mayPlace(@org.jetbrains.annotations.NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player playerIn) { return false; }
        });

        // Player inventory on the left
        int invX = 16;
        int invY = 32;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        // Hotbar on the left
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
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
        // IDs 0-5: Select a slot
        if (id >= 0 && id <= 5) {
            this.selectedSlot = id;
            return true;
        }

        // ID 8 is convert, doesn't need a selected slot
        if (id == 8) {
            machine.tryConvertOneMass(player);
            return true;
        }

        // Other actions require a slot to be selected
        if (this.selectedSlot == -1) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Please select a slot first."));
            return false;
        }

        switch (id) {
            case 6: // Unlock Selected Slot
                machine.tryUnlockSlot(player, this.selectedSlot);
                break;
            case 7: // Remove Sepith Line from Selected Slot
                machine.tryRemoveSepithLine(player, this.selectedSlot);
                break;
            // IDs 9-15: Set Sepith Line
            case 9: machine.trySetSepithLine(player, this.selectedSlot, Element.EARTH); break;
            case 10: machine.trySetSepithLine(player, this.selectedSlot, Element.WATER); break;
            case 11: machine.trySetSepithLine(player, this.selectedSlot, Element.FIRE); break;
            case 12: machine.trySetSepithLine(player, this.selectedSlot, Element.WIND); break;
            case 13: machine.trySetSepithLine(player, this.selectedSlot, Element.TIME); break;
            case 14: machine.trySetSepithLine(player, this.selectedSlot, Element.SPACE); break;
            case 15: machine.trySetSepithLine(player, this.selectedSlot, Element.MIRAGE); break;
            default:
                return false;
        }
        broadcastChanges(); // Ensure client gets updated orbment data
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.slots.get(0).getItem() != machine.getOrbment()) {
            this.slots.get(0).set(machine.getOrbment());
        }
    }

    public ItemStack getOrbmentStack() {
        return machine.getOrbment();
    }
}
