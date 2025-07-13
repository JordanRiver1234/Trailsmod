package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Listens for your P-key each client tick and toggles the HUD flag.
 * Also stops casting animation when AuraEntity is gone.
 */
@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID,
        bus    = Mod.EventBusSubscriber.Bus.FORGE,
        value  = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        // Toggle HUD
        if (ClientSetup.TOGGLE_EP_HUD.consumeClick()) {
            ClientSetup.showEP = !ClientSetup.showEP;
        }

        // Auto-reset cast animation
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) return;

        boolean hasAuraNearby = level.getEntitiesOfClass(AuraEntity.class, player.getBoundingBox().inflate(2.0))
                .stream().anyMatch(e -> {
                    var uuid = e.getOwnerUUID(); // You must implement this in AuraEntity
                    return uuid != null && uuid.equals(player.getUUID());
                });

        if (!hasAuraNearby) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GeoItem geoItem) {
                geoItem.triggerAnim(player, GeoItem.getId(stack), "cast_controller", "idle");
            }
        }
    }
}
