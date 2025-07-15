package net.JordanRiver.KisekiLegend.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.common.extensions.IForgeBlockEntity;

import javax.annotation.Nullable;
import java.util.*;

public class OrbmentMachineBlockEntity extends BlockEntity implements MenuProvider, IForgeBlockEntity {

    private ItemStack orbment = ItemStack.EMPTY;
    private int unlockAnimationTimer = 0;
    private boolean orbmentRemoved = false; // Track if orbment was manually removed

    public static final Codec<OrbmentMachineBlockEntity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(BlockEntity::getBlockPos),
            BlockState.CODEC.fieldOf("state").forGetter(BlockEntity::getBlockState),
            ItemStack.CODEC.optionalFieldOf("orbment", ItemStack.EMPTY)
                    .forGetter(machine -> machine.orbment)
    ).apply(instance, OrbmentMachineBlockEntity::new));

    public OrbmentMachineBlockEntity(BlockPos pos, BlockState state, ItemStack orbment) {
        super(ModBlockEntities.ORBMENT_MACHINE.get(), pos, state);
        this.orbment = orbment;
    }

    public OrbmentMachineBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, ItemStack.EMPTY);
    }

    public boolean hasOrbment() {
        return !orbment.isEmpty() && !orbmentRemoved;
    }

    public ItemStack getOrbment() {
        return orbmentRemoved ? ItemStack.EMPTY : orbment;
    }

    public void setOrbment(ItemStack stack) {
        this.orbment = stack;
        this.orbmentRemoved = stack.isEmpty();
        setChanged();

        if (level != null && !level.isClientSide) {
            // Send update packet to all nearby players
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            if (level instanceof ServerLevel serverLevel) {
                Packet<ClientGamePacketListener> packet = getUpdatePacket();
                if (packet != null) {
                    for (Player player : serverLevel.players()) {
                        if (player.distanceToSqr(worldPosition.getCenter()) < 64 * 64) {
                            ((ServerPlayer) player).connection.send(packet);
                        }
                    }
                }
            }
        }
    }

    public void removeOrbment() {
        this.orbmentRemoved = true;
        this.orbment = ItemStack.EMPTY;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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

        int totalAmount = 0;
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || s.getItem() != ModItems.SEPITH_MASS.get()) continue;
            CustomData data = s.get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            int amtPerItem = data.copyTag().getInt("amount");
            if (amtPerItem <= 0) continue;
            totalAmount += amtPerItem * s.getCount();
            candidates.add(s);
        }

        if (totalAmount < req) {
            player.displayClientMessage(Component.literal("❌ Not enough Sepith Mass!"), true);
            return;
        }

        int remaining = req;
        for (ItemStack s : candidates) {
            CompoundTag tag = s.get(DataComponents.CUSTOM_DATA).copyTag();
            int amtPerItem = tag.getInt("amount");
            int count = s.getCount();

            int wholeToConsume = Math.min(count, remaining / amtPerItem);
            if (wholeToConsume > 0) {
                remaining -= wholeToConsume * amtPerItem;
                s.shrink(wholeToConsume);
                count -= wholeToConsume;
            }

            if (remaining > 0 && count > 0) {
                int newAmt = amtPerItem - remaining;
                tag.putInt("amount", newAmt);
                s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                remaining = 0;
            }

            if (remaining <= 0) break;
        }

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
                case "fire" -> ModItems.FIRE_MASS.get();
                case "wind" -> ModItems.WIND_MASS.get();
                case "time" -> ModItems.TIME_MASS.get();
                case "space" -> ModItems.SPACE_MASS.get();
                case "mirage" -> ModItems.MIRAGE_MASS.get();
                default -> null;
            };

            if (target == null) continue;

            int toRemove = 1;
            for (ItemStack s : player.getInventory().items) {
                if (s.isEmpty() || s.getItem() != target) continue;
                CustomData data = s.get(DataComponents.CUSTOM_DATA);
                if (data == null) continue;
                CompoundTag tag = data.copyTag();
                int available = tag.getInt(e);
                int used = Math.min(available, toRemove);
                tag.putInt(e, available - used);
                s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                if (tag.getInt(e) <= 0) s.shrink(1);
                toRemove -= used;
                if (toRemove <= 0) break;
            }
        }

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!orbment.isEmpty()) {
            tag.put("Orbment", orbment.save(provider, new CompoundTag()));
        }
        tag.putBoolean("OrbmentRemoved", orbmentRemoved);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Orbment")) {
            orbment = ItemStack.parse(provider, tag.getCompound("Orbment")).orElse(ItemStack.EMPTY);
        } else {
            orbment = ItemStack.EMPTY;
        }
        orbmentRemoved = tag.getBoolean("OrbmentRemoved");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, provider) -> {
            CompoundTag tag = new CompoundTag();
            if (!orbment.isEmpty()) {
                tag.put("Orbment", orbment.save(provider, new CompoundTag()));
            }
            tag.putBoolean("OrbmentRemoved", orbmentRemoved);
            return tag;
        });
    }

    // Correct method signature for Forge 1.21.1
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        if (!orbment.isEmpty()) {
            tag.put("Orbment", orbment.save(provider, new CompoundTag()));
        }
        tag.putBoolean("OrbmentRemoved", orbmentRemoved);
        return tag;
    }

    // Correct method signature for Forge 1.21.1
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        if (tag.contains("Orbment")) {
            this.orbment = ItemStack.parse(provider, tag.getCompound("Orbment")).orElse(ItemStack.EMPTY);
        } else {
            this.orbment = ItemStack.EMPTY;
        }
        this.orbmentRemoved = tag.getBoolean("OrbmentRemoved");

        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            if (tag.contains("Orbment")) {
                this.orbment = ItemStack.parse(provider, tag.getCompound("Orbment")).orElse(ItemStack.EMPTY);
            } else {
                this.orbment = ItemStack.EMPTY;
            }
            this.orbmentRemoved = tag.getBoolean("OrbmentRemoved");

            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
}