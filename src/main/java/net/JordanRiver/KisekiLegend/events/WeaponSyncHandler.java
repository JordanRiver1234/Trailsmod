package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Add this as a separate event handler class or add to existing event handler
@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, value = Dist.CLIENT)
public class WeaponSyncHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide()) return;

        ItemStack mainHand = event.player.getMainHandItem();
        if (!mainHand.isEmpty() && isWeaponItem(mainHand)) {
            // Force slot data refresh every few ticks to ensure sync
            if (event.player.tickCount % 20 == 0) { // Every second
                WeaponSlotData slotData = WeaponSlotData.getOrCreate(mainHand);
                if (slotData.getActiveSlotCount() > 0) {
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemEquipped(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.level().isClientSide()) return;

        ItemStack newStack = event.getTo();
        ItemStack oldStack = event.getFrom();

        // When weapon is equipped
        if (!newStack.isEmpty() && isWeaponItem(newStack)) {


            // Force immediate slot data refresh
            WeaponSlotData slotData = WeaponSlotData.getOrCreate(newStack);

            // Debug the slot data
            slotData.debugSlots();
        }

        // When weapon is unequipped
        if (!oldStack.isEmpty() && isWeaponItem(oldStack)) {
        }
    }

    // Copy the isWeaponItem method here too, or make it public static in WeaponSlotRenderer
    private static boolean isWeaponItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        // Check vanilla weapons
        if (item instanceof SwordItem || item instanceof AxeItem ||
                item instanceof TridentItem || item instanceof BowItem || item instanceof CrossbowItem) {
            return true;
        }

        // Check if it has weapon slot data
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().contains("orbal_slots");
    }
}

