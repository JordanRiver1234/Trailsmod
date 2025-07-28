package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SelectQuartzRecipePacket {
    private final ResourceLocation recipeId;
    private final BlockPos blockPos;

    public SelectQuartzRecipePacket(ResourceLocation recipeId, BlockPos blockPos) {
        this.recipeId = recipeId;
        this.blockPos = blockPos;
    }

    public static void encode(SelectQuartzRecipePacket packet, FriendlyByteBuf buffer) {
        if (packet.recipeId != null) {
            buffer.writeBoolean(true);
            buffer.writeResourceLocation(packet.recipeId);
        } else {
            buffer.writeBoolean(false);
        }
        buffer.writeBlockPos(packet.blockPos);
    }

    public static SelectQuartzRecipePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new SelectQuartzRecipePacket(recipeId, buffer.readBlockPos());
    }

    // In SelectQuartzRecipePacket.java

    public static void handle(SelectQuartzRecipePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.level().getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity blockEntity) {
                // If there are items stored in the machine from a previous recipe, return them to the player.
                // This ensures no items are lost when switching.
                if (!blockEntity.getStoredItems().getAllKeys().isEmpty()) {
                    blockEntity.returnStoredItemsToPlayer(player);
                }

                // Set the new recipe. This method will clear any old progress and sync the machine's state to the client.
                blockEntity.setActiveRecipe(packet.recipeId);
            }
        });
        context.setPacketHandled(true);
    }
}