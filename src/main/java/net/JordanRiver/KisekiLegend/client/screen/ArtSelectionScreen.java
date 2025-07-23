package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.network.SetSelectedArtPacket;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.KeyMapping;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.SetFavoritePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArtSelectionScreen extends Screen {

    private static final ResourceLocation CLOCK_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/clock_background.png");
    private static final ResourceLocation ELEMENT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/element_panel.png");
    private static final ResourceLocation ART_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/art_panel.png");
    private static final ResourceLocation CENTER_CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/center_core.png");
    private static final ResourceLocation CLOCK_HAND_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/clock_hand.png");
    private static final ResourceLocation FAVORITE_COG_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/favorite_cog.png");
    private static final ResourceLocation SEARCH_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/search_icon.png");
    private double dragStartX, dragStartY;
    private static final double DRAG_THRESHOLD = 5.0;

    private final Player player;
    private OrbmentComponent orbmentComponent;
    private enum UIState { OPENING, MAIN_VIEW, TRANSITION_TO_SPELLS, SPELL_VIEW, TRANSITION_TO_MAIN, CLOSING }
    private UIState currentState = UIState.OPENING;
    private long stateTransitionTime = 0;
    private float overallAlpha = 0f;
    private Element selectedElement = null;
    private float outerRingRotation = 0f;
    private float targetOuterRingRotation = 0f;
    private List<ArtsRegistry.ArtDefinition> availableArts = new ArrayList<>();
    private List<ArtsRegistry.ArtDefinition> lockedArts = new ArrayList<>();
    private List<ArtsRegistry.ArtDefinition> displayedArts = new ArrayList<>();
    private List<ArtsRegistry.ArtDefinition> searchResults = new ArrayList<>();
    private float searchScrollOffset = 0f;
    private ArtsRegistry.ArtDefinition hoveredArt = null;
    private ArtsRegistry.ArtDefinition draggingArt = null;
    private long mouseDownTime = 0;
    private boolean isDragging = false;
    private String searchQuery = "";
    private boolean searchActive = false;
    private boolean searchIconHovered = false;
    private int favoriteGlowSlot = -1;
    private int selectedElementIndex = -1;
    private int selectedArtIndex = -1;
    private long favoriteGlowTime = 0;
    private ElementSector lastHoveredElement = null;
    private boolean justDroppedFavorite = false;

    private float currentSpiralRotation = 0f;
    private float targetSpiralRotation = 0f;

    private static class ElementSector {
        final Element element;
        final int position;
        final String romanNumeral;
        final int color;
        final ResourceLocation icon;
        ElementSector(Element element, int position, String romanNumeral, int color, String iconName) {
            this.element = element;
            this.position = position;
            this.romanNumeral = romanNumeral;
            this.color = color;
            this.icon = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/elements/" + iconName + ".png");
        }
    }
    private final List<ElementSector> elementSectors = List.of(
            new ElementSector(Element.FIRE, 1, "I", 0xFFFF4500, "fire"),
            new ElementSector(Element.WATER, 3, "III", 0xFF00BFFF, "water"),
            new ElementSector(Element.WIND, 4, "IV", 0xFF32CD32, "wind"),
            new ElementSector(Element.EARTH, 6, "VI", 0xFFDAA520, "earth"),
            new ElementSector(Element.TIME, 8, "VIII", 0xFF996CC, "time"),
            new ElementSector(Element.SPACE, 9, "IX", 0xFFD9D522, "space"),
            new ElementSector(Element.MIRAGE, 11, "XI", 0xFFB9F2FF, "mirage")
    );
    public ArtSelectionScreen() {
        super(Component.translatable("gui.kisekilegend.art_selection_screen"));
        this.player = Minecraft.getInstance().player;
        ItemStack orbmentStack = findOrbment(player);
        this.orbmentComponent = orbmentStack.isEmpty() ? new OrbmentComponent() : OrbmentItem.loadComponentClientSide(orbmentStack, this.player.level());
        this.stateTransitionTime = System.currentTimeMillis();
    }

    private ItemStack findOrbment(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof OrbmentItem) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof OrbmentItem) return offHand;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) return stack;
        }
        return ItemStack.EMPTY;
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override
    public void onClose() {
        super.onClose();
        if (currentState != UIState.CLOSING) {
            this.stateTransitionTime = System.currentTimeMillis();
            this.currentState = UIState.CLOSING;
            playSound(ModSoundEvents.UI_CLOCK_CLOSE.get());
        }
    }
    @Override
    public void init() {
        super.init();
        if (this.minecraft != null && this.minecraft.level != null) {
            this.minecraft.level.tickRateManager().setFrozen(false);
        }
        refreshOrbmentData();
    }
    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.level != null) {
            this.minecraft.level.tickRateManager().setFrozen(false);
        }
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (isStateInvalid()) return;
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        updateState(mouseX, mouseY);
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        float effectiveAlpha = overallAlpha * 0.95f;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, effectiveAlpha);

        drawOuterRim(poseStack);
        renderClockBackground(guiGraphics, poseStack);
        renderSearchBar(guiGraphics, poseStack, mouseX, mouseY);
        renderMainView(guiGraphics, poseStack, mouseX, mouseY);

        renderSpellView(guiGraphics, poseStack, mouseX, mouseY);

        renderFavorites(guiGraphics, poseStack, mouseX, mouseY);
        renderCenterCore(guiGraphics, poseStack);
        renderClockHand(guiGraphics, poseStack, mouseX, mouseY);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        renderSearchResults(guiGraphics, mouseX, mouseY);
        renderDraggedArt(guiGraphics, mouseX, mouseY);
        RenderSystem.disableBlend();
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }
    private void updateState(int mouseX, int mouseY) {
        long timeSinceTransition = System.currentTimeMillis() - stateTransitionTime;
        float progress;

        this.currentSpiralRotation = Mth.lerp(0.2f, this.currentSpiralRotation, this.targetSpiralRotation);

        switch (currentState) {
            case OPENING:
                progress = Mth.clamp(timeSinceTransition / 500f, 0, 1);
                overallAlpha = easeOutCubic(progress);
                if (progress >= 1.0f) transitionToState(UIState.MAIN_VIEW);
                break;
            case MAIN_VIEW:
                outerRingRotation = Mth.lerp(0.1f, outerRingRotation, targetOuterRingRotation);
                break;
            case TRANSITION_TO_SPELLS:
                progress = Mth.clamp(timeSinceTransition / 500f, 0, 1);
                outerRingRotation = Mth.lerp(easeOutCubic(progress), outerRingRotation, targetOuterRingRotation);
                if (progress >= 1.0f) transitionToState(UIState.SPELL_VIEW);
                break;
            case TRANSITION_TO_MAIN:
                progress = Mth.clamp(timeSinceTransition / 500f, 0, 1);
                targetOuterRingRotation = 0f;
                outerRingRotation = Mth.lerp(easeOutCubic(progress), outerRingRotation, targetOuterRingRotation);
                if (progress >= 1.0f) {
                    selectedElement = null;
                    transitionToState(UIState.MAIN_VIEW);
                }
                break;
            case CLOSING:
                progress = Mth.clamp(timeSinceTransition / 400f, 0, 1);
                overallAlpha = 1.0f - easeInCubic(progress);
                if (progress >= 1.0f && this.minecraft != null) this.minecraft.setScreen(null);
                break;
            default: break;
        }
    }
    private void transitionToState(UIState newState) {
        this.currentState = newState;
        this.stateTransitionTime = System.currentTimeMillis();
        float angleStep = 32.0f;

        if (newState == UIState.MAIN_VIEW) {
            selectedArtIndex = -1;
        } else if (newState == UIState.SPELL_VIEW) {
            selectedElementIndex = -1;
            selectedArtIndex = displayedArts.isEmpty() ? -1 : 0;
            if (selectedArtIndex != -1) {
                hoveredArt = displayedArts.get(0);
                this.targetSpiralRotation = selectedArtIndex * angleStep;
                this.currentSpiralRotation = this.targetSpiralRotation;
            }
        }
    }
    private float easeOutCubic(float x) { return 1 - (float) Math.pow(1 - x, 3); }
    private float easeInCubic(float x) { return x * x * x; }

    private void drawOuterRim(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0, 0, -10); // Draw behind the clock
        float baseSize = Math.min(width, height);
        float outerRadius = baseSize * 0.36f;
        float innerRadius = baseSize * 0.325f;
        int color = 0x4A2D12; // Dark brown color
        float alpha = 0.5f;   // 50% transparency

        int segments = 36; // Reduced segments for a more pixelated look
        float angleIncrement = 360.0f / segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(i * angleIncrement);
            float angle2 = (float) Math.toRadians((i + 1) * angleIncrement);

            Vector2f v1 = new Vector2f((float) Math.cos(angle1) * innerRadius, (float) Math.sin(angle1) * innerRadius);
            Vector2f v2 = new Vector2f((float) Math.cos(angle1) * outerRadius, (float) Math.sin(angle1) * outerRadius);
            Vector2f v3 = new Vector2f((float) Math.cos(angle2) * outerRadius, (float) Math.sin(angle2) * outerRadius);
            Vector2f v4 = new Vector2f((float) Math.cos(angle2) * innerRadius, (float) Math.sin(angle2) * innerRadius);

            drawQuad(poseStack, v1, v2, v3, v4, color, alpha);
        }
        poseStack.popPose();
    }

    private void renderClockBackground(GuiGraphics guiGraphics, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(outerRingRotation));
        int size = (int) (Math.min(width, height) * 0.65f);
        guiGraphics.blit(CLOCK_BACKGROUND_TEXTURE, -size / 2, -size / 2, 0, 0, size, size, size, size);
        float numeralRadius = size * 0.42f;
        for (int i = 1; i <= 12; i++) {
            float angle = 90 - (i * 30);
            Vector2f pos = getCircularPosition(0, 0, numeralRadius, angle);
            String numeral = getRomanNumeral(i);
            guiGraphics.drawCenteredString(this.font, numeral, (int) pos.x, (int) pos.y - 4, 0xFFE0C0A0);
        }
        poseStack.popPose();
    }
    private void renderMainView(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        if (currentState != UIState.MAIN_VIEW && currentState != UIState.TRANSITION_TO_SPELLS && currentState != UIState.TRANSITION_TO_MAIN) return;

        float transitionProgress = getTransitionProgress(UIState.TRANSITION_TO_SPELLS, UIState.TRANSITION_TO_MAIN);
        float baseSize = Math.min(width, height) * 0.65f;
        float panelRadius = baseSize * 0.35f;
        float panelSize = baseSize * 0.14f;

        ElementSector mouseHovered = null;
        for (ElementSector sector : elementSectors) {
            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
            if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, panelSize * 1.2f)) {
                mouseHovered = sector;
            }
        }
        ElementSector currentlyHovered = mouseHovered;
        if (currentlyHovered == null && selectedElementIndex >= 0 && selectedElementIndex < elementSectors.size()) {
            currentlyHovered = elementSectors.get(selectedElementIndex);
        }
        if (!Objects.equals(currentlyHovered, lastHoveredElement)) {
            if (currentlyHovered != null) {
                playSound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 0.6f, 1.0f);
            }
            lastHoveredElement = currentlyHovered;
        }
        for (int i = 0; i < elementSectors.size(); i++) {
            ElementSector sector = elementSectors.get(i);
            poseStack.pushPose();
            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
            boolean isHovered = sector == currentlyHovered;
            boolean isKeyboardSelected = (selectedElementIndex == i && mouseHovered == null);
            float popAmount = isHovered ? 5f : 0f;
            float panelAlpha = 1.0f;
            if (selectedElement != null && selectedElement != sector.element) {
                float slideOutRadius = panelRadius + (baseSize * 0.2f * easeOutCubic(transitionProgress));
                pos = getCircularPosition(0, 0, slideOutRadius, outerRingRotation + angle);
                panelAlpha = Mth.lerp(easeOutCubic(transitionProgress), 1.0f, 0.4f);
            }
            poseStack.translate(pos.x, pos.y, 20 + popAmount);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, panelAlpha);
            guiGraphics.blit(ELEMENT_PANEL_TEXTURE, (int) (-panelSize / 2), (int) (-panelSize / 2), 0, 0, (int) panelSize, (int) panelSize, (int) panelSize, (int) panelSize);
            if (popAmount > 0) {
                int color = sector.color;
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                RenderSystem.setShaderColor(r, g, b, 0.7f);
                guiGraphics.blit(ELEMENT_PANEL_TEXTURE, (int) (-panelSize / 2), (int) (-panelSize / 2), 0, 0, (int) panelSize, (int) panelSize, (int) panelSize, (int) panelSize);
            }
            if (isKeyboardSelected) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 0.0f, 0.3f);
                guiGraphics.blit(ELEMENT_PANEL_TEXTURE, (int) (-panelSize / 2 - 2), (int) (-panelSize / 2 - 2), 0, 0, (int) panelSize + 4, (int) panelSize + 4, (int) panelSize + 4, (int) panelSize + 4);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, panelAlpha);
            float iconSize = panelSize * 0.7f;
            guiGraphics.blit(sector.icon, (int) (-iconSize / 2), (int) (-iconSize / 2), 0, 0, (int) iconSize, (int) iconSize, (int) iconSize, (int) iconSize);
            poseStack.popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderSpellView(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        if (currentState != UIState.SPELL_VIEW && currentState != UIState.TRANSITION_TO_SPELLS && currentState != UIState.TRANSITION_TO_MAIN) return;

        float transitionProgress = getTransitionProgress(UIState.TRANSITION_TO_SPELLS, UIState.TRANSITION_TO_MAIN);
        if (transitionProgress < 0.01f) return;

        poseStack.pushPose();
        poseStack.translate(0, 0, 15);

        int listSize = displayedArts.size();
        if (listSize == 0) {
            poseStack.popPose();
            return;
        }

        // --- Parameters for Spiral ---
        float baseSize = Math.min(width, height);
        float sweetSpotAngle = -110.0f;
        float angleStep = 32.0f;
        float visibleAngleRange = 540f;
        float fadeZoneDegrees = 120f;
        float radiusStart = baseSize * 0.25f;
        float radiusEnd = baseSize * 0.55f;
        float panelHeight = 36f;
        float textScale = 0.7f;
        float panelTiltAngle = 20f;

        // --- Endless Loop Calculation ---
        float startVisibleAngle = currentSpiralRotation - (sweetSpotAngle * -1) - fadeZoneDegrees;
        float endVisibleAngle = startVisibleAngle + visibleAngleRange + (fadeZoneDegrees * 2);
        int startIndex = Mth.floor(startVisibleAngle / angleStep);
        int endIndex = Mth.ceil(endVisibleAngle / angleStep);

        hoveredArt = null;
        Vector2f mouseVec = new Vector2f(mouseX - width / 2f, mouseY - height / 2f);

        for (int i = startIndex; i <= endIndex; i++) {
            int artIndex = Math.floorMod(i, listSize);
            ArtsRegistry.ArtDefinition art = displayedArts.get(artIndex);

            float itemNaturalAngle = i * angleStep;
            float displayAngle = sweetSpotAngle + itemNaturalAngle - this.currentSpiralRotation;

            // --- Alpha Calculation ---
            float alpha;
            float fadeStart = sweetSpotAngle - fadeZoneDegrees;
            float fadeEnd = sweetSpotAngle + visibleAngleRange + fadeZoneDegrees;

            if (displayAngle < sweetSpotAngle) {
                alpha = Mth.clamp((displayAngle - fadeStart) / fadeZoneDegrees, 0, 1);
            } else if (displayAngle > sweetSpotAngle + visibleAngleRange) {
                alpha = Mth.clamp((fadeEnd - displayAngle) / fadeZoneDegrees, 0, 1);
            } else {
                alpha = 1.0f;
            }
            alpha *= transitionProgress;

            if (alpha <= 0.001f) continue;

            // --- Radius Calculation (with dynamic oscillation) ---
            float radialProgress = Mth.clamp((displayAngle - sweetSpotAngle) / visibleAngleRange, 0, 1);
            float baseRadius = Mth.lerp(radialProgress, radiusStart, radiusEnd);

            // This creates the illusory in-and-out "breathing" movement seen in the reference GIF.
            float radiusAmplitude = baseSize * 0.015f; // Controls the magnitude of the pulse.
            float radiusFrequency = 2.0f;           // Controls how fast it pulses as you scroll.
            float oscillation = Mth.sin((float)Math.toRadians(this.currentSpiralRotation) * radiusFrequency);

            float radius = baseRadius + oscillation * radiusAmplitude;


            boolean isLocked = lockedArts.contains(art);
            boolean isHovered = false;

            // --- Vertex Calculation ---
            float halfAngleStep = (angleStep * 0.95f) / 2f;
            Vector2f v1 = getCircularPosition(0, 0, radius - panelHeight / 2, displayAngle - halfAngleStep - panelTiltAngle);
            Vector2f v2 = getCircularPosition(0, 0, radius + panelHeight / 2, displayAngle - halfAngleStep + panelTiltAngle);
            Vector2f v3 = getCircularPosition(0, 0, radius + panelHeight / 2, displayAngle + halfAngleStep + panelTiltAngle);
            Vector2f v4 = getCircularPosition(0, 0, radius - panelHeight / 2, displayAngle + halfAngleStep - panelTiltAngle);

            // --- Precise Hover Detection ---
            if (!isLocked && isPointInQuad(mouseVec, v1, v2, v3, v4)) {
                hoveredArt = art;
                isHovered = true;
            }

            boolean isSelected = (artIndex == selectedArtIndex && !isLocked);

            // --- Panel Glow ---
            int glowColor = 0;
            float glowAlpha = 0.0f;
            int elementalColor = selectedElement != null ? getElementColor(selectedElement) : 0xFFD700;

            if (isSelected) {
                glowColor = elementalColor;
                glowAlpha = 0.6f;
            }
            if (isHovered) {
                glowColor = elementalColor;
                glowAlpha = 1.0f;
            }

            poseStack.pushPose();
            poseStack.translate(0, 0, isHovered ? 5 : 0);

            // Draw Glow
            if (glowAlpha > 0) {
                float glowHeight = panelHeight + 4f;
                float glowAngleStep = halfAngleStep + 0.75f;
                Vector2f gv1 = getCircularPosition(0, 0, radius - glowHeight / 2, displayAngle - glowAngleStep - panelTiltAngle);
                Vector2f gv2 = getCircularPosition(0, 0, radius + glowHeight / 2, displayAngle - glowAngleStep + panelTiltAngle);
                Vector2f gv3 = getCircularPosition(0, 0, radius + glowHeight / 2, displayAngle + glowAngleStep + panelTiltAngle);
                Vector2f gv4 = getCircularPosition(0, 0, radius - glowHeight / 2, displayAngle + glowAngleStep - panelTiltAngle);
                drawQuad(poseStack, gv1, gv2, gv3, gv4, glowColor, alpha * glowAlpha * 0.4f);
            }

            // --- Draw Textured Panel ---
            float r = 1.0f; float g = 1.0f; float b = 1.0f;
            if (isLocked) {
                r = 0.5f; g = 0.5f; b = 0.5f;
            }
            drawTexturedQuad(poseStack, v1, v2, v3, v4, ART_PANEL_TEXTURE, r, g, b, alpha);
            poseStack.popPose();

            // --- Draw Text ---
            poseStack.pushPose();
            Vector2f textPos = getCircularPosition(0, 0, radius, displayAngle);
            poseStack.translate(textPos.x, textPos.y, isHovered ? 7 : 2);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(displayAngle + 90.0f));
            poseStack.scale(textScale, textScale, textScale);

            // Final text position adjustment
            poseStack.translate(18, 8, 0);

            String formattedName = art.name().replace(" ", "\n");
            Component textComponent = Component.literal(formattedName);
            int wrapWidth = 60;
            int textHeight = font.wordWrapHeight(formattedName, wrapWidth);
            int textColor = isLocked ? 0xAAAAAA : 0xFFFFFF;
            int finalTextColor = ((int)(alpha * 255) << 24) | textColor;
            guiGraphics.drawWordWrap(font, textComponent, -wrapWidth / 2, -textHeight / 2, wrapWidth, finalTextColor);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    // Method for drawing solid color quads using TRIANGLES
    private void drawQuad(PoseStack poseStack, Vector2f v1, Vector2f v2, Vector2f v3, Vector2f v4, int color, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        Matrix4f matrix = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.addVertex(matrix, v1.x, v1.y, 0.0F).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, v2.x, v2.y, 0.0F).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, v3.x, v3.y, 0.0F).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, v1.x, v1.y, 0.0F).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, v3.x, v3.y, 0.0F).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, v4.x, v4.y, 0.0F).setColor(red, green, blue, alpha);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.enableCull();
    }

    // Method for drawing textured quads using TRIANGLES
    private void drawTexturedQuad(PoseStack poseStack, Vector2f v1, Vector2f v2, Vector2f v3, Vector2f v4, ResourceLocation texture, float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.addVertex(matrix, v1.x, v1.y, 0.0F).setUv(0, 1);
        bufferBuilder.addVertex(matrix, v2.x, v2.y, 0.0F).setUv(0, 0);
        bufferBuilder.addVertex(matrix, v3.x, v3.y, 0.0F).setUv(1, 0);
        bufferBuilder.addVertex(matrix, v1.x, v1.y, 0.0F).setUv(0, 1);
        bufferBuilder.addVertex(matrix, v3.x, v3.y, 0.0F).setUv(1, 0);
        bufferBuilder.addVertex(matrix, v4.x, v4.y, 0.0F).setUv(1, 1);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    private void renderCenterCore(GuiGraphics guiGraphics, PoseStack poseStack) {
        float baseSize = Math.min(width, height) * 0.65f;
        float coreSize = baseSize * 0.2f;
        float pulse = 1.0f + (float)Math.sin(System.currentTimeMillis() / 400.0) * 0.02f;
        poseStack.pushPose();
        poseStack.translate(0, 0, 30);
        poseStack.scale(pulse, pulse, pulse);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        guiGraphics.blit(CENTER_CORE_TEXTURE, (int)(-coreSize/2), (int)(-coreSize/2), 0, 0, (int)coreSize, (int)coreSize, (int)coreSize, (int)coreSize);

        ArtsRegistry.ArtDefinition artToDisplay = null;
        if (currentState == UIState.SPELL_VIEW) {
            if (hoveredArt != null) {
                artToDisplay = hoveredArt;
            } else if (selectedArtIndex >= 0 && selectedArtIndex < displayedArts.size()) {
                artToDisplay = displayedArts.get(selectedArtIndex);
            }
        }

        if (artToDisplay != null) {
            String name = artToDisplay.name();
            float bgRadius = (coreSize / 2f) * 0.95f;
            drawCircularTextBg(guiGraphics, 0, 0, bgRadius, 0xC0000000);

            float textScale = Math.min(1.0f, (bgRadius * 1.6f) / font.width(name));
            poseStack.pushPose();
            poseStack.scale(textScale, textScale, textScale);
            guiGraphics.drawCenteredString(font, name, 0, -font.lineHeight / 2, 0xFFFFFF);
            poseStack.popPose();

        } else if (currentState == UIState.SPELL_VIEW) {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            guiGraphics.drawCenteredString(font, "[ESC or Right-Click to Back]", 0, 0, 0xAAAAAA);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
    private void renderFavorites(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        float baseSize = Math.min(width, height) * 0.65f;
        float favRadius = baseSize * 0.2f;
        float cogSize = baseSize * 0.1f;
        for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
            float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
            Vector2f pos = getCircularPosition(0, 0, favRadius, angle);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, 50);
            guiGraphics.blit(FAVORITE_COG_TEXTURE, (int)(-cogSize/2), (int)(-cogSize/2), 0, 0, (int)cogSize, (int)cogSize, (int)cogSize, (int)cogSize);
            if (favoriteGlowSlot == i && System.currentTimeMillis() - favoriteGlowTime < 500) {
                float glowAlpha = 1.0f - (System.currentTimeMillis() - favoriteGlowTime) / 500f;
                int finalColor = Mth.ceil(glowAlpha * 255.0F) << 24 | 0xFFFF80;
                guiGraphics.fill((int)(-cogSize/2), (int)(-cogSize/2), (int)(cogSize/2), (int)(cogSize/2), finalColor);
            }
            String favArtName = orbmentComponent.getFavorite(i);
            if (favArtName != null && !favArtName.isEmpty()) {
                ArtsRegistry.ArtDefinition favArt = ArtsRegistry.ALL_ARTS.stream()
                        .filter(art -> art.name().equals(favArtName))
                        .findFirst().orElse(null);

                boolean isAvailable = false;
                if (favArt != null) {
                    int[] sepithCounts = orbmentComponent.getSepithCounts();
                    isAvailable = favArt.elementCost().entrySet().stream()
                            .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());
                }

                if (!isAvailable) {
                    RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.7f);
                    guiGraphics.blit(FAVORITE_COG_TEXTURE, (int)(-cogSize/2), (int)(-cogSize/2), 0, 0, (int)cogSize, (int)cogSize, (int)cogSize, (int)cogSize);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }
                int textColor = isAvailable ? 0xFFFFFF : 0x606060;
                poseStack.pushPose();
                poseStack.scale(0.5f, 0.5f, 0.5f);
                guiGraphics.drawCenteredString(font, favArtName, 0, -4, textColor);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
    private void renderClockHand(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        float centerX = width / 2f; float centerY = height / 2f;
        float baseSize = Math.min(width, height) * 0.65f;
        float handLength = baseSize * 0.5f; float handWidth = handLength * 0.08f;
        double angleRad = Mth.atan2(mouseY - centerY, mouseX - centerX);
        float angleDeg = (float) Math.toDegrees(angleRad) + 90f;
        poseStack.pushPose();
        poseStack.translate(0, 0, 40);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angleDeg));
        guiGraphics.blit(CLOCK_HAND_TEXTURE, (int)(-handWidth/2), (int)(-handLength * 0.85f), 0, 0, (int)handWidth, (int)handLength, (int)handWidth, (int)handLength);
        poseStack.popPose();
    }
    private void renderSearchBar(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        float baseSize = Math.min(width, height);
        float searchRadius = baseSize * 0.35f;
        float iconSize = 24;

        Vector2f searchIconPos = getCircularPosition(0, 0, searchRadius, -90);
        searchIconHovered = isMouseOver(mouseX, mouseY, width / 2f + searchIconPos.x, height / 2f + searchIconPos.y, iconSize * 1.2f);

        float popAmount = searchIconHovered || searchActive ? 8f : 0f;
        float scale = searchIconHovered || searchActive ? 1.1f : 1.0f;

        poseStack.pushPose();
        poseStack.translate(searchIconPos.x, searchIconPos.y - popAmount, 5);
        poseStack.scale(scale, scale, scale);

        guiGraphics.blit(SEARCH_ICON_TEXTURE, (int)(-iconSize/2), (int)(-iconSize/2), 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize);
        poseStack.popPose();

        if (searchActive) {
            String textToRender = searchQuery + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            for (int i = 0; i < textToRender.length(); i++) {
                char c = textToRender.charAt(i);
                float charAngle = -90 + 15 + (i * 8);
                Vector2f charPos = getCircularPosition(0, 0, searchRadius, charAngle);
                guiGraphics.drawCenteredString(font, String.valueOf(c), (int)charPos.x, (int)charPos.y - 4, 0xFFFFFF);
            }
        }
    }
    private void renderSearchResults(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (searchResults.isEmpty()) return;
        int listWidth = 150;
        int resultHeight = 15;
        int maxVisible = 8;
        float x = (this.width - listWidth) / 2f;
        float y = this.height / 2.0f - (Math.min(maxVisible, searchResults.size()) * resultHeight) / 2f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);
        drawCircularTextBg(guiGraphics, width/2f, height/2f, Math.min(width, height) * 0.3f, 0xCC000000);
        int start = (int)(searchScrollOffset / resultHeight);
        for (int i = start; i < Math.min(start + maxVisible, searchResults.size()); i++) {
            ArtsRegistry.ArtDefinition art = searchResults.get(i);
            float currentY = y + (i - start) * resultHeight;
            guiGraphics.fill((int)x, (int)currentY, (int)(x + listWidth), (int)(currentY + resultHeight), 0x90222222);
            guiGraphics.drawString(font, art.name(), (int)x + 5, (int)currentY + 4, 0xFFFFFF);
            if (isMouseOver(mouseX, mouseY, x + listWidth/2, currentY + resultHeight/2, listWidth, resultHeight)) {
                guiGraphics.hLine((int)x, (int)(x + listWidth - 1), (int)(currentY + resultHeight - 1), 0xFFFFFFFF);
            }
        }
        guiGraphics.pose().popPose();
    }
    private void renderDraggedArt(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isDragging && draggingArt != null) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(mouseX, mouseY, 300); // Draw at mouse cursor
            float panelSize = 48;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            int panelColor = 0xEE202028;
            int borderColor = (selectedElement != null ? getElementColor(selectedElement) : 0xFFD700) | 0xFF000000;
            guiGraphics.fill(-(int)(panelSize/2), -(int)(panelSize/2), (int)(panelSize/2), (int)(panelSize/2), panelColor);
            guiGraphics.renderOutline(-(int)(panelSize/2) -1, -(int)(panelSize/2)-1, (int)panelSize+2, (int)panelSize+2, borderColor);
            guiGraphics.flush();

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            poseStack.scale(0.7f, 0.7f, 0.7f);
            guiGraphics.drawWordWrap(font, Component.literal(draggingArt.name()), -28, -12, 60, 0xFFFFFF);
            poseStack.popPose();
        }
    }
    private void setSelectedArt(ArtsRegistry.ArtDefinition art) {
        if (art == null || player == null || lockedArts.contains(art)) {
            playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
            return;
        }

        int[] sepithCounts = orbmentComponent.getSepithCounts();
        boolean canCast = art.elementCost().entrySet().stream()
                .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());

        if (!canCast) {
            playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
            return;
        }

        NetworkHandler.sendToServer(new SetSelectedArtPacket(art.name()));
        playSound(ModSoundEvents.ART_SELECT.get(), 1.0f, 1.0f);
        this.onClose(); // This closes the screen upon successful selection
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isStateInvalid()) return super.mouseClicked(mouseX, mouseY, button);

        if (justDroppedFavorite) {
            justDroppedFavorite = false;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mouseDownTime = System.currentTimeMillis();
            dragStartX = mouseX;
            dragStartY = mouseY;
            isDragging = false;
            draggingArt = null;

            // Search icon
            if (searchIconHovered) {
                searchActive = !searchActive;
                if (!searchActive) searchResults.clear();
                return true;
            }

            if (currentState == UIState.SPELL_VIEW && hoveredArt != null && !lockedArts.contains(hoveredArt)) {
                draggingArt = hoveredArt;
                return true;
            }

            // Favorite slot detection
            float baseSize = Math.min(width, height) * 0.65f;
            float favRadius = baseSize * 0.2f;
            float cogSize = baseSize * 0.12f;

            for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
                float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
                Vector2f pos = getCircularPosition(0, 0, favRadius, angle);
                if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, cogSize)) {
                    String favArtName = orbmentComponent.getFavorite(i);
                    if (favArtName != null && !favArtName.isEmpty()) {
                        ArtsRegistry.ArtDefinition favArt = ArtsRegistry.ALL_ARTS.stream()
                                .filter(art -> art.name().equals(favArtName))
                                .findFirst().orElse(null);
                        if (favArt != null) setSelectedArt(favArt);
                        return true;
                    }
                }
            }

            // Search results handling
            if (!searchResults.isEmpty()) {
                int listWidth = 150;
                int resultHeight = 15;
                int maxVisible = 8;
                float x = (width - listWidth) / 2f;
                float y = height/2f - (Math.min(maxVisible, searchResults.size()) * resultHeight) / 2f;
                int start = (int)(searchScrollOffset / resultHeight);

                for (int i = start; i < Math.min(start + maxVisible, searchResults.size()); i++) {
                    float currentY = y + (i - start) * resultHeight;
                    if (isMouseOver(mouseX, mouseY, x + listWidth/2, currentY + resultHeight/2, listWidth, resultHeight)) {
                        draggingArt = searchResults.get(i);
                        return true;
                    }
                }
            }

            // Element selection in main view
            if (currentState == UIState.MAIN_VIEW) {
                float panelRadius = baseSize * 0.35f;
                float panelSize = baseSize * 0.14f;
                for (ElementSector sector : elementSectors) {
                    float angle = 90 - (sector.position * 30);
                    Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
                    if (isMouseOver(mouseX, mouseY, width/2f + pos.x, height/2f + pos.y, panelSize)) {
                        selectElement(sector);
                        return true;
                    }
                }
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (currentState == UIState.SPELL_VIEW) {
                transitionToState(UIState.TRANSITION_TO_MAIN);
                playSound(ModSoundEvents.UI_RINGS_ENGAGE.get());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void refreshOrbmentData() {
        if (isStateInvalid()) return;
        ItemStack currentStack = findOrbment(player);
        if (!currentStack.isEmpty()) {
            this.orbmentComponent = OrbmentItem.loadComponentClientSide(currentStack, player.level());
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isStateInvalid()) return super.mouseReleased(mouseX, mouseY, button);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingArt != null) {
            double dragDistance = Math.sqrt(Math.pow(mouseX - dragStartX, 2) + Math.pow(mouseY - dragStartY, 2));

            if (isDragging) {
                // Handle drag-and-drop to favorites
                float baseSize = Math.min(width, height) * 0.65f;
                float favRadius = baseSize * 0.2f;
                float cogSize = baseSize * 0.15f; // Increased drop zone

                boolean droppedOnFavorite = false;
                for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
                    float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
                    Vector2f pos = getCircularPosition(0, 0, favRadius, angle);
                    if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, cogSize)) {
                        NetworkHandler.sendToServer(new SetFavoritePacket(i, draggingArt.name()));
                        orbmentComponent.setFavorite(i, draggingArt.name());
                        playSound(ModSoundEvents.UI_FAVORITE_SET.get());
                        favoriteGlowSlot = i;
                        favoriteGlowTime = System.currentTimeMillis();
                        droppedOnFavorite = true;
                        this.justDroppedFavorite = true; // Set flag to prevent immediate click
                        break;
                    }
                }

                if (!droppedOnFavorite) {
                    playSound(ModSoundEvents.CAST_FAIL.get(), 0.5f, 0.8f);
                }
            } else if (dragDistance < DRAG_THRESHOLD) {
                // Handle click-to-select
                if (!lockedArts.contains(draggingArt)) {
                    int[] sepithCounts = orbmentComponent.getSepithCounts();
                    boolean canCast = draggingArt.elementCost().entrySet().stream()
                            .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());

                    if (canCast) {
                        setSelectedArt(draggingArt);
                    } else {
                        playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
                    }
                }
            }
        }

        isDragging = false;
        draggingArt = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isStateInvalid()) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingArt != null && !isDragging) {
            double totalDragDistance = Math.sqrt(Math.pow(mouseX - dragStartX, 2) + Math.pow(mouseY - dragStartY, 2));
            if (totalDragDistance > DRAG_THRESHOLD) {
                isDragging = true;
                if (searchResults.contains(draggingArt)) {
                    searchResults.clear();
                    searchActive = false;
                    searchQuery = "";
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isStateInvalid()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!searchResults.isEmpty()) {
            int listWidth = 150; int maxVisible = 8;
            float x = (width - listWidth) / 2f; float y = height/2f - (Math.min(maxVisible, searchResults.size()) * 15) / 2f;
            if (isMouseOver(mouseX, mouseY, x + listWidth/2, y + (Math.min(maxVisible, searchResults.size())*15)/2f, listWidth, Math.min(maxVisible, searchResults.size())*15)) {
                searchScrollOffset = Mth.clamp(searchScrollOffset - (float)scrollY * 15, 0, Math.max(0, (searchResults.size() - maxVisible) * 15));
                return true;
            }
        }
        if (currentState == UIState.SPELL_VIEW) {
            int direction = (int) -Math.signum(scrollY);
            if (direction != 0) {
                navigateRadialMenu(0, direction);
                playSound(ModSoundEvents.UI_SPIRAL_TICK.get());
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isStateInvalid()) return super.charTyped(codePoint, modifiers);
        if (searchActive) {
            if (Character.isLetterOrDigit(codePoint) || codePoint == ' ') {
                searchQuery += codePoint;
                performSearch();
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isStateInvalid()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searchActive) {
                searchActive = false;
                searchQuery = "";
                searchResults.clear();
                return true;
            }
            if (currentState == UIState.SPELL_VIEW) {
                transitionToState(UIState.TRANSITION_TO_MAIN);
                playSound(ModSoundEvents.UI_RINGS_ENGAGE.get());
                selectedArtIndex = -1;
                return true;
            }
            this.onClose();
            return true;
        }
        if (!searchActive && ClientSetup.OPEN_RADIAL_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                performSearch();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                performSearch();
                return true;
            }
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            if (currentState == UIState.SPELL_VIEW) {
                navigateRadialMenu(0, (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_UP) ? -1 : 1);
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (currentState == UIState.MAIN_VIEW && selectedElementIndex >= 0 && selectedElementIndex < elementSectors.size()) {
                selectElement(elementSectors.get(selectedElementIndex));
                return true;
            }
            if (currentState == UIState.SPELL_VIEW && selectedArtIndex != -1) {
                setSelectedArt(displayedArts.get(selectedArtIndex));
                return true;
            }
        }
        return false;
    }
    private void navigateRadialMenu(int deltaX, int deltaY) {
        if (currentState == UIState.MAIN_VIEW) {
            if (selectedElementIndex == -1) {
                selectedElementIndex = 0;
            } else {
                selectedElementIndex = (selectedElementIndex + deltaY + elementSectors.size()) % elementSectors.size();
            }
            playSound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 0.6f, 1.0f);
        } else if (currentState == UIState.SPELL_VIEW && !displayedArts.isEmpty()) {
            float angleStep = 32.0f;
            this.targetSpiralRotation += deltaY * angleStep;

            int newIndex = (int) Math.round(this.targetSpiralRotation / angleStep);
            selectedArtIndex = Math.floorMod(newIndex, displayedArts.size());
            if (selectedArtIndex >= 0 && selectedArtIndex < displayedArts.size()) {
                hoveredArt = displayedArts.get(selectedArtIndex);
            }
        }
    }
    private void performSearch() {
        if (searchQuery.isBlank()) {
            searchResults.clear();
            return;
        }
        String lowerQuery = searchQuery.toLowerCase();
        searchResults = ArtsRegistry.ALL_ARTS.stream()
                .filter(art -> art.name().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing(ArtsRegistry.ArtDefinition::name))
                .collect(Collectors.toList());
        searchScrollOffset = 0;
    }

    private void selectElement(ElementSector sector) {
        this.selectedElement = sector.element;
        this.targetOuterRingRotation = -(sector.position - 12) * 30;
        loadArtsForElement(sector.element);
        if (isStateInvalid()) return;
        transitionToState(UIState.TRANSITION_TO_SPELLS);
        playSound(ModSoundEvents.UI_RINGS_ENGAGE.get());
    }

    private void loadArtsForElement(Element element) {
        ItemStack liveStack = findOrbment(player);
        OrbmentComponent liveComponent = liveStack.isEmpty() ? new OrbmentComponent() : OrbmentItem.loadComponentClientSide(liveStack, player.level());
        liveComponent.recalculate();
        this.orbmentComponent = liveComponent;
        int[] sepithCounts = liveComponent.getSepithCounts();

        List<ArtsRegistry.ArtDefinition> allElementArts = ArtsRegistry.ALL_ARTS.stream()
                .filter(art -> art.mainElement() == element)
                .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.epCost().split(" ")[0])))
                .collect(Collectors.toList());
        availableArts.clear();
        lockedArts.clear();
        for (ArtsRegistry.ArtDefinition art : allElementArts) {
            boolean canCast = art.elementCost().entrySet().stream()
                    .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());
            if (canCast) {
                availableArts.add(art);
            } else {
                lockedArts.add(art);
            }
        }
        displayedArts.clear();
        displayedArts.addAll(availableArts);
        displayedArts.addAll(lockedArts);
    }

    private float getTransitionProgress(UIState inState, UIState outState) {
        long timeSinceTransition = System.currentTimeMillis() - stateTransitionTime;
        if (currentState == inState) {
            return Mth.clamp(timeSinceTransition / 500f, 0, 1);
        } else if (currentState == outState) {
            return 1.0f - Mth.clamp(timeSinceTransition / 500f, 0, 1);
        } else if (currentState == UIState.SPELL_VIEW && inState == UIState.TRANSITION_TO_SPELLS) {
            return 1.0f;
        }
        return 0f;
    }
    private int getElementColor(Element element) {
        return elementSectors.stream()
                .filter(s -> s.element == element)
                .findFirst()
                .map(s -> s.color)
                .orElse(0xFFFFFF);
    }
    private void drawCircularTextBg(GuiGraphics gfx, float cx, float cy, float radius, int color) {
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int y = (int)-radius; y <= radius; y++) {
            for (int x = (int)-radius; x <= radius; x++) {
                if (x*x + y*y <= radius*radius) {
                    gfx.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
        gfx.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ps.popPose();
    }
    private Vector2f getCircularPosition(float cx, float cy, float r, float angle) { float rad = (float) Math.toRadians(angle); return new Vector2f(cx + r * (float) Math.cos(rad), cy + r * (float) Math.sin(rad)); }
    private boolean isMouseOver(double mx, double my, float cx, float cy, float size) { return isMouseOver(mx, my, cx, cy, size, size); }
    private boolean isMouseOver(double mx, double my, float cx, float cy, float w, float h) { return mx >= cx - w/2 && mx <= cx + w/2 && my >= cy - h/2 && my <= cy + h/2; }

    // Helper method for precise point-in-quadrilateral check
    private boolean isPointInQuad(Vector2f point, Vector2f v1, Vector2f v2, Vector2f v3, Vector2f v4) {
        boolean d1 = isLeftOfLine(point, v1, v2);
        boolean d2 = isLeftOfLine(point, v2, v3);
        boolean d3 = isLeftOfLine(point, v3, v4);
        boolean d4 = isLeftOfLine(point, v4, v1);
        return (d1 == d2) && (d2 == d3) && (d3 == d4);
    }

    // Helper for isPointInQuad
    private boolean isLeftOfLine(Vector2f point, Vector2f lineStart, Vector2f lineEnd) {
        return ((lineEnd.x - lineStart.x) * (point.y - lineStart.y) - (lineEnd.y - lineStart.y) * (point.x - lineStart.x)) > 0;
    }

    private boolean isStateInvalid() {
        if (this.player == null || this.player.level() == null || this.orbmentComponent == null) {
            KisekiLegend.LOGGER.error("ArtSelectionScreen has an invalid state and will be closed to prevent a crash.");
            if (Minecraft.getInstance().screen == this) {
                this.onClose();
            }
            return true;
        }
        return false;
    }
    private String getRomanNumeral(int n) { return switch (n) { case 1->"I"; case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; case 6->"VI"; case 7->"VII"; case 8->"VIII"; case 9->"IX"; case 10->"X"; case 11->"XI"; case 12->"XII"; default->""; }; }
    private void playSound(net.minecraft.sounds.SoundEvent s) { playSound(s, 1.0f, 1.0f); }
    private void playSound(net.minecraft.sounds.SoundEvent s, float v, float p) { if (this.minecraft != null && this.minecraft.getSoundManager() != null) this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(s, p, v)); }
}