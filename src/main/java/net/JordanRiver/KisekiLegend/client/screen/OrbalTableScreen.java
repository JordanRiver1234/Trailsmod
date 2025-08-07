package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
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
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import java.util.ArrayList;
import java.util.List;

public class OrbalTableScreen extends AbstractContainerScreen<OrbalTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KisekiLegend.MOD_ID, "textures/gui/orbal_table.png");
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    // Add these new fields near the top of your class
    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 252; // Increased height for more space

    private final OrbalTableBlockEntity blockEntity;
    private PoseStack poseStack;
    private MultiBufferSource bufferSource;
    private final List<SteampunkClock> clocks = new ArrayList<>();
    private final List<SteampunkValve> valves = new ArrayList<>();
    private final List<BrassTubing> tubing = new ArrayList<>();
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
    // 3D Rotation and Animation
    private float weaponRotationY = 0.0f;
    private float weaponRotationX = 15.0f; // Slight tilt for better view
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;
    private float targetRotationY = 0.0f;
    private float targetRotationX = 15.0f;
    private long animationStartTime = 0;
    // Animation
    private float animationProgress = 0.0f;
    private static final long ANIMATION_DURATION = 500; // in milliseconds
    // Visual Effects
    // Enhanced Brown & Blue Steampunk Color Palette
    private static final int STEAMPUNK_BROWN_DARK = 0xFF4A3429;
    private static final int STEAMPUNK_BROWN_MEDIUM = 0xFF6B4C3A;
    private static final int STEAMPUNK_BROWN_LIGHT = 0xFF8B6F47;
    private static final int STEAMPUNK_BLUE_DARK = 0xFF1E3A5F;
    private static final int STEAMPUNK_BLUE_MEDIUM = 0xFF2E4F7A;
    private static final int STEAMPUNK_BLUE_LIGHT = 0xFF4A6FA5;
    private static final int STEAMPUNK_COPPER = 0xFFB87333;
    private static final int STEAMPUNK_BRASS = 0xFFD4AF37;
    private static final int STEAMPUNK_STEAM = 0x40E6F3FF;
    private final List<FloatingParticle> particles = new ArrayList<>();
    private final List<CircuitLine> circuitLines = new ArrayList<>();
    private long lastParticleSpawn = 0;
    // UI Elements
    private Button confirmButton;
    private Button addSlotButton;
    private Button removeSlotButton;
    private Button changeElementButton;
    private Button closeSlotButton;
    private final List<SteampunkGear> gears = new ArrayList<>();
    private final List<SteamPipe> steamPipes = new ArrayList<>();
    private final List<PressureGauge> pressureGauges = new ArrayList<>();
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


    // === CONSTRUCTOR REPLACEMENT ===
// Replace your constructor with:
    public OrbalTableScreen(OrbalTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.getBlockEntity();
        // Full screen setup
        this.imageWidth = 0; // Will be set in init()
        this.imageHeight = 0;
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();

        // Initialize circuit lines
        initializeCircuitLines();
    }
    // === METHOD REPLACEMENT: init() ===
    @Override
    protected void init() {
        // Fixed size
        this.animationStartTime = System.currentTimeMillis();
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        super.init(); // This now correctly sets up player inventory slots
        initializeButtons();
        checkWeaponStatus();
        initializeParticles(); // Particles will still be fullscreen
    }
    // === NEW INITIALIZATION METHODS ===
