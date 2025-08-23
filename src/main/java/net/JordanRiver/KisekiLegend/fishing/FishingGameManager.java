package net.JordanRiver.KisekiLegend.fishing;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.KisekiFishingHook;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem;
import net.JordanRiver.KisekiLegend.network.FishingGamePacket;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class FishingGameManager {
    private static boolean isActive = false;
    private static FishingGameState gameState;
    private static long exclamationStartTime;
    private static boolean exclamationShown = false;
    private static long hitEffectStartTime;
    private static boolean hitEffectShown = false;

    // Game phases
    public enum GamePhase {
        CASTING,           // Waiting for exclamation mark
        EXCLAMATION,       // Showing exclamation mark with timing window
        HIT_EFFECT,        // Showing HIT! effect after successful timing
        FISHING_GAME,      // Active fishing with fish
        COMPLETED,         // Game finished
        FAILED             // Game failed/cancelled
    }


    public static void startFishingGame(Player player, RodType rodType, Vec3 waterPosition, String bait, int dynamicBoundarySize) {
        if (isActive) return;

        KisekiLegend.LOGGER.info("=== FISHING GAME MANAGER - RECEIVED BOUNDARY SIZE: " + dynamicBoundarySize + " ===");

        isActive = true;
        gameState = new FishingGameState(player, rodType, waterPosition, bait, dynamicBoundarySize);
        gameState.setPhase(GamePhase.CASTING);

        // Verify the boundary was set correctly
        KisekiLegend.LOGGER.info("=== GAME STATE BOUNDARY AFTER CREATION: " + gameState.getBoundingBoxSize() + " ===");

        player.level().playSound(player, player.blockPosition(),
                ModSoundEvents.FISHING_REEL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        float timeBonus = rodType.getTimeBonus();
        long baseDelay = 1000 + (long)(Math.random() * 2000);
        long adjustedDelay = (long)(baseDelay * (1.0f - timeBonus));

        scheduleExclamationMark(adjustedDelay);
    }

// Remove or comment out the old method to force using the new one
// public static void startFishingGame(Player player, RodType rodType, Vec3 waterPosition, String bait) {
//     startFishingGame(player, rodType, waterPosition, bait, 6);
// }

    private static void scheduleExclamationMark(long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                if (isActive && gameState.getPhase() == GamePhase.CASTING) {
                    showExclamationMark();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void showExclamationMark() {
        KisekiLegend.LOGGER.info("DEBUG: Showing exclamation mark");

        if (!isActive) {
            KisekiLegend.LOGGER.info("DEBUG: Not active, returning");
            return;
        }

        exclamationStartTime = System.currentTimeMillis();
        exclamationShown = true;
        gameState.setPhase(GamePhase.EXCLAMATION);

        KisekiLegend.LOGGER.info("DEBUG: Set phase to EXCLAMATION");

        // Play exclamation sound
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.level().playSound(mc.player, mc.player.blockPosition(),
                    ModSoundEvents.FISHING_EXCLAMATION.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    public static boolean handleExclamationClick() {
        if (!isActive || !exclamationShown || gameState.getPhase() != GamePhase.EXCLAMATION) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceExclamation = currentTime - exclamationStartTime;

        // Perfect timing window: stages 2-3 (500-1500ms)
        // Good timing window: extended to include early stage 4 (500-1800ms)
        if (timeSinceExclamation >= 500 && timeSinceExclamation <= 1800) {
            showHitEffect();
            return true;
        } else {
            failFishing("Missed the timing!");
            return false;
        }
    }
    public static void initializeFishOnServer(ServerPlayer player, Vec3 waterPosition, String bait, String rodTypeName) {
        if (!isActive || gameState == null) return;

        try {
            RodType rodType = RodType.valueOf(rodTypeName);

            // Set fish data on server side
            FishData fishData = FishRegistry.getRandomFish(bait, rodType);
            if (fishData == null) {
                KisekiLegend.LOGGER.error("No fish data found for bait: " + bait);
                return;
            }

            gameState.setCurrentFishData(fishData);
            gameState.setFishStamina(fishData.getStamina());

            // Calculate fish position
            int boundingBoxSize = fishData.getRarity().getBoundingBoxSize();
            double offsetX = (Math.random() - 0.5) * boundingBoxSize;
            double offsetZ = (Math.random() - 0.5) * boundingBoxSize;
            Vec3 fishPosition = waterPosition.add(offsetX, 0.1, offsetZ);

            gameState.setFishPosition(fishPosition);

            // Create server entities with proper hook tracking
            spawnServerEntities(player, fishData, fishPosition, waterPosition);

        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error in server-side fish initialization", e);
        }
    }
    private static void spawnServerEntities(ServerPlayer player, FishData fishData, Vec3 fishPosition, Vec3 waterPosition) {
        ServerLevel serverLevel = player.serverLevel();

        try {
            // Spawn fish on server
            BaseFishEntity fish = FishTypeRegistry.createFishEntity(fishData.getName(), serverLevel);
            if (fish != null) {
                fish.setPos(fishPosition.x, fishPosition.y, fishPosition.z);
                fish.setNoGravity(true);
                fish.setInvulnerable(true);
                fish.setPersistenceRequired();
                fish.setGlowingTag(true);
                fish.setInvisible(true); // Server fish is invisible

                serverLevel.addFreshEntity(fish);
                gameState.setServerFish(fish);
            }

            // Spawn hook on server with static positioning
            Vec3 hookPos = waterPosition.add(0, 0.3, 0);
            KisekiFishingHook hook = new KisekiFishingHook(player, serverLevel, 0, 0);

            // CRITICAL: Spawn first, then set position
            boolean hookSpawned = serverLevel.addFreshEntity(hook);

            if (hookSpawned) {
                // Set static position after spawning
                hook.setStaticPosition(hookPos);
                gameState.setFishingHook(hook);
                gameState.setServerHook(hook); // Track server hook separately
            }

        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Server entity spawn error", e);
        }
    }
    private static void showHitEffect() {
        if (!isActive) return;

        hitEffectStartTime = System.currentTimeMillis();
        hitEffectShown = true;
        gameState.setPhase(GamePhase.HIT_EFFECT);

        // Play hit success sound
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.level().playSound(mc.player, mc.player.blockPosition(),
                    ModSoundEvents.FISHING_HIT_EFFECT.get(), SoundSource.PLAYERS, 1.0f, 1.3f);
        }

        // Schedule transition to active fishing after HIT! effect
        new Thread(() -> {
            try {
                Thread.sleep(1500); // HIT! effect duration
                if (isActive && gameState.getPhase() == GamePhase.HIT_EFFECT) {
                    // Run on main thread to avoid issues
                    Minecraft.getInstance().execute(() -> {
                        if (isActive) {
                            startActiveFishing();
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void startActiveFishing() {
        if (!isActive) return;

        gameState.setPhase(GamePhase.FISHING_GAME);

        // Enable fish rendering on client BEFORE sending packet
        FishRenderManager.enableFishRendering();

        // Initialize fish on client side first
        gameState.initializeFish();

        // Send packet to server to initialize fish
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && gameState != null) {
            NetworkHandler.sendToServer(new FishingGamePacket(
                    gameState.getWaterPosition(),
                    gameState.getBait(),
                    gameState.getRodType().name()
            ));
        }

        // Play sound on client
        if (mc.player != null) {
            mc.player.level().playSound(mc.player, mc.player.blockPosition(),
                    ModSoundEvents.FISHING_TENSION_START.get(), SoundSource.PLAYERS, 0.8f, 0.9f);
        }
    }



    public static void updateFishingGame() {
        if (!isActive || gameState == null) return;

        switch (gameState.getPhase()) {
            case EXCLAMATION:
                updateExclamationPhase();
                break;
            case HIT_EFFECT:
                updateHitEffectPhase();
                break;
            case FISHING_GAME:
                updateActiveFishingPhase();
                break;
        }
    }

    private static void updateExclamationPhase() {
        long currentTime = System.currentTimeMillis();
        long timeSinceExclamation = currentTime - exclamationStartTime;

        // If exclamation mark times out (2 seconds), fail the fishing
        if (timeSinceExclamation > 2000) {
            failFishing("Too slow!");
        }
    }

    private static void updateHitEffectPhase() {
        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - hitEffectStartTime;

        // HIT! effect automatically transitions after 1.5 seconds
        if (timeSinceHit > 1500) {
            startActiveFishing();
        }
    }

    private static void updateActiveFishingPhase() {
        gameState.updateFish();
        gameState.updateTension();

        // Check win/lose conditions
        if (gameState.getFishStamina() <= 0) {
            completeFishing();
        } else if (gameState.getTension() >= gameState.getMaxTension()) {
            failFishing("Line broke!");
        }
    }

    private static void completeFishing() {
        if (!isActive || gameState == null) {
            KisekiLegend.LOGGER.info("Complete fishing blocked - not active or no state");
            return;
        }

        KisekiLegend.LOGGER.info("=== FISHING COMPLETED SUCCESSFULLY ===");

        // Play success sound
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.level().playSound(mc.player, mc.player.blockPosition(),
                    ModSoundEvents.FISHING_SUCCESS.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // CRITICAL: End the fishing game IMMEDIATELY to restore player movement
        gameState.setPhase(GamePhase.COMPLETED);

        // Clear player fishing reference to allow movement
        Player player = gameState.getPlayer();
        if (player.fishing != null) {
            player.fishing = null;
        }

        // Reset rod animation
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof KisekiFishingRodItem rod) {
            rod.resetToIdle(player);
        } else if (offHand.getItem() instanceof KisekiFishingRodItem rod) {
            rod.resetToIdle(player);
        }

        // Disable fish rendering
        FishRenderManager.disableFishRendering();

        // Set game as inactive so player can move
        isActive = false;

        // THEN award rewards with animation
        gameState.awardRewards();

        // Schedule final cleanup after animation
        scheduleGameEnd(5000); // This will clean up entities after animation
    }


    private static void failFishing(String reason) {
        if (!isActive) return;

        gameState.setPhase(GamePhase.FAILED);

        // CRITICAL: Remove hooks immediately on failure (no animation)
        if (gameState.getFishingHook() != null) {
            gameState.getFishingHook().discard();
        }
        if (gameState.getServerHook() != null) {
            gameState.getServerHook().discard();
        }

        // Play fail sound
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.level().playSound(mc.player, mc.player.blockPosition(),
                    ModSoundEvents.FISHING_FAIL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            mc.player.displayClientMessage(
                    Component.literal("Fishing failed: " + reason), true);
        }

        // End fishing immediately
        endFishing();
    }
    private static void scheduleRewardGiving() {
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Let bobber animation play
                if (isActive && gameState != null) {
                    gameState.awardRewards();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void scheduleGameEnd(long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                // Only clean up entities - game is already ended
                if (gameState != null) {
                    gameState.cleanup();
                    gameState = null;
                }

                // Reset remaining variables
                exclamationShown = false;
                hitEffectShown = false;

                KisekiLegend.LOGGER.info("=== FINAL FISHING CLEANUP COMPLETED ===");

                // Force garbage collection
                System.gc();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    public static void endFishing() {
        if (!isActive) {
            KisekiLegend.LOGGER.info("endFishing called but not active");
            return;
        }

        KisekiLegend.LOGGER.info("=== ENDING FISHING GAME ===");

        // Reset rod animation first
        if (gameState != null && gameState.getPlayer() != null) {
            Player player = gameState.getPlayer();
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            if (mainHand.getItem() instanceof KisekiFishingRodItem rod) {
                rod.resetToIdle(player);
            } else if (offHand.getItem() instanceof KisekiFishingRodItem rod) {
                rod.resetToIdle(player);
            }
        }

        // Disable fish rendering
        FishRenderManager.disableFishRendering();

        // Clean up state
        if (gameState != null) {
            gameState.cleanup();
        }

        // Reset all static variables
        isActive = false;
        gameState = null;
        exclamationShown = false;
        hitEffectShown = false;

        KisekiLegend.LOGGER.info("=== FISHING GAME ENDED ===");

        // Force garbage collection to clean up entities
        System.gc();
    }


    public static void handleMovement(float deltaX, float deltaZ) {
        if (!isActive || gameState == null || gameState.getPhase() != GamePhase.FISHING_GAME) {
            return;
        }
        gameState.moveCatchZone(deltaX, deltaZ);
    }

    public static void handleReeling(boolean isReeling) {
        if (!isActive || gameState == null || gameState.getPhase() != GamePhase.FISHING_GAME) {
            return;
        }

        // Add debug logging
        KisekiLegend.LOGGER.info("HANDLE REELING: " + isReeling + " | Phase: " + gameState.getPhase());

        gameState.setReeling(isReeling);
    }

    // Getters for rendering
    public static boolean isActive() { return isActive; }
    public static FishingGameState getGameState() { return gameState; }
    public static boolean isExclamationShown() { return exclamationShown; }
    public static boolean isHitEffectShown() { return hitEffectShown; }

    public static long getExclamationProgress() {
        if (!exclamationShown) return 0;
        return System.currentTimeMillis() - exclamationStartTime;
    }

    public static long getHitEffectProgress() {
        if (!hitEffectShown) return 0;
        return System.currentTimeMillis() - hitEffectStartTime;
    }
}