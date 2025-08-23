package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kisekilegend", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VanillaEntityLootHandler {

    @SubscribeEvent
    public static void onEntityDrops(LivingDropsEvent event) {
        // Check if the entity is a frog and was killed by a player
        if (event.getEntity() instanceof Frog && event.getSource().getEntity() instanceof Player) {
            event.getDrops().add(event.getEntity().spawnAtLocation(new ItemStack(ModItems.FROG.get()), 0.0f));
        }

        if (event.getEntity() instanceof Silverfish && event.getSource().getEntity() instanceof Player) {
            event.getDrops().add(event.getEntity().spawnAtLocation(new ItemStack(ModItems.RIVER_BUG.get()), 0.0f));
        }
// Add after your existing frog/silverfish handlers
        if (event.getEntity() instanceof BaseFishEntity fishEntity && event.getSource().getEntity() instanceof Player) {
            String fishType = fishEntity.getFishType();
            ItemStack fishItem = new ItemStack(ModItems.getFishItem(fishType));
            event.getDrops().add(event.getEntity().spawnAtLocation(fishItem, 0.0f));
        }
        // You can add more vanilla entities here
        // Example for other entities:
        /*
        if (event.getEntity() instanceof Squid && event.getSource().getEntity() instanceof Player) {
            event.getDrops().add(event.getEntity().spawnAtLocation(new ItemStack(ModItems.SQUID.get()), 0.0f));
        }
        */
    }
}