// Add these new methods:
    private void initializeCircuitLines() {
        circuitLines.clear();
        // These will be properly positioned in init() when we have screen dimensions
    }

    private void repositionCircuitLines() {
        circuitLines.clear();
        int centerX = width / 2;
        int centerY = height / 2;

        circuitLines.add(new CircuitLine(50, 50, centerX - 200, 50, 0xFFC07020)); // Bronze
        circuitLines.add(new CircuitLine(width - 50, 50, centerX + 200, 50, 0xFFC07020)); // Bronze
        circuitLines.add(new CircuitLine(50, height - 50, centerX - 200, height - 50, 0xFF8B4513)); // Dark Brown
        circuitLines.add(new CircuitLine(width - 50, height - 50, centerX + 200, height - 50, 0xFF8B4513)); // Dark Brown
        // Vertical lines
        circuitLines.add(new CircuitLine(50, 100, 50, height - 100, 0xFF888888));
        circuitLines.add(new CircuitLine(width - 50, 100, width - 50, height - 100, 0xFF888888));
    }

    private void initializeParticles() {
        particles.clear();
        // Spawn initial particles
        for (int i = 0; i < 20; i++) {
            spawnParticle();
        }
        initializeSteampunkDecorations();
    }

    private void initializeSteampunkDecorations() {
        gears.clear();
        steamPipes.clear();
        pressureGauges.clear();
        clocks.clear();
        valves.clear();
        tubing.clear();

        // Add gears with new color palette
        gears.add(new SteampunkGear(60, 60, 25, 0.01f, STEAMPUNK_BRASS, 12));
        gears.add(new SteampunkGear(width - 80, 70, 30, -0.008f, STEAMPUNK_COPPER, 16));
        gears.add(new SteampunkGear(40, height - 80, 20, 0.012f, STEAMPUNK_BROWN_MEDIUM, 10));
        gears.add(new SteampunkGear(width - 60, height - 90, 28, -0.009f, STEAMPUNK_BLUE_MEDIUM, 14));

        // Medium gears
        gears.add(new SteampunkGear(width / 4, 40, 15, 0.015f, STEAMPUNK_BROWN_LIGHT, 8));
        gears.add(new SteampunkGear(3 * width / 4, height - 50, 18, -0.011f, STEAMPUNK_BLUE_LIGHT, 9));

        // Small accent gears
        gears.add(new SteampunkGear(width / 6, height / 3, 12, 0.02f, STEAMPUNK_COPPER, 6));
        gears.add(new SteampunkGear(5 * width / 6, 2 * height / 3, 14, -0.018f, STEAMPUNK_BRASS, 7));

        // Steam pipes with new colors
        steamPipes.add(new SteamPipe(20, 100, 20, height - 120, 8, STEAMPUNK_BROWN_DARK));
        steamPipes.add(new SteamPipe(width - 25, 90, width - 25, height - 110, 6, STEAMPUNK_BLUE_DARK));
        steamPipes.add(new SteamPipe(100, 30, width - 120, 30, 5, STEAMPUNK_BROWN_MEDIUM));

        // Pressure gauges
        pressureGauges.add(new PressureGauge(100, height - 60, 25, STEAMPUNK_BROWN_DARK));
        pressureGauges.add(new PressureGauge(width - 100, 80, 20, STEAMPUNK_BLUE_DARK));

        // Steampunk clocks
        clocks.add(new SteampunkClock(width / 3, 50, 18, STEAMPUNK_BROWN_MEDIUM));
        clocks.add(new SteampunkClock(2 * width / 3, height - 70, 16, STEAMPUNK_BLUE_MEDIUM));

        // Valves
        valves.add(new SteampunkValve(150, height - 120, 15, STEAMPUNK_COPPER));
        valves.add(new SteampunkValve(width - 150, 120, 18, STEAMPUNK_BRASS));
        valves.add(new SteampunkValve(width / 2, 60, 12, STEAMPUNK_BROWN_LIGHT));

        // Brass tubing connections
        tubing.add(new BrassTubing(80, 80, width - 100, 100, STEAMPUNK_BRASS));
        tubing.add(new BrassTubing(60, height - 100, width - 80, height - 120, STEAMPUNK_COPPER));
        tubing.add(new BrassTubing(width / 4, height / 2, 3 * width / 4, height / 2 + 40, STEAMPUNK_BROWN_LIGHT));
    }

    private void spawnParticle() {
        if (particles.size() < 50) {
            float x = (float)(Math.random() * width);
            float y = height + 10;
            particles.add(new FloatingParticle(x, y));
        }
    }

    // === METHOD REPLACEMENT: initializeButtons() in OrbalTableScreen.java ===
    private void initializeButtons() {
        repositionCircuitLines();
        int panelX = this.leftPos + this.imageWidth - 170;
        int panelY = this.topPos + 15;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 24;

        addSlotButton = createStyledButton(Component.literal("⚡ Add Slot"), btn -> setOperationMode(OperationMode.ADD_SLOT), panelX + 5, panelY + 45, buttonWidth, buttonHeight);
        removeSlotButton = createStyledButton(Component.literal("Remove Quartz"), btn -> setOperationMode(OperationMode.REMOVE_SLOT), panelX + 5, panelY + 45 + spacing, buttonWidth, buttonHeight);
        changeElementButton = createStyledButton(Component.literal("Change Element"), btn -> setOperationMode(OperationMode.CHANGE_ELEMENT), panelX + 5, panelY + 45 + spacing * 2, buttonWidth, buttonHeight);
        closeSlotButton = createStyledButton(Component.literal("Close Slot"), btn -> setOperationMode(OperationMode.CLOSE_SLOT), panelX + 5, panelY + 45 + spacing * 3, buttonWidth, buttonHeight);


        // Element buttons grid - Mirage moved to right of Fire
        int elementY = panelY + 45 + spacing * 4 + 5;
        int elementButtonWidth = 32;
        int elementButtonHeight = 20;
        int elementSpacingX = 35;
        int elementSpacingY = 22;

// Element buttons with new colors
        earthButton = createElementButton(Component.literal("Earth"), btn -> setSelectedElement("earth"), panelX + 5, elementY, elementButtonWidth, elementButtonHeight, STEAMPUNK_BROWN_MEDIUM);
        waterButton = createElementButton(Component.literal("Water"), btn -> setSelectedElement("water"), panelX + 5 + elementSpacingX, elementY, elementButtonWidth, elementButtonHeight, STEAMPUNK_BLUE_MEDIUM);
        fireButton = createElementButton(Component.literal("Fire"), btn -> setSelectedElement("fire"), panelX + 5 + elementSpacingX * 2, elementY, elementButtonWidth, elementButtonHeight, STEAMPUNK_COPPER);
        mirageButton = createElementButton(Component.literal("Mirage"), btn -> setSelectedElement("mirage"), panelX + 5 + elementSpacingX * 3, elementY, elementButtonWidth, elementButtonHeight, 0xFF888888);

        windButton = createElementButton(Component.literal("Wind"), btn -> setSelectedElement("wind"), panelX + 5, elementY + elementSpacingY, elementButtonWidth, elementButtonHeight, STEAMPUNK_BROWN_LIGHT);
        timeButton = createElementButton(Component.literal("Time"), btn -> setSelectedElement("time"), panelX + 5 + elementSpacingX, elementY + elementSpacingY, elementButtonWidth, elementButtonHeight, STEAMPUNK_BRASS);
        spaceButton = createElementButton(Component.literal("Space"), btn -> setSelectedElement("space"), panelX + 5 + elementSpacingX * 2, elementY + elementSpacingY, elementButtonWidth, elementButtonHeight, STEAMPUNK_BLUE_LIGHT);

        int actionY = this.topPos + this.imageHeight - 30;
        confirmButton = createStyledButton(Component.literal("⚡ Start Operation"), btn -> confirmOperation(), panelX + 5, actionY - 5, buttonWidth, buttonHeight);

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
        updateButtonStates();
    }
    private Button createStyledButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return Button.builder(text, onPress).bounds(x, y, width, height).build();
    }

    private Button createElementButton(Component text, Button.OnPress onPress, int x, int y, int width, int height, int color) {
        return Button.builder(text, onPress).bounds(x, y, width, height).build();
    }
    private static class SteampunkClock {
        final int x, y, radius;
        final int faceColor, handColor;
        float hourAngle, minuteAngle;
        final float hourSpeed, minuteSpeed;

        SteampunkClock(int x, int y, int radius, int faceColor) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.faceColor = faceColor;
            this.handColor = STEAMPUNK_COPPER;
            this.hourAngle = (float)(Math.random() * Math.PI * 2);
            this.minuteAngle = (float)(Math.random() * Math.PI * 2);
            this.hourSpeed = 0.001f;
            this.minuteSpeed = 0.012f;
        }

        void update() {
            hourAngle += hourSpeed;
            minuteAngle += minuteSpeed;
            if (hourAngle > Math.PI * 2) hourAngle -= (float)(Math.PI * 2);
            if (minuteAngle > Math.PI * 2) minuteAngle -= (float)(Math.PI * 2);
        }
    }

    private static class SteampunkValve {
        final int x, y, size;
        final int valveColor;
        float wheelRotation;
        final float rotationSpeed;
        boolean isActive;

        SteampunkValve(int x, int y, int size, int valveColor) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.valveColor = valveColor;
            this.wheelRotation = 0;
            this.rotationSpeed = 0.02f + (float)(Math.random() * 0.01f);
            this.isActive = Math.random() < 0.3f;
        }

        void update() {
            if (isActive) {
                wheelRotation += rotationSpeed;
                if (wheelRotation > Math.PI * 2) wheelRotation -= (float)(Math.PI * 2);
            }

            // Randomly change activity
            if (Math.random() < 0.001f) {
                isActive = !isActive;
            }
        }
    }

    private static class BrassTubing {
        final int startX, startY, endX, endY;
        final int[] controlPoints;
        final int tubeColor, highlightColor;
        float flowProgress;
        final float flowSpeed;

        BrassTubing(int startX, int startY, int endX, int endY, int tubeColor) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.tubeColor = tubeColor;
            this.highlightColor = (tubeColor & 0x00FFFFFF) | 0x80000000;
            this.flowProgress = (float)(Math.random() * Math.PI * 2);
            this.flowSpeed = 0.03f + (float)(Math.random() * 0.02f);

            // Simple control points for curved tubes
            int midX = (startX + endX) / 2;
            int midY = (startY + endY) / 2;
            int offsetX = (int)((Math.random() - 0.5) * 50);
            int offsetY = (int)((Math.random() - 0.5) * 50);
            this.controlPoints = new int[]{midX + offsetX, midY + offsetY};
        }

        void update() {
            flowProgress += flowSpeed;
            if (flowProgress > Math.PI * 2) flowProgress -= (float)(Math.PI * 2);
        }
    }
    // Steam/Circuit Animation Classes
    private static class FloatingParticle {
        float x, y, size, speed, life, maxLife;
        int color;

        FloatingParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.size = 1 + (float)Math.random() * 2;
            this.speed = 0.5f + (float)Math.random() * 1.5f;
            this.maxLife = this.life = 60 + (int)(Math.random() * 120);
            this.color = Math.random() < 0.3 ? 0xFFC07020 : 0xFF888888; // Bronze or gray
              }

        void update() {
            y -= speed;
            life--;
            float alpha = Math.max(0, life / maxLife);
            color = (color & 0x00FFFFFF) | ((int)(alpha * 255) << 24);
        }

        boolean isAlive() { return life > 0; }
    }

    private static class CircuitLine {
        final int startX, startY, endX, endY;
        final int color;
        float pulsePhase;

        CircuitLine(int startX, int startY, int endX, int endY, int color) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color;
            this.pulsePhase = (float)(Math.random() * Math.PI * 2);
        }
    }
    private static class SteampunkGear {
        final float x, y, size, rotationSpeed;
        float rotation;
        final int color, strokeColor;
        final int teeth;

        SteampunkGear(float x, float y, float size, float rotationSpeed, int color, int teeth) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.rotationSpeed = rotationSpeed;
            this.color = color;
            this.strokeColor = (color & 0x00FFFFFF) | 0xFF000000;
            this.teeth = teeth;
            this.rotation = (float)(Math.random() * Math.PI * 2);
        }

        void update() {
            rotation += rotationSpeed;
            if (rotation > Math.PI * 2) rotation -= (float)(Math.PI * 2);
        }
    }

    private static class SteamPipe {
        final int startX, startY, endX, endY, width;
        final int pipeColor, steamColor;
        float steamProgress;
        final float steamSpeed;

        SteamPipe(int startX, int startY, int endX, int endY, int width, int pipeColor) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.width = width;
            this.pipeColor = pipeColor;
            this.steamColor = 0x60FFFFFF;
            this.steamProgress = (float)(Math.random() * Math.PI * 2);
            this.steamSpeed = 0.02f + (float)(Math.random() * 0.03f);
        }

        void update() {
            steamProgress += steamSpeed;
            if (steamProgress > Math.PI * 2) steamProgress -= (float)(Math.PI * 2);
        }
    }

    private static class PressureGauge {
        final int x, y, radius;
        final int faceColor, needleColor;
        float needleAngle, targetAngle;
        final float needleSpeed;

        PressureGauge(int x, int y, int radius, int faceColor) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.faceColor = faceColor;
            this.needleColor = 0xFFFF6666;
            this.needleAngle = -90;
            this.targetAngle = -90 + (float)(Math.random() * 180);
            this.needleSpeed = 0.5f + (float)(Math.random() * 1.0f);
        }

        void update() {
            // Smooth needle movement
            needleAngle += (targetAngle - needleAngle) * 0.02f;

            // Occasionally change target
            if (Math.random() < 0.005f) {
                targetAngle = -90 + (float)(Math.random() * 180);
            }
        }
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
                        // DEBUG: Print the actual coordinates being sent
                        System.out.println("=== SLOT CREATION DEBUG ===");
                        System.out.println("Selected position Z: " + selectedPosition.modelZ);
                        System.out.println("Position array: [" + selectedPosition.modelX + ", " + selectedPosition.modelY + ", " + selectedPosition.modelZ + "]");

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

    // Replace your containerTick() method with:
    @Override
    public void containerTick() {
        super.containerTick();

        // Update animations
        updateAnimations();

        // Smooth rotation interpolation
        if (!isDragging) {
            weaponRotationY += (targetRotationY - weaponRotationY) * 0.1f;
            weaponRotationX += (targetRotationX - weaponRotationX) * 0.1f;
        }

        // Check weapon changes
        ItemStack currentServerWeapon = blockEntity.getWeaponItem();
        if (!ItemStack.isSameItemSameComponents(currentServerWeapon, currentWeapon)) {
            System.out.println("=== WEAPON CHANGED IN CONTAINER TICK ===");
            System.out.println("Old weapon: " + (currentWeapon.isEmpty() ? "EMPTY" : currentWeapon.getDisplayName().getString()));
            System.out.println("New weapon: " + (currentServerWeapon.isEmpty() ? "EMPTY" : currentServerWeapon.getDisplayName().getString()));

            checkWeaponStatus();

            if (weaponAnalysisMode && weaponSlotData != null) {
                System.out.println("Forcing immediate GUI refresh with " + weaponSlotData.getSlots().size() + " slots");
            }
        }
    }
    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();

        // Update opening animation
        if (this.animationProgress < 1.0f) {
            long elapsedTime = System.currentTimeMillis() - this.animationStartTime;
            float progress = Math.min((float)elapsedTime / ANIMATION_DURATION, 1.0f);
            this.animationProgress = 1 - (float)Math.pow(1 - progress, 3);
        }

        // Update particles
        particles.removeIf(particle -> !particle.isAlive());
        particles.forEach(FloatingParticle::update);

        // Spawn new particles occasionally
        if (currentTime - lastParticleSpawn > 100) {
            spawnParticle();
            lastParticleSpawn = currentTime;
        }

        // Update circuit line pulses
        for (CircuitLine line : circuitLines) {
            line.pulsePhase += 0.05f;
        }

        // Update steampunk decorations
        gears.forEach(SteampunkGear::update);
        steamPipes.forEach(SteamPipe::update);
        pressureGauges.forEach(PressureGauge::update);
        // Add these lines at the end of updateAnimations():
        clocks.forEach(SteampunkClock::update);
        valves.forEach(SteampunkValve::update);
        tubing.forEach(BrassTubing::update);
    }
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);

        // Clear hover state if over weapon slot to prevent white box
        if (this.hoveredSlot != null && this.hoveredSlot.index == 0) {
            this.hoveredSlot = null;
        }
    }
    // === METHOD REPLACEMENT: renderBg() ===
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // This still renders the cool animated background fullscreen
        renderSteampunkBackground(guiGraphics);

        // Render the main UI panels within the GUI bounds
        render3DWeaponAnalysis(guiGraphics, mouseX, mouseY, partialTick);
        renderMaterialSlotsPanel(guiGraphics);
        renderControlPanel(guiGraphics);
        // Note: The player inventory background is drawn by super.render() using vanilla textures
    }
    // === NEW BACKGROUND RENDERING METHOD ===
