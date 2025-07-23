// src/main/java/net/JordanRiver/KisekiLegend/items/EPTicker.java
package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Regenerates EP once every 40 ticks (~2 seconds) so you can actually watch it.
 */
@Mod.EventBusSubscriber
public class EPTicker {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        Player player = event.player;

        // Use player's tick count instead of static counter
        if (player.tickCount % 40 != 0) return;
        // find orbment item in inventory or offhand
        ItemStack orb = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof OrbmentItem) { orb = s; break; }
        }
        if (orb.isEmpty()) {
            for (ItemStack s : player.getInventory().offhand) {
                if (s.getItem() instanceof OrbmentItem) { orb = s; break; }
            }
        }
        if (orb.isEmpty()) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level(), (ServerPlayer) player);
        comp.regenerateEP();
        OrbmentItem.saveComponent(orb, comp, player.level(), (ServerPlayer) player);
    }
}
