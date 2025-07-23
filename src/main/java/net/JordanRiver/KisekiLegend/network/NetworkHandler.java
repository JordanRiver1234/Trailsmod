package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(SetFavoritePacket.class, id++)
                .encoder(SetFavoritePacket::encode)
                .decoder(SetFavoritePacket::decode)
                .consumerMainThread(SetFavoritePacket::handle)
                .add();

        INSTANCE.messageBuilder(SetSelectedArtPacket.class, id++)
                .encoder(SetSelectedArtPacket::encode)
                .decoder(SetSelectedArtPacket::decode)
                .consumerMainThread(SetSelectedArtPacket::handle)
                .add();
        INSTANCE.messageBuilder(OrbmentSyncPacket.class, id++)
                .encoder(OrbmentSyncPacket::encode)
                .decoder(OrbmentSyncPacket::decode)
                .consumerMainThread(OrbmentSyncPacket::handle)
                .add();
    }
    public static void sendToServer(Object packet) {
        INSTANCE.send(packet, net.minecraftforge.network.PacketDistributor.SERVER.noArg());
    }
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(packet, net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
    }
}