// Add this new method:
    private void renderSteampunkBackground(GuiGraphics guiGraphics) {
        // Dark gradient background
// Replace the background gradient line:
        guiGraphics.fillGradient(0, 0, width, height, STEAMPUNK_BROWN_DARK | 0xFF000000, STEAMPUNK_BLUE_DARK | 0xFF000000);
        // Render animated particles
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 10);
        for (FloatingParticle particle : particles) {
            if (particle.isAlive()) {
                int size = (int)particle.size;
                guiGraphics.fill((int)particle.x - size/2, (int)particle.y - size/2,
                        (int)particle.x + size/2, (int)particle.y + size/2, particle.color);
            }
        }
        guiGraphics.pose().popPose();

        // Render circuit lines with pulsing effect
        for (CircuitLine line : circuitLines) {
            float pulse = (float)(Math.sin(line.pulsePhase) * 0.3 + 0.7);
            int alpha = (int)(pulse * 128);
            int pulseColor = (line.color & 0x00FFFFFF) | (alpha << 24);

            // Draw line with glow effect
            guiGraphics.fill(line.startX, line.startY, line.endX, line.startY + 1, pulseColor);
            guiGraphics.fill(line.startX, line.startY, line.startX + 1, line.endY, pulseColor);
        }

        // Corner decorations
        renderCornerDecorations(guiGraphics);
        // Render steampunk decorations
        renderSteampunkDecorations(guiGraphics);
    }
    // === NEW CORNER DECORATIONS METHOD ===
