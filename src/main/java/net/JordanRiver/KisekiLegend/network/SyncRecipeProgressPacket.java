package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.capability.PlayerRecipeProgressProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SyncRecipeProgressPacket {
    private final Set<String> completedRecipes;
    private final UUID targetPlayerUUID; // ADD THIS

    public SyncRecipeProgressPacket(Set<ResourceLocation> completed, UUID playerUUID) {
        this.completedRecipes = completed.stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
        this.targetPlayerUUID = playerUUID; // ADD THIS
    }

    public static void encode(SyncRecipeProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.completedRecipes, FriendlyByteBuf::writeUtf);
        buffer.writeUUID(packet.targetPlayerUUID); // ADD THIS
    }

    public static SyncRecipeProgressPacket decode(FriendlyByteBuf buffer) {
        Set<String> completed = buffer.readCollection(HashSet::new, FriendlyByteBuf::readUtf);
        UUID playerUUID = buffer.readUUID(); // ADD THIS

        Set<ResourceLocation> resourceLocations = completed.stream()
                .map(ResourceLocation::parse)
                .collect(Collectors.toSet());
        return new SyncRecipeProgressPacket(resourceLocations, playerUUID); // UPDATE THIS
    }

    public static void handle(SyncRecipeProgressPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null && player.getUUID().equals(packet.targetPlayerUUID)) { // ADD UUID CHECK
                player.getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(progress -> {
                    progress.clearProgress();
                    for (String recipeStr : packet.completedRecipes) {
                        try {
                            ResourceLocation recipeId = ResourceLocation.parse(recipeStr);
                            progress.markRecipeCompleted(recipeId);
                            System.out.println("Client received recipe completion: " + recipeId);
                        } catch (Exception e) {
                            System.err.println("Failed to sync recipe: " + recipeStr);
                        }
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}