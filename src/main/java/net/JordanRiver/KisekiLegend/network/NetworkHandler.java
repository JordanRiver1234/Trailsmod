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
        // ADD THESE THREE
        // ADD THIS NEW PACKET:
        INSTANCE.messageBuilder(OrbalTableRenderUpdatePacket.class, id++)
                .encoder(OrbalTableRenderUpdatePacket::encode)
                .decoder(OrbalTableRenderUpdatePacket::decode)
                .consumerMainThread(OrbalTableRenderUpdatePacket::handle)
                .add();

        INSTANCE.messageBuilder(FishingGamePacket.class, id++)
                .encoder(FishingGamePacket::encode)
                .decoder(FishingGamePacket::decode)
                .consumerMainThread(FishingGamePacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncRecipeProgressPacket.class, id++)
                .decoder(SyncRecipeProgressPacket::decode)
                .encoder(SyncRecipeProgressPacket::encode)
                .consumerMainThread(SyncRecipeProgressPacket::handle)
                .add();
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
        INSTANCE.messageBuilder(OrbalTableSyncPacket.class, id++)
                .encoder(OrbalTableSyncPacket::encode)
                .decoder(OrbalTableSyncPacket::decode)
                .consumerMainThread(OrbalTableSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(OrbalTableOperationPacket.class, id++)
                .encoder(OrbalTableOperationPacket::encode)
                .decoder(OrbalTableOperationPacket::decode)
                .consumerMainThread(OrbalTableOperationPacket::handle)
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
    // Remove the duplicate sendToAllClients method that's causing the erasure error
// Keep only this one:
    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(message, net.minecraftforge.network.PacketDistributor.ALL.noArg());
    }

    // Fix the sendToPlayer method for Forge 1.21.1:
    public static <MSG> void sendToPlayer(MSG message, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(message, net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
    }

    /**
     * Sends a packet from the server to all connected clients.
     */

}
