package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.quartz.QuartzDefinition;
import net.JordanRiver.KisekiLegend.quartz.QuartzRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kisekilegend")
public class OrbmentProcHandler {
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent ev) {
        if (!(ev.getEntity() instanceof Player p)) return;

        ItemStack orb = p.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, p.level());

        if (!(ev.getTarget() instanceof LivingEntity target)) return;

        // loop each slot
        for (int i = 0; i < OrbmentComponent.MAX_SLOTS; i++) {
            ItemStack s = comp.getInventory().getStackInSlot(i);
            if (!(s.getItem() instanceof QuartzItem q)) continue;

            String id = q.getQuartzId();
            QuartzDefinition def = QuartzRegistry.get(id);
            if (def == null) continue;

            // for deathblow_2 we want to break the quartz
            if ("deathblow_2".equals(id)) {
                def.applyOnHit(target, p);
                // remove the quartz
                comp.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                OrbmentItem.saveInventory(orb,
                        comp.getInventory(),
                        comp.getUnlockedSlots(),
                        p.level()
                );
            } else {
                // normal on‐hit effect
                def.applyOnHit(target, p);
            }
        }
    }
}