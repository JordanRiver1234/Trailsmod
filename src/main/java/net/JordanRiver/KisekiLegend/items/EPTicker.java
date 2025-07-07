// src/main/java/net/JordanRiver/KisekiLegend/items/EPTicker.java
package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
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
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Player player = ev.player;

        // only tick every 40 ticks:
        if (++tickCounter < 40) return;
        tickCounter = 0;

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

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.regenerateEP();
        OrbmentItem.saveComponent(orb, comp, player.level());
    }
}
