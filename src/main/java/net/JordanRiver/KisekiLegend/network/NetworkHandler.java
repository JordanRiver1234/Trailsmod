// --- MODIFIED: net/JordanRiver/KisekiLegend/network/NetworkHandler.java ---
package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
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
        // The registration logic itself is correct and doesn't need to change.
        INSTANCE.messageBuilder(SetFavoritePacket.class, id++).encoder(SetFavoritePacket::encode).decoder(SetFavoritePacket::decode).consumerMainThread(SetFavoritePacket::handle).add();
        INSTANCE.messageBuilder(SetSelectedArtPacket.class, id++).encoder(SetSelectedArtPacket::encode).decoder(SetSelectedArtPacket::decode).consumerMainThread(SetSelectedArtPacket::handle).add();
        INSTANCE.messageBuilder(OrbmentSyncPacket.class, id++).encoder(OrbmentSyncPacket::encode).decoder(OrbmentSyncPacket::decode).consumerMainThread(OrbmentSyncPacket::handle).add();
        INSTANCE.messageBuilder(SelectQuartzRecipePacket.class, id++)
                .encoder(SelectQuartzRecipePacket::encode)
                .decoder(SelectQuartzRecipePacket::decode)
                .consumerMainThread(SelectQuartzRecipePacket::handle) // Select packet uses its own handle
                .add();
        // Make sure this line for your sync packet looks like this
        INSTANCE.messageBuilder(QuartzMachineSyncPacket.class, id++)
                .encoder(QuartzMachineSyncPacket::encode)
                .decoder(QuartzMachineSyncPacket::decode)
                .consumerMainThread(QuartzMachineSyncPacket::handle) // Ensure this is consumerMainThread
                .add();
        // ADD THESE THREE PACKETS
        INSTANCE.messageBuilder(InsertMaterialPacket.class, id++)
                .encoder(InsertMaterialPacket::encode)
                .decoder(InsertMaterialPacket::decode)
                .consumerMainThread(InsertMaterialPacket::handle)
                .add();
        INSTANCE.messageBuilder(RemoveMaterialPacket.class, id++)
                .encoder(RemoveMaterialPacket::encode)
                .decoder(RemoveMaterialPacket::decode)
                .consumerMainThread(RemoveMaterialPacket::handle)
                .add();
        INSTANCE.messageBuilder(StartSynthesisPacket.class, id++)
                .encoder(StartSynthesisPacket::encode)
                .decoder(StartSynthesisPacket::decode)
                .consumerMainThread(StartSynthesisPacket::handle)
                .add();
        INSTANCE.messageBuilder(ReturnStoredItemsPacket.class, id++)
                .encoder(ReturnStoredItemsPacket::encode)
                .decoder(ReturnStoredItemsPacket::decode)
                .consumerMainThread(ReturnStoredItemsPacket::handle)
                .add();
    }

    /**
     * Sends a packet from the client to the server.
     */
    public static void sendToServer(Object packet) {
        // CORRECTED: Use the general 'send' method with the SERVER distributor.
        // The argument order is (packet, target).
        INSTANCE.send(packet, PacketDistributor.SERVER.noArg());
    }

    /**
     * Sends a packet from the server to a specific player.
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        // The packet object comes FIRST, then the target.
        INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
    }

    /**
     * Sends a packet from the server to all connected clients.
     */
    public static void sendToAllClients(Object packet) {
        // The packet object comes FIRST, then the target.
        INSTANCE.send(packet, PacketDistributor.ALL.noArg());
    }
}