// Add this new method:
    private void renderCornerDecorations(GuiGraphics guiGraphics) {
        int cornerSize = 40;
        int glowColor = 0x40C07020; // Transparent Bronze
        // Top-left corner
        guiGraphics.fill(20, 20, 20 + cornerSize, 22, glowColor);
        guiGraphics.fill(20, 20, 22, 20 + cornerSize, glowColor);

        // Top-right corner
        guiGraphics.fill(width - 20 - cornerSize, 20, width - 20, 22, glowColor);
        guiGraphics.fill(width - 22, 20, width - 20, 20 + cornerSize, glowColor);

        // Bottom-left corner
        guiGraphics.fill(20, height - 22, 20 + cornerSize, height - 20, glowColor);
        guiGraphics.fill(20, height - 20 - cornerSize, 22, height - 20, glowColor);

        // Bottom-right corner
        guiGraphics.fill(width - 20 - cornerSize, height - 22, width - 20, height - 20, glowColor);
        guiGraphics.fill(width - 22, height - 20 - cornerSize, width - 20, height - 20, glowColor);
    }

    private void renderSteampunkDecorations(GuiGraphics guiGraphics) {
        // Render steam pipes first (background layer)
        for (SteamPipe pipe : steamPipes) {
            // Pipe body
            guiGraphics.fill(pipe.startX - pipe.width/2, pipe.startY,
                    pipe.startX + pipe.width/2, pipe.endY, pipe.pipeColor);

            // Steam effect
            float steamIntensity = (float)(Math.sin(pipe.steamProgress) * 0.3 + 0.7);
            int steamAlpha = (int)(steamIntensity * 96);
            int steamColor = (pipe.steamColor & 0x00FFFFFF) | (steamAlpha << 24);

            // Steam segments
            for (int i = 0; i < 5; i++) {
                int steamY = pipe.startY + (pipe.endY - pipe.startY) * i / 5;
                int steamWidth = pipe.width + (int)(Math.sin(pipe.steamProgress + i) * 3);
                guiGraphics.fill(pipe.startX - steamWidth/2, steamY,
                        pipe.startX + steamWidth/2, steamY + 10, steamColor);
            }

        }

        // Render pressure gauges
        for (PressureGauge gauge : pressureGauges) {
            // Gauge face (circle)
            drawCircle(guiGraphics, gauge.x, gauge.y, gauge.radius, gauge.faceColor);
            drawCircle(guiGraphics, gauge.x, gauge.y, gauge.radius - 2, 0xFF1A1A1A);

            // Gauge markings
            for (int i = 0; i <= 10; i++) {
                float angle = (float)Math.toRadians(-90 + i * 18); // 180 degrees total
                int markLength = (i % 5 == 0) ? 8 : 4;
                int startRadius = gauge.radius - 5;
                int endRadius = startRadius - markLength;

                int startX = gauge.x + (int)(Math.cos(angle) * startRadius);
                int startY = gauge.y + (int)(Math.sin(angle) * startRadius);
                int endX = gauge.x + (int)(Math.cos(angle) * endRadius);
                int endY = gauge.y + (int)(Math.sin(angle) * endRadius);

                drawLine(guiGraphics, startX, startY, endX, endY, 0xFFAAAAAA);
            }

            // Needle
            float needleRad = (float)Math.toRadians(gauge.needleAngle);
            int needleEndX = gauge.x + (int)(Math.cos(needleRad) * (gauge.radius - 8));
            int needleEndY = gauge.y + (int)(Math.sin(needleRad) * (gauge.radius - 8));
            drawLine(guiGraphics, gauge.x, gauge.y, needleEndX, needleEndY, gauge.needleColor);

            // Center dot
            guiGraphics.fill(gauge.x - 2, gauge.y - 2, gauge.x + 2, gauge.y + 2, 0xFFFFFFFF);
        }

        // Render gears (foreground layer)
        for (SteampunkGear gear : gears) {
            drawAnimatedGear(guiGraphics, gear);
        }
        // Add these at the end of renderSteampunkDecorations():

// Render clocks
        for (SteampunkClock clock : clocks) {
            drawClock(guiGraphics, clock);
        }

// Render valves
        for (SteampunkValve valve : valves) {
            drawValve(guiGraphics, valve);
        }

// Render tubing
        for (BrassTubing tube : tubing) {
            drawTubing(guiGraphics, tube);
        }
    }
    private void drawClock(GuiGraphics guiGraphics, SteampunkClock clock) {
        // Clock face
        drawCircle(guiGraphics, clock.x, clock.y, clock.radius, clock.faceColor);
        drawCircle(guiGraphics, clock.x, clock.y, clock.radius - 2, STEAMPUNK_BROWN_LIGHT);

        // Hour markers
        for (int i = 0; i < 12; i++) {
            float angle = (float)(i * Math.PI / 6 - Math.PI / 2);
            int markRadius = clock.radius - 4;
            int x = clock.x + (int)(Math.cos(angle) * markRadius);
            int y = clock.y + (int)(Math.sin(angle) * markRadius);
            guiGraphics.fill(x - 1, y - 1, x + 1, y + 1, STEAMPUNK_COPPER);
        }

        // Hour hand
        int hourX = clock.x + (int)(Math.cos(clock.hourAngle - Math.PI / 2) * (clock.radius * 0.5f));
        int hourY = clock.y + (int)(Math.sin(clock.hourAngle - Math.PI / 2) * (clock.radius * 0.5f));
        drawLine(guiGraphics, clock.x, clock.y, hourX, hourY, clock.handColor);

        // Minute hand
        int minuteX = clock.x + (int)(Math.cos(clock.minuteAngle - Math.PI / 2) * (clock.radius * 0.8f));
        int minuteY = clock.y + (int)(Math.sin(clock.minuteAngle - Math.PI / 2) * (clock.radius * 0.8f));
        drawLine(guiGraphics, clock.x, clock.y, minuteX, minuteY, clock.handColor);

        // Center
        guiGraphics.fill(clock.x - 2, clock.y - 2, clock.x + 2, clock.y + 2, STEAMPUNK_BRASS);
    }

    private void drawValve(GuiGraphics guiGraphics, SteampunkValve valve) {
        // Valve body
        guiGraphics.fill(valve.x - valve.size/2, valve.y - valve.size/4,
                valve.x + valve.size/2, valve.y + valve.size/4, valve.valveColor);

        // Rotating wheel
        for (int i = 0; i < 8; i++) {
            float angle = valve.wheelRotation + (float)(i * Math.PI / 4);
            int spokeX = valve.x + (int)(Math.cos(angle) * valve.size * 0.6f);
            int spokeY = valve.y + (int)(Math.sin(angle) * valve.size * 0.6f);
            drawLine(guiGraphics, valve.x, valve.y, spokeX, spokeY,
                    valve.isActive ? STEAMPUNK_BRASS : STEAMPUNK_BROWN_MEDIUM);
        }

        // Center hub
        drawCircle(guiGraphics, valve.x, valve.y, valve.size / 4, STEAMPUNK_BROWN_DARK);
    }

    private void drawTubing(GuiGraphics guiGraphics, BrassTubing tube) {
        // Simple curved tube using multiple line segments
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            float t = (float)i / segments;
            float nextT = (float)(i + 1) / segments;

            // Simple quadratic curve
            int x1 = (int)((1-t)*(1-t)*tube.startX + 2*(1-t)*t*tube.controlPoints[0] + t*t*tube.endX);
            int y1 = (int)((1-t)*(1-t)*tube.startY + 2*(1-t)*t*tube.controlPoints[1] + t*t*tube.endY);
            int x2 = (int)((1-nextT)*(1-nextT)*tube.startX + 2*(1-nextT)*nextT*tube.controlPoints[0] + nextT*nextT*tube.endX);
            int y2 = (int)((1-nextT)*(1-nextT)*tube.startY + 2*(1-nextT)*nextT*tube.controlPoints[1] + nextT*nextT*tube.endY);

            drawLine(guiGraphics, x1, y1, x2, y2, tube.tubeColor);

            // Flow effect
            if (Math.sin(tube.flowProgress + t * Math.PI * 4) > 0.5f) {
                drawLine(guiGraphics, x1 - 1, y1 - 1, x2 - 1, y2 - 1, tube.highlightColor);
            }
        }
    }
    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    guiGraphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int x = x1, y = y1;
        while (true) {
            guiGraphics.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }

    private void drawAnimatedGear(GuiGraphics guiGraphics, SteampunkGear gear) {
        float centerX = gear.x;
        float centerY = gear.y;
        float outerRadius = gear.size;
        float innerRadius = gear.size * 0.6f;
        float toothHeight = gear.size * 0.2f;

        // Draw gear body (inner circle)
        drawCircle(guiGraphics, (int)centerX, (int)centerY, (int)innerRadius, gear.color);
        drawCircle(guiGraphics, (int)centerX, (int)centerY, (int)innerRadius - 2, gear.strokeColor);

        // Draw teeth
        for (int i = 0; i < gear.teeth; i++) {
            float angle = gear.rotation + (float)(2 * Math.PI * i / gear.teeth);

            // Tooth positions
            float toothStartAngle = angle - 0.1f;
            float toothEndAngle = angle + 0.1f;

            // Draw tooth as a small rectangle
            for (float a = toothStartAngle; a <= toothEndAngle; a += 0.02f) {
                int x1 = (int)(centerX + Math.cos(a) * innerRadius);
                int y1 = (int)(centerY + Math.sin(a) * innerRadius);
                int x2 = (int)(centerX + Math.cos(a) * (innerRadius + toothHeight));
                int y2 = (int)(centerY + Math.sin(a) * (innerRadius + toothHeight));

                drawLine(guiGraphics, x1, y1, x2, y2, gear.color);
            }
        }

        // Center hole
        drawCircle(guiGraphics, (int)centerX, (int)centerY, (int)(gear.size * 0.15f), 0xFF000000);
    }
    // === METHOD REPLACEMENT: render3DWeaponAnalysis() ===
    private void render3DWeaponAnalysis(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 3D weapon display area - Panel slightly narrower, moved left
// Animate panel sliding in from the left
        int baseWeaponX = this.leftPos + 15;
        int weaponDisplayX = (int)(baseWeaponX - (1 - animationProgress) * (200 + baseWeaponX));
        int weaponDisplayY = this.topPos + 15;
        int weaponDisplayWidth = 180; // Reduced from 200
        int weaponDisplayHeight = 120;

        renderWeaponDisplayPanel(guiGraphics, weaponDisplayX, weaponDisplayY, weaponDisplayWidth, weaponDisplayHeight);
        ItemStack displayWeapon = blockEntity.getWeaponItem();

        if (!displayWeapon.isEmpty()) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            try {
                int weaponCenterX = weaponDisplayX + weaponDisplayWidth / 2;
                int weaponCenterY = weaponDisplayY + weaponDisplayHeight / 2;

                poseStack.translate(weaponCenterX, weaponCenterY, 100);

                // You can adjust the 28.0f value to make the weapon bigger or smaller
                float scale = 70f * (weaponDisplayWidth / 180f);
                poseStack.scale(scale, -scale, scale);

                // Apply 3D rotations
                poseStack.mulPose(Axis.XP.rotationDegrees(weaponRotationX));
                poseStack.mulPose(Axis.YP.rotationDegrees(weaponRotationY));

                // Render weapon
                RenderSystem.enableDepthTest();
                Minecraft.getInstance().getItemRenderer().render(displayWeapon, ItemDisplayContext.GUI, false, poseStack, guiGraphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Minecraft.getInstance().getItemRenderer().getModel(displayWeapon, null, null, 0));
                guiGraphics.flush();
                RenderSystem.disableDepthTest();

            } finally {
                poseStack.popPose();
            }
            renderWeaponInfo(guiGraphics, displayWeapon, weaponDisplayX, weaponDisplayY + weaponDisplayHeight - 15, weaponDisplayWidth);
        }

        if (weaponSlotData != null) {
            renderEnhancedSlots(guiGraphics, weaponDisplayX, weaponDisplayY, weaponDisplayWidth, weaponDisplayHeight, mouseX, mouseY);
        }
    }
    // === NEW WEAPON DISPLAY PANEL METHOD ===
