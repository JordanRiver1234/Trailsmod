package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class OrbmentSyncPacket {
    private final CompoundTag componentData;
    private final int inventorySlot;

    public OrbmentSyncPacket(CompoundTag componentData, int inventorySlot) {
        this.componentData = componentData;
        this.inventorySlot = inventorySlot;
    }

    // Copy the encode/decode pattern from SetFavoritePacket
    public static void encode(OrbmentSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.componentData);
        buffer.writeInt(packet.inventorySlot);
    }

    public static OrbmentSyncPacket decode(FriendlyByteBuf buffer) {
        return new OrbmentSyncPacket(buffer.readNbt(), buffer.readInt());
    }

    public static void handle(OrbmentSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            // CLIENT side - update the inventory item
            if (context.isClientSide()) {
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null && packet.inventorySlot >= 0) {
                    ItemStack stack = player.getInventory().getItem(packet.inventorySlot);
                    if (stack.getItem() instanceof OrbmentItem) {
                        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.of(packet.componentData));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}