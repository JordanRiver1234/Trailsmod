// src/main/java/net/JordanRiver/KisekiLegend/client/hud/EPOverlay.java
package net.JordanRiver.KisekiLegend.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the EP bar unconditionally every GUI frame.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EPOverlay {
    @SubscribeEvent
    public static void onAnyGuiOverlay(CustomizeGuiOverlayEvent ev) {
        if (!ClientSetup.showEP) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // find your orbment item anywhere in the inventory:
        ItemStack orb = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof OrbmentItem) { orb = s; break; }
        }
        if (orb.isEmpty()) {
            for (ItemStack s : player.getInventory().offhand) {
                if (s.getItem() instanceof OrbmentItem) { orb = s; break; }
            }
        }
        if (orb.isEmpty()) return;

        OrbmentComponent comp = OrbmentItem.loadComponentClientSide(orb, player.level());
        float ratio = (float) comp.getCurrentEP() / comp.getMaxEP();

        // draw at top‐left
        int barW = 100, barH = 10;
        int x = 10, y = 10;
        GuiGraphics gui = ev.getGuiGraphics();

        // background
        gui.fill(x, y, x + barW, y + barH, 0x55000000);
        // filled portion
        gui.fill(x, y, x + (int) (barW * ratio), y + barH, 0xFF00AAFF);
        // numeric readout
        String txt = comp.getCurrentEP() + " / " + comp.getMaxEP();
        gui.drawString(mc.font, txt, x, y + barH + 2, 0xFFFFFF);
    }
}