// Add this new method:
    private void renderWeaponDisplayPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Main panel background
        guiGraphics.fill(x, y, x + width, y + height, 0x88000000);

        int borderColor = 0xFFC07020; // Bronze
        int innerBorder = 0x40C07020; // Transparent Bronze

        // Outer border
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y, borderColor);
        guiGraphics.fill(x - 2, y + height, x + width + 2, y + height + 2, borderColor);
        guiGraphics.fill(x - 2, y, x, y + height, borderColor);
        guiGraphics.fill(x + width, y, x + width + 2, y + height, borderColor);

        // Inner glow
        guiGraphics.fill(x, y, x + width, y + 4, innerBorder);
        guiGraphics.fill(x, y + height - 4, x + width, y + height, innerBorder);
        guiGraphics.fill(x, y, x + 4, y + height, innerBorder);
        guiGraphics.fill(x + width - 4, y, x + width, y + height, innerBorder);

        // Corner accents
        int accentSize = 20;
        guiGraphics.fill(x + 10, y + 10, x + 10 + accentSize, y + 12, borderColor);
        guiGraphics.fill(x + 10, y + 10, x + 12, y + 10 + accentSize, borderColor);

        guiGraphics.fill(x + width - 10 - accentSize, y + 10, x + width - 10, y + 12, borderColor);
        guiGraphics.fill(x + width - 12, y + 10, x + width - 10, y + 10 + accentSize, borderColor);
    }

    // === NEW WEAPON INFO METHOD ===
