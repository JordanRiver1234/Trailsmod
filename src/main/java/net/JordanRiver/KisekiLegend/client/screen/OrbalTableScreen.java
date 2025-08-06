package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotQuadBuilder;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotRenderer;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.menu.OrbalTableMenu;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.OrbalTableOperationPacket;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.ArrayList;
import java.util.List;

public class OrbalTableScreen extends AbstractContainerScreen<OrbalTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KisekiLegend.MOD_ID, "textures/gui/orbal_table.png");
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    private final OrbalTableBlockEntity blockEntity;
    private PoseStack poseStack;
    private MultiBufferSource bufferSource;

    // UI State
    private boolean weaponAnalysisMode = false;
    private ItemStack currentWeapon = ItemStack.EMPTY;
    private WeaponSlotData weaponSlotData;
    private List<SlotPosition> availableSlotPositions = new ArrayList<>();
    private SlotPosition selectedPosition = null;
    private SlotPosition hoveredPosition = null;
    private String selectedElementType = "earth";
    private OperationMode operationMode = OperationMode.ADD_SLOT;
    private SlotPosition clickMark = null; // Single click mark that moves when repositioned

    // UI Elements
    private Button confirmButton;
    private Button cancelButton;
    private Button addSlotButton;
    private Button removeSlotButton;
    private Button changeElementButton;
    private Button closeSlotButton;

    // Element buttons
    private Button earthButton, waterButton, fireButton, windButton;
    private Button timeButton, spaceButton, mirageButton;

    public enum OperationMode {
        ADD_SLOT, REMOVE_SLOT, CHANGE_ELEMENT, CLOSE_SLOT
    }

    public static class SlotPosition {
        public final float modelX, modelY, modelZ; // 3D model coordinates
        public final int screenX, screenY; // Screen coordinates

        public SlotPosition(float modelX, float modelY, float modelZ, int screenX, int screenY) {
            this.modelX = modelX;
            this.modelY = modelY;
            this.modelZ = modelZ;
            this.screenX = screenX;
            this.screenY = screenY;
        }
    }


    public OrbalTableScreen(OrbalTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 256;
        this.imageHeight = 220; // Increased height for visible slots
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    protected void init() {
        // FIXED: Use proper height for full texture display
        this.imageWidth = 256;
        this.imageHeight = 240; // Increased to prevent cutoff
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        super.init();
        initializeButtons();
        checkWeaponStatus();
    }

    private void initializeButtons() {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        // FIXED: Move buttons higher to avoid inventory overlap
        addSlotButton = Button.builder(Component.literal("Add Slot"),
                        btn -> {
                            System.out.println("=== ADD SLOT BUTTON CLICKED ===");
                            setOperationMode(OperationMode.ADD_SLOT);
                        })
                .bounds(leftPos + 170, topPos + 15, 80, 18).build();

        // Replace the button initialization:
        removeSlotButton = Button.builder(Component.literal("Remove Quartz"),
                        btn -> setOperationMode(OperationMode.REMOVE_SLOT))
                .bounds(leftPos + 170, topPos + 35, 80, 18).build();

        changeElementButton = Button.builder(Component.literal("Change Element"),
                        btn -> setOperationMode(OperationMode.CHANGE_ELEMENT))
                .bounds(leftPos + 170, topPos + 55, 80, 18).build();

        closeSlotButton = Button.builder(Component.literal("Close Slot"),
                        btn -> setOperationMode(OperationMode.CLOSE_SLOT))
                .bounds(leftPos + 170, topPos + 75, 80, 18).build();

        // Element buttons - moved higher
        earthButton = Button.builder(Component.literal("E"), btn -> {
                    System.out.println("=== EARTH BUTTON CLICKED ===");
                    setSelectedElement("earth");
                })
                .bounds(leftPos + 170, topPos + 95, 20, 16).build();
        waterButton = Button.builder(Component.literal("W"), btn -> setSelectedElement("water"))
                .bounds(leftPos + 192, topPos + 95, 20, 16).build();
        fireButton = Button.builder(Component.literal("F"), btn -> setSelectedElement("fire"))
                .bounds(leftPos + 214, topPos + 95, 20, 16).build();
        windButton = Button.builder(Component.literal("Wi"), btn -> setSelectedElement("wind"))
                .bounds(leftPos + 170, topPos + 113, 22, 16).build();

        timeButton = Button.builder(Component.literal("T"), btn -> setSelectedElement("time"))
                .bounds(leftPos + 194, topPos + 113, 20, 16).build();
        spaceButton = Button.builder(Component.literal("S"), btn -> setSelectedElement("space"))
                .bounds(leftPos + 216, topPos + 113, 20, 16).build();
        mirageButton = Button.builder(Component.literal("M"), btn -> setSelectedElement("mirage"))
                .bounds(leftPos + 170, topPos + 131, 40, 16).build();

        // Confirm/Cancel buttons - moved much higher
        confirmButton = Button.builder(Component.literal("Start Operation"),
                        btn -> confirmOperation())
                .bounds(leftPos + 170, topPos + 155, 90, 18).build();

        cancelButton = Button.builder(Component.literal("Cancel"),
                        btn -> cancelOperation())
                .bounds(leftPos + 190, topPos + 175, 50, 18).build();

        // Add all buttons
        addRenderableWidget(addSlotButton);
        addRenderableWidget(removeSlotButton);
        addRenderableWidget(changeElementButton);
        addRenderableWidget(closeSlotButton);
        addRenderableWidget(earthButton);
        addRenderableWidget(waterButton);
        addRenderableWidget(fireButton);
        addRenderableWidget(windButton);
        addRenderableWidget(timeButton);
        addRenderableWidget(spaceButton);
        addRenderableWidget(mirageButton);
        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);

        updateButtonStates();
    }

    private void checkWeaponStatus() {
        ItemStack weapon = blockEntity.getWeaponItem();
        System.out.println("=== CHECK WEAPON STATUS ===");
        System.out.println("Weapon: " + (weapon.isEmpty() ? "EMPTY" : weapon.getDisplayName().getString()));

        // Use universal weapon detection
        boolean isValidWeapon = !weapon.isEmpty() && WeaponSlotRenderer.isWeaponOrTool(weapon);

        if (isValidWeapon && !ItemStack.isSameItemSameComponents(weapon, currentWeapon)) {
            currentWeapon = weapon.copy();
            weaponSlotData = WeaponSlotData.getOrCreate(weapon);
            weaponAnalysisMode = true;

            System.out.println("Weapon analysis mode activated for: " + weapon.getDisplayName().getString());
            System.out.println("Item class: " + weapon.getItem().getClass().getSimpleName());
        } else if (!isValidWeapon) {
            weaponAnalysisMode = false;
            currentWeapon = ItemStack.EMPTY;
            weaponSlotData = null;
        }

        updateButtonStates();
    }
