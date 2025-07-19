package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class OrbmentMachineScreen extends AbstractContainerScreen<OrbmentMachineMenu> {
    private static final int WIDTH = 480;
    private static final int HEIGHT = 256;

    private int centerX;
    private int centerY;
    private int selectedSlot = -1;

    private Button unlockButton, removeLineButton, convertButton;
    private final List<Button> setLineButtons = new ArrayList<>();

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
        this.centerX = leftPos + 270;
        this.centerY = topPos + 100;

        // --- Right Panel Buttons (Bottom) ---
        int buttonX = leftPos + 350;
        int bottomButtonY = topPos + 185; // MOVED UP

        unlockButton = addRenderableWidget(Button.builder(Component.literal("Unlock Slot"), b -> onButtonClick(6))
                .bounds(buttonX, bottomButtonY, 110, 18).build());

        removeLineButton = addRenderableWidget(Button.builder(Component.literal("Remove Line (1 SM)"), b -> onButtonClick(7))
                .bounds(buttonX, bottomButtonY - 22, 110, 18).build());

        convertButton = addRenderableWidget(Button.builder(Component.literal("Convert Mass (7:1)"), b -> onButtonClick(8))
                .bounds(buttonX, bottomButtonY - 44, 110, 18).build());

        // --- Set Line Buttons (Moved Up) ---
        int setButtonY = topPos + 90;
        addSetLineButton(Element.EARTH, 9, buttonX, setButtonY);
        addSetLineButton(Element.WATER, 10, buttonX, setButtonY + 12);
        addSetLineButton(Element.FIRE, 11, buttonX, setButtonY + 24);
        addSetLineButton(Element.WIND, 12, buttonX, setButtonY + 36);
        addSetLineButton(Element.TIME, 13, buttonX + 60, setButtonY);
        addSetLineButton(Element.SPACE, 14, buttonX + 60, setButtonY + 12);
        addSetLineButton(Element.MIRAGE, 15, buttonX + 60, setButtonY + 24);

        updateButtonStates();
    }

    private void addSetLineButton(Element element, int buttonId, int x, int y) {
        String name = element.getName().substring(0, 1).toUpperCase() + element.getName().substring(1);
        Button button = addRenderableWidget(Button.builder(Component.literal(name), b -> onButtonClick(buttonId))
                .bounds(x, y, 50, 10).build());
        setLineButtons.add(button);
    }

    private void onButtonClick(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateButtonStates() {
        ItemStack orb = this.menu.getOrbmentStack();
        if (orb.isEmpty() || minecraft == null || minecraft.level == null) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, minecraft.level);

        boolean slotIsSelected = selectedSlot != -1;
        boolean isUnlocked = slotIsSelected && comp.isSlotUnlocked(selectedSlot);
        boolean isEmpty = slotIsSelected && comp.getInventory().getStackInSlot(selectedSlot).isEmpty();

        unlockButton.active = slotIsSelected && !isUnlocked;
        removeLineButton.active = slotIsSelected && isUnlocked && isEmpty && comp.getSepithLines()[selectedSlot] != Element.NONE;
        convertButton.active = true;

        for (Button b : setLineButtons) {
            b.active = slotIsSelected && isUnlocked && isEmpty;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < OrbmentComponent.MAX_SLOTS; i++) {
                double angle = 2 * Math.PI * i / 6 - Math.PI / 2;
                int sx = (int) (centerX + 40 * Math.cos(angle)) - 9;
                int sy = (int) (centerY + 40 * Math.sin(angle)) - 9;
                if (mouseX >= sx && mouseX <= sx + 18 && mouseY >= sy && mouseY <= sy + 18) {
                    // --- MODIFIED SECTION START ---
                    ItemStack orb = this.menu.getOrbmentStack();
                    if (!orb.isEmpty() && minecraft != null && minecraft.level != null) {
                        OrbmentComponent comp = OrbmentItem.loadComponent(orb, minecraft.level);
                        if (!comp.isSlotUnlocked(i)) {
                            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSoundEvents.ORBMENT_SLOT_LOCKED.get(), 1.0F));
                        }
                    }
                    // --- MODIFIED SECTION END ---

                    selectedSlot = i;
                    onButtonClick(i);
                    updateButtonStates();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        drawPanel(gui, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828);
        drawGearShadows(gui, leftPos + 8, topPos + 8, 172, 122);

        drawGoldFrame(gui, leftPos + 8, topPos + 8, 172, 122, 1);

        drawPanel(gui, centerX - 64, centerY - 64, 128, 128, 0xFF3E2E20);
        drawGearShadows(gui, centerX - 64, centerY - 64, 128, 128);

        drawGoldFrame(gui, centerX - 64, centerY - 64, 128, 128, 2);

        drawPanel(gui, leftPos + 340, topPos + 8, 130, 200, 0xFF493420);
        drawGearShadows(gui, leftPos + 340, topPos + 8, 130, 200);

        drawGoldFrame(gui, leftPos + 340, topPos + 8, 130, 200, 3);

        gui.drawString(this.font, "Player Inventory", leftPos + 16, topPos + 12, 0xFFDDAA);
        gui.drawString(this.font, "Orbment Core", centerX - 36, centerY - 60, 0xFFD700);
        gui.drawString(this.font, "Orbment Workshop", leftPos + 350, topPos + 12, 0xFFFFBB);

        renderConversionDiagram(gui);

        gui.pose().pushPose();
        gui.pose().translate(leftPos + 352, topPos + 78, 0);
        gui.pose().scale(0.8f, 0.8f, 0.8f);
        gui.drawString(this.font, "Set Sepith Line (10 Mass)", 0, 0, 0xFFDDAA);
        gui.pose().popPose();

        ItemStack orb = this.menu.getOrbmentStack();
        if (orb.isEmpty() || minecraft == null || minecraft.level == null) return;
        OrbmentComponent comp = OrbmentItem.loadComponent(orb, minecraft.level);
        boolean[] unlockedStatus = comp.getUnlockedStatus();
        Element[] sepithLines = comp.getSepithLines();

        for (int i = 0; i < OrbmentComponent.MAX_SLOTS; i++) {
            double angle = 2 * Math.PI * i / 6 - Math.PI / 2;
            int sx = (int) (centerX + 40 * Math.cos(angle)) - 9;
            int sy = (int) (centerY + 40 * Math.sin(angle)) - 9;

            int outlineColor = sepithLines[i].getColor();
            int innerColor = 0xFF404040;

            gui.fill(sx - 1, sy - 1, sx + 19, sy + 19, outlineColor);
            gui.fill(sx, sy, sx + 18, sy + 18, innerColor);

            if (i == selectedSlot) {
                gui.fill(sx - 1, sy - 1, sx + 19, sy + 19, 0x80FFFFFF);
            }

            if (!unlockedStatus[i]) {
                gui.drawString(this.font, "X", sx + 6, sy + 5, 0xFF3333);
            }
        }
    }

    private void renderConversionDiagram(GuiGraphics gui) {
        int panelX = leftPos + 340;
        int panelWidth = 130;
        int startY = topPos + 28;

        float scale = 0.8f; // Items are slightly smaller
        int itemSize = (int)(16 * scale);
        int spacing = 1;

        int row1Width = (4 * itemSize) + (3 * spacing);
        int row2Width = (3 * itemSize) + (2 * spacing);

        int row1X = panelX + (panelWidth - row1Width) / 2;
        int row2X = panelX + (panelWidth - row2Width) / 2;
        int row2Y = startY + itemSize + spacing;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 100);
        gui.pose().scale(scale, scale, 1f);

        int r1x = (int)(row1X / scale);
        int r2x = (int)(row2X / scale);
        int r1y = (int)(startY / scale);
        int r2y = (int)(row2Y / scale);
        int s = (int)((itemSize + spacing) / scale);

        gui.renderItem(new ItemStack(ModItems.EARTH_MASS.get()), r1x, r1y);
        gui.renderItem(new ItemStack(ModItems.WATER_MASS.get()), r1x + s, r1y);
        gui.renderItem(new ItemStack(ModItems.FIRE_MASS.get()), r1x + 2*s, r1y);
        gui.renderItem(new ItemStack(ModItems.WIND_MASS.get()), r1x + 3*s, r1y);

        gui.renderItem(new ItemStack(ModItems.TIME_MASS.get()), r2x, r2y);
        gui.renderItem(new ItemStack(ModItems.SPACE_MASS.get()), r2x + s, r2y);
        gui.renderItem(new ItemStack(ModItems.MIRAGE_MASS.get()), r2x + 2*s, r2y);
        gui.pose().popPose();

        int arrowY = row2Y + itemSize;
        int resultY = arrowY + 9;
        gui.drawString(font, "->", panelX + (panelWidth - 8) / 2, arrowY, 0xFFFFFF, true);

        gui.pose().pushPose();
        gui.pose().translate(0,0,100);
        gui.pose().scale(scale, scale, 1f);
        gui.renderItem(new ItemStack(ModItems.SEPITH_MASS.get()), (int)((panelX + (panelWidth - itemSize) / 2)/scale), (int)(resultY/scale));
        gui.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {}

    @Override
    public void render(GuiGraphics gui, int mx, int my, float partial) {
        this.renderBackground(gui, mx, my, partial);
        super.render(gui, mx, my, partial);
        this.renderTooltip(gui, mx, my);
    }
}
