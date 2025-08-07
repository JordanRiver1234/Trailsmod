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
        final int materialSlotY = 144;

        // ADD WEAPON SLOT FIRST (this was missing!)
        this.addSlot(new WeaponSlot(blockEntity.getInventory(), 0, -1000, -1000)); // Hidden position, rendered in 3D

        // Material slots in correct visual order (SM, E, W, F, Wi, T, S, M)
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), SEPITH_MASS_SLOT, ModItems.SEPITH_MASS.get(), 20, materialSlotY));  // SM
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), EARTH_MASS_SLOT, ModItems.EARTH_MASS.get(), 43, materialSlotY));   // E
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), WATER_MASS_SLOT, ModItems.WATER_MASS.get(), 66, materialSlotY));   // W
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), FIRE_MASS_SLOT, ModItems.FIRE_MASS.get(), 89, materialSlotY));     // F
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), WIND_MASS_SLOT, ModItems.WIND_MASS.get(), 112, materialSlotY));    // Wi
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), TIME_MASS_SLOT, ModItems.TIME_MASS.get(), 135, materialSlotY));    // T
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), SPACE_MASS_SLOT, ModItems.SPACE_MASS.get(), 158, materialSlotY));  // S
        this.addSlot(new ElementalMassSlot(blockEntity.getInventory(), MIRAGE_MASS_SLOT, ModItems.MIRAGE_MASS.get(), 181, materialSlotY)); // M

        // Player inventory
        int playerInvX = 17;
        int playerInvY = 168;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, playerInvX + j * 18, playerInvY + i * 18));
            }
        }

        // Player hotbar
        int hotbarY = 226;
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, playerInvX + k * 18, hotbarY));
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

            // Container slots: 0-8 (weapon + 8 material slots)
            // Player inventory: 9-35 (27 slots)
            // Player hotbar: 36-44 (9 slots)

            if (index < 9) {
                // Moving FROM container TO player inventory
                if (!this.moveItemStackTo(stackInSlot, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving FROM player inventory TO container
                boolean moved = false;

                if (isWeaponOrTool(stackInSlot)) {
                    // Try weapon slot (index 0)
                    if (this.slots.get(0).mayPlace(stackInSlot) && !this.slots.get(0).hasItem()) {
                        ItemStack toMove = stackInSlot.split(1); // Only move 1 weapon
                        this.slots.get(0).set(toMove);
                        moved = true;
                    }
                } else if (isElementalMass(stackInSlot)) {
                    // Try appropriate elemental slot
                    moved = moveToElementalSlot(stackInSlot);
                }

                // If specific placement failed, try any available container slot
                if (!moved) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
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
        // Check if the target slot is empty before moving
        if (stack.is(ModItems.SEPITH_MASS.get()) && !this.slots.get(1).hasItem()) {
            return this.moveItemStackTo(stack, 1, 2, false);
        } else if (stack.is(ModItems.EARTH_MASS.get()) && !this.slots.get(2).hasItem()) {
            return this.moveItemStackTo(stack, 2, 3, false);
        } else if (stack.is(ModItems.WATER_MASS.get()) && !this.slots.get(3).hasItem()) {
            return this.moveItemStackTo(stack, 3, 4, false);
        } else if (stack.is(ModItems.FIRE_MASS.get()) && !this.slots.get(4).hasItem()) {
            return this.moveItemStackTo(stack, 4, 5, false);
        } else if (stack.is(ModItems.WIND_MASS.get()) && !this.slots.get(5).hasItem()) {
            return this.moveItemStackTo(stack, 5, 6, false);
        } else if (stack.is(ModItems.TIME_MASS.get()) && !this.slots.get(6).hasItem()) {
            return this.moveItemStackTo(stack, 6, 7, false);
        } else if (stack.is(ModItems.SPACE_MASS.get()) && !this.slots.get(7).hasItem()) {
            return this.moveItemStackTo(stack, 7, 8, false);
        } else if (stack.is(ModItems.MIRAGE_MASS.get()) && !this.slots.get(8).hasItem()) {
            return this.moveItemStackTo(stack, 8, 9, false);
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