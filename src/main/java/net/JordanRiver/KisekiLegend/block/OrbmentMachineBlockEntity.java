package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class OrbmentMachineBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack orbment = ItemStack.EMPTY;

    public OrbmentMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBMENT_MACHINE.get(), pos, state);
    }

    // --- Core Data Management ---

    public boolean hasOrbment() {
        return !orbment.isEmpty();
    }

    public ItemStack getOrbment() {
        return orbment;
    }

    public void setOrbment(ItemStack stack) {
        this.orbment = stack.copy();
        setChangedAndSync();
    }

    public void removeOrbment() {
        this.orbment = ItemStack.EMPTY;
        setChangedAndSync();
    }

    private void saveOrbmentData(OrbmentComponent component, ServerPlayer player) {
        if (!orbment.isEmpty() && level != null) {
            OrbmentItem.saveComponent(orbment, component, level, player);
            setChangedAndSync();
        }
    }

    // --- Gameplay Logic ---

    public void tryUnlockSlot(Player player, int slot) {
        if (level == null || level.isClientSide || orbment.isEmpty() || slot < 0 || slot >= OrbmentComponent.MAX_SLOTS) return;

        OrbmentComponent component = OrbmentItem.loadComponent(orbment, level, (ServerPlayer) player);
        // Ensure lines are initialized but don't re-randomize if already set
        // Only initialize lines once when the orbment is first created
        if (!component.areLinesInitialized()) {
            component.initializeLines();
            saveOrbmentData(component, (ServerPlayer) player); // Save immediately after initialization
              }
        if (component.isSlotUnlocked(slot)) {
            player.sendSystemMessage(Component.literal("Slot is already unlocked."));
            return;
        }

        final int UNLOCK_COST = 1; // Cost in Sepith Mass
        if (findAndRemoveItems(player.getInventory(), ModItems.SEPITH_MASS.get(), UNLOCK_COST)) {
            component.unlockSlot(slot);
            saveOrbmentData(component, (ServerPlayer) player);
            level.playSound(null, getBlockPos(), ModSoundEvents.ORBMENT_SLOT_UNLOCK.get(), SoundSource.BLOCKS, 1.0f, 1.2f); // <-- MODIFIED
            player.sendSystemMessage(Component.literal("Unlocked slot " + (slot + 1) + "!"));
        } else {
            player.sendSystemMessage(Component.literal("You need " + UNLOCK_COST + " Sepith Mass to unlock a slot."));
        }
    }


    public void tryRemoveSepithLine(Player player, int slot) {
        if (level == null || level.isClientSide || orbment.isEmpty() || slot < 0 || slot >= OrbmentComponent.MAX_SLOTS) return;

        OrbmentComponent component = OrbmentItem.loadComponent(orbment, level, (ServerPlayer) player);
        if (!component.isSlotUnlocked(slot)) {
            player.sendSystemMessage(Component.literal("Cannot modify a locked slot."));
            return;
        }
        if (component.getSepithLines()[slot] == Element.NONE) {
            player.sendSystemMessage(Component.literal("Slot has no Sepith Line to remove."));
            return;
        }
        if (!component.getInventory().getStackInSlot(slot).isEmpty()){
            player.sendSystemMessage(Component.literal("Cannot modify a slot with a quartz in it."));
            return;
        }

        final int REMOVE_COST = 1; // Cost in Sepith Mass
        if (findAndRemoveItems(player.getInventory(), ModItems.SEPITH_MASS.get(), REMOVE_COST)) {
            component.removeSepithLine(slot);
            saveOrbmentData(component, (ServerPlayer) player);
            level.playSound(null, getBlockPos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
            player.sendSystemMessage(Component.literal("Reverted slot " + (slot + 1) + " to neutral."));
        } else {
            player.sendSystemMessage(Component.literal("You need " + REMOVE_COST + " Sepith Mass to revert a slot."));
        }
    }

    public void trySetSepithLine(Player player, int slot, Element element) {
        if (level == null || level.isClientSide || orbment.isEmpty() || slot < 0 || slot >= OrbmentComponent.MAX_SLOTS || element == Element.NONE) return;

        OrbmentComponent component = OrbmentItem.loadComponent(orbment, level, (ServerPlayer) player);
        if (!component.isSlotUnlocked(slot)) {
            player.sendSystemMessage(Component.literal("Cannot modify a locked slot."));
            return;
        }
        if (!component.getInventory().getStackInSlot(slot).isEmpty()){
            player.sendSystemMessage(Component.literal("Cannot modify a slot with a quartz in it."));
            return;
        }
        // BUG FIX: Add check to ensure the line is neutral before setting a new one.
        if (component.getSepithLines()[slot] != Element.NONE) {
            player.sendSystemMessage(Component.literal("You must remove the existing line before setting a new one."));
            return;
        }

        Item requiredMass = getMassItemForElement(element);
        if (requiredMass == null) {
            player.sendSystemMessage(Component.literal("Invalid element specified."));
            return;
        }

        final int SET_COST = 10;
        if (findAndRemoveItems(player.getInventory(), requiredMass, SET_COST)) {
            component.setSepithLine(slot, element);
            saveOrbmentData(component, (ServerPlayer) player);
            level.playSound(null, getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.5f);
            player.sendSystemMessage(Component.literal("Set slot " + (slot + 1) + " to a " + element.getName() + " line!"));
        } else {
            player.sendSystemMessage(Component.literal("You need " + SET_COST + " " + requiredMass.getDescription().getString() + " to set this line."));
        }
    }

    /**
     * Tries to convert one of each of the 7 elemental masses into a single Sepith Mass.
     * @param player The player attempting the conversion.
     */
    public void tryConvertOneMass(Player player) {
        if (level == null || level.isClientSide) return;

        final int CONVERSION_COST = 1;
        Item[] requiredMasses = {
                ModItems.EARTH_MASS.get(), ModItems.WATER_MASS.get(), ModItems.FIRE_MASS.get(),
                ModItems.WIND_MASS.get(), ModItems.TIME_MASS.get(), ModItems.SPACE_MASS.get(),
                ModItems.MIRAGE_MASS.get()
        };

        // Check if player has enough of all required masses
        boolean hasAllMasses = true;
        for (Item massItem : requiredMasses) {
            if (player.getInventory().countItem(massItem) < CONVERSION_COST) {
                hasAllMasses = false;
                break;
            }
        }

        if (hasAllMasses) {
            // Remove one of each mass
            for (Item massItem : requiredMasses) {
                findAndRemoveItems(player.getInventory(), massItem, CONVERSION_COST);
            }

            // Add one sepith mass
            player.getInventory().add(new ItemStack(ModItems.SEPITH_MASS.get(), 1));
            level.playSound(null, getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
            player.sendSystemMessage(Component.literal("Converted 7 elemental masses to 1 Sepith Mass."));
        } else {
            player.sendSystemMessage(Component.literal("You need at least " + CONVERSION_COST + " of each of the 7 elemental masses."));
        }
    }


    private boolean findAndRemoveItems(Inventory inventory, Item item, int count) {
        int foundCount = 0;

        // First pass: count how many we have
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                foundCount += stack.getCount();
            }
        }

        // Check if we have enough
        if (foundCount < count) {
            return false;
        }

        // Second pass: actually remove the items
        int remaining = count;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                // Clean up empty stacks
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        return true;
    }
    private Item getMassItemForElement(Element element) {
        return switch (element) {
            case EARTH -> ModItems.EARTH_MASS.get();
            case WATER -> ModItems.WATER_MASS.get();
            case FIRE -> ModItems.FIRE_MASS.get();
            case WIND -> ModItems.WIND_MASS.get();
            case TIME -> ModItems.TIME_MASS.get();
            case SPACE -> ModItems.SPACE_MASS.get();
            case MIRAGE -> ModItems.MIRAGE_MASS.get();
            default -> null;
        };
    }

    // --- Ticking and Rendering ---

    public void tickClientParticles() {
        if (level instanceof ServerLevel sl && hasOrbment() && sl.random.nextInt(10) == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                    1, 0, 0.1, 0, 0.01);
        }
    }

    // --- Menu Provider ---

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Orbment Machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInv, @NotNull Player player) {
        return new OrbmentMachineMenu(id, playerInv, this);
    }

    // --- NBT and Syncing ---

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasOrbment()) {
            tag.put("Orbment", orbment.save(provider));
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Orbment")) {
            orbment = ItemStack.parse(provider, tag.getCompound("Orbment")).orElse(ItemStack.EMPTY);
        } else {
            // This is crucial for visual updates when the orbment is removed.
            orbment = ItemStack.EMPTY;
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
