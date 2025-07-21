package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.KeyMapping;
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

    // region Constants and Resources
    private static final ResourceLocation CLOCK_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/clock_background.png");
    private static final ResourceLocation ELEMENT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/element_panel.png");
    private static final ResourceLocation CENTER_CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/center_core.png");
    private static final ResourceLocation CLOCK_HAND_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/clock_hand.png");
    private static final ResourceLocation FAVORITE_COG_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/favorite_cog.png");
    private static final ResourceLocation SEARCH_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/search_icon.png");
    private static final ResourceLocation GEAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/inner_gear.png");
    private static final ResourceLocation SPELL_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/spell_panel.png");

    private final Player player;
    private final OrbmentComponent orbmentComponent; // This now serves as a snapshot for rendering
    // endregion

    // region UI State Management
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
    private long favoriteGlowTime = 0;
    private ElementSector lastHoveredElement = null;
    // endregion

    // region Element and Color Data
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
    // endregion

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
        return false;
    }

    @Override
    public void onClose() {
        if (currentState != UIState.CLOSING) {
            this.stateTransitionTime = System.currentTimeMillis();
            this.currentState = UIState.CLOSING;
            playSound(ModSoundEvents.UI_CLOCK_CLOSE.get());
        }
    }
    @Override
    public void init() {
        super.init();
        if (minecraft != null && minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
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
        // Transparent
    }

    // region State and Animation Logic
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
    private void transitionToState(UIState newState) { this.currentState = newState; this.stateTransitionTime = System.currentTimeMillis(); }
    private float easeOutCubic(float x) { return 1 - (float) Math.pow(1 - x, 3); }
    private float easeInCubic(float x) { return x * x * x; }
    // endregion

    // region Rendering Components (No changes needed in this region)
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

    private void renderMainView(GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY) {
        if (currentState != UIState.MAIN_VIEW && currentState != UIState.TRANSITION_TO_SPELLS && currentState != UIState.TRANSITION_TO_MAIN) return;
        float transitionProgress = getTransitionProgress(UIState.TRANSITION_TO_SPELLS, UIState.TRANSITION_TO_MAIN);
        float baseSize = Math.min(width, height) * 0.65f;
        float panelRadius = baseSize * 0.32f;
        float panelSize = baseSize * 0.12f;

        ElementSector currentlyHovered = null;
        for (ElementSector sector : elementSectors) {
            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
            if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, panelSize)) {
                currentlyHovered = sector;
            }
        }

        if (!Objects.equals(currentlyHovered, lastHoveredElement)) {
            if (currentlyHovered != null) {
                playSound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 0.6f, 1.0f);
            }
            lastHoveredElement = currentlyHovered;
        }

        for (ElementSector sector : elementSectors) {
            poseStack.pushPose();
            float angle = 90 - (sector.position * 30);
            Vector2f pos = getCircularPosition(0, 0, panelRadius, outerRingRotation + angle);
            float popAmount = sector == currentlyHovered ? 5f : 0f;
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
                RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >> 24) & 0xFF) / 255f * 0.7f);
                guiGraphics.blit(ELEMENT_PANEL_TEXTURE, (int) (-panelSize / 2), (int) (-panelSize / 2), 0, 0, (int) panelSize, (int) panelSize, (int) panelSize, (int) panelSize);
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
            // ✅ FIX: Render favorites with a higher Z-level to ensure they are in front
            poseStack.translate(pos.x, pos.y, 50);
            guiGraphics.blit(FAVORITE_COG_TEXTURE, (int)(-cogSize/2), (int)(-cogSize/2), 0, 0, (int)cogSize, (int)cogSize, (int)cogSize, (int)cogSize);

            if (favoriteGlowSlot == i && System.currentTimeMillis() - favoriteGlowTime < 500) {
                float glowAlpha = 1.0f - (System.currentTimeMillis() - favoriteGlowTime) / 500f;
                int finalColor = Mth.ceil(glowAlpha * 255.0F) << 24 | 0xFFFF80;
                guiGraphics.fill((int)(-cogSize/2), (int)(-cogSize/2), (int)(cogSize/2), (int)(cogSize/2), finalColor);
            }

            String favArtName = orbmentComponent.getFavorite(i);
            if (favArtName != null && !favArtName.isEmpty()) {
                boolean isAvailable = availableArtNames.contains(favArtName);
                int textColor = isAvailable ? 0xFFFFFF : 0x808080;

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
    // endregion

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                        boolean isAvailable = availableArts.stream().anyMatch(art -> art.name().equals(favArtName));
                        if (isAvailable) {
                            ArtsRegistry.ALL_ARTS.stream()
                                    .filter(art -> art.name().equals(favArtName))
                                    .findFirst()
                                    .ifPresent(this::setSelectedArt);
                        } else {
                            playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isDragging && draggingArt != null) {
                float baseSize = Math.min(width, height) * 0.65f;
                float favRadius = baseSize * 0.2f;
                float cogSize = baseSize * 0.1f;
                for (int i = 0; i < OrbmentComponent.MAX_FAVORITES; i++) {
                    float angle = 90 - (i * (360f / OrbmentComponent.MAX_FAVORITES));
                    Vector2f pos = getCircularPosition(0, 0, favRadius, angle);
                    if (isMouseOver(mouseX, mouseY, width / 2f + pos.x, height / 2f + pos.y, cogSize)) {
                        // ✅ FIX: Use the load-modify-save pattern for robust data persistence
                        ItemStack liveOrbmentStack = findOrbment(player);
                        if (!liveOrbmentStack.isEmpty()) {
                            OrbmentComponent liveComponent = OrbmentItem.loadComponent(liveOrbmentStack, player.level());
                            liveComponent.setFavorite(i, draggingArt.name());
                            OrbmentItem.saveComponent(liveOrbmentStack, liveComponent, player.level());
                            // Update the local component to reflect the change immediately
                            this.orbmentComponent.setFavorite(i, draggingArt.name());
                        }
                        playSound(ModSoundEvents.UI_FAVORITE_SET.get());
                        favoriteGlowSlot = i;
                        favoriteGlowTime = System.currentTimeMillis();
                        break;
                    }
                }
            } else if (draggingArt != null && (System.currentTimeMillis() - mouseDownTime < 200)) {
                setSelectedArt(draggingArt);
            }
            isDragging = false;
            draggingArt = null;
            if (!searchQuery.isBlank() && searchActive) performSearch();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
        if (searchActive) {
            if (Character.isLetterOrDigit(codePoint) || codePoint == ' ') {
                searchQuery += codePoint;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchActive) {
            // Let the vanilla screen handle typing, backspace, etc.
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // ✅ FIX: This is the definitive movement fix.
        // It checks against the player's actual keybinds for movement.
        if (minecraft != null && (minecraft.options.keyUp.matches(keyCode, scanCode) ||
                minecraft.options.keyDown.matches(keyCode, scanCode) ||
                minecraft.options.keyLeft.matches(keyCode, scanCode) ||
                minecraft.options.keyRight.matches(keyCode, scanCode) ||
                minecraft.options.keyJump.matches(keyCode, scanCode) ||
                minecraft.options.keySprint.matches(keyCode, scanCode) ||
                minecraft.options.keyShift.matches(keyCode, scanCode))) {
            // This is a movement key, do not handle it. Let the game do it.
            return false;
        }

        // Handle menu-specific keys after checking movement keys
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || ClientSetup.OPEN_RADIAL_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    // region Helper and Logic Methods
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
        transitionToState(UIState.TRANSITION_TO_SPELLS);
        playSound(ModSoundEvents.UI_RINGS_ENGAGE.get());
    }

    private void setSelectedArt(ArtsRegistry.ArtDefinition art) {
        if (lockedArts.contains(art)) {
            playSound(ModSoundEvents.CAST_FAIL.get(), 0.8f, 1.2f);
            return;
        }

        // ✅ FIX: Use the load-modify-save pattern for robust data persistence
        ItemStack liveOrbmentStack = findOrbment(player);
        if (!liveOrbmentStack.isEmpty()) {
            OrbmentComponent liveComponent = OrbmentItem.loadComponent(liveOrbmentStack, player.level());
            liveComponent.setLastSelectedArtName(art.name());
            OrbmentItem.saveComponent(liveOrbmentStack, liveComponent, player.level());
        }

        playSound(ModSoundEvents.UI_ART_SELECT_CHIME.get());
        this.onClose();
    }

    private void loadArtsForElement(Element element) {
        // Load fresh data from the item stack to ensure UI is up-to-date
        ItemStack liveStack = findOrbment(player);
        OrbmentComponent liveComponent = liveStack.isEmpty() ? new OrbmentComponent() : OrbmentItem.loadComponent(liveStack, player.level());
        liveComponent.recalculate();

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
    private String getRomanNumeral(int n) { return switch (n) { case 1->"I"; case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; case 6->"VI"; case 7->"VII"; case 8->"VIII"; case 9->"IX"; case 10->"X"; case 11->"XI"; case 12->"XII"; default->""; }; }
    private void playSound(net.minecraft.sounds.SoundEvent s) { playSound(s, 1.0f, 1.0f); }
    private void playSound(net.minecraft.sounds.SoundEvent s, float v, float p) { if (this.minecraft != null && this.minecraft.getSoundManager() != null) this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(s, p, v)); }
    // endregion
}