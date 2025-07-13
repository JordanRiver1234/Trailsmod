package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OrbmentModelHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof OrbmentItem) {
            ensureHandModel(mainHand);
        } else {
            // Remove 3D model from non-hand items
            removeHandModel(mainHand);
        }

        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof OrbmentItem) {
            ensureHandModel(offHand);
        } else {
            removeHandModel(offHand);
        }

        // Remove 3D model from hotbar items that are not in hand
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = player.getInventory().getItem(i);
            if (hotbarStack.getItem() instanceof OrbmentItem &&
                    hotbarStack != mainHand && hotbarStack != offHand) {
                removeHandModel(hotbarStack);
            }
        }
    }

    private static void ensureHandModel(ItemStack stack) {
        // When in hand, should have custom model data = 1 for 3D rendering
        if (!stack.has(DataComponents.CUSTOM_MODEL_DATA) ||
                stack.get(DataComponents.CUSTOM_MODEL_DATA).value() != 1) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        }
    }

    private static void removeHandModel(ItemStack stack) {
        // Remove custom model data for 2D rendering
        if (stack.has(DataComponents.CUSTOM_MODEL_DATA)) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }
}