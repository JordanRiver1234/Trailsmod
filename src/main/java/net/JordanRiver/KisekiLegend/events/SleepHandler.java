package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SleepHandler {
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        // server‐side only
        if (event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();

        // scan inventory + offhand for your OrbmentItem
        for (ItemStack stack : player.getInventory().items) {
            refillOrb(stack, player);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            refillOrb(stack, player);
        }
    }

    private static void refillOrb(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof OrbmentItem)) return;
        OrbmentComponent comp = OrbmentItem.loadComponent(stack, player.level());
        comp.fillToMaxEP();
        OrbmentItem.saveComponent(stack, comp, player.level());
    }
}

