package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class RemoveMaterialPacket {
    private final BlockPos blockPos;
    private final String nodeId;
    private final String materialType;

    public RemoveMaterialPacket(BlockPos blockPos, String nodeId, String materialType) {
        this.blockPos = blockPos;
        this.nodeId = nodeId;
        this.materialType = materialType;
    }

    public static void encode(RemoveMaterialPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.nodeId);
        buffer.writeUtf(packet.materialType);
    }

    public static RemoveMaterialPacket decode(FriendlyByteBuf buffer) {
        return new RemoveMaterialPacket(buffer.readBlockPos(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(RemoveMaterialPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.level().isLoaded(packet.blockPos) && player.distanceToSqr(packet.blockPos.getCenter()) < 64) {
                if (player.level().getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity machine) {
                    machine.removeLastItemFromNode(packet.nodeId, packet.materialType, player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}