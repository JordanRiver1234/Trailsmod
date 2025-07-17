package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class OrbmentScreen extends AbstractContainerScreen<OrbmentMenu> {
    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 256;
    private static final int ARTS_W = 464;
    private static final int ARTS_H = 92;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_MARGIN = 4;

    private int centerX, centerY;
    private int scrollOffset = 0;
    private static final int MAX_ARTS_VISIBLE = 4;
    private List<ArtsRegistry.ArtDefinition> artsUnlocked = new ArrayList<>();
    private boolean draggingThumb = false;

    public OrbmentScreen(OrbmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.titleLabelX = -9999;
        this.inventoryLabelX = -9999;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        // This centerX is for the logical slot positions from the Menu file. It remains unchanged from your file.
        centerX = this.leftPos + 240;
        centerY = this.topPos + 95;
    }

    private void clampScroll() {
        int maxOffset = Math.max(0, artsUnlocked.size() - MAX_ARTS_VISIBLE);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private void updateUnlockedArts() {
        Map<String, Integer> totals = calculateElementalTotals();
        artsUnlocked.clear();
        for (var art : ArtsRegistry.ALL_ARTS) {
            boolean ok = true;
            for (var cost : art.elementCost().entrySet()) {
                if (totals.getOrDefault(cost.getKey().toLowerCase(), 0) < cost.getValue()) {
                    ok = false;
                    break;
                }
            }
            if (ok) artsUnlocked.add(art);
        }
        clampScroll();
    }

    private Map<String, Integer> calculateElementalTotals() {
        this.menu.getOrbmentComponent().recalculate();
        int[] sepith = this.menu.getOrbmentComponent().getSepithCounts();
        return Map.of(
                "earth", sepith[0], "water", sepith[1], "wind", sepith[2],
                "fire", sepith[3], "space", sepith[4], "mirage", sepith[5], "time", sepith[6]
        );
    }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mouseX, int mouseY) {
        updateUnlockedArts();

        // A new variable for the visual center of the panel, leaving the slot logic untouched.
        int panelCenterX = this.leftPos + 266; // MOVED FURTHER RIGHT

        // Draw custom panels - UPDATED PLAYER INVENTORY PANEL TO MATCH ORBMENT MACHINE
        drawPanel(gui, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828);
        drawPanel(gui, panelCenterX - 64, centerY - 64, 128, 128, 0xFF3E2E20); // Centered using new variable
        drawPanel(gui, leftPos + 340, topPos + 8, 130, 136, 0xFF493420);
        int ax = leftPos + 8, ay = topPos + 156;
        drawPanel(gui, ax, ay, ARTS_W, ARTS_H, 0xFF392418);

        // Draw headings
        gui.drawString(this.font, "Player Inventory", leftPos + 16, topPos + 12, 0xFFDDAA, false);
        gui.drawString(this.font, "Orbment Core", panelCenterX - 36, centerY - 60, 0xFFD700); // Centered using new variable
        gui.drawString(this.font, "Sepith Totals", leftPos + 348, topPos + 16, 0xFFFFBB);
        gui.drawString(this.font, "Arts Unlocked", ax + 8, ay + 4, 0x77DDFF);

        // Draw custom backgrounds for the orbment slots
        for (int i = 0; i < OrbmentMenu.ORBMENT_SLOT_COUNT; i++) {
            Slot slot = this.menu.getSlot(i);
            Element[] sepithLines = this.menu.getOrbmentComponent().getSepithLines();
            Element lineElement = sepithLines[slot.index];

            gui.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.leftPos + slot.x + 17, this.topPos + slot.y + 17, lineElement.getColor());
            gui.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0xFF404040);
        }

        boolean[] unlockedStatus = this.menu.getOrbmentComponent().getUnlockedStatus();
        for (int i = 0; i < 6; i++) {
            if (!unlockedStatus[i]) {
                Slot slot = this.menu.getSlot(i);
                gui.drawString(this.font, "X", this.leftPos + slot.x + 5, this.topPos + slot.y + 4, 0xFF3333, true);
            }
        }

        // Scrollbar
        int trackX = ax + ARTS_W - SCROLL_MARGIN - SCROLL_W;
        int trackY = ay + SCROLL_MARGIN;
        int trackH = ARTS_H - SCROLL_MARGIN * 2;
        int maxOff = Math.max(1, artsUnlocked.size() - MAX_ARTS_VISIBLE);
        gui.fill(trackX, trackY, trackX + SCROLL_W, trackY + trackH, 0xFF2E1F14);
        if (artsUnlocked.size() > MAX_ARTS_VISIBLE) {
            int thumbH = Math.max(10, trackH * MAX_ARTS_VISIBLE / artsUnlocked.size());
            int thumbY = trackY + (trackH - thumbH) * scrollOffset / maxOff;
            gui.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH, 0xFF704214);
        }

        renderArtsTable(gui, ax, ay);

        Map<String, Integer> totals = calculateElementalTotals();
        int tx = leftPos + 348, ty = topPos + 32, off = 0;
        for (var e : totals.entrySet()) {
            gui.drawString(this.font, e.getKey() + ": " + e.getValue(), tx, ty + off, 0xFFFFFF);
            off += 12;
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        // Check if mouse is over a slot
        Slot hoveredSlot = this.getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack itemStack = hoveredSlot.getItem();
            gui.renderTooltip(this.font, itemStack, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // First render the background and screen elements
        super.render(gui, mouseX, mouseY, partialTick);

        // Then render tooltips on top
        this.renderTooltip(gui, mouseX, mouseY);
    }

    private void renderArtsTable(GuiGraphics gui, int ax, int ay) {
        float scale = 0.75f;
        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1f);

        int sx = (int) ((ax + 8) / scale);
        int sy = (int) ((ay + 18) / scale);

        // Arts Table columns adjusted - moved effect left and widened it
        int col_elem = sx + (int) (100 / scale);
        int col_cost = sx + (int) (160 / scale);
        int col_time = sx + (int) (210 / scale);
        int col_power = sx + (int) (260 / scale);
        int col_effect = sx + (int) (300 / scale); // MOVED LEFT

        gui.drawString(this.font, "Name", sx, sy, 0xFFFFAA);
        gui.drawString(this.font, "Elem", col_elem, sy, 0xFFFFAA);
        gui.drawString(this.font, "Cost", col_cost, sy, 0xFFFFAA);
        gui.drawString(this.font, "Time", col_time, sy, 0xFFFFAA);
        gui.drawString(this.font, "Power", col_power, sy, 0xFFFFAA);
        gui.drawString(this.font, "Effect", col_effect, sy, 0xFFFFAA);

        sy += (int) (12 / scale);

        if (artsUnlocked.isEmpty()) {
            gui.drawString(this.font, "<no arts unlocked>", sx, sy, 0x888888);
        } else {
            for (int i = 0; i < MAX_ARTS_VISIBLE && i + scrollOffset < artsUnlocked.size(); i++) {
                var art = artsUnlocked.get(i + scrollOffset);
                int y = sy + (int) ((i * 12) / scale);

                gui.drawString(this.font, truncate(art.name(), 15), sx, y, 0xFFFFFF);
                gui.drawString(this.font, truncate(formatElem(art.elementCost()), 7), col_elem, y, 0x99FF99);
                gui.drawString(this.font, truncate(art.epCost(), 6), col_cost, y, 0xFFDDDD);
                gui.drawString(this.font, truncate(art.castTime(), 5), col_time, y, 0xFFFFBB);
                gui.drawString(this.font, truncate(art.power(), 5), col_power, y, 0xFFAAAA);
                // Effect column widened by increasing truncate limit
                gui.drawString(this.font, truncate(art.effectDescription(), 35), col_effect, y, 0xAAAAFF); // WIDENED
            }
        }
        gui.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int trackX = leftPos + 8 + ARTS_W - SCROLL_MARGIN - SCROLL_W;
            int trackY = topPos + 156 + SCROLL_MARGIN;
            int trackH = ARTS_H - SCROLL_MARGIN * 2;
            if (mx >= trackX && mx <= trackX + SCROLL_W && my >= trackY && my <= trackY + trackH) {
                draggingThumb = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (draggingThumb && b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int trackY = topPos + 156 + SCROLL_MARGIN;
            int trackH = ARTS_H - SCROLL_MARGIN * 2;
            int maxOff = Math.max(1, artsUnlocked.size() - MAX_ARTS_VISIBLE);
            double p = (my - trackY) / (double) trackH;
            scrollOffset = (int) (p * maxOff + 0.5);
            clampScroll();
            return true;
        }
        return super.mouseDragged(mx, my, b, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        if (draggingThumb && b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingThumb = false;
            return true;
        }
        return super.mouseReleased(mx, my, b);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollOffset++;
            clampScroll();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollOffset--;
            clampScroll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String formatElem(Map<String, Integer> e) {
        return e.entrySet().stream()
                .map(x -> x.getValue() + x.getKey().substring(0, 1).toUpperCase())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    private void drawPanel(GuiGraphics gui, int x, int y, int w, int h, int fill) {
        int border = 0xFF704214, shadow = 0xFF1A0F05;
        gui.fill(x + 2, y + 2, x + w + 2, y + h + 2, shadow);
        gui.fill(x, y, x + w, y + h, fill);
        gui.fill(x, y, x + w, y + 1, border);
        gui.fill(x, y + h - 1, x + w, y + h, border);
        gui.fill(x, y, x + 1, y + h, border);
        gui.fill(x + w - 1, y, x + w, y + h, border);
    }
}