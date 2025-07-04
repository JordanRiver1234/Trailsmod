package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import java.util.*;

public class OrbmentMachineBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack orbment = ItemStack.EMPTY;
    private int unlockAnimationTimer = 0;

    public OrbmentMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBMENT_MACHINE.get(), pos, state);
    }

    public boolean hasOrbment() {
        return !orbment.isEmpty();
    }

    public ItemStack getOrbment() {
        return orbment;
    }

    public void setOrbment(ItemStack stack) {
        this.orbment = stack;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Orbment Machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OrbmentMachineMenu(id, inv, this);
    }

    public void unlockNextSlot(Player player) {
        if (!(orbment.getItem() instanceof OrbmentItem)) return;
        var comp = OrbmentItem.loadComponent(orbment, level);
        int curr = comp.getUnlockedSlots();
        if (curr < OrbmentComponent.MAX_SLOTS) {
            comp.setUnlockedSlots(curr + 1);
            ItemStack newOrbment = orbment.copy();
            OrbmentItem.saveInventory(newOrbment, comp.getInventory(), comp.getUnlockedSlots(), level);
            this.orbment = newOrbment;
            this.unlockAnimationTimer = 20;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void tickClientParticles() {
        if (unlockAnimationTimer > 0 && level instanceof ServerLevel server) {
            --unlockAnimationTimer;
            if (unlockAnimationTimer % 4 == 0) {
                server.sendParticles(
                        ParticleTypes.ENCHANT,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        8,
                        0.3, 0.3, 0.3,
                        0.1
                );
            }
        }
    }
    public void tryUnlockSlotWithSepith(Player player, int req) {
        if (!(orbment.getItem() instanceof OrbmentItem)) return;

        // 1) Count total available Sepith mass
        int totalAmount = 0;
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || s.getItem() != ModItems.SEPITH_MASS.get()) continue;
            CustomData data = s.get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            CompoundTag tag = data.copyTag();
            int amtPerItem = tag.getInt("amount");
            if (amtPerItem <= 0) continue;
            totalAmount += amtPerItem * s.getCount();
            candidates.add(s);
        }
        if (totalAmount < req) {
            player.displayClientMessage(Component.literal("❌ Not enough Sepith Mass!"), true);
            return;
        }

        // 2) Precise consumption: use whole items first, then a partial if needed
        int remaining = req;
        for (ItemStack s : candidates) {
            CompoundTag tag = s.get(DataComponents.CUSTOM_DATA).copyTag();
            int amtPerItem = tag.getInt("amount");
            int count = s.getCount();

            // a) Remove as many whole items as we can
            int wholeToConsume = Math.min(count, remaining / amtPerItem);
            if (wholeToConsume > 0) {
                remaining -= wholeToConsume * amtPerItem;
                s.shrink(wholeToConsume);
                count -= wholeToConsume;
            }

            // b) If we still need more, do a partial consume on one remaining item
            if (remaining > 0 && count > 0) {
                int newAmt = amtPerItem - remaining;
                tag.putInt("amount", newAmt);
                s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                remaining = 0;
            }

            if (remaining <= 0) break;
        }

        // 3) Finally, unlock the next slot
        unlockNextSlot(player);
    }


    public void tryConvertFullSetToSepith(Player player) {
        String[] elements = {"earth", "water", "fire", "wind", "time", "space", "mirage"};
        Map<String, Integer> counts = getElementalMassCounts(player);
        for (String e : elements) {
            if (counts.getOrDefault(e, 0) < 1) {
                player.displayClientMessage(Component.literal("❌ Missing 1x " + e + " mass!"), true);
                return;
            }
        }
        for (String e : elements) {
            Item target = switch (e) {
                case "earth" -> ModItems.EARTH_MASS.get();
                case "water" -> ModItems.WATER_MASS.get();
                case "fire"  -> ModItems.FIRE_MASS.get();
                case "wind"  -> ModItems.WIND_MASS.get();
                case "time"  -> ModItems.TIME_MASS.get();
                case "space" -> ModItems.SPACE_MASS.get();
                case "mirage"-> ModItems.MIRAGE_MASS.get();
                default        -> null;
            };
            if (target == null) continue;
            int toRemove = 1;
            for (ItemStack s : player.getInventory().items) {
                if (s.isEmpty() || s.getItem() != target) continue;
                CompoundTag tag = s.get(DataComponents.CUSTOM_DATA).copyTag();
                int available = tag.getInt(e);
                int used = Math.min(available, toRemove);
                tag.putInt(e, available - used);
                s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                if (tag.getInt(e) <= 0) s.shrink(1);
                toRemove -= used;
                if (toRemove <= 0) break;
            }
        }
        // spawn mixed sepith
        ItemStack sep = new ItemStack(ModItems.SEPITH_MASS.get());
        CompoundTag st = new CompoundTag();
        st.putString("element", "mixed");
        st.putInt("amount", 1);
        sep.set(DataComponents.CUSTOM_DATA, CustomData.of(st));
        if (level instanceof ServerLevel srv) {
            srv.addFreshEntity(new ItemEntity(
                    srv,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                    sep
            ));
        }
        player.displayClientMessage(Component.literal("✔ Converted full set to Sepith Mass"), true);
    }

    public Map<String, Integer> getElementalMassCounts(Player player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var e : List.of("earth", "water", "fire", "wind", "time", "space", "mirage"))
            counts.put(e, 0);
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty()) continue;
            CustomData d = s.get(DataComponents.CUSTOM_DATA);
            if (d == null) continue;
            CompoundTag tag = d.copyTag();
            for (String e : counts.keySet()) {
                if (tag.contains(e)) counts.merge(e, tag.getInt(e), Integer::sum);
            }
        }
        return counts;
    }
}
