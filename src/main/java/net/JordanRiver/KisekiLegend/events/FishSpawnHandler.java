package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FishSpawnHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // DISABLED: All natural fish spawning removed to save memory
        // Fish now only exist during fishing games and from spawn eggs

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        // Only keep aggressive cleanup to remove any stray fish
        if (event.player.level().getGameTime() % 1200 == 0) { // Every minute
            forceCleanupStrayFish(event.player);
        }
    }

    private static void forceCleanupStrayFish(net.minecraft.world.entity.player.Player player) {
        try {
            // Remove ALL wild fish - only keep bucket fish and named fish
            var allFish = player.level().getEntitiesOfClass(BaseFishEntity.class,
                    new AABB(player.blockPosition()).inflate(128));

            allFish.stream()
                    .filter(fish -> !fish.fromBucket() && !fish.hasCustomName() && !fish.isPersistenceRequired())
                    .forEach(fish -> {
                        try {
                            fish.discard();
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });

        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error during fish cleanup", e);
        }
    }
}