package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.capability.PlayerRecipeProgressCapability;
import net.JordanRiver.KisekiLegend.capability.PlayerRecipeProgressProvider;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.SyncRecipeProgressPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID)
public class CapabilityEventHandler {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerRecipeProgressCapability.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).isPresent()) {
                event.addCapability(ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "recipe_progress"),
                        new PlayerRecipeProgressProvider());
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(progress -> {
                System.out.println("Player logged in, syncing " + progress.getCompletedRecipes().size() + " completed recipes");

                // Sync progress to client when they log in
                NetworkHandler.sendToPlayer(new SyncRecipeProgressPacket(progress.getCompletedRecipes(), serverPlayer.getUUID()), serverPlayer);
            });
        }
    }
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(oldStore -> {
            event.getEntity().getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(newStore -> {
                // Use proper provider instead of null
                if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                    newStore.deserializeNBT(serverLevel.registryAccess(), oldStore.serializeNBT(serverLevel.registryAccess()));
                }
            });
        });
    }
}