package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.*;

public class OrbmentScreen extends AbstractContainerScreen<OrbmentMenu> {
    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 256;

    private int centerX, centerY;
    private int scrollOffset = 0;
    private static final int MAX_ARTS_VISIBLE = 5;
    private List<String> artsUnlocked = new ArrayList<>();

    public OrbmentScreen(OrbmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        centerX = leftPos + imageWidth / 2;
        centerY = topPos + 100;
    }

    private Map<String, Integer> calculateElementalTotals() {
        // force a re-count of whatever is currently in the handler:
        this.menu.getOrbmentComponent().recalculate();

        int[] sepith = this.menu.getOrbmentComponent().getSepithCounts();
        String[] names = { "Earth","Water","Wind","Fire","Space","Mirage","Time" };
        Map<String,Integer> totals = new LinkedHashMap<>();
        for(int i=0;i<names.length;i++){
            totals.put(names[i], sepith[i]);
        }
        return totals;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Draw background panels
        drawPanel(guiGraphics, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828); // Inventory
        drawPanel(guiGraphics, centerX - 64, centerY - 64, 128, 128, 0xFF3E2E20); // Core
        drawPanel(guiGraphics, leftPos + 340, topPos + 8, 130, 112, 0xFF493420); // Sepith
        drawPanel(guiGraphics, leftPos + 8, topPos + 140, 212, 92, 0xFF392418); // Arts

        // Labels
        guiGraphics.drawString(this.font, "Orbment Inventory", leftPos + 16, topPos + 12, 0xFFDDAA);
        guiGraphics.drawString(this.font, "Orbment Core", centerX - 36, centerY - 54, 0xFFD700);
        guiGraphics.drawString(this.font, "Sepith Totals", leftPos + 348, topPos + 16, 0xFFFFBB);
        guiGraphics.drawString(this.font, "Arts Unlocked", leftPos + 16, topPos + 148, 0x77DDFF);
        guiGraphics.drawString(this.font, "- (based on equipped quartz)", leftPos + 16, topPos + 160, 0xAAAAAA);

        for (int i = 0; i < MAX_ARTS_VISIBLE && (i + scrollOffset) < artsUnlocked.size(); i++) {
            String name = artsUnlocked.get(i + scrollOffset);
            guiGraphics.drawString(this.font, "- " + name, leftPos + 16, topPos + 180 + i * 10, 0xFFFFFF);
        }
// Inside renderBg() of OrbmentScreen:
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI * i / 6 - Math.PI / 2;
            int slotX = (int) (centerX + 40 * Math.cos(angle)) - 9;
            int slotY = (int) (centerY + 40 * Math.sin(angle)) - 9;

            // Draw a simple white slot background (replace with actual texture if needed)
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFFCCCCCC); // Light gray
        }

        // Sepith values
        Map<String, Integer> totals = calculateElementalTotals();
        int sepithX = leftPos + 348;
        int sepithY = topPos + 32;
        int offset = 0;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            guiGraphics.drawString(this.font, entry.getKey() + ": " + entry.getValue(), sepithX, sepithY + offset, 0xFFFFFF);
            offset += 10;
        }

        // Draw 'X' marker on locked slots
        int unlocked = this.menu.getOrbmentComponent().getUnlockedSlots();
        int radius = 40;
        for (int i = 0; i < 6; i++) {
            if (i >= unlocked) {
                double angle = 2 * Math.PI * i / 6 - Math.PI / 2;
                int x = (int) (centerX + radius * Math.cos(angle)) - 3;
                int y = (int) (centerY + radius * Math.sin(angle)) - 4;
                guiGraphics.drawString(this.font, "X", x, y, 0xFF3333);
            }
        }
    }

    private void drawPanel(GuiGraphics gui, int x, int y, int w, int h, int fillColor) {
        int borderColor = 0xFF704214; // Copper
        int shadowColor = 0xFF1A0F05;

        gui.fill(x + 2, y + 2, x + w + 2, y + h + 2, shadowColor); // Shadow
        gui.fill(x, y, x + w, y + h, fillColor);                   // Main fill
        gui.fill(x, y, x + w, y + 1, borderColor);                // Top border
        gui.fill(x, y + h - 1, x + w, y + h, borderColor);        // Bottom border
        gui.fill(x, y, x + 1, y + h, borderColor);                // Left border
        gui.fill(x + w - 1, y, x + w, y + h, borderColor);        // Right border
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Skip default "Inventory" label
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD0101010); // Dark background
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
