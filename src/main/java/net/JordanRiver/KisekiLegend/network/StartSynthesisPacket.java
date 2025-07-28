package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Sent from CLIENT to SERVER when the "Start Synthesis" button is clicked.
 */
public class StartSynthesisPacket {
    private final BlockPos blockPos;

    public StartSynthesisPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(StartSynthesisPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static StartSynthesisPacket decode(FriendlyByteBuf buffer) {
        return new StartSynthesisPacket(buffer.readBlockPos());
    }

    public static void handle(StartSynthesisPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.level().isLoaded(packet.blockPos) && player.distanceToSqr(packet.blockPos.getCenter()) < 64) {
                if (player.level().getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity machine) {
                    machine.startSynthesis();
                }
            }
        });
        context.setPacketHandled(true);
    }
}