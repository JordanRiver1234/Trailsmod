package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.fishing.FishingGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FishingInputHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (FishingGameManager.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!FishingGameManager.isActive()) return;

        Minecraft mc = Minecraft.getInstance();

        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            FishingGameManager.endFishing();
            return;
        }

        if (event.getKey() >= 49 && event.getKey() <= 57) {
            event.setCanceled(true);
        }


    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !FishingGameManager.isActive()) return;
        if (FishingGameManager.getGameState() == null ||
                FishingGameManager.getGameState().getPhase() != FishingGameManager.GamePhase.FISHING_GAME) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float moveSpeed = 0.15f;
        Vec3 movement = Vec3.ZERO;

        // Get camera direction vectors
        Vec3 lookVec = mc.player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 forwardVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        // Apply movement based on key presses
        if (mc.options.keyUp.isDown()) {
            movement = movement.add(forwardVec.scale(moveSpeed));
        }
        if (mc.options.keyDown.isDown()) {
            movement = movement.add(forwardVec.scale(-moveSpeed));
        }
        if (mc.options.keyLeft.isDown()) {
            movement = movement.add(rightVec.scale(-moveSpeed));
        }
        if (mc.options.keyRight.isDown()) {
            movement = movement.add(rightVec.scale(moveSpeed));
        }

        if (movement.lengthSqr() > 0) {
            FishingGameManager.handleMovement((float)movement.x, (float)movement.z);
        }

        // REMOVE THIS CONFLICTING REELING LOGIC:
        // if (FishingGameManager.getGameState() != null &&
        //         FishingGameManager.getGameState().getPhase() == FishingGameManager.GamePhase.FISHING_GAME) {
        //     boolean isReeling = mc.options.keyUse.isDown();
        //     FishingGameManager.handleReeling(isReeling);
        // }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (!FishingGameManager.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (FishingGameManager.getGameState() != null) {
                    switch (FishingGameManager.getGameState().getPhase()) {
                        case EXCLAMATION:
                            FishingGameManager.handleExclamationClick();
                            break;
                        case FISHING_GAME:
                            // ADD THIS: Handle left click for reeling
                            FishingGameManager.handleReeling(true);
                            break;
                    }
                }
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                // ADD THIS: Stop reeling when mouse released
                if (FishingGameManager.getGameState() != null &&
                        FishingGameManager.getGameState().getPhase() == FishingGameManager.GamePhase.FISHING_GAME) {
                    FishingGameManager.handleReeling(false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (FishingGameManager.isActive()) {
            // Disable all player movement during fishing
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }
}