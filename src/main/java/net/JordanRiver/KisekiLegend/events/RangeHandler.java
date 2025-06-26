package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kisekilegend", value = Dist.CLIENT) // client only
public class RangeHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem ev) {
        Player player = ev.getEntity();
        if (player.level().isClientSide) return;

        if (!player.isShiftKeyDown()) return;

        ItemStack orb = ev.getItemStack();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        if (!comp.hasQuartz("range_1")) return;

        ev.setCanceled(true);

        ThrownEnderpearl pearl = new ThrownEnderpearl(player.level(), player);
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        player.level().addFreshEntity(pearl);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

}
