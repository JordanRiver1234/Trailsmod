package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class InsertMaterialPacket {
    private final BlockPos blockPos;
    private final String nodeId;
    private final int slotIndex;
    private final String materialType;

    public InsertMaterialPacket(BlockPos blockPos, String nodeId, int slotIndex, String materialType) {
        this.blockPos = blockPos;
        this.nodeId = nodeId;
        this.slotIndex = slotIndex;
        this.materialType = materialType;
    }

    public static void encode(InsertMaterialPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.nodeId);
        buffer.writeInt(packet.slotIndex);
        buffer.writeUtf(packet.materialType);
    }

    public static InsertMaterialPacket decode(FriendlyByteBuf buffer) {
        return new InsertMaterialPacket(buffer.readBlockPos(), buffer.readUtf(), buffer.readInt(), buffer.readUtf());
    }

    public static void handle(InsertMaterialPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            System.out.println("=== SERVER PACKET RECEIVED ===");
            System.out.println("BlockPos: " + packet.blockPos);
            System.out.println("NodeId: " + packet.nodeId);
            System.out.println("SlotIndex: " + packet.slotIndex);
            System.out.println("MaterialType: " + packet.materialType);

            ServerPlayer player = context.getSender();
            if (player == null) {
                System.out.println("FAIL: Player is null");
                return;
            }

            if (!player.level().isLoaded(packet.blockPos)) {
                System.out.println("FAIL: Chunk not loaded");
                return;
            }

            if (player.distanceToSqr(packet.blockPos.getCenter()) >= 64) {
                System.out.println("FAIL: Player too far away");
                return;
            }

            if (player.level().getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity machine) {
                System.out.println("SUCCESS: Calling insertMaterialFromSlot");
                machine.insertMaterialFromSlot(player, packet.nodeId, packet.materialType, packet.slotIndex);
            } else {
                System.out.println("FAIL: Block entity not found or wrong type");
            }

            System.out.println("=== END SERVER PACKET ===");
        });
        context.setPacketHandled(true);
    }
}