package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class OrbalTableMenu extends AbstractContainerMenu {
    private final OrbalTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    // Slot indices
    public static final int WEAPON_SLOT = 0;
    public static final int EARTH_MASS_SLOT = 1;
    public static final int WATER_MASS_SLOT = 2;
    public static final int FIRE_MASS_SLOT = 3;
    public static final int WIND_MASS_SLOT = 4;
    public static final int TIME_MASS_SLOT = 5;
    public static final int SPACE_MASS_SLOT = 6;
    public static final int MIRAGE_MASS_SLOT = 7;
    public static final int SEPITH_MASS_SLOT = 8;

    public OrbalTableMenu(int containerId, Inventory playerInventory, OrbalTableBlockEntity blockEntity, BlockPos pos) {
        super(ModMenuTypes.ORBAL_TABLE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), pos);

        // FIXED: Make elemental mass slots more visible - positioned below weapon display
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory() , SEPITH_MASS_SLOT, ModItems.SEPITH_MASS.get(), 0, 140)); // Far left position

        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), EARTH_MASS_SLOT, ModItems.EARTH_MASS.get(), 20, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), WATER_MASS_SLOT, ModItems.WATER_MASS.get(), 40, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), FIRE_MASS_SLOT, ModItems.FIRE_MASS.get(), 60, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), WIND_MASS_SLOT, ModItems.WIND_MASS.get(), 80, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), TIME_MASS_SLOT, ModItems.TIME_MASS.get(), 100, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), SPACE_MASS_SLOT, ModItems.SPACE_MASS.get(), 120, 140));
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), MIRAGE_MASS_SLOT, ModItems.MIRAGE_MASS.get(), 140, 140));

        // Player inventory positioned more to the left for 256x240 layout
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 17 + j * 18, 158 + i * 18)); // Changed from 48 to 28
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 17 + k * 18, 216)); // Changed from 48 to 28 7
        }
    }
    // Add this constructor to OrbalTableMenu for network creation
    public OrbalTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), extraData.readBlockPos());
    }


    private static OrbalTableBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof OrbalTableBlockEntity blockEntity) {
            return blockEntity;
        }
        throw new IllegalStateException("Block entity not found at position: " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            // If clicking on block entity slots (0-8) - changed from 0-7
            if (index < 9) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(stackInSlot, 9, this.slots.size(), true)) { // Changed from 8 to 9
                    return ItemStack.EMPTY;
                }

            } else {
                // If clicking on player inventory
                if (isWeaponOrTool(stackInSlot)) {
                    // Move to weapon slot
                    if (!this.moveItemStackTo(stackInSlot, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isElementalMass(stackInSlot)) {
                    // AUTO-MOVE elemental mass to correct slots
                    if (!moveToElementalSlot(stackInSlot)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    private boolean moveToElementalSlot(ItemStack stack) {
        if (stack.is(ModItems.SEPITH_MASS.get())) {
            return this.moveItemStackTo(stack, SEPITH_MASS_SLOT, SEPITH_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.EARTH_MASS.get())) {
            return this.moveItemStackTo(stack, EARTH_MASS_SLOT, EARTH_MASS_SLOT + 1, false);

        } else if (stack.is(ModItems.WATER_MASS.get())) {
            return this.moveItemStackTo(stack, WATER_MASS_SLOT, WATER_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.FIRE_MASS.get())) {
            return this.moveItemStackTo(stack, FIRE_MASS_SLOT, FIRE_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.WIND_MASS.get())) {
            return this.moveItemStackTo(stack, WIND_MASS_SLOT, WIND_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.TIME_MASS.get())) {
            return this.moveItemStackTo(stack, TIME_MASS_SLOT, TIME_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.SPACE_MASS.get())) {
            return this.moveItemStackTo(stack, SPACE_MASS_SLOT, SPACE_MASS_SLOT + 1, false);
        } else if (stack.is(ModItems.MIRAGE_MASS.get())) {
            return this.moveItemStackTo(stack, MIRAGE_MASS_SLOT, MIRAGE_MASS_SLOT + 1, false);
        }
        return false;
    }

    private boolean isWeaponOrTool(ItemStack stack) {
        return stack.isDamageableItem() ||
                stack.getItem() instanceof net.minecraft.world.item.TieredItem ||
                stack.getItem() instanceof net.minecraft.world.item.SwordItem ||
                stack.getItem() instanceof net.minecraft.world.item.AxeItem ||
                stack.getItem() instanceof net.minecraft.world.item.PickaxeItem ||
                stack.getItem() instanceof net.minecraft.world.item.ShovelItem ||
                stack.getItem() instanceof net.minecraft.world.item.HoeItem ||
                stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                stack.getItem() instanceof net.minecraft.world.item.TridentItem;
    }

    private boolean isElementalMass(ItemStack stack) {
        return stack.is(ModItems.SEPITH_MASS.get()) ||
                stack.is(ModItems.EARTH_MASS.get()) ||
                stack.is(ModItems.WATER_MASS.get()) ||
                stack.is(ModItems.FIRE_MASS.get()) ||
                stack.is(ModItems.WIND_MASS.get()) ||
                stack.is(ModItems.TIME_MASS.get()) ||
                stack.is(ModItems.SPACE_MASS.get()) ||
                stack.is(ModItems.MIRAGE_MASS.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    public OrbalTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public static class WeaponSlot extends SlotItemHandler {
        public WeaponSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            System.out.println("=== WEAPON SLOT MAY PLACE ===");
            System.out.println("Checking item: " + stack.getDisplayName().getString());

            boolean canPlace = stack.isDamageableItem() ||
                    stack.getItem() instanceof net.minecraft.world.item.TieredItem ||
                    stack.getItem() instanceof net.minecraft.world.item.SwordItem ||
                    stack.getItem() instanceof net.minecraft.world.item.AxeItem ||
                    stack.getItem() instanceof net.minecraft.world.item.PickaxeItem ||
                    stack.getItem() instanceof net.minecraft.world.item.ShovelItem ||
                    stack.getItem() instanceof net.minecraft.world.item.HoeItem ||
                    stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                    stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                    stack.getItem() instanceof net.minecraft.world.item.TridentItem;

            System.out.println("Can place in weapon slot: " + canPlace);
            return canPlace;
        }



        @Override
        public void setChanged() {
            System.out.println("=== WEAPON SLOT CHANGED ===");
            super.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }



    }

    // Custom slot for specific elemental mass items
    public static class ElementalMassSlot extends SlotItemHandler {
        private final net.minecraft.world.item.Item allowedItem;

        public ElementalMassSlot(net.minecraftforge.items.IItemHandler itemHandler, int index,
                                 net.minecraft.world.item.Item allowedItem, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.allowedItem = allowedItem;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(allowedItem);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}