// Add this new method:
    private void renderWeaponInfo(GuiGraphics guiGraphics, ItemStack weapon, int x, int y, int width) {
        String weaponName = weapon.getDisplayName().getString();
        int textWidth = minecraft.font.width(weaponName);
        int centerX = x + (width - textWidth) / 2;

        // Background for text
        guiGraphics.fill(centerX - 5, y - 2, centerX + textWidth + 5, y + 12, 0x88000000);
        guiGraphics.drawString(minecraft.font, weaponName, centerX, y, 0xFFC07020, true); // Bronze text


    }
    private void renderEnhancedSlots(GuiGraphics guiGraphics, int displayX, int displayY, int displayWidth, int displayHeight, int mouseX, int mouseY) {
        // Remove all 2D slot rendering - slots will now be rendered directly on the 3D weapon model

        // Only render crosshair for ADD_SLOT mode
        renderEnhancedCrosshair(guiGraphics, displayX, displayY, displayWidth, displayHeight, mouseX, mouseY);

        // Only render click mark for selected positions
        renderEnhancedClickMark(guiGraphics, displayX, displayY, displayWidth, displayHeight);
    }
    // === NEW ENHANCED CROSSHAIR METHOD ===
// Add this new method:
    private void renderEnhancedCrosshair(GuiGraphics guiGraphics, int displayX, int displayY, int displayWidth, int displayHeight, int mouseX, int mouseY) {
        if (operationMode == OperationMode.ADD_SLOT && weaponSlotData != null && weaponSlotData.getSlotCount() < 3) {
            if (mouseX >= displayX && mouseX <= displayX + displayWidth &&
                    mouseY >= displayY && mouseY <= displayY + displayHeight) {

                int crosshairColor = getElementColor(selectedElementType);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 300);

                // Enhanced crosshair with glow
                guiGraphics.fill(mouseX - 12, mouseY - 2, mouseX + 12, mouseY + 2, crosshairColor | 0xFF000000);
                guiGraphics.fill(mouseX - 2, mouseY - 12, mouseX + 2, mouseY + 12, crosshairColor | 0xFF000000);

                // Outer glow
                guiGraphics.fill(mouseX - 14, mouseY - 1, mouseX + 14, mouseY + 1, (crosshairColor | 0x40000000));
                guiGraphics.fill(mouseX - 1, mouseY - 14, mouseX + 1, mouseY + 14, (crosshairColor | 0x40000000));

                guiGraphics.pose().popPose();
            }
        }
    }
    private void renderEnhancedClickMark(GuiGraphics guiGraphics, int displayX, int displayY, int displayWidth, int displayHeight) {
        if (clickMark != null) {
            // Use SAME transformation as WeaponSlotQuadBuilder positioning
            float cosY = (float)Math.cos(Math.toRadians(weaponRotationY));
            float sinY = (float)Math.sin(Math.toRadians(weaponRotationY));

            // Forward transform: model -> screen (same as quad builder)
            float screenX = (clickMark.modelX * cosY - clickMark.modelZ * sinY) * 25.0f;
            float screenY = clickMark.modelY * 25.0f;

            int markScreenX = displayX + displayWidth / 2 + (int)screenX;
            int markScreenY = displayY + displayHeight / 2 + (int)screenY;
            int markColor = getElementColor(selectedElementType);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500);

            int size = 8;
            guiGraphics.fill(markScreenX - size / 2, markScreenY - size / 2,
                    markScreenX + size / 2, markScreenY + size / 2, markColor);
            guiGraphics.renderOutline(markScreenX - size / 2 - 1, markScreenY - size / 2 - 1,
                    size + 2, size + 2, 0xFFFFFFFF);

            guiGraphics.pose().popPose();
        }
    }
    private void renderMaterialSlotsPanel(GuiGraphics guiGraphics) {
        // Fixed slot indices to match container order - slot 1 is SEPITH_MASS_SLOT in the menu
        final int[] materialSlotIndices = {1, 2, 3, 4, 5, 6, 7, 8}; // SM first, then E,W,F,Wi,T,S,M
        final String[] slotLabels = {"SM", "E", "W", "F", "Wi", "T", "S", "M"};

        // Find the bounding box of all material slots
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int index : materialSlotIndices) {
            Slot slot = this.menu.getSlot(index);
            minX = Math.min(minX, slot.x);
            maxX = Math.max(maxX, slot.x);
        }

        int panelY = this.topPos + 138;
        int panelHeight = 28;
        int panelX = this.leftPos + minX - 4;
        int panelWidth = (maxX - minX) + 16 + 8;

        int outerBorderColor = 0xFF8B4513;
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, outerBorderColor);

        for (int i = 0; i < materialSlotIndices.length; i++) {
            int slotIndex = materialSlotIndices[i];
            Slot slot = this.menu.getSlot(slotIndex);
            int slotCenterX = this.leftPos + slot.x + 8;
            int slotCenterY = this.topPos + slot.y + 8;

            guiGraphics.renderOutline(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18, 0xFF444444);

            String label = slotLabels[i];
            int labelX = slotCenterX - minecraft.font.width(label) / 2;
            guiGraphics.drawString(minecraft.font, label, labelX, slotCenterY + 10, 0xFF888888, false);
        }
    }

    // === METHOD REPLACEMENT: renderControlPanel() ===
    private void renderControlPanel(GuiGraphics guiGraphics) {
        // Right side control panel
        int basePanelX = this.leftPos + this.imageWidth - 175;
        int panelX = (int)(basePanelX + (1 - animationProgress) * (this.width - basePanelX));
        int panelY = this.topPos + 10;
        int panelWidth = 165;
        int panelHeight = this.imageHeight - 20;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);

        // Steampunk border styling
        int borderColor = 0xFFC07020; // Brownish/Bronze steampunk color
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, borderColor);
        guiGraphics.renderOutline(panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 0x40000000 | borderColor);

        String title = "ORBAL OPERATIONS";
        int titleWidth = minecraft.font.width(title);
        int titleX = panelX + (panelWidth - titleWidth) / 2;
        guiGraphics.drawString(minecraft.font, title, titleX, panelY + 10, borderColor, true);

        if (operationMode != null) {
            String modeText = "MODE: " + operationMode.name().replace("_", " ");
            guiGraphics.fill(panelX + 5, panelY + 25, panelX + panelWidth - 5, panelY + 40, 0x66000000 | borderColor);
            guiGraphics.drawString(minecraft.font, modeText, panelX + 10, panelY + 28, 0xFFFFFFFF, true);
        }

        // REMOVED the "ELEMENT: ..." text rendering block to create more space
    }
    // === CONTROL PANEL CIRCUITS METHOD ===
    private void renderControlPanelCircuits(GuiGraphics guiGraphics, int panelX, int panelY, int panelWidth, int panelHeight) {
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time * 0.005) * 0.3 + 0.7);
        int pulseAlpha = (int)(pulse * 128);
        int circuitColor = (0xFF00FF & 0x00FFFFFF) | (pulseAlpha << 24);

        // Vertical circuit lines
        int lineSpacing = 15;
        for (int i = 0; i < panelHeight / lineSpacing; i++) {
            int lineY = panelY + i * lineSpacing;
            if (i % 3 == 0) { // Every third line
                guiGraphics.fill(panelX + 5, lineY, panelX + 15, lineY + 1, circuitColor);
                guiGraphics.fill(panelX + panelWidth - 15, lineY, panelX + panelWidth - 5, lineY + 1, circuitColor);
            }
        }

        // Connection nodes
        int nodeSize = 2;
        for (int i = 0; i < 5; i++) {
            int nodeY = panelY + 60 + i * 40;
            // Left nodes
            guiGraphics.fill(panelX + 8 - nodeSize, nodeY - nodeSize,
                    panelX + 8 + nodeSize, nodeY + nodeSize, 0xFFFF00FF);
            // Right nodes
            guiGraphics.fill(panelX + panelWidth - 8 - nodeSize, nodeY - nodeSize,
                    panelX + panelWidth - 8 + nodeSize, nodeY + nodeSize, 0xFFFF00FF);
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
    // === MOUSE RELEASE HANDLING ===
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) { // Right click release
            isDragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    // === METHOD REPLACEMENT: isMouseOverWeaponShape() ===
    private boolean isMouseOverWeaponShape(int mouseX, int mouseY, int weaponDisplayX, int weaponDisplayY, int weaponDisplayWidth, int weaponDisplayHeight) {
        if (mouseX < weaponDisplayX || mouseX > weaponDisplayX + weaponDisplayWidth ||
                mouseY < weaponDisplayY || mouseY > weaponDisplayY + weaponDisplayHeight) {
            return false;
        }
        if (currentWeapon.isEmpty()) return false;
        // Generous circular hit detection for the larger 3D view
        int centerX = weaponDisplayX + weaponDisplayWidth / 2;
        int centerY = weaponDisplayY + weaponDisplayHeight / 2;
        double distance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        double maxDistance = Math.min(weaponDisplayWidth, weaponDisplayHeight) * 0.45; // 45% of smaller dimension
        return distance <= maxDistance;
    }



    private int getElementColor(String element) {
        return switch (element.toLowerCase()) {
            case "earth" -> 0x8B4513;      // Brown (Unchanged)
            case "water" -> 0xFF336699;      // Desaturated Blue
            case "fire" -> 0xFFCC5500;       // Burnt Orange
            case "wind" -> 0xFF808000;       // Olive Green
            case "time" -> 0xFFAAAAAA;      // Light Grey/Silver
            case "space" -> 0xFFDAA520;      // Goldenrod
            case "mirage" -> 0xFF888888;     // Grey (Unchanged)
            default -> 0xFF404040;          // Gray (Unchanged)
        };
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (weaponAnalysisMode && !currentWeapon.isEmpty()) {
            int weaponDisplayX = this.leftPos + 15;
            int weaponDisplayY = this.topPos + 15;
            int weaponDisplayWidth = 180;
            int weaponDisplayHeight = 120;

            if (mouseX >= weaponDisplayX && mouseX <= weaponDisplayX + weaponDisplayWidth &&
                    mouseY >= weaponDisplayY && mouseY <= weaponDisplayY + weaponDisplayHeight) {

                if (button == 0) { // Left click
                    if (isMouseOverWeaponShape((int)mouseX, (int)mouseY,
                            weaponDisplayX, weaponDisplayY, weaponDisplayWidth, weaponDisplayHeight)) {

                        if (operationMode == OperationMode.ADD_SLOT && weaponSlotData.getSlotCount() < 3) {
                            float relativeX = (float)(mouseX - (weaponDisplayX + weaponDisplayWidth/2));
                            float relativeY = (float)(mouseY - (weaponDisplayY + weaponDisplayHeight/2));

                            float cosY = (float)Math.cos(Math.toRadians(weaponRotationY));
                            float sinY = (float)Math.sin(Math.toRadians(weaponRotationY));

                            // Better depth calculation - keep slots on weapon surface
                            float modelX = (relativeX * cosY) / 25.0f;
                            float modelY = relativeY / 25.0f;
                            // Z-depth based on weapon rotation and click position
                            // Simple front/back determination
// Default to front unless specifically clicking back area or holding shift
                            float modelZ = 0.3f; // Default front

// Back area detection: consider weapon rotation
                            float normalizedRotation = ((weaponRotationY % 360) + 360) % 360; // Normalize to 0-360

                            if (normalizedRotation > 90 && normalizedRotation < 270) {
                                // Weapon is rotated to show back, so normal clicks create back slots
                                modelZ = -0.5f;
                            } else if (relativeX < -20) {
                                // Left side of screen when weapon facing forward = back
                                modelZ = -0.5f;
                            }

// Force back placement if holding SHIFT key
                            if (Screen.hasShiftDown()) {
                                modelZ = -0.5f;
                                System.out.println("SHIFT held - forcing back placement with Z: " + modelZ);
                            }

                            System.out.println("Created slot with Z: " + modelZ + " (rotation: " + normalizedRotation + ", relativeX: " + relativeX + ")");


                            selectedPosition = new SlotPosition(modelX, modelY, modelZ, (int)mouseX, (int)mouseY);
                            clickMark = new SlotPosition(modelX, modelY, modelZ, 0, 0);

                            updateButtonStates();
                            return true;
                        }

                        if (operationMode != OperationMode.ADD_SLOT && weaponSlotData != null) {
                            // Use 3D ray casting to detect slot clicks
                            int slotIndex = detect3DSlotClick(mouseX, mouseY, weaponDisplayX, weaponDisplayY, weaponDisplayWidth, weaponDisplayHeight);

                            if (slotIndex >= 0) {
                                WeaponSlotData.WeaponSlot slot = weaponSlotData.getSlot(slotIndex);
                                if (slot != null) {
                                    selectedPosition = new SlotPosition(slot.posX, slot.posY, slot.posZ, 0, 0);
                                    clickMark = new SlotPosition(slot.posX, slot.posY, slot.posZ, 0, 0);
                                    updateButtonStates();
                                    System.out.println("Selected 3D slot " + slotIndex);
                                    return true;
                                }
                            }
                        }
                    }
                } else if (button == 1) { // Right click - start rotation
                    isDragging = true;
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int detect3DSlotClick(double mouseX, double mouseY, int displayX, int displayY, int displayWidth, int displayHeight) {
        if (weaponSlotData == null) return -1;

        List<WeaponSlotData.WeaponSlot> slots = weaponSlotData.getSlots();

        for (int i = 0; i < slots.size(); i++) {
            WeaponSlotData.WeaponSlot slot = slots.get(i);
            if (slot.isClosed) continue;

            // Use SAME transformation as click mark rendering
            float cosY = (float)Math.cos(Math.toRadians(weaponRotationY));
            float sinY = (float)Math.sin(Math.toRadians(weaponRotationY));

            float screenX = (slot.posX * cosY - slot.posZ * sinY) * 25.0f;
            float screenY = slot.posY * 25.0f;

            float slotScreenX = displayX + displayWidth / 2 + screenX;
            float slotScreenY = displayY + displayHeight / 2 + screenY;

            float distance = (float)Math.sqrt(
                    Math.pow(mouseX - slotScreenX, 2) +
                            Math.pow(mouseY - slotScreenY, 2)
            );

            if (distance < 20.0f) {
                return i;
            }
        }
        return -1;
    }
    // === METHOD REPLACEMENT: mouseDragged() ===
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 1) { // Right click drag
            double rotationSpeed = 4.0; // Increased from 2.0 for more sensitivity
            targetRotationY += (float)((mouseX - lastMouseX) * rotationSpeed);
            targetRotationX += (float)((mouseY - lastMouseY) * rotationSpeed);

            targetRotationX = Math.max(-60, Math.min(60, targetRotationX));

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    // === METHOD REPLACEMENT: renderLabels() ===
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        // This method is now empty to hide the "Inventory" title text.
    }

    // === METHOD REPLACEMENT: render() ===
// This replaces your entire existing render method.
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Call the default render method. It handles:
        // 1. Calling renderBackground(renderBg) to draw our custom UI
        // 2. Drawing all the container slots (including player inventory) and the items in them
        // 3. Rendering all buttons and widgets
        // 4. Calling renderTooltip to draw tooltips
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // We only need to add our custom tooltips after the fact.
        renderElementTooltips(guiGraphics, mouseX, mouseY);
    }
    // === ELEMENT TOOLTIPS METHOD ===
    private void renderElementTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (earthButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Earth Element - Requires Earth Mass"), mouseX, mouseY);
        } else if (waterButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Water Element - Requires Water Mass"), mouseX, mouseY);
        } else if (fireButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Fire Element - Requires Fire Mass"), mouseX, mouseY);
        } else if (windButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Wind Element - Requires Wind Mass"), mouseX, mouseY);
        } else if (timeButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Time Element - Requires Time Mass"), mouseX, mouseY);
        } else if (spaceButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Space Element - Requires Space Mass"), mouseX, mouseY);
        } else if (mirageButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.literal("Mirage Element - Requires Mirage Mass"), mouseX, mouseY);
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