package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
    private static final ResourceLocation CENTER_CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/center_core.png");
    private static final ResourceLocation CLOCK_HAND_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/clock_hand.png");
    private static final ResourceLocation FAVORITE_COG_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/favorite_cog.png");
    private static final ResourceLocation SEARCH_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/search_icon.png");
    private static final ResourceLocation GEAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/inner_gear.png");
    private static final ResourceLocation SPELL_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/spell_panel.png");
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
    private float spiralScrollAngle = 0f;
    private float searchScrollOffset = 0f;
    private ArtsRegistry.ArtDefinition hoveredArt = null;
    private ArtsRegistry.ArtDefinition draggingArt = null;
    private long mouseDownTime = 0;
    private boolean isDragging = false;
    private String searchQuery = "";
    private boolean searchActive = false;
    private int favoriteGlowSlot = -1;
    // Add these fields to your class
    private int selectedElementIndex = -1; // Track currently selected element
    private int selectedArtIndex = -1;     // Track currently selected art
    private long favoriteGlowTime = 0;
    private ElementSector lastHoveredElement = null;
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
            new ElementSector(Element.FIRE, 1, "I", 0x90FF4500, "fire"),
            new ElementSector(Element.WATER, 3, "III", 0x9000BFFF, "water"),
            new ElementSector(Element.WIND, 4, "IV", 0x9032CD32, "wind"),
            new ElementSector(Element.EARTH, 6, "VI", 0x90DAA520, "earth"),
            new ElementSector(Element.TIME, 8, "VIII", 0x909966CC, "time"),
            new ElementSector(Element.SPACE, 9, "IX", 0xFFD9D522, "space"),
            new ElementSector(Element.MIRAGE, 11, "XI", 0x90B9F2FF, "mirage")
    );
    public ArtSelectionScreen() {
        super(Component.translatable("gui.kisekilegend.art_selection_screen"));
        this.player = Minecraft.getInstance().player;
        ItemStack orbmentStack = findOrbment(player);
        this.orbmentComponent = orbmentStack.isEmpty() ? new OrbmentComponent() : OrbmentItem.loadComponent(orbmentStack, this.player.level());
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
        return false; // Never pause the game
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
        // Force the game to continue running
        if (this.minecraft != null && this.minecraft.level != null) {
            this.minecraft.level.tickRateManager().setFrozen(false);
        }
        refreshOrbmentData();
    }
    @Override
    public void tick() {
        super.tick();
        // Keep the world ticking
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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        updateState(mouseX, mouseY);
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        float effectiveAlpha = overallAlpha * 0.95f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, effectiveAlpha);
        renderSteampunkGears(guiGraphics, poseStack);
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
    // Reset selections when transitioning states
    private void transitionToState(UIState newState) {
        this.currentState = newState;
        this.stateTransitionTime = System.currentTimeMillis();

        // Reset selections when changing states
        if (newState == UIState.MAIN_VIEW) {
            selectedArtIndex = -1;
        } else if (newState == UIState.SPELL_VIEW) {
            selectedElementIndex = -1;
            selectedArtIndex = 0; // Start at first art
            if (!displayedArts.isEmpty()) {
                hoveredArt = displayedArts.get(0);
            }
        }
    }    private float easeOutCubic(float x) { return 1 - (float) Math.pow(1 - x, 3); }
    private float easeInCubic(float x) { return x * x * x; }
    private void renderSteampunkGears(GuiGraphics guiGraphics, PoseStack poseStack) {
        long time = System.currentTimeMillis();
        int size = (int) (Math.min(width, height) * 0.7f);

        poseStack.pushPose();
        poseStack.translate(0, 0, -10);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(time / 120f));
        guiGraphics.blit(GEAR_TEXTURE, -size/2, -size/2, 0, 0, size, size, size, size);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(size * 0.35, size * 0.35, 0);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-time / 60f));
        guiGraphics.blit(GEAR_TEXTURE, -size/4, -size/4, 0, 0, size/2, size/2, size/2, size/2);
        poseStack.popPose();

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
    // Update your renderMainView method to show keyboard selection
    private void renderMainView(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        if (currentState != UIState.MAIN_VIEW && currentState != UIState.TRANSITION_TO_SPELLS && currentState != UIState.TRANSITION_TO_MAIN) return;

        float transitionProgress = getTransitionProgress(UIState.TRANSITION_TO_SPELLS, UIState.TRANSITION_TO_MAIN);
        float baseSize = Math.min(width, height) * 0.65f;
        float panelRadius = baseSize * 0.32f;
        float panelSize = baseSize * 0.12f;

        // Check mouse hover
        ElementSector mouseHovered = null;
        for (ElementSector sector : elementSectors) {
            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
            if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, panelSize)) {
                mouseHovered = sector;
            }
        }

        // Update hover state (mouse takes priority over keyboard)
        ElementSector currentlyHovered = mouseHovered;
        if (currentlyHovered == null && selectedElementIndex >= 0 && selectedElementIndex < elementSectors.size()) {
            currentlyHovered = elementSectors.get(selectedElementIndex);
        }

        // Play hover sound when changing selection
        if (!Objects.equals(currentlyHovered, lastHoveredElement)) {
            if (currentlyHovered != null) {
                playSound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 0.6f, 1.0f);
            }
            lastHoveredElement = currentlyHovered;
        }

        // Render elements with selection highlighting
        for (int i = 0; i < elementSectors.size(); i++) {
            ElementSector sector = elementSectors.get(i);
            poseStack.pushPose();

            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);

            // Determine if this element should be highlighted
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

            // Render hover effect
            if (popAmount > 0) {
                int color = sector.color;
                RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >> 24) & 0xFF) / 255f * 0.7f);
                guiGraphics.blit(ELEMENT_PANEL_TEXTURE, (int) (-panelSize / 2), (int) (-panelSize / 2), 0, 0, (int) panelSize, (int) panelSize, (int) panelSize, (int) panelSize);
            }

            // Render keyboard selection highlight (different from hover)
            if (isKeyboardSelected) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 0.0f, 0.3f); // Yellow highlight for keyboard selection
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
        float baseSize = Math.min(width, height);
        poseStack.pushPose();
        poseStack.translate(0, 0, 15);
        hoveredArt = null;
        if (displayedArts.isEmpty()) {
            poseStack.popPose();
            return;
        }
        float anglePerItem = 28f;
        float startRadius = baseSize * 0.52f;
        float endRadius = baseSize * 0.2f;
        float startAngle = -110f;
        float visibleAngleRange = 270f;
        for (int i = 0; i < displayedArts.size(); i++) {
            ArtsRegistry.ArtDefinition art = displayedArts.get(i);
            float itemProgress = displayedArts.size() > 1 ? (float) i / (displayedArts.size() - 1) : 0;
            float radius = Mth.lerp(itemProgress, startRadius, endRadius);
            float angle = startAngle + (i * anglePerItem) - spiralScrollAngle;
            float alpha = 1.0f;
            float fadeZone = 30f;
            if (angle < startAngle) alpha = Mth.clamp(1.0f - (startAngle - angle) / fadeZone, 0, 1);
            else if (angle > startAngle + visibleAngleRange) alpha = Mth.clamp(1.0f - (angle - (startAngle + visibleAngleRange)) / fadeZone, 0, 1);
            alpha *= transitionProgress;
            if (alpha < 0.01f) continue;
            Vector2f pos = getCircularPosition(0, 0, radius, angle);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, 2);
            boolean isLocked = lockedArts.contains(art);
            float textScale = 0.6f;
            int textWidth = font.width(art.name());
            float panelWidth = (textWidth * textScale) + 10;
            float panelHeight = 14;
            if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, panelWidth, panelHeight)) {
                hoveredArt = art;
            }
            float r = isLocked ? 0.4f : 0.8f;
            float g = isLocked ? 0.4f : 0.8f;
            float b = isLocked ? 0.4f : 0.9f;
            if(hoveredArt == art) { r += 0.2f; g += 0.2f; b += 0.2f; }
            RenderSystem.setShaderColor(r, g, b, alpha);
            guiGraphics.blit(SPELL_PANEL_TEXTURE, (int)(-panelWidth / 2), (int)(-panelHeight / 2), 0, 0, (int)panelWidth, (int)panelHeight, (int)panelWidth, (int)panelHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.pushPose();
            poseStack.scale(textScale, textScale, textScale);
            int textColor = isLocked ? 0xAAAAAA : 0xFFFFFF;
            int finalTextColor = ((int) (alpha * 255) << 24) | textColor;
            guiGraphics.drawCenteredString(font, art.name(), 0, -4, finalTextColor);
            poseStack.popPose();
            poseStack.popPose();
        }
        poseStack.popPose();
    }
    private void renderCenterCore(GuiGraphics guiGraphics, PoseStack poseStack) {
        float baseSize = Math.min(width, height) * 0.65f;
        float coreSize = baseSize * 0.2f;
        float pulse = 1.0f + (float)Math.sin(System.currentTimeMillis() / 400.0) * 0.02f;
        poseStack.pushPose();
        poseStack.translate(0, 0, 30);
        poseStack.scale(pulse, pulse, pulse);
        guiGraphics.blit(CENTER_CORE_TEXTURE, (int)(-coreSize/2), (int)(-coreSize/2), 0, 0, (int)coreSize, (int)coreSize, (int)coreSize, (int)coreSize);
        if (hoveredArt != null) {
            String desc = hoveredArt.effectDescription();
            int maxWidth = (int)(coreSize * 0.9f);
            int textHeight = font.wordWrapHeight(desc, maxWidth);
            int textY = -textHeight / 2;
            float textScale = 0.7f;
            float bgRadius = (maxWidth / textScale / 2f) + 4;
            drawCircularTextBg(guiGraphics, 0, 0, bgRadius, 0xC0000000);
            poseStack.pushPose();
            poseStack.scale(textScale, textScale, textScale);
            guiGraphics.drawWordWrap(font, Component.literal(desc), -maxWidth/2, textY, maxWidth, 0xFFFFFF);
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

        List<String> availableArtNames = this.availableArts.stream().map(ArtsRegistry.ArtDefinition::name).collect(Collectors.toList());

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
                // Check if art exists and if requirements are met
                boolean artExists = ArtsRegistry.ALL_ARTS.stream().anyMatch(art -> art.name().equals(favArtName));
                boolean isAvailable = false;

                if (artExists) {
                    ArtsRegistry.ArtDefinition favArt = ArtsRegistry.ALL_ARTS.stream()
                            .filter(art -> art.name().equals(favArtName))
                            .findFirst().orElse(null);

                    if (favArt != null) {
                        int[] sepithCounts = orbmentComponent.getSepithCounts();
                        isAvailable = favArt.elementCost().entrySet().stream()
                                .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());
                    }
                }

                // Render cog with appropriate coloring
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
        float baseSize = Math.min(width, height) * 0.65f;
        float searchRadius = baseSize * 0.48f;
        float iconSize = 24;

        Vector2f searchIconPos = getCircularPosition(0, 0, searchRadius, -90);
        guiGraphics.blit(SEARCH_ICON_TEXTURE, (int)(searchIconPos.x - iconSize/2), (int)(searchIconPos.y - iconSize/2), 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize);

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

        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        float discRadius = Math.min(width, height) * 0.3f;
        drawCircularTextBg(guiGraphics, centerX, centerY, discRadius, 0xCC000000);

        float x = (width - listWidth) / 2f;
        float y = centerY - (Math.min(maxVisible, searchResults.size()) * resultHeight) / 2f;

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
            poseStack.translate(mouseX, mouseY, 300);
            float panelSize = 48;
            guiGraphics.blit(SPELL_PANEL_TEXTURE, (int)(-panelSize/2), (int)(-panelSize/2), 0, 0, (int)panelSize, (int)panelSize, (int)panelSize, (int)panelSize);
            poseStack.scale(0.7f, 0.7f, 0.7f);
            guiGraphics.drawWordWrap(font, Component.literal(draggingArt.name()), -28, -12, 60, 0xFFFFFF);
            poseStack.popPose();
        }
    }
    private void setSelectedArt(ArtsRegistry.ArtDefinition art) {
        if (art == null || player == null) {
            return;
        }

        // Check if the art is available (player has required sepith)
        int[] sepithCounts = orbmentComponent.getSepithCounts();
        boolean isAvailable = art.elementCost().entrySet().stream()
                .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());

        if (!isAvailable) {
            // Play failure sound if art can't be cast
            playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
            return;
        }

        // Send packet to server to set the selected art
        NetworkHandler.sendToServer(new SetSelectedArtPacket(art.name()));

        // Play success sound
        playSound(ModSoundEvents.ART_SELECT.get(), 1.0f, 1.0f);

        // Optionally close the screen after selection
        this.onClose();
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isStateInvalid()) return super.mouseClicked(mouseX, mouseY, button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mouseDownTime = System.currentTimeMillis();

            float baseSize = Math.min(width, height) * 0.65f;
            float favRadius = baseSize * 0.2f;
            float cogSize = baseSize * 0.1f;

            for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
                float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
                Vector2f pos = getCircularPosition(0, 0, favRadius, angle);
                if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, cogSize)) {
                    String favArtName = orbmentComponent.getFavorite(i);
                    if (favArtName != null && !favArtName.isEmpty()) {
                        // Find the ArtDefinition for the favorite
                        ArtsRegistry.ArtDefinition favArt = ArtsRegistry.ALL_ARTS.stream()
                                .filter(art -> art.name().equals(favArtName))
                                .findFirst().orElse(null);

                        if (favArt != null) {
                            // Perform a direct, real-time availability check
                            int[] sepithCounts = this.orbmentComponent.getSepithCounts();
                            boolean isAvailable = favArt.elementCost().entrySet().stream()
                                    .allMatch(cost -> sepithCounts[OrbmentComponent.ELEMENT_INDEX.get(cost.getKey())] >= cost.getValue());

                            if (isAvailable) {
                                setSelectedArt(favArt);
                            } else {
                                playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
                            }
                        }
                        return true;
                    }
                }
            }
            if (!searchResults.isEmpty()) {
                int listWidth = 150; int resultHeight = 15; int maxVisible = 8;
                float x = (width - listWidth) / 2f; float y = height/2f - (Math.min(maxVisible, searchResults.size()) * resultHeight) / 2f;
                int start = (int)(searchScrollOffset / resultHeight);
                for (int i = start; i < Math.min(start + maxVisible, searchResults.size()); i++) {
                    float currentY = y + (i - start) * resultHeight;
                    if (isMouseOver(mouseX, mouseY, x + listWidth/2, currentY + resultHeight/2, listWidth, resultHeight)) {
                        draggingArt = searchResults.get(i);
                        return true;
                    }
                }
            }
            float searchRadius = baseSize * 0.48f;
            float iconSize = 24;
            Vector2f searchIconPos = getCircularPosition(width/2f, height/2f, searchRadius, -90);
            if (isMouseOver(mouseX, mouseY, searchIconPos.x, searchIconPos.y, iconSize)) {
                searchActive = !searchActive;
                if (!searchActive) searchResults.clear();
                return true;
            }
            if (currentState == UIState.MAIN_VIEW) {
                float panelRadius = baseSize * 0.32f;
                float panelSize = baseSize * 0.12f;
                for (ElementSector sector : elementSectors) {
                    float angle = 90 - (sector.position * 30);
                    Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
                    if (isMouseOver(mouseX, mouseY, width/2f + pos.x, height/2f + pos.y, panelSize)) {
                        selectElement(sector);
                        return true;
                    }
                }
            } else if (currentState == UIState.SPELL_VIEW && hoveredArt != null) {
                draggingArt = hoveredArt;
                return true;
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
            this.orbmentComponent = OrbmentItem.loadComponent(currentStack, player.level());
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isStateInvalid()) return super.mouseReleased(mouseX, mouseY, button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isDragging && draggingArt != null) {
                float baseSize = Math.min(width, height) * 0.65f;
                float favRadius = baseSize * 0.2f;
                float cogSize = baseSize * 0.1f;

                for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
                    float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
                    Vector2f pos = getCircularPosition(0, 0, favRadius, angle);

                    if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, cogSize)) {
                        // Send packet to SERVER for persistence
                        NetworkHandler.sendToServer(new SetFavoritePacket(i, draggingArt.name()));

                        // Save to the actual item stack for immediate local update
                        // ...
                        ItemStack orbmentStack = findOrbment(player);
// Add a null-check for player.level() to prevent the crash
                        if (!orbmentStack.isEmpty() && player.level() != null) {
                            OrbmentComponent component = OrbmentItem.loadComponent(orbmentStack, player.level());
                            component.setFavorite(i, draggingArt.name());
                            OrbmentItem.saveComponent(orbmentStack, component, player.level());
                            this.orbmentComponent = component; // Update local reference
                        }

                        // UI feedback
                        playSound(ModSoundEvents.UI_FAVORITE_SET.get());
                        favoriteGlowSlot = i;
                        favoriteGlowTime = System.currentTimeMillis();
                        break; // Exit loop after setting favorite
                    }
                }
            } else if (draggingArt != null && (System.currentTimeMillis() - mouseDownTime < 200)) {
                setSelectedArt(draggingArt);
            }

            isDragging = false;
            draggingArt = null;

            if (!searchQuery.isBlank() && searchActive) {
                performSearch();
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isStateInvalid()) return super.mouseDragged(mouseX, mouseY, button, dragX,dragY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingArt != null && !isDragging) {
            if (Math.abs(dragX) > 3 || Math.abs(dragY) > 3) {
                isDragging = true;
                searchResults.clear();
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
            float anglePerItem = 28f;
            float visibleAngleRange = 270f;
            float totalAngularSize = displayedArts.size() * anglePerItem;
            float maxScroll = Math.max(0, totalAngularSize - visibleAngleRange);

            spiralScrollAngle = Mth.clamp(spiralScrollAngle - (float)scrollY * 25f, 0, maxScroll);
            playSound(ModSoundEvents.UI_SPIRAL_TICK.get());
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
                performSearch(); // Performs search as you type for a better UX
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

        // Handle Escape key - this is essential for menu navigation
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

        // Handle menu keybind - but only when NOT searching
        if (!searchActive && ClientSetup.OPEN_RADIAL_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        // Handle search-specific keys ONLY when search is active
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
            // When searching is active, don't handle any other keys - let them pass through
            return false;
        }

        // Handle navigation keys (arrow keys, WASD) - these should NEVER be consumed by the menu
        if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_D ||
                keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            // Always let movement keys pass through to the game
            return false;
        }

        // Handle menu selection keys ONLY when not searching
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (currentState == UIState.MAIN_VIEW && selectedElementIndex >= 0 && selectedElementIndex < elementSectors.size()) {
                selectElement(elementSectors.get(selectedElementIndex));
                return true;
            }
            if (currentState == UIState.SPELL_VIEW && hoveredArt != null) {
                setSelectedArt(hoveredArt);
                return true;
            }
        }

        // Let ALL other keys (including movement) pass through to the game
        return false;
    }
    private void navigateRadialMenu(int deltaX, int deltaY) {
        if (currentState == UIState.MAIN_VIEW) {
            // Navigate elements in main view
            if (selectedElementIndex == -1) {
                selectedElementIndex = 0; // Start at first element
            } else {
                selectedElementIndex += deltaY; // Use deltaY for up/down navigation
                // Wrap around
                if (selectedElementIndex < 0) selectedElementIndex = elementSectors.size() - 1;
                if (selectedElementIndex >= elementSectors.size()) selectedElementIndex = 0;
            }

            // Play hover sound
            playSound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 0.6f, 1.0f);

        } else if (currentState == UIState.SPELL_VIEW && !displayedArts.isEmpty()) {
            // Navigate arts in spell view
            if (selectedArtIndex == -1) {
                selectedArtIndex = 0;
            } else {
                selectedArtIndex += deltaY; // Use deltaY for up/down navigation
                // Wrap around
                if (selectedArtIndex < 0) selectedArtIndex = displayedArts.size() - 1;
                if (selectedArtIndex >= displayedArts.size()) selectedArtIndex = 0;
            }

            hoveredArt = displayedArts.get(selectedArtIndex);

            // Adjust scroll to keep selected art visible
            adjustScrollForSelectedArt();
        }
    }
    private void adjustScrollForSelectedArt() {
        if (selectedArtIndex == -1 || displayedArts.isEmpty()) return;

        float anglePerItem = 28f;
        float visibleAngleRange = 270f;
        float startAngle = -110f;

        float selectedAngle = selectedArtIndex * anglePerItem;
        float viewStart = spiralScrollAngle;
        float viewEnd = spiralScrollAngle + visibleAngleRange;

        // If selected art is outside visible range, adjust scroll
        if (selectedAngle < viewStart) {
            spiralScrollAngle = Math.max(0, selectedAngle - 30f); // Add some padding
        } else if (selectedAngle > viewEnd) {
            float maxScroll = Math.max(0, displayedArts.size() * anglePerItem - visibleAngleRange);
            spiralScrollAngle = Math.min(maxScroll, selectedAngle - visibleAngleRange + 60f); // Add padding
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
        // Load fresh data from the item stack to ensure UI is up-to-date
        ItemStack liveStack = findOrbment(player);
        OrbmentComponent liveComponent = liveStack.isEmpty() ? new OrbmentComponent() : OrbmentItem.loadComponent(liveStack, player.level());
        liveComponent.recalculate();

        // Add this line to sync the local reference:
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
            if (canCast) availableArts.add(art);
            else lockedArts.add(art);
        }
        displayedArts.clear();
        displayedArts.addAll(availableArts);
        displayedArts.addAll(lockedArts);
        spiralScrollAngle = 0;
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

    private void drawCircularTextBg(GuiGraphics gfx, float cx, float cy, float radius, int color) {
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        for (int y = (int)-radius; y <= radius; y++) {
            for (int x = (int)-radius; x <= radius; x++) {
                if (x*x + y*y <= radius*radius) {
                    gfx.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
        ps.popPose();
    }
    private Vector2f getCircularPosition(float cx, float cy, float r, float angle) { float rad = (float) Math.toRadians(angle); return new Vector2f(cx + r * (float) Math.cos(rad), cy + r * (float) Math.sin(rad)); }
    private boolean isMouseOver(double mx, double my, float cx, float cy, float size) { return isMouseOver(mx, my, cx, cy, size, size); }
    private boolean isMouseOver(double mx, double my, float cx, float cy, float w, float h) { return mx >= cx - w/2 && mx <= cx + w/2 && my >= cy - h/2 && my <= cy + h/2; }
    private boolean isStateInvalid() {
        // A central check for all critical components.
        if (this.player == null || this.player.level() == null || this.orbmentComponent == null) {
            // Log an error to help with debugging, then close the screen safely.
            KisekiLegend.LOGGER.error("ArtSelectionScreen has an invalid state and will be closed to prevent a crash.");
            this.onClose();
            return true;
        }
        return false;
    }
    private String getRomanNumeral(int n) { return switch (n) { case 1->"I"; case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; case 6->"VI"; case 7->"VII"; case 8->"VIII"; case 9->"IX"; case 10->"X"; case 11->"XI"; case 12->"XII"; default->""; }; }
    private void playSound(net.minecraft.sounds.SoundEvent s) { playSound(s, 1.0f, 1.0f); }
    private void playSound(net.minecraft.sounds.SoundEvent s, float v, float p) { if (this.minecraft != null && this.minecraft.getSoundManager() != null) this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(s, p, v)); }
}