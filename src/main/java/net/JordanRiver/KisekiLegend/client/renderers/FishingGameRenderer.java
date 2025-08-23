package net.JordanRiver.KisekiLegend.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.fishing.FishRenderManager;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.fishing.FishingGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.BufferUploader;
import java.util.Optional;


@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FishingGameRenderer {
    // Exclamation mark textures (4 stages)
    private static final ResourceLocation EXCLAMATION_STAGE_1 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/exclamation_stage_1.png");
    private static final ResourceLocation EXCLAMATION_STAGE_2 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/exclamation_stage_2.png");
    private static final ResourceLocation EXCLAMATION_STAGE_3 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/exclamation_stage_3.png");
    private static final ResourceLocation EXCLAMATION_STAGE_4 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/exclamation_stage_4.png");
    private static final ResourceLocation TENSION_ICON_1 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/tension_icon_1.png");
    private static final ResourceLocation TENSION_ICON_2 =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/tension_icon_2.png");
    // HIT! effect texture
    private static final ResourceLocation HIT_EFFECT =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/fishing/hit_effect.png");



    @SubscribeEvent
    public static void onRenderGui(CustomizeGuiOverlayEvent event) {
        if (!FishingGameManager.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        FishingGameState gameState = FishingGameManager.getGameState();
        if (gameState == null) return;

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            switch (gameState.getPhase()) {
                case EXCLAMATION:
                    KisekiLegend.LOGGER.info("DEBUG: Rendering exclamation mark");
                    renderExclamationMark(graphics, screenWidth, screenHeight);
                    break;
                case FISHING_GAME:
                    KisekiLegend.LOGGER.info("DEBUG: Rendering fishing game - fish render enabled: " + FishRenderManager.shouldRenderFish());
                    renderFishingGame(graphics, screenWidth, screenHeight, gameState);
                    break;
                case HIT_EFFECT:
                    KisekiLegend.LOGGER.info("DEBUG: Rendering hit effect");
                    renderHitEffect(graphics, screenWidth, screenHeight);
                    break;
            }
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error in fishing renderer", e);
        } finally {
            RenderSystem.disableBlend();
        }
    }

    private static void renderExclamationMark(GuiGraphics graphics, int screenWidth, int screenHeight) {
        long progress = FishingGameManager.getExclamationProgress();

        // Determine which stage to show based on progress
        ResourceLocation texture;
        if (progress < 500) {
            texture = EXCLAMATION_STAGE_1;
        } else if (progress < 1000) {
            texture = EXCLAMATION_STAGE_2;
        } else if (progress < 1500) {
            texture = EXCLAMATION_STAGE_3;
        } else {
            texture = EXCLAMATION_STAGE_4;
        }

        int size = 64;
        int x = (screenWidth - size) / 2;
        int y = (screenHeight - size) / 2 - 50;

        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();

        // Show timing window (stages 2-3)
        if (progress >= 500 && progress <= 1500) {
            // Green border for timing window
            graphics.fill(x - 2, y - 2, x + size + 2, y, 0xFF00FF00);
            graphics.fill(x - 2, y + size, x + size + 2, y + size + 2, 0xFF00FF00);
            graphics.fill(x - 2, y, x, y + size, 0xFF00FF00);
            graphics.fill(x + size, y, x + size + 2, y + size, 0xFF00FF00);
        }
    }

    private static void renderHitEffect(GuiGraphics graphics, int screenWidth, int screenHeight) {
        long hitProgress = FishingGameManager.getHitEffectProgress();
        float progress = Math.min(1.0f, hitProgress / 1500.0f);

        // Use the actual HIT_EFFECT texture
        int size = (int)(128 * (1.0f + progress * 0.5f));
        int x = (screenWidth - size) / 2;
        int y = (screenHeight - size) / 2 - 30;

        float alpha = 1.0f - progress;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Render the actual HIT effect texture
        graphics.blit(HIT_EFFECT, x, y, 0, 0, size, size, size, size);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderFishingGame(GuiGraphics graphics, int screenWidth, int screenHeight, FishingGameState gameState) {
        renderStaminaBar(graphics, screenWidth, screenHeight, gameState);
        renderTensionMeter(graphics, screenWidth, screenHeight, gameState);


    }


    private static void renderStaminaBar(GuiGraphics graphics, int screenWidth, int screenHeight, FishingGameState gameState) {
        int barWidth = 200;
        int barHeight = 20;
        int x = screenWidth - barWidth - 20;
        int y = 20;

        // Background
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        if (gameState.getCurrentFishData() != null) {
            float staminaRatio = (float) gameState.getFishStamina() / gameState.getCurrentFishData().getStamina();
            int fillWidth = (int) (barWidth * staminaRatio);

            // Color changes based on stamina level
            int staminaColor = staminaRatio > 0.6f ? 0xFF00AA00 :
                    staminaRatio > 0.3f ? 0xFFAAAA00 : 0xFFAA0000;
            graphics.fill(x, y, x + fillWidth, y + barHeight, staminaColor);
        }

        graphics.drawString(Minecraft.getInstance().font, "Fish Stamina", x, y - 12, 0xFFFFFFFF);
    }

    private static void renderTensionMeter(GuiGraphics graphics, int screenWidth, int screenHeight, FishingGameState gameState) {
        int barWidth = 200;
        int barHeight = 12;
        int x = screenWidth - barWidth - 20;
        int y = 50;

        // Background
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        float tensionRatio = gameState.getTension() / gameState.getMaxTension();
        int fillWidth = (int) (barWidth * tensionRatio);

        // Color based on tension level
        int color = tensionRatio > 0.8f ? 0xFFAA0000 :
                tensionRatio > 0.5f ? 0xFFAAAA00 : 0xFF8B4513;

        graphics.fill(x, y, x + fillWidth, y + barHeight, color);

        // Only render tension icons when fish is in tension state - positioned at current tension level
        if (gameState.isFishInTensionState()) {
            int tensionIconX = x + Math.max(5, fillWidth - 16); // Move with tension level
            int iconY = y - 2;
            renderTensionIcons(graphics, tensionIconX, iconY);
        }

        // No text, no fish icon - just the tension meter and occasional tension icons
    }

    private static void renderTensionIcons(GuiGraphics graphics, int x, int y) {
        long time = System.currentTimeMillis();
        // Alternate between icons every 300ms for pulsing effect
        boolean useFirstIcon = (time / 300) % 2 == 0;

        ResourceLocation iconTexture = useFirstIcon ? TENSION_ICON_1 : TENSION_ICON_2;

        int iconSize = 16;
        RenderSystem.enableBlend();
        graphics.blit(iconTexture, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onRenderWorldLast(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!FishingGameManager.isActive()) return;

        FishingGameState gameState = FishingGameManager.getGameState();
        if (gameState == null || gameState.getPhase() != FishingGameManager.GamePhase.FISHING_GAME) return;

        render3DFishingElements(gameState);
    }

    private static void render3DFishingElements(FishingGameState gameState) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 waterPos = gameState.getWaterPosition();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(waterPos.x - cameraPos.x, waterPos.y - cameraPos.y, waterPos.z - cameraPos.z);

        render3DBoundingBox(poseStack, gameState);
        render3DCatchZone(poseStack, gameState);
    }

    private static void render3DBoundingBox(PoseStack poseStack, FishingGameState gameState) {
        int boundingSize = gameState.getBoundingBoxSize();
        float halfSize = boundingSize / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Blue outline color (ARGB format)
        int alpha = 255, red = 0, green = 128, blue = 255;

        // Draw rectangle outline - make sure it's centered on water position
        buffer.addVertex(matrix, -halfSize, 0.05f, -halfSize).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, halfSize, 0.05f, -halfSize).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, halfSize, 0.05f, halfSize).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, -halfSize, 0.05f, halfSize).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, -halfSize, 0.05f, -halfSize).setColor(red, green, blue, alpha);

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        // Debug log for boundary box
        KisekiLegend.LOGGER.info("BOUNDARY BOX - Size: " + boundingSize +
                ", Half: " + halfSize +
                ", Water center: " + String.format("%.2f, %.2f", gameState.getWaterPosition().x, gameState.getWaterPosition().z));
    }

    private static void render3DCatchZone(PoseStack poseStack, FishingGameState gameState) {
        Vec3 catchPos = gameState.getCatchZonePosition();
        Vec3 waterPos = gameState.getWaterPosition();

        float relX = (float)(catchPos.x - waterPos.x);
        float relZ = (float)(catchPos.z - waterPos.z);
        float radius = gameState.getCatchZoneRadius();

        poseStack.pushPose();
        poseStack.translate(relX, 0.06, relZ);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Color based on whether over fish
        boolean overFish = gameState.isCatchZoneOverFish();
        int red = overFish ? 0 : 255;
        int green = 255;
        int blue = overFish ? 255 : 0;
        int alpha = 200;

        // Draw circle outline
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            buffer.addVertex(matrix, x, 0, z).setColor(red, green, blue, alpha);
        }

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }
    private static void renderFishIconSprite(GuiGraphics graphics, int x, int y, FishingGameState gameState) {
        if (gameState.getCurrentFishData() == null) return;

        int rarityColor = switch (gameState.getCurrentFishData().getRarity()) {
            case COMMON -> 0xFFFFFFFF;
            case UNCOMMON -> 0xFF00AA00;
            case RARE -> 0xFF0080FF;
            case LEGENDARY -> 0xFFFFAA00;
        };

        // Draw a simple colored square as fish representation
        graphics.fill(x, y, x + 10, y + 8, rarityColor);
        graphics.fill(x - 1, y - 1, x + 11, y, 0xFF000000); // Black border
        graphics.fill(x - 1, y + 8, x + 11, y + 9, 0xFF000000);
        graphics.fill(x - 1, y, x, y + 8, 0xFF000000);
        graphics.fill(x + 10, y, x + 11, y + 8, 0xFF000000);
    }

    private static void renderCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        // Save current render state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }

        // Restore render state
        RenderSystem.disableBlend();
    }

    private static void renderCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int thickness = 2;
        for (int t = 0; t < thickness; t++) {
            int r = radius + t;
            for (int angle = 0; angle < 360; angle += 2) {
                double radians = Math.toRadians(angle);
                int x = centerX + (int)(r * Math.cos(radians));
                int y = centerY + (int)(r * Math.sin(radians));
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }


}