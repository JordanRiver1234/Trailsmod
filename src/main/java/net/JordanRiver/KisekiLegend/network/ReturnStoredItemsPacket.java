package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ReturnStoredItemsPacket {
    private final BlockPos blockPos;

    public ReturnStoredItemsPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(ReturnStoredItemsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static ReturnStoredItemsPacket decode(FriendlyByteBuf buffer) {
        return new ReturnStoredItemsPacket(buffer.readBlockPos());
    }

    public static void handle(ReturnStoredItemsPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level().getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity blockEntity) {
                blockEntity.returnStoredItemsToPlayer(player);
                blockEntity.clearStoredItems();
                blockEntity.setChanged();
                blockEntity.syncToClient();
            }
        });
        context.setPacketHandled(true);
    }
}