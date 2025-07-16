package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.OrbmentMachineMenu;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int mx, int my) {
        drawPanel(gui, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828);
        drawPanel(gui, centerX - 64, centerY - 64, 128, 128, 0xFF3E2E20);
        drawPanel(gui, leftPos + 340, topPos + 8, 130, 200, 0xFF493420);

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