// Add this method to your OrbalTableScreen class for debugging quartz textures

    private void debugQuartzTextureIssue() {
        if (weaponSlotData != null) {
            for (WeaponSlotData.WeaponSlot slot : weaponSlotData.getSlots()) {
                if (slot.hasQuartz() && slot.quartzItem.getItem() instanceof QuartzItem quartzItem) {
                    String quartzId = quartzItem.getQuartzId();
                    System.out.println("=== DEBUGGING QUARTZ TEXTURE ISSUE ===");
                    System.out.println("Quartz ID: " + quartzId);
                    System.out.println("Quartz Item: " + slot.quartzItem.getItem());
                    System.out.println("Expected texture path: textures/item/" + quartzId + ".png");

                    // Call the debug method from WeaponSlotQuadBuilder

                    // Test if the file exists in your mod
                    ResourceLocation testTexture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "item/" + quartzId);
                    System.out.println("Testing texture resource: " + testTexture);

                    break; // Only debug first quartz found
                }
            }
        }
    }

    // Call this method in your containerTick() or confirmOperation() method
// to debug when quartz is inserted:
// debugQuartzTextureIssue();
    private void debugSlotCoordinates() {
        if (weaponSlotData != null) {
            System.out.println("=== SLOT COORDINATES DEBUG ===");
            List<WeaponSlotData.WeaponSlot> slots = weaponSlotData.getSlots();
            for (int i = 0; i < slots.size(); i++) {
                WeaponSlotData.WeaponSlot slot = slots.get(i);
                if (!slot.isClosed) {
                    System.out.println("Slot " + i + ":");
                    System.out.println("  GUI coordinates: (" + slot.posX + ", " + slot.posY + ", " + slot.posZ + ")");
                    System.out.println("  World render: (" + (slot.posX * 0.1f) + ", " + (slot.posY * 0.08f) + ", " + (slot.posZ * 0.1f + 0.05f) + ")");
                    System.out.println("  Element: " + slot.elementType);
                    System.out.println("  Has quartz: " + slot.hasQuartz());
                }
            }
        }
    }
    private void setOperationMode(OperationMode mode) {
        System.out.println("=== SET OPERATION MODE ===");
        System.out.println("Mode: " + mode);
        System.out.println("Has weapon: " + weaponAnalysisMode);
        System.out.println("Weapon slot data: " + (weaponSlotData != null ? weaponSlotData.getSlotCount() + " slots" : "null"));

        if (this.operationMode == mode) {
            this.operationMode = null; // Clear if clicking same button
            this.selectedPosition = null;
            this.clickMark = null;
            minecraft.gui.setOverlayMessage(Component.literal("Mode: None"), false);
            System.out.println("Mode cleared");
        } else {
            this.operationMode = mode;
            this.selectedPosition = null;
            this.clickMark = null;
            minecraft.gui.setOverlayMessage(Component.literal("Mode: " + mode.name().replace("_", " ")), false);
            System.out.println("Mode set to: " + mode);
        }

        updateButtonStates();
    }

    private void setSelectedElement(String element) {
        this.selectedElementType = element;
        updateButtonStates();

        // Force GUI refresh
        if (minecraft != null) {
            minecraft.gui.setOverlayMessage(Component.literal("Selected: " + element), false);
        }
    }
    private boolean checkSepithRequirements() {
        // Check slot 8 for sepith mass
        ItemStack sepithStack = blockEntity.getInventory().getStackInSlot(8);
        return !sepithStack.isEmpty() && sepithStack.getItem().toString().contains("sepith_mass") && sepithStack.getCount() >= 5;
    }
    private void updateButtonStates() {
        boolean hasWeapon = weaponAnalysisMode && weaponSlotData != null;
        boolean hasSelection = selectedPosition != null;
        boolean canAddSlot = hasWeapon && weaponSlotData.getSlotCount() < 3;
        boolean hasSlots = hasWeapon && weaponSlotData.getSlotCount() > 0;

        System.out.println("=== UPDATE BUTTON STATES ===");
        System.out.println("Has weapon: " + hasWeapon);
        System.out.println("Can add slot: " + canAddSlot);
        System.out.println("Has slots: " + hasSlots);
        System.out.println("Current slots: " + (weaponSlotData != null ? weaponSlotData.getSlotCount() : "null"));

        // Check material requirements for add slot
        boolean hasElementMaterials = hasWeapon && selectedElementType != null &&
                blockEntity.hasRequiredMaterials(selectedElementType, 10);

        // Check sepith mass for close slot
        boolean hasSepithMass = hasWeapon && blockEntity.hasRequiredMaterials("sepith", 5);

        // Check if selected slot has quartz for remove operation
        boolean selectedSlotHasQuartz = false;
        if (hasWeapon && hasSelection && operationMode == OperationMode.REMOVE_SLOT) {
            int slotIndex = findClickedSlot();
            if (slotIndex >= 0) {
                WeaponSlotData.WeaponSlot slot = weaponSlotData.getSlot(slotIndex);
                selectedSlotHasQuartz = slot != null && slot.hasQuartz();
            }
        }

        // Operation buttons with enhanced state checking
        addSlotButton.active = hasWeapon && canAddSlot;
        removeSlotButton.active = hasWeapon && hasSlots;
        changeElementButton.active = hasWeapon && hasSlots;
        closeSlotButton.active = hasWeapon && hasSlots && hasSepithMass;

        // Visual feedback for material requirements
        if (operationMode == OperationMode.ADD_SLOT) {
            addSlotButton.active = addSlotButton.active && (selectedElementType == null || hasElementMaterials);
        }

        if (operationMode == OperationMode.CHANGE_ELEMENT) {
            changeElementButton.active = changeElementButton.active && (selectedElementType == null || hasElementMaterials);
        }

        if (operationMode == OperationMode.REMOVE_SLOT) {
            removeSlotButton.active = removeSlotButton.active && (hasSelection ? selectedSlotHasQuartz : true);
        }

        System.out.println("Add slot button active: " + addSlotButton.active);
        System.out.println("Has element materials: " + hasElementMaterials);
        System.out.println("Has sepith mass: " + hasSepithMass);
        System.out.println("Selected slot has quartz: " + selectedSlotHasQuartz);

        // Element buttons
        boolean showElements = hasWeapon && (operationMode == OperationMode.ADD_SLOT ||
                operationMode == OperationMode.CHANGE_ELEMENT);
        earthButton.visible = showElements;
        waterButton.visible = showElements;
        fireButton.visible = showElements;
        windButton.visible = showElements;
        timeButton.visible = showElements;
        spaceButton.visible = showElements;
        mirageButton.visible = showElements;

        // Confirm/Cancel buttons with enhanced requirements
        boolean canConfirm = hasWeapon && hasSelection;

        // Additional requirement checks for confirm button
        if (canConfirm && operationMode != null) {
            switch (operationMode) {
                case ADD_SLOT -> canConfirm = canConfirm && selectedElementType != null && hasElementMaterials;
                case CHANGE_ELEMENT -> canConfirm = canConfirm && selectedElementType != null && hasElementMaterials;
                case CLOSE_SLOT -> canConfirm = canConfirm && hasSepithMass;
                case REMOVE_SLOT -> canConfirm = canConfirm && selectedSlotHasQuartz;
            }
        }

        confirmButton.active = canConfirm;
        cancelButton.active = hasWeapon;
    }


    private void showMaterialError(String elementType, int required) {
        // Create a simple popup message
        Component message = Component.literal("Not enough " + elementType + " mass! Required: " + required);

        // Show as a temporary overlay
        minecraft.gui.setOverlayMessage(message, false);

        // Also play a sound
        minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
    }
    private void confirmOperation() {
        System.out.println("=== CONFIRM OPERATION CALLED ===");
        System.out.println("Selected position: " + (selectedPosition != null ? "SET" : "NULL"));
        System.out.println("Weapon slot data: " + (weaponSlotData != null ? "SET" : "NULL"));
        System.out.println("Operation mode: " + operationMode);

        if (selectedPosition == null || weaponSlotData == null || operationMode == null) {
            System.out.println("OPERATION CANCELLED - missing requirements");
            return;
        }

        // Enhanced requirement checking with user feedback
        OrbalTableOperationPacket packet = null;

        switch (operationMode) {
            case ADD_SLOT -> {
                if (weaponSlotData.getSlotCount() < 3) {
                    if (selectedElementType == null) {
                        showMaterialError("Please select an element type first", 0);
                        return;
                    } else if (!blockEntity.hasRequiredMaterials(selectedElementType, 10)) {
                        showMaterialError(selectedElementType + " mass", 10);
                        return;
                    } else {
                        packet = new OrbalTableOperationPacket(
                                blockEntity.getBlockPos(),
                                OrbalTableOperationPacket.OperationType.ADD_SLOT,
                                selectedElementType,
                                -1,
                                new float[]{selectedPosition.modelX, selectedPosition.modelY, selectedPosition.modelZ}

                        );
                    }
                }
            }
            case REMOVE_SLOT -> {
                int slotIndex = findClickedSlot();
                if (slotIndex >= 0) {
                    WeaponSlotData.WeaponSlot slot = weaponSlotData.getSlot(slotIndex);
                    if (slot != null && slot.hasQuartz()) {
                        packet = new OrbalTableOperationPacket(
                                blockEntity.getBlockPos(),
                                OrbalTableOperationPacket.OperationType.REMOVE_SLOT,
                                "",
                                slotIndex,
                                null
                        );
                    } else {
                        showMaterialError("No quartz in selected slot", 0);
                        return;
                    }
                }
            }
            case CHANGE_ELEMENT -> {
                int slotIndex = findClickedSlot();
                if (slotIndex >= 0) {
                    if (selectedElementType == null) {
                        showMaterialError("Please select an element type first", 0);
                        return;
                    } else if (!blockEntity.hasRequiredMaterials(selectedElementType, 10)) {
                        showMaterialError(selectedElementType + " mass", 10);
                        return;
                    } else {
                        packet = new OrbalTableOperationPacket(
                                blockEntity.getBlockPos(),
                                OrbalTableOperationPacket.OperationType.CHANGE_ELEMENT,
                                selectedElementType,
                                slotIndex,
                                null
                        );
                    }
                }
            }
            case CLOSE_SLOT -> {
                int slotIndex = findClickedSlot();
                if (slotIndex >= 0) {
                    if (!blockEntity.hasRequiredMaterials("sepith", 5)) {
                        showMaterialError("Sepith mass", 5);
                        return;
                    } else {
                        packet = new OrbalTableOperationPacket(
                                blockEntity.getBlockPos(),
                                OrbalTableOperationPacket.OperationType.CLOSE_SLOT,
                                "",
                                slotIndex,
                                null
                        );
                    }
                }
            }
        }

        if (packet != null) {
            NetworkHandler.sendToServer(packet);
            selectedPosition = null;
            clickMark = null;
            // Add this line after successful slot creation
            debugSlotCoordinates();
            updateButtonStates();

        }
    }
    private int findClickedSlot() {
        if (weaponSlotData == null || selectedPosition == null) return -1;

        List<WeaponSlotData.WeaponSlot> allSlots = weaponSlotData.getSlots();
        for (int i = 0; i < allSlots.size(); i++) {
            WeaponSlotData.WeaponSlot slot = allSlots.get(i);
            if (slot.isClosed) continue; // Skip closed slots

            float dx = slot.posX - selectedPosition.modelX;
            float dy = slot.posY - selectedPosition.modelY;
            float dz = slot.posZ - selectedPosition.modelZ;
            float distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq < 0.1f) { // Close enough
                System.out.println("Found clicked slot at list index: " + i);
                return i; // Return the actual list index, not the visual index
            }
        }
        return -1;
    }

    // Add this to the cancelOperation method
    private void cancelOperation() {
        selectedPosition = null;
        hoveredPosition = null;
        clickMark = null; // FIXED: Clear click mark when canceling
        updateButtonStates();
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // FIXED: Force GUI refresh when weapon changes
        ItemStack currentServerWeapon = blockEntity.getWeaponItem();
        if (!ItemStack.isSameItemSameComponents(currentServerWeapon, currentWeapon)) {
            System.out.println("=== WEAPON CHANGED IN CONTAINER TICK ===");
            System.out.println("Old weapon: " + (currentWeapon.isEmpty() ? "EMPTY" : currentWeapon.getDisplayName().getString()));
            System.out.println("New weapon: " + (currentServerWeapon.isEmpty() ? "EMPTY" : currentServerWeapon.getDisplayName().getString()));

            checkWeaponStatus();

            // Force immediate visual update
            if (weaponAnalysisMode && weaponSlotData != null) {
                System.out.println("Forcing immediate GUI refresh with " + weaponSlotData.getSlots().size() + " slots");
            }
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);

        // Clear hover state if over weapon slot to prevent white box
        if (this.hoveredSlot != null && this.hoveredSlot.index == 0) {
            this.hoveredSlot = null;
        }
    }
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Render the main GUI texture
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 240);

        // Render weapon analysis overlay if active
        if (weaponAnalysisMode && weaponSlotData != null) {
            renderWeaponAnalysis(guiGraphics, x, y, mouseX, mouseY);
        }

        // NEW: Render slot info in top right
        renderSlotInfo(guiGraphics, x, y);
    }

    private void renderWeaponAnalysis(GuiGraphics guiGraphics, int screenX, int screenY, int mouseX, int mouseY) {
        int weaponDisplayX = screenX + 20;
        int weaponDisplayY = screenY + 20;
        int weaponDisplayWidth = 140;
        int weaponDisplayHeight = 90;

        // Draw weapon display background with border
        guiGraphics.fill(weaponDisplayX, weaponDisplayY, weaponDisplayX + weaponDisplayWidth, weaponDisplayY + weaponDisplayHeight, 0x88000000);
        guiGraphics.hLine(weaponDisplayX, weaponDisplayX + weaponDisplayWidth, weaponDisplayY, 0xFFFFFFFF);
        guiGraphics.hLine(weaponDisplayX, weaponDisplayX + weaponDisplayWidth, weaponDisplayY + weaponDisplayHeight, 0xFFFFFFFF);
        guiGraphics.vLine(weaponDisplayX, weaponDisplayY, weaponDisplayY + weaponDisplayHeight, 0xFFFFFFFF);
        guiGraphics.vLine(weaponDisplayX + weaponDisplayWidth, weaponDisplayY, weaponDisplayY + weaponDisplayHeight, 0xFFFFFFFF);

        ItemStack displayWeapon = blockEntity.getWeaponItem();

        // FIRST: Render weapon at LOWEST Z-index (background)
        if (!displayWeapon.isEmpty()) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            try {
                int weaponCenterX = weaponDisplayX + weaponDisplayWidth / 2;
                int weaponCenterY = weaponDisplayY + weaponDisplayHeight / 2;

                poseStack.translate(weaponCenterX, weaponCenterY, 0);
                poseStack.scale(4.5f, 4.5f, 1.0f);

                guiGraphics.renderItem(displayWeapon, -8, -8);
            } finally {
                poseStack.popPose();
            }

            // Add weapon name below
            String weaponName = displayWeapon.getDisplayName().getString();
            int textWidth = minecraft.font.width(weaponName);
            guiGraphics.drawString(minecraft.font, weaponName,
                    weaponDisplayX + (weaponDisplayWidth - textWidth) / 2,
                    weaponDisplayY + weaponDisplayHeight + 5, 0xFFFFFFFF, true);
        }

        // SECOND: Render slots with FIXED positioning
        if (weaponSlotData != null) {
            List<WeaponSlotData.WeaponSlot> activeSlots = new ArrayList<>();
            for (WeaponSlotData.WeaponSlot slot : weaponSlotData.getSlots()) {
                if (!slot.isClosed) {
                    activeSlots.add(slot);
                }
            }

            // RENDER SLOTS AT HIGH Z-INDEX with corrected positioning
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);

            for (int i = 0; i < activeSlots.size(); i++) {
                WeaponSlotData.WeaponSlot slot = activeSlots.get(i);
                // FIXED: Use consistent scaling with mouse clicking
                int slotScreenX = weaponDisplayX + weaponDisplayWidth / 2 + (int) (slot.posX * 25);
                int slotScreenY = weaponDisplayY + weaponDisplayHeight / 2 + (int) (slot.posY * 20);

                // Render slot texture background
                ResourceLocation slotTexture = getSlotTexture(slot.elementType);
                int slotSize = 12;

                guiGraphics.blit(slotTexture, slotScreenX - slotSize/2, slotScreenY - slotSize/2,
                        0, 0, slotSize, slotSize, slotSize, slotSize);

                if (slot.hasQuartz()) {
                    // Render quartz texture on top
                    if (slot.quartzItem.getItem() instanceof QuartzItem quartzItem) {
                        ResourceLocation quartzTexture = getQuartzTexture(quartzItem.getQuartzId());
                        if (quartzTexture != null) {
                            int quartzSize = 8;

                            // FIXED: Set shader color to white before rendering
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                            // Try rendering the actual item instead of the texture
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(slotScreenX - quartzSize/2, slotScreenY - quartzSize/2, 0);
                            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);

                            // Render the actual quartz item instead of just the texture
                            guiGraphics.renderItem(slot.quartzItem, 0, 0);

                            guiGraphics.pose().popPose();

                            // FIXED: Remove the glow effect that might be causing color issues
                            // int glowColor = getElementColor(slot.elementType) | 0x40000000;
                            // guiGraphics.fill(slotScreenX - quartzSize/2 - 1, slotScreenY - quartzSize/2 - 1,
                            //         slotScreenX + quartzSize/2 + 1, slotScreenY + quartzSize/2 + 1, glowColor);
                        }
                    }
                }

                // Add slot number indicator below slot
                String slotNumber = String.valueOf(i + 1);
                int textX = slotScreenX - minecraft.font.width(slotNumber) / 2;
                int textY = slotScreenY + slotSize/2 + 3;
                guiGraphics.drawString(minecraft.font, slotNumber, textX, textY, 0xFFFFFFFF, true);
            }
            guiGraphics.pose().popPose();
        }

        // FIXED: Crosshair rendering with proper positioning
        if (operationMode == OperationMode.ADD_SLOT && weaponSlotData != null && weaponSlotData.getSlotCount() < 3) {
            if (mouseX >= weaponDisplayX && mouseX <= weaponDisplayX + weaponDisplayWidth &&
                    mouseY >= weaponDisplayY && mouseY <= weaponDisplayY + weaponDisplayHeight) {

                int crosshairColor = getElementColor(selectedElementType);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 300);
                guiGraphics.fill(mouseX - 6, mouseY - 1, mouseX + 6, mouseY + 1, crosshairColor | 0xFF000000);
                guiGraphics.fill(mouseX - 1, mouseY - 6, mouseX + 1, mouseY + 6, crosshairColor | 0xFF000000);
                guiGraphics.pose().popPose();
            }
        }

        // FIXED: Click mark rendering with corrected positioning
        if (clickMark != null) {
            int markScreenX = weaponDisplayX + weaponDisplayWidth / 2 + (int) (clickMark.modelX * 25);
            int markScreenY = weaponDisplayY + weaponDisplayHeight / 2 + (int) (clickMark.modelY * 20);
            int markColor = getElementColor(selectedElementType);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500);
            guiGraphics.fill(markScreenX - 6, markScreenY - 6, markScreenX + 6, markScreenY + 6, 0xFFFFFFFF);
            guiGraphics.fill(markScreenX - 5, markScreenY - 5, markScreenX + 5, markScreenY + 5, markColor | 0xFF000000);
            guiGraphics.fill(markScreenX - 3, markScreenY - 3, markScreenX + 3, markScreenY + 3, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
        }
    }
    private ResourceLocation getSlotTexture(String elementType) {
        return switch (elementType.toLowerCase()) {
            case "earth" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/earth_slot.png");
            case "water" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/water_slot.png");
            case "fire" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/fire_slot.png");
            case "wind" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/wind_slot.png");
            case "time" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/time_slot.png");
            case "space" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/space_slot.png");
            case "mirage" -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/mirage_slot.png");
            default -> ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/slot/earth_slot.png");
        };
    }

    private ResourceLocation getQuartzTexture(String quartzId) {
        // Debug the texture path
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/item/" + quartzId + ".png");
        return texture;
    }
    private void renderSlotInfo(GuiGraphics guiGraphics, int screenX, int screenY) {
        if (!weaponAnalysisMode || weaponSlotData == null) return;

        // Top right area - above all buttons
        int infoX = screenX + 170;
        int infoY = screenY + 5;
        int infoWidth = 80;
        int infoHeight = 8;

        List<WeaponSlotData.WeaponSlot> activeSlots = new ArrayList<>();
        for (WeaponSlotData.WeaponSlot slot : weaponSlotData.getSlots()) {
            if (!slot.isClosed) {
                activeSlots.add(slot);
            }
        }

        // Background for slot info
        int totalHeight = (activeSlots.size() + 1) * infoHeight + 4;
        guiGraphics.fill(infoX - 2, infoY - 2, infoX + infoWidth + 2, infoY + totalHeight, 0x88000000);
        guiGraphics.hLine(infoX - 2, infoX + infoWidth + 2, infoY - 2, 0xFFFFFFFF);
        guiGraphics.hLine(infoX - 2, infoX + infoWidth + 2, infoY + totalHeight, 0xFFFFFFFF);
        guiGraphics.vLine(infoX - 2, infoY - 2, infoY + totalHeight, 0xFFFFFFFF);
        guiGraphics.vLine(infoX + infoWidth + 2, infoY - 2, infoY + totalHeight, 0xFFFFFFFF);

        // Header - REMOVED quartz indicators
        String headerText = "Slots: " + activeSlots.size() + "/3";
        guiGraphics.drawString(minecraft.font, headerText, infoX, infoY, 0xFFFFFFFF, false);

        // Individual slot info - REMOVED quartz indicators
        for (int i = 0; i < activeSlots.size(); i++) {
            WeaponSlotData.WeaponSlot slot = activeSlots.get(i);
            int lineY = infoY + (i + 1) * infoHeight;

            // Just slot number and element (no quartz indicator)
            String slotText = (i + 1) + ": " + slot.elementType.substring(0, 1).toUpperCase() +
                    slot.elementType.substring(1).toLowerCase();

            // Color based on element
            int elementColor = getElementColor(slot.elementType);
            guiGraphics.drawString(minecraft.font, slotText, infoX, lineY, elementColor | 0xFF000000, false);

            // REMOVED: Quartz indicator code
        }
    }
    private boolean isMouseOverWeaponShape(int mouseX, int mouseY, int weaponDisplayX, int weaponDisplayY, int weaponDisplayWidth, int weaponDisplayHeight) {
        // Basic rectangular bounds check first
        if (mouseX < weaponDisplayX || mouseX > weaponDisplayX + weaponDisplayWidth ||
                mouseY < weaponDisplayY || mouseY > weaponDisplayY + weaponDisplayHeight) {
            return false;
        }

        if (currentWeapon.isEmpty()) return false;

        int relativeX = mouseX - weaponDisplayX;
        int relativeY = mouseY - weaponDisplayY;

        // FIXED: More precise weapon shape detection
        float centerX = weaponDisplayWidth * 0.5f;
        float centerY = weaponDisplayHeight * 0.5f;
        float distFromCenterX = Math.abs(relativeX - centerX) / centerX;
        float distFromCenterY = Math.abs(relativeY - centerY) / centerY;

        // WEAPON-SPECIFIC SHAPE DETECTION with modded weapon support
        String itemName = currentWeapon.getItem().toString().toLowerCase();

        if (currentWeapon.getItem() instanceof net.minecraft.world.item.SwordItem || itemName.contains("sword")) {
            // Sword shape - long and narrow
            if (relativeY < weaponDisplayHeight * 0.75) { // Blade area
                return distFromCenterX < 0.3f; // Narrow blade
            } else { // Hilt area
                return distFromCenterX < 0.4f; // Slightly wider hilt
            }
        } else if (currentWeapon.getItem() instanceof net.minecraft.world.item.AxeItem || itemName.contains("axe")) {
            // Axe shape - wider head, narrow handle
            if (relativeY < weaponDisplayHeight * 0.4) { // Axe head
                return distFromCenterX < 0.6f;
            } else { // Handle
                return distFromCenterX < 0.2f;
            }
        } else if (currentWeapon.getItem() instanceof net.minecraft.world.item.BowItem || itemName.contains("bow")) {
            // Bow shape - curved
            float bowWidth = 0.3f + 0.4f * (1 - distFromCenterY); // Wider in center
            return distFromCenterX < bowWidth * 0.5f;
        } else {
            // FIXED: Default shape for modded weapons - conservative rectangular area
            return distFromCenterX < 0.6f && distFromCenterY < 0.8f;
        }
    }

    private int getElementColor(String element) {
        return switch (element.toLowerCase()) {
            case "earth" -> 0x8B4513; // Brown
            case "water" -> 0x0066CC; // Blue
            case "fire" -> 0xFFD92222;  // Red-Orange
            case "wind" -> 0x90EE90;  // Light Green
            case "time" -> 0x9370DB;  // Purple
            case "space" -> 0xFFD9D522; // yellow
            case "mirage" -> 0xFF888888; // grey
            default -> 0xFF404040;      // Gray
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && weaponAnalysisMode) { // Left click
            int weaponDisplayX = leftPos + 20;
            int weaponDisplayY = topPos + 20;
            int weaponDisplayWidth = 140;
            int weaponDisplayHeight = 90;

            if (mouseX >= weaponDisplayX && mouseX <= weaponDisplayX + weaponDisplayWidth &&
                    mouseY >= weaponDisplayY && mouseY <= weaponDisplayY + weaponDisplayHeight) {

                if (currentWeapon.isEmpty() || !isMouseOverWeaponShape((int)mouseX, (int)mouseY,
                        weaponDisplayX, weaponDisplayY, weaponDisplayWidth, weaponDisplayHeight)) {
                    return true;
                }

                if (operationMode == OperationMode.ADD_SLOT && weaponSlotData.getSlotCount() < 3) {
                    // FIXED: Corrected coordinate mapping for proper positioning
                    float relativeX = (float)(mouseX - weaponDisplayX - weaponDisplayWidth/2);
                    float relativeY = (float)(mouseY - weaponDisplayY - weaponDisplayHeight/2);

                    // Convert to model coordinates with proper scaling
                    float modelX = relativeX / 25.0f;  // Matches the render scaling
                    float modelY = relativeY / 20.0f;  // Matches the render scaling

                    selectedPosition = new SlotPosition(modelX, modelY, 0.0f,
                            (int)(mouseX - leftPos), (int)(mouseY - topPos));

                    // FIXED: Click mark positioning to match rendering
                    clickMark = new SlotPosition(modelX, modelY, 0.0f,
                            weaponDisplayX + weaponDisplayWidth/2 + (int)(modelX * 25),
                            weaponDisplayY + weaponDisplayHeight/2 + (int)(modelY * 20));

                    System.out.println("=== SLOT POSITION SET ===");
                    System.out.println("Click at screen: " + mouseX + ", " + mouseY);
                    System.out.println("Relative: " + relativeX + ", " + relativeY);
                    System.out.println("Model coordinates: " + modelX + ", " + modelY);
                    System.out.println("Click mark at: " + clickMark.screenX + ", " + clickMark.screenY);

                    updateButtonStates();
                    return true;
                }

                // FIXED: Existing slot clicking with corrected coordinate mapping
                if (operationMode != OperationMode.ADD_SLOT && weaponSlotData != null) {
                    List<WeaponSlotData.WeaponSlot> activeSlots = new ArrayList<>();
                    for (WeaponSlotData.WeaponSlot slot : weaponSlotData.getSlots()) {
                        if (!slot.isClosed) {
                            activeSlots.add(slot);
                        }
                    }

                    for (int i = 0; i < activeSlots.size(); i++) {
                        WeaponSlotData.WeaponSlot slot = activeSlots.get(i);
                        // FIXED: Use same scaling as rendering for consistency
                        int slotScreenX = weaponDisplayX + weaponDisplayWidth / 2 + (int) (slot.posX * 25);
                        int slotScreenY = weaponDisplayY + weaponDisplayHeight / 2 + (int) (slot.posY * 20);

                        // FIXED: Proper hit detection area
                        if (mouseX >= slotScreenX - 12 && mouseX <= slotScreenX + 12 &&
                                mouseY >= slotScreenY - 12 && mouseY <= slotScreenY + 12) {

                            selectedPosition = new SlotPosition(slot.posX, slot.posY, slot.posZ,
                                    slotScreenX - leftPos, slotScreenY - topPos);

                            clickMark = new SlotPosition(slot.posX, slot.posY, slot.posZ,
                                    slotScreenX, slotScreenY);

                            updateButtonStates();
                            System.out.println("Selected existing slot " + (i+1) + " at position: " + slot.posX + ", " + slot.posY);
                            return true;
                        }
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        guiGraphics.drawString(this.font, "S", 4, 130, 0xFFFFFFFF, false); // Sepith (white)

        guiGraphics.drawString(this.font, "E", 24, 130, 0x8B4513, false); // Earth
        guiGraphics.drawString(this.font, "W", 44, 130, 0x0066CC, false); // Water
        guiGraphics.drawString(this.font, "F", 64, 130, 0xFF2222, false); // Fire
        guiGraphics.drawString(this.font, "Wi", 83, 130, 0x90EE90, false); // Wind
        guiGraphics.drawString(this.font, "T", 104, 130, 0x9370DB, false); // Time
        guiGraphics.drawString(this.font, "S", 124, 130, 0xFFD922, false); // Space
        guiGraphics.drawString(this.font, "M", 143, 130, 0x888888, false); // Mirage
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);


        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Only render custom tooltips for element buttons - NOT in weapon area
        if (earthButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Earth Element"), mouseX, mouseY);
        } else if (waterButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Water Element"), mouseX, mouseY);
        } else if (fireButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Fire Element"), mouseX, mouseY);
        } else if (windButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Wind Element"), mouseX, mouseY);
        } else if (timeButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Time Element"), mouseX, mouseY);
        } else if (spaceButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Space Element"), mouseX, mouseY);
        } else if (mirageButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Mirage Element"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        // Check if we're hovering over the weapon display area
        int weaponDisplayX = leftPos + 20;
        int weaponDisplayY = topPos + 15;
        int weaponDisplayWidth = 140;
        int weaponDisplayHeight = 90;

        if (weaponAnalysisMode &&
                x >= weaponDisplayX && x <= weaponDisplayX + weaponDisplayWidth &&
                y >= weaponDisplayY && y <= weaponDisplayY + weaponDisplayHeight) {
            // Don't render tooltip in weapon area
            return;
        }

        // Check if hovering over weapon slot (slot 0)
        if (this.hoveredSlot != null && this.hoveredSlot.index == 0) {
            // Don't render tooltip for weapon slot
            return;
        }

        super.renderTooltip(guiGraphics, x, y);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // Always allow escape to close menu
            cancelOperation(); // Clear any active operation
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}