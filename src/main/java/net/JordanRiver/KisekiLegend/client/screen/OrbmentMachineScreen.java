package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class OrbmentMachineScreen extends AbstractContainerScreen<OrbmentMachineMenu> {
    private static final int WIDTH = 480;
    private static final int HEIGHT = 256;

    private int centerX;
    private int centerY;
    private static final int UNLOCK_COST = 1;

    public OrbmentMachineScreen(OrbmentMachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;
        this.centerX = leftPos + imageWidth / 2;
        this.centerY = topPos + 88;

        addRenderableWidget(Button.builder(
                Component.literal("Unlock Slot (" + UNLOCK_COST + ")"),
                b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0)
        ).bounds(leftPos + 350, topPos + 145, 110, 18).build());

        addRenderableWidget(Button.builder(
                Component.literal("Convert Full Set"),
                b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1)
        ).bounds(leftPos + 350, topPos + 120, 110, 18).build());
    }

    private void drawPanel(GuiGraphics gui, int x, int y, int w, int h, int fill) {
        int border = 0xFF704214;
        int shadow = 0xFF1A0F05;
        gui.fill(x + 2, y + 2, x + w + 2, y + h + 2, shadow);
        gui.fill(x, y, x + w, y + h, fill);
        gui.fill(x, y, x + w, y + 1, border);
        gui.fill(x, y + h - 1, x + w, y + h, border);
        gui.fill(x, y, x + 1, y + h, border);
        gui.fill(x + w - 1, y, x + w, y + h, border);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        // Draw the static panels
        drawPanel(gui, centerX - 64, centerY - 64, 128, 128, 0xFF3E2E20);  // Orbment core panel
        drawPanel(gui, leftPos + 340, topPos + 8, 130, 185, 0xFF493420);      // Conversion panel

        // Draw labels
        gui.drawString(this.font, "Orbment Core", centerX - 36, centerY - 54, 0xFFD700);
        gui.drawString(this.font, "Convert Mass", leftPos + 350, topPos + 12, 0xFFFFBB);

        // Fetch current orbment and unlocked slots
        OrbmentMachineMenu m = (OrbmentMachineMenu) this.menu;
        ItemStack orb = m.getOrbmentStack();
        OrbmentComponent comp = OrbmentItem.loadComponent(orb, minecraft.level);
        int unlocked = comp.getUnlockedSlots();

        // Draw slot placeholders and X for locked slots
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI * i / 6 - Math.PI / 2;
            int sx = (int) (centerX + 40 * Math.cos(angle)) - 9;
            int sy = (int) (centerY + 40 * Math.sin(angle)) - 9;
            gui.fill(sx, sy, sx + 18, sy + 18, 0xFFCCCCCC);
            if (i >= unlocked) {
                gui.drawString(this.font, "X", sx + 6, sy + 5, 0xFF3333);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {
        // No extra labels
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float partial) {
        renderBackground(gui, mx, my, partial);
        super.render(gui, mx, my, partial);
        renderTooltip(gui, mx, my);
    }
}
