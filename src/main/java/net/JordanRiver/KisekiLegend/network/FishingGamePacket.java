package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.fishing.RodType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class FishingGamePacket {
    private final Vec3 waterPosition;
    private final String bait;
    private final String rodTypeName;

    public FishingGamePacket(Vec3 waterPosition, String bait, String rodTypeName) {
        this.waterPosition = waterPosition;
        this.bait = bait;
        this.rodTypeName = rodTypeName;
    }

    public static void encode(FishingGamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.waterPosition.x);
        buffer.writeDouble(packet.waterPosition.y);
        buffer.writeDouble(packet.waterPosition.z);
        buffer.writeUtf(packet.bait);
        buffer.writeUtf(packet.rodTypeName);
    }

    public static FishingGamePacket decode(FriendlyByteBuf buffer) {
        Vec3 waterPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        String bait = buffer.readUtf();
        String rodTypeName = buffer.readUtf();
        return new FishingGamePacket(waterPosition, bait, rodTypeName);
    }

    public static void handle(FishingGamePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            if (!context.isClientSide()) {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    // Initialize fish on server side
                    FishingGameManager.initializeFishOnServer(player, packet.waterPosition, packet.bait, packet.rodTypeName);
                }
            }
        });
        context.setPacketHandled(true);
    }
}