package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record OrbalTableRenderUpdatePacket(BlockPos blockPos, boolean hasWeapon) implements CustomPacketPayload {
    public static final Type<OrbalTableRenderUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "orbal_table_render_update")
    );

    public static final StreamCodec<FriendlyByteBuf, OrbalTableRenderUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OrbalTableRenderUpdatePacket::blockPos,
            ByteBufCodecs.BOOL, OrbalTableRenderUpdatePacket::hasWeapon,
            OrbalTableRenderUpdatePacket::new
    );

    @Override
    public Type<OrbalTableRenderUpdatePacket> type() {
        return TYPE;
    }

    // Use the same pattern as your other packets
    public static void encode(OrbalTableRenderUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeBoolean(packet.hasWeapon);
    }

    public static OrbalTableRenderUpdatePacket decode(FriendlyByteBuf buffer) {
        return new OrbalTableRenderUpdatePacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(OrbalTableRenderUpdatePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Force complete renderer refresh
                mc.levelRenderer.setBlocksDirty(
                        packet.blockPos.getX() - 1, packet.blockPos.getY() - 1, packet.blockPos.getZ() - 1,
                        packet.blockPos.getX() + 1, packet.blockPos.getY() + 1, packet.blockPos.getZ() + 1
                );
            }
        });
        context.setPacketHandled(true);
    }
}