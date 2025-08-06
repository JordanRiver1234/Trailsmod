package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Optional;

public class OrbalTableSyncPacket {
    private final BlockPos blockPos;
    private final boolean monitorOpen;
    private final boolean chainsawActive;
    private final int operationProgress;
    private final ItemStack weapon;
    private final int monitorOpenTicks; // ADD THIS LINE

    public OrbalTableSyncPacket(BlockPos pos, boolean monitorOpen, boolean chainsawActive, int operationProgress, ItemStack weapon, int monitorOpenTicks) {
        this.blockPos = pos;
        this.monitorOpen = monitorOpen;
        this.chainsawActive = chainsawActive;
        this.operationProgress = operationProgress;
        this.weapon = weapon;
        this.monitorOpenTicks = monitorOpenTicks; // Add this line

    }

    public static void encode(OrbalTableSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeBoolean(packet.monitorOpen);
        buffer.writeBoolean(packet.chainsawActive);
        buffer.writeInt(packet.operationProgress);
        buffer.writeInt(packet.monitorOpenTicks); // Add this line



        // Write weapon data
        if (packet.weapon.isEmpty()) {
            buffer.writeBoolean(false);
            System.out.println("Encoded: NO WEAPON");
        } else {
            buffer.writeBoolean(true);
            buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(packet.weapon.getItem()));
            buffer.writeInt(packet.weapon.getCount());
            buffer.writeInt(packet.weapon.getDamageValue());

            // Write NBT data directly as CompoundTag
            CompoundTag itemTag = new CompoundTag();
            // Save all components including custom data
            try {
                // Manual component serialization for cross-platform compatibility
                CustomData customData = packet.weapon.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    itemTag.put("custom_data", customData.copyTag());
                }

                // Add other important components if needed
                if (packet.weapon.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                    // Store attribute modifiers info if needed
                    itemTag.putBoolean("has_modifiers", true);
                }

                buffer.writeNbt(itemTag);
            } catch (Exception e) {
                buffer.writeNbt(new CompoundTag()); // Empty tag as fallback
            }
        }
    }

    public static OrbalTableSyncPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        boolean monitorOpen = buffer.readBoolean();
        boolean chainsawActive = buffer.readBoolean();
        int operationProgress = buffer.readInt();
        int monitorOpenTicks = buffer.readInt(); // Add this line

        ItemStack weapon = ItemStack.EMPTY;
        if (buffer.readBoolean()) {
            ResourceLocation itemId = buffer.readResourceLocation();
            int count = buffer.readInt();
            int damage = buffer.readInt();
            CompoundTag itemTag = buffer.readNbt();



            try {
                Item item = BuiltInRegistries.ITEM.get(itemId);
                weapon = new ItemStack(item, count);
                weapon.setDamageValue(damage);

                // Restore NBT data
                if (itemTag != null && itemTag.contains("custom_data")) {
                    weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(itemTag.getCompound("custom_data")));
                }

                System.out.println("Decoded weapon: " + weapon.getDisplayName().getString());
            } catch (Exception e) {
                System.out.println("Error decoding weapon: " + e.getMessage());
                weapon = ItemStack.EMPTY;
            }
        } else {
        }

        return new OrbalTableSyncPacket(pos, monitorOpen, chainsawActive, operationProgress, weapon, monitorOpenTicks);
    }
    public static void handle(OrbalTableSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(packet.blockPos) instanceof OrbalTableBlockEntity blockEntity) {


                blockEntity.updateClientState(packet.monitorOpen, packet.chainsawActive, packet.operationProgress, packet.monitorOpenTicks);

                // CRITICAL: Set client weapon and sync to inventory
                blockEntity.setClientWeapon(packet.weapon);

                // FIXED: Also update inventory slot on client side
                if (!packet.weapon.isEmpty()) {
                    blockEntity.getInventory().setStackInSlot(0, packet.weapon.copy());
                } else {
                    blockEntity.getInventory().setStackInSlot(0, ItemStack.EMPTY);
                }

                // Force model data update
                blockEntity.requestModelDataUpdate();

                // Force renderer refresh
                if (mc.levelRenderer != null) {
                    mc.levelRenderer.setBlocksDirty(
                            packet.blockPos.getX() - 1, packet.blockPos.getY() - 1, packet.blockPos.getZ() - 1,
                            packet.blockPos.getX() + 1, packet.blockPos.getY() + 1, packet.blockPos.getZ() + 1
                    );
                }

            }
        });
        context.setPacketHandled(true);
    }
}