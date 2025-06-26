package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kisekilegend", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OrbmentBuffHandler {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof OrbmentItem) {
                OrbmentComponent component = OrbmentItem.loadComponent(stack, player.level());
                component.tickBuffs(player);
                return;
            }
        }
    }
}
