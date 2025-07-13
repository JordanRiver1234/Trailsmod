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
            ensureHandModel(mainHand, true); // true = in active hand
        }

        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof OrbmentItem) {
            ensureHandModel(offHand, true); // true = in active hand
        }

        // Check hotbar items (but not the currently held item)
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = player.getInventory().getItem(i);
            if (hotbarStack.getItem() instanceof OrbmentItem) {
                // Only process if it's NOT the currently held item
                if (hotbarStack != mainHand && hotbarStack != offHand) {
                    ensureHotbarModel(hotbarStack);
                }
            }
        }
    }

    private static void ensureHandModel(ItemStack stack, boolean inHand) {
        // When in hand, should have custom model data = 1 for 3D rendering
        if (!stack.has(DataComponents.CUSTOM_MODEL_DATA) ||
                stack.get(DataComponents.CUSTOM_MODEL_DATA).value() != 1) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        }
    }

    private static void ensureHotbarModel(ItemStack stack) {
        // When in hotbar (but not in hand), should have custom model data = 0 or none for 2D rendering
        if (stack.has(DataComponents.CUSTOM_MODEL_DATA) &&
                stack.get(DataComponents.CUSTOM_MODEL_DATA).value() == 1) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA); // Remove to use default 2D model
        }
    }
}