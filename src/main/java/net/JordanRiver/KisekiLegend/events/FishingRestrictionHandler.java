package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FishingRestrictionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (FishingGameManager.isActive() && FishingGameManager.getGameState() != null) {
            if (event.getPlayer().equals(FishingGameManager.getGameState().getPlayer())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (FishingGameManager.isActive() && FishingGameManager.getGameState() != null) {
            if (event.getEntity().equals(FishingGameManager.getGameState().getPlayer())) {
                // Allow item pickup but prevent hotbar switching
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (FishingGameManager.isActive() && FishingGameManager.getGameState() != null) {
            if (event.getEntity().equals(FishingGameManager.getGameState().getPlayer())) {
                // Allow fishing rod interactions but block other block interactions
                if (!(event.getItemStack().getItem() instanceof net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (FishingGameManager.isActive() && FishingGameManager.getGameState() != null) {
            if (event.getEntity().equals(FishingGameManager.getGameState().getPlayer())) {
                // Only allow fishing rod usage during minigame
                if (!(event.getItemStack().getItem() instanceof net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
