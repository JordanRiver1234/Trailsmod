// src/main/java/net/JordanRiver/KisekiLegend/client/ClientEventHandler.java
package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens for your P-key each client tick and toggles the HUD flag.
 */
@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID,
        bus    = Mod.EventBusSubscriber.Bus.FORGE,
        value  = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (ClientSetup.TOGGLE_EP_HUD.consumeClick()) {
            ClientSetup.showEP = !ClientSetup.showEP;
        }
    }
}
