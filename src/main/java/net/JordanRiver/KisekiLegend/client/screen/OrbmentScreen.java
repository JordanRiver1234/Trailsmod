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
// Draw custom panels
        drawPanel(gui, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828);
        drawGearShadows(gui, leftPos + 8, topPos + 8, 172, 122);

        drawGoldFrame(gui, leftPos + 8, topPos + 8, 172, 122, 1);

        drawPanel(gui, panelCenterX - 64, centerY - 64, 128, 128, 0xFF3E2E20);
        drawGearShadows(gui, panelCenterX - 64, centerY - 64, 128, 128);

        drawGoldFrame(gui, panelCenterX - 64, centerY - 64, 128, 128, 2);

        drawPanel(gui, leftPos + 340, topPos + 8, 130, 136, 0xFF493420);
        drawGearShadows(gui, leftPos + 340, topPos + 8, 130, 136);

        drawGoldFrame(gui, leftPos + 340, topPos + 8, 130, 136, 3);

        int ax = leftPos + 8, ay = topPos + 156;
        drawPanel(gui, ax, ay, ARTS_W, ARTS_H, 0xFF392418);
        drawGearShadows(gui, ax, ay, ARTS_W, ARTS_H);

        drawGoldFrame(gui, ax, ay, ARTS_W - 10, ARTS_H, 4); // Shortened to avoid scrollbar
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

        // Arts Table columns - moved everything left, widened elem and effect
        int col_elem = sx + (int) (80 / scale);
        int col_cost = sx + (int) (130 / scale);
        int col_time = sx + (int) (170 / scale);
        int col_power = sx + (int) (210 / scale);
        int col_effect = sx + (int) (250 / scale);

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
                gui.drawString(this.font, truncate(formatElem(art.elementCost()), 9), col_elem, y, 0x99FF99); // Increased from 7 to 9                gui.drawString(this.font, truncate(art.epCost(), 6), col_cost, y, 0xFFDDDD);
                gui.drawString(this.font, truncate(art.castTime(), 5), col_time, y, 0xFFFFBB);
                gui.drawString(this.font, truncate(art.power(), 5), col_power, y, 0xFFAAAA);
                // Effect column widened by increasing truncate limit
                gui.drawString(this.font, truncate(art.effectDescription(), 45), col_effect, y, 0xAAAAFF); // Increased from 35 to 45
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
    private void drawGoldFrame(GuiGraphics gui, int x, int y, int w, int h, int style) {
        int gold = 0xFFD4AF37;
        int darkGold = 0xFFB8860B;
        int lightGold = 0xFFFFD700;

        // Base frame
        gui.fill(x - 2, y - 2, x + w + 2, y - 1, gold);
        gui.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, gold);
        gui.fill(x - 2, y - 2, x - 1, y + h + 2, gold);
        gui.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, gold);

        // Decorative elements based on style
        switch (style) {
            case 1: // Inventory frame - simple scrollwork
                drawScrollwork(gui, x - 4, y - 4, 8, 8, gold, lightGold);
                drawScrollwork(gui, x + w - 4, y - 4, 8, 8, gold, lightGold);
                drawScrollwork(gui, x - 4, y + h - 4, 8, 8, gold, lightGold);
                drawScrollwork(gui, x + w - 4, y + h - 4, 8, 8, gold, lightGold);
                break;
            case 2: // Orbment core frame - ornate circular
                drawOrnateCorners(gui, x - 3, y - 3, w + 6, h + 6, gold, darkGold, lightGold);
                break;
            case 3: // Right panel frame - elegant curves
                drawElegantFrame(gui, x - 2, y - 2, w + 4, h + 4, gold, darkGold);
                break;
            case 4: // Arts panel frame - minimal to avoid scrollbar
                drawMinimalFrame(gui, x - 1, y - 1, w + 2, h + 2, gold, lightGold);
                break;
        }
    }

    private void drawScrollwork(GuiGraphics gui, int x, int y, int w, int h, int gold, int light) {
        gui.fill(x + 1, y, x + w - 1, y + 1, light);
        gui.fill(x, y + 1, x + 1, y + h - 1, light);
        gui.fill(x + 2, y + 2, x + w - 2, y + 3, gold);
        gui.fill(x + w - 1, y + 1, x + w, y + h - 1, gold);
    }

    private void drawOrnateCorners(GuiGraphics gui, int x, int y, int w, int h, int gold, int dark, int light) {
        // Top corners
        gui.fill(x, y, x + 6, y + 1, light);
        gui.fill(x + w - 6, y, x + w, y + 1, light);
        gui.fill(x, y + 1, x + 3, y + 3, gold);
        gui.fill(x + w - 3, y + 1, x + w, y + 3, gold);

        // Bottom corners
        gui.fill(x, y + h - 1, x + 6, y + h, light);
        gui.fill(x + w - 6, y + h - 1, x + w, y + h, light);
        gui.fill(x, y + h - 3, x + 3, y + h - 1, gold);
        gui.fill(x + w - 3, y + h - 3, x + w, y + h - 1, gold);
    }

    private void drawElegantFrame(GuiGraphics gui, int x, int y, int w, int h, int gold, int dark) {
        // Curved top
        gui.fill(x + 2, y, x + w - 2, y + 1, gold);
        gui.fill(x + 1, y + 1, x + w - 1, y + 2, gold);

        // Curved bottom
        gui.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, gold);
        gui.fill(x + 2, y + h - 1, x + w - 2, y + h, gold);

        // Sides with curves
        gui.fill(x, y + 2, x + 1, y + h - 2, gold);
        gui.fill(x + w - 1, y + 2, x + w, y + h - 2, gold);
    }

    private void drawMinimalFrame(GuiGraphics gui, int x, int y, int w, int h, int gold, int light) {
        // Simple corner accents only
        gui.fill(x, y, x + 4, y + 1, light);
        gui.fill(x + w - 4, y, x + w, y + 1, light);
        gui.fill(x, y + h - 1, x + 4, y + h, light);
        gui.fill(x + w - 4, y + h - 1, x + w, y + h, light);
    }
    private void drawGearShadows(GuiGraphics gui, int x, int y, int w, int h) {
        int shadowColor = 0xFF3D2817; // Dark brownish color
        int gearSize = 32; // Bigger size
        int offset = 0; // Position at exact corner

        // Draw quarter gear shadows at each corner (facing towards corners)
        drawQuarterGear(gui, x + offset, y + offset, gearSize, shadowColor, 0); // Top-left (top-left quarter)
        drawQuarterGear(gui, x + w - gearSize + offset, y + offset, gearSize, shadowColor, 1); // Top-right (top-right quarter)
        drawQuarterGear(gui, x + offset, y + h - gearSize + offset, gearSize, shadowColor, 2); // Bottom-left (bottom-left quarter)
        drawQuarterGear(gui, x + w - gearSize + offset, y + h - gearSize + offset, gearSize, shadowColor, 3); // Bottom-right (bottom-right quarter)
    }

    private void drawQuarterGear(GuiGraphics gui, int x, int y, int size, int color, int corner) {
        // Corner determines which quarter to draw:
        // 0 = top-left corner (show top-left quarter of gear)
        // 1 = top-right corner (show top-right quarter of gear)
        // 2 = bottom-left corner (show bottom-left quarter of gear)
        // 3 = bottom-right corner (show bottom-right quarter of gear)

        int centerX, centerY;

        // Position the gear center so the correct quarter faces the corner
        switch (corner) {
            case 0: // Top-left corner
                centerX = x;
                centerY = y;
                break;
            case 1: // Top-right corner
                centerX = x + size;
                centerY = y;
                break;
            case 2: // Bottom-left corner
                centerX = x;
                centerY = y + size;
                break;
            case 3: // Bottom-right corner
            default:
                centerX = x + size;
                centerY = y + size;
                break;
        }

        int outerRadius = size;
        int innerRadius = (int)(outerRadius * 0.7);
        int teethCount = 20; // More teeth for larger gear
        int toothHeight = (int)(outerRadius * 0.2);
        int toothWidth = (int)(outerRadius * 0.15);
        int holeRadius = (int)(outerRadius * 0.25);

        // Draw gear teeth first
        for (int i = 0; i < teethCount; i++) {
            double angle = 2 * Math.PI * i / teethCount;

            // Calculate tooth position
            int toothCenterX = centerX + (int)((innerRadius + toothHeight/2) * Math.cos(angle));
            int toothCenterY = centerY + (int)((innerRadius + toothHeight/2) * Math.sin(angle));

            // Draw rectangular tooth
            for (int dx = -toothWidth/2; dx <= toothWidth/2; dx++) {
                for (int dy = -toothHeight/2; dy <= toothHeight/2; dy++) {
                    // Rotate the tooth rectangle
                    int rotatedX = (int)(dx * Math.cos(angle) - dy * Math.sin(angle));
                    int rotatedY = (int)(dx * Math.sin(angle) + dy * Math.cos(angle));

                    int pixelX = toothCenterX + rotatedX;
                    int pixelY = toothCenterY + rotatedY;

                    // Only draw if within the panel bounds
                    if (pixelX >= x && pixelX < x + size && pixelY >= y && pixelY < y + size) {
                        gui.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
                    }
                }
            }
        }

        // Draw main gear body (circle)
        for (int px = x; px < x + size; px++) {
            for (int py = y; py < y + size; py++) {
                int dx = px - centerX;
                int dy = py - centerY;
                int distance = (int)Math.sqrt(dx * dx + dy * dy);

                // Main gear body
                if (distance <= innerRadius) {
                    gui.fill(px, py, px + 1, py + 1, color);
                }
            }
        }

        // Draw center hole (lighter brownish color to show depth)
        int holeColor = 0xFF4A3426; // Slightly lighter brown
        for (int px = x; px < x + size; px++) {
            for (int py = y; py < y + size; py++) {
                int dx = px - centerX;
                int dy = py - centerY;
                int distance = (int)Math.sqrt(dx * dx + dy * dy);

                if (distance <= holeRadius) {
                    gui.fill(px, py, px + 1, py + 1, holeColor);
                }
            }
        }

        // Add inner detail rings (darker brown for contrast)
        int ringRadius1 = (int)(innerRadius * 0.8);
        int ringRadius2 = (int)(innerRadius * 0.9);
        int ringColor = 0xFF2A1F11; // Darker brown for ring details

        for (int px = x; px < x + size; px++) {
            for (int py = y; py < y + size; py++) {
                int dx = px - centerX;
                int dy = py - centerY;
                int distance = (int)Math.sqrt(dx * dx + dy * dy);

                if (distance == ringRadius1 || distance == ringRadius2) {
                    gui.fill(px, py, px + 1, py + 1, ringColor);
                }
            }
        }
    }
}