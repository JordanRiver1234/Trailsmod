// src/main/java/net/JordanRiver/KisekiLegend/fishing/FishingGameState.java
package net.JordanRiver.KisekiLegend.fishing;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.KisekiFishingHook;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.entity.ModEntities;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.FishBucketItem;
import net.JordanRiver.KisekiLegend.network.FishingGamePacket;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FishingGameState {
    private final Player player;
    private final RodType rodType;
    private final Vec3 waterPosition;
    private FishingGameManager.GamePhase phase;



    // Fish and game state
    private net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity currentFish;
    private FishData currentFishData;
    private Vec3 fishPosition;
    private Vec3 catchZonePosition;
    private int fishStamina;
    private float tension;
    private final float maxTension = 100.0f;
    private boolean isReeling = false;
    private boolean fishInTensionState = false;
    private long lastTensionChange = 0;
    private net.minecraft.world.entity.projectile.FishingHook fishingHook;
    // Movement and timing
    private Vec3 fishVelocity;
    private long lastMoveTime;
    private int boundingBoxSize;
    private final String bait;
    private float catchZoneRadius; // This will vary based on rod type

    public FishingGameState(Player player, RodType rodType, Vec3 waterPosition, String bait, int dynamicBoundarySize) {
        this.player = player;
        this.rodType = rodType;
        this.waterPosition = waterPosition;
        this.bait = bait;
        this.catchZonePosition = waterPosition.add(0, 0.1, 0);
        this.fishVelocity = new Vec3(0, 0, 0);
        this.lastMoveTime = System.currentTimeMillis();
        this.boundingBoxSize = dynamicBoundarySize;

        // FIXED: Progressive catch zone sizes based on rod type
        this.catchZoneRadius = switch (rodType) {
            case PROGRESS_ROD -> 1.2f;        // Smallest catch zone
            case MARINE_STAR_ROD -> 1.4f;     // Slightly bigger
            case PISCES_HEART -> 1.6f;        // Moderate size
            case BAMBOO_FISHING_ROD -> 1.8f;  // Getting easier
            case METAL_TRIDENT_ROD -> 2.0f;   // Much easier
            case LAKELORD_II -> 2.3f;         // Very forgiving
            case AQUA_MASTER -> 2.8f;         // Largest, most forgiving
        };

        KisekiLegend.LOGGER.info("=== CATCH ZONE SET ===");
        KisekiLegend.LOGGER.info("Rod: " + rodType.name() + " | Radius: " + this.catchZoneRadius);
    }

    // Keep old constructor for compatibility
    public FishingGameState(Player player, RodType rodType, Vec3 waterPosition, String bait) {
        this(player, rodType, waterPosition, bait, 6);
    }

    public void initializeFish() {
        KisekiLegend.LOGGER.info("DEBUG: Initializing fish with bait: " + bait);

        // Enable fish rendering FIRST
        FishRenderManager.enableFishRendering();
        KisekiLegend.LOGGER.info("DEBUG: Fish rendering enabled: " + FishRenderManager.shouldRenderFish());

        currentFishData = FishRegistry.getRandomFish(bait, rodType);

        if (currentFishData != null) {
            KisekiLegend.LOGGER.info("DEBUG: Selected fish: " + currentFishData.getName());

            fishStamina = currentFishData.getStamina();

            if (rodType.hasAffinityWith(bait)) {
                fishStamina = (int)(fishStamina * 0.85f);
            }

            boundingBoxSize = currentFishData.getRarity().getBoundingBoxSize();

            double offsetX = (Math.random() - 0.5) * boundingBoxSize;
            double offsetZ = (Math.random() - 0.5) * boundingBoxSize;
            fishPosition = waterPosition.add(offsetX, -0.3, offsetZ);

            // Always spawn on client side for rendering
            if (player.level().isClientSide) {
                KisekiLegend.LOGGER.info("DEBUG: Client side - spawning visual entities");
                spawnClientSideEntities();
            }
        }
    }
    private void spawnClientSideEntities() {
        if (currentFishData == null) return;

        try {
            // Spawn fish first
            fishPosition = waterPosition.add(0, -0.3, 0);
            currentFish = FishTypeRegistry.createFishEntity(currentFishData.getName(), player.level());
            if (currentFish != null) {
                currentFish.setPos(fishPosition.x, fishPosition.y, fishPosition.z);
                currentFish.setNoGravity(true);
                currentFish.setInvulnerable(true);
                currentFish.setPersistenceRequired();

                player.level().addFreshEntity(currentFish);
            }

            // CRITICAL: Enhanced hook spawning and positioning
            Vec3 hookPos = waterPosition.add(0, 0.2, 0);
            KisekiFishingHook kisekiHook = new KisekiFishingHook(ModEntities.KISEKI_FISHING_HOOK.get(), player.level());
            kisekiHook.setOwner(player);

            // Spawn the hook first
            boolean hookSpawned = player.level().addFreshEntity(kisekiHook);

            if (hookSpawned) {
                // CRITICAL: Set multiple references to ensure it's tracked
                fishingHook = kisekiHook;
                player.fishing = kisekiHook;
                catchZonePosition = hookPos;

                // Force initial position with multiple methods
                kisekiHook.setStaticPosition(hookPos);

                // Also force direct positioning as backup
                kisekiHook.setPos(hookPos.x, hookPos.y, hookPos.z);
                kisekiHook.setDeltaMovement(Vec3.ZERO);
                kisekiHook.setNoGravity(true);

                KisekiLegend.LOGGER.info("Client hook spawned and positioned at: " + hookPos);
            }

        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error spawning fishing entities", e);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public RodType getRodType() {
        return rodType;
    }

    public void setCurrentFishData(FishData fishData) {
        this.currentFishData = fishData;
        if (fishData != null) {
            this.boundingBoxSize = fishData.getRarity().getBoundingBoxSize();
        }
    }
    public void spawnFishEntityOnServer() {
        if (currentFishData == null || player.level().isClientSide) {
            KisekiLegend.LOGGER.info("DEBUG: Server spawn blocked - fishData: " + (currentFishData != null) + ", isClient: " + player.level().isClientSide);
            return;
        }

        try {
            KisekiLegend.LOGGER.info("DEBUG: Creating fish entity on server: " + currentFishData.getName());
            currentFish = FishTypeRegistry.createFishEntity(currentFishData.getName(), player.level());

            if (currentFish != null) {
                KisekiLegend.LOGGER.info("DEBUG: Fish entity created, setting position");
                currentFish.setPos(fishPosition.x, fishPosition.y, fishPosition.z);
                // Add this validation
                if (fishPosition == null) {
                    KisekiLegend.LOGGER.error("Fish position is null!");
                    return;
                }

                currentFish.setNoGravity(true);
                currentFish.setInvulnerable(true);
                currentFish.setPersistenceRequired();
                currentFish.setGlowingTag(true);

                boolean spawned = ((ServerLevel) player.level()).addFreshEntity(currentFish);
                KisekiLegend.LOGGER.info("=== SERVER FISH SPAWN: " + spawned + " ===");
            } else {
                KisekiLegend.LOGGER.error("Failed to create fish entity!");
            }
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Fish spawn error", e);
        }
    }

    public void spawnFishingHookOnServer() {
        if (fishingHook != null || player.level().isClientSide) return;

        try {
            KisekiFishingHook kisekiHook = new KisekiFishingHook(player, player.level(), 0, 0);
            Vec3 hookPos = catchZonePosition.add(0, 0.1, 0);
            kisekiHook.setStaticPosition(hookPos);
            fishingHook = kisekiHook;
            fishingHook.setGlowingTag(true);


            boolean spawned = ((ServerLevel) player.level()).addFreshEntity(fishingHook);
            KisekiLegend.LOGGER.info("=== HOOK SPAWN SUCCESS ===");
            KisekiLegend.LOGGER.info("Hook spawned: " + spawned + " at " + hookPos);
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Hook spawn error", e);
        }
    }
    public void setFishStamina(int stamina) {
        this.fishStamina = stamina;
    }

    public void setFishPosition(Vec3 position) {
        this.fishPosition = position;
    }
    public void updateFish() {
        if (currentFishData == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastMoveTime) / 1000.0f;
        lastMoveTime = currentTime;

        updateFishMovement(deltaTime);

        // Verify hook position every few updates
        if (currentTime % 500 == 0) {
            verifyHookPosition();
        }

        // FIX: Improved stamina drain logic
        if (isReeling) {
            boolean overFish = isCatchZoneOverFish();

            if (overFish && !fishInTensionState) {
                // Successfully draining stamina
                int staminaDrain = Math.max(1, (int)(30 * deltaTime)); // Ensure at least 1 point per update
                int oldStamina = fishStamina;
                fishStamina = Math.max(0, fishStamina - staminaDrain);

                // Add debug logging
                KisekiLegend.LOGGER.info("STAMINA DRAIN: " + oldStamina + " -> " + fishStamina +
                        " (drained " + staminaDrain + ") | OverFish: " + overFish +
                        " | Reeling: " + isReeling + " | TensionState: " + fishInTensionState);
            } else if (overFish && fishInTensionState) {
                // Reeling during tension - no stamina drain, but increases tension
                KisekiLegend.LOGGER.info("REELING IN TENSION STATE - no stamina drain");
            } else {
                // Not over fish while reeling
                KisekiLegend.LOGGER.info("REELING but not over fish - no stamina drain");
            }
        }

        updateTensionStates(deltaTime);
    }

    private void verifyHookPosition() {
        if (fishingHook == null || fishingHook.isRemoved()) return;
        if (catchZonePosition == null) return;

        // Check if hook has drifted from intended position
        Vec3 currentPos = fishingHook.position();
        double distance = currentPos.distanceTo(catchZonePosition);

        if (distance > 0.1) { // If hook has moved more than 0.1 blocks
            // Force it back to correct position
            KisekiLegend.LOGGER.info("Hook drifted " + distance + " blocks, forcing back to position");
            forceHookPosition(fishingHook, catchZonePosition);
        }
    }
    private void updateFishMovement(float deltaTime) {
        if (currentFishData == null) return;

        // Base speed varies by rarity - make it more realistic
        float baseSpeed = switch (currentFishData.getRarity()) {
            case COMMON -> 0.8f;      // Slow, easier to catch
            case UNCOMMON -> 1.2f;    // Moderate speed
            case RARE -> 1.8f;        // Fast and erratic
            case LEGENDARY -> 2.5f;   // Very fast, challenging
        };

        // FIXED: More natural fish movement patterns
        if (Math.random() < 0.15f) { // Change direction every ~7 ticks
            double angle = Math.random() * 2 * Math.PI;

            // Add some randomness to make movement less predictable
            double speedVariation = 0.7 + Math.random() * 0.6; // 70% to 130% of base speed
            float actualSpeed = baseSpeed * (float)speedVariation;

            fishVelocity = new Vec3(
                    Math.cos(angle) * actualSpeed,
                    0,
                    Math.sin(angle) * actualSpeed
            );
        } else if (Math.random() < 0.05f) {
            // Occasionally pause (like real fish)
            fishVelocity = fishVelocity.scale(0.3);
        } else if (Math.random() < 0.02f) {
            // Rare sudden direction change (escape behavior)
            double escapeAngle = Math.random() * 2 * Math.PI;
            fishVelocity = new Vec3(
                    Math.cos(escapeAngle) * baseSpeed * 1.5f,
                    0,
                    Math.sin(escapeAngle) * baseSpeed * 1.5f
            );
        }

        // Apply movement
        Vec3 newPos = fishPosition.add(fishVelocity.scale(deltaTime));

        // Boundary constraints (keep same as before)
        double halfSize = boundingBoxSize / 2.0;
        double minX = waterPosition.x - halfSize;
        double maxX = waterPosition.x + halfSize;
        double minZ = waterPosition.z - halfSize;
        double maxZ = waterPosition.z + halfSize;

        double clampedX = Math.max(minX, Math.min(maxX, newPos.x));
        double clampedZ = Math.max(minZ, Math.min(maxZ, newPos.z));

        newPos = new Vec3(clampedX, waterPosition.y - 0.3, clampedZ);

        // FIXED: More natural boundary bouncing
        if (clampedX == minX || clampedX == maxX) {
            // Don't just reverse - create more natural wall avoidance
            double avoidanceAngle = Math.random() * Math.PI - Math.PI/2; // -90 to +90 degrees
            fishVelocity = new Vec3(
                    Math.cos(avoidanceAngle) * baseSpeed,
                    0,
                    fishVelocity.z
            );
        }
        if (clampedZ == minZ || clampedZ == maxZ) {
            double avoidanceAngle = Math.random() * Math.PI - Math.PI/2;
            fishVelocity = new Vec3(
                    fishVelocity.x,
                    0,
                    Math.sin(avoidanceAngle) * baseSpeed
            );
        }

        fishPosition = newPos;

        // Update entities with smooth rotation
        updateFishEntity(currentFish);
        updateFishEntity(serverFish);
    }


    private void updateFishEntity(BaseFishEntity fish) {
        if (fish != null && !fish.isRemoved()) {
            fish.setPos(fishPosition.x, fishPosition.y, fishPosition.z);
            fish.xo = fishPosition.x;
            fish.yo = fishPosition.y;
            fish.zo = fishPosition.z;
            fish.xOld = fishPosition.x;
            fish.yOld = fishPosition.y;
            fish.zOld = fishPosition.z;

            // FIXED: Smoother rotation based on velocity
            if (fishVelocity.horizontalDistanceSqr() > 0.001) {
                float targetYaw = (float) Math.toDegrees(Math.atan2(-fishVelocity.x, fishVelocity.z));

                // Smooth rotation instead of instant snap
                float currentYaw = fish.getYRot();
                float yawDiff = targetYaw - currentYaw;

                // Handle angle wrapping
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;

                // Gradually rotate (max 10 degrees per update)
                float rotationStep = Math.max(-10, Math.min(10, yawDiff));
                fish.setYRot(currentYaw + rotationStep);
            }

            // Update bounding box
            fish.setBoundingBox(fish.getBoundingBox().move(
                    fishPosition.x - fish.getX(),
                    fishPosition.y - fish.getY(),
                    fishPosition.z - fish.getZ()
            ));
        }
    }
    private BaseFishEntity serverFish;
    public void setFishingHook(net.minecraft.world.entity.projectile.FishingHook hook) {
        this.fishingHook = hook;
    }
    public void setServerFish(BaseFishEntity fish) {
        this.serverFish = fish;
    }

    public BaseFishEntity getServerFish() {
        return serverFish;
    }
    private void updateTensionStates(float deltaTime) {
        long currentTime = System.currentTimeMillis();

        // Apply time bonus to reduce tension frequency (better rods = less frequent tension)
        float tensionChance = Math.max(0.001f, 0.005f * (1.0f - rodType.getTimeBonus() * 0.5f)); // Much lower chance

        // Randomly enter tension state (less frequent with better rods)
        if (!fishInTensionState && Math.random() < tensionChance) {
            enterTensionState();
        }

        // Exit tension state after duration
        if (fishInTensionState && currentTime - lastTensionChange > 3000) { // 3 seconds
            exitTensionState();
        }
    }

    private void enterTensionState() {
        fishInTensionState = true;
        lastTensionChange = System.currentTimeMillis();

        // Play tension sound
        player.level().playSound(player, player.blockPosition(),
                ModSoundEvents.FISHING_TENSION_START.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    public void removeFishingHook(boolean animate) {
        if (fishingHook != null && !fishingHook.isRemoved()) {
            if (animate && currentFishData != null && fishingHook instanceof KisekiFishingHook kisekiHook) {
                ItemStack fishItem = new ItemStack(ModItems.getFishItem(currentFishData.getName()));
                kisekiHook.animateReturnWithRewards(fishItem);
                // Don't set fishingHook to null here - let animation complete
                KisekiLegend.LOGGER.info("Hook removal with animation started");
                return;
            } else {
                // Immediate removal without animation
                fishingHook.discard();
                fishingHook = null;
                KisekiLegend.LOGGER.info("Hook removed immediately without animation");
            }
        }
    }

    public net.minecraft.world.entity.projectile.FishingHook getFishingHook() {
        return fishingHook;
    }
    private void exitTensionState() {
        fishInTensionState = false;
        lastTensionChange = System.currentTimeMillis();

        // Play tension release sound
        player.level().playSound(player, player.blockPosition(),
                ModSoundEvents.FISHING_TENSION_END.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    public void updateTension() {
        float deltaTime = 1.0f / 60.0f;

        if (isReeling && fishInTensionState) {
            // Only increase tension when reeling during tension state
            tension = Math.min(maxTension, tension + 40 * deltaTime);
        }
        // Remove the decrease logic - tension never decreases
    }


    public void moveCatchZone(float deltaX, float deltaZ) {
        if (catchZonePosition == null || fishingHook == null) return;

        Vec3 newPos = catchZonePosition.add(deltaX, 0, deltaZ);

        // Apply boundary constraints
        double halfSize = boundingBoxSize / 2.0;
        double minX = waterPosition.x - halfSize;
        double maxX = waterPosition.x + halfSize;
        double minZ = waterPosition.z - halfSize;
        double maxZ = waterPosition.z + halfSize;

        Vec3 constrainedPos = new Vec3(
                Math.max(minX, Math.min(maxX, newPos.x)),
                waterPosition.y + 0.2,
                Math.max(minZ, Math.min(maxZ, newPos.z))
        );

        catchZonePosition = constrainedPos;

        // CRITICAL: Force hook to move with catch zone
        if (fishingHook instanceof KisekiFishingHook kisekiHook && !kisekiHook.isReturning()) {
            kisekiHook.setStaticPosition(catchZonePosition);
        } else if (!fishingHook.isRemoved()) {
            // Force position for any other hook type
            fishingHook.setPos(catchZonePosition.x, catchZonePosition.y, catchZonePosition.z);
            fishingHook.setDeltaMovement(Vec3.ZERO);
            fishingHook.setNoGravity(true);

            // Force interpolation values
            fishingHook.xo = catchZonePosition.x;
            fishingHook.yo = catchZonePosition.y;
            fishingHook.zo = catchZonePosition.z;
            fishingHook.xOld = catchZonePosition.x;
            fishingHook.yOld = catchZonePosition.y;
            fishingHook.zOld = catchZonePosition.z;
        }
    }


    private void forceHookPosition(net.minecraft.world.entity.projectile.FishingHook hook, Vec3 position) {
        if (hook == null || hook.isRemoved()) return;

        if (hook instanceof KisekiFishingHook kisekiHook) {
            if (!kisekiHook.isReturning()) {
                // CRITICAL: Force position update multiple ways
                kisekiHook.setStaticPosition(position);

                // Also directly set entity position as backup
                kisekiHook.setPos(position.x, position.y, position.z);
                kisekiHook.setDeltaMovement(Vec3.ZERO);

                // Force interpolation
                kisekiHook.xo = position.x;
                kisekiHook.yo = position.y;
                kisekiHook.zo = position.z;
                kisekiHook.xOld = position.x;
                kisekiHook.yOld = position.y;
                kisekiHook.zOld = position.z;
            }
        } else if (hook != null) {
            // For any other hook type, force position aggressively
            hook.setPos(position.x, position.y, position.z);
            hook.setDeltaMovement(Vec3.ZERO);
            hook.setNoGravity(true);

            // Force ALL interpolation values
            hook.xo = position.x;
            hook.yo = position.y;
            hook.zo = position.z;
            hook.xOld = position.x;
            hook.yOld = position.y;
            hook.zOld = position.z;

            // FIXED: Use accessible method for bounding box update
            hook.setBoundingBox(hook.getBoundingBox().move(
                    position.x - hook.getX(),
                    position.y - hook.getY(),
                    position.z - hook.getZ()
            ));
        }
    }


    public boolean isCatchZoneOverFish() {
        if (catchZonePosition == null || fishPosition == null) {
            KisekiLegend.LOGGER.info("CATCH ZONE CHECK FAILED - null positions");
            return false;
        }

        double distance = fishPosition.distanceTo(catchZonePosition);
        double effectiveRadius = catchZoneRadius;

        if (currentFishData != null) {
            effectiveRadius *= currentFishData.getCatchZoneSize();
        }

        // Make it reasonably forgiving
        effectiveRadius = Math.max(effectiveRadius, 2.0f);

        boolean isOver = distance <= effectiveRadius;

        // Enhanced debug logging
        KisekiLegend.LOGGER.info("CATCH ZONE CHECK - Distance: " + String.format("%.2f", distance) +
                ", Radius: " + String.format("%.2f", effectiveRadius) +
                ", Over: " + isOver +
                ", Fish: " + String.format("%.2f,%.2f,%.2f", fishPosition.x, fishPosition.y, fishPosition.z) +
                ", Catch: " + String.format("%.2f,%.2f,%.2f", catchZonePosition.x, catchZonePosition.y, catchZonePosition.z));

        return isOver;
    }
    public float getCatchZoneRadius() {
        float effectiveRadius = catchZoneRadius;
        if (currentFishData != null) {
            effectiveRadius *= currentFishData.getCatchZoneSize();
        }
        return effectiveRadius;
    }

    public String getBait() { return bait; }

    public void awardRewards() {
        if (currentFishData == null) {
            KisekiLegend.LOGGER.info("No fish data for rewards");
            return;
        }

        KisekiLegend.LOGGER.info("Awarding rewards for: " + currentFishData.getName());

        // Create fish item for visual animation
        ItemStack fishItem = new ItemStack(ModItems.getFishItem(currentFishData.getName()));

        // Give sepith rewards immediately
        for (String sepithType : currentFishData.getSepithRewards()) {
            ItemStack sepithItem = new ItemStack(ModItems.getSepithItem(sepithType));
            if (!player.getInventory().add(sepithItem)) {
                player.drop(sepithItem, false);
            }
        }

        // RARE CHANCE: Award fish bucket based on rarity
        double fishBucketChance = switch (currentFishData.getRarity()) {
            case COMMON -> 0.02;      // 2% chance
            case UNCOMMON -> 0.05;    // 5% chance
            case RARE -> 0.10;        // 10% chance
            case LEGENDARY -> 0.25;   // 25% chance
        };

        if (Math.random() < fishBucketChance) {
            ItemStack fishBucket = new ItemStack(ModItems.getFishBucket(currentFishData.getName()));
            if (fishBucket.getItem() != Items.AIR) {
                // Pre-fill the bucket with the fish data
                if (fishBucket.getItem() instanceof FishBucketItem) {
                    // The bucket already contains the fish type, no need to add NBT
                }

                if (!player.getInventory().add(fishBucket)) {
                    player.drop(fishBucket, false);
                }

                // Special message for fish bucket
                player.displayClientMessage(
                        Component.literal("✦ BONUS: " + currentFishData.getName() + " in Water Bucket! ✦"), true);

                // Play special sound for fish bucket drop
                player.level().playSound(player, player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        }

        // Award experience
        int expAmount = switch (currentFishData.getRarity()) {
            case COMMON -> 5;
            case UNCOMMON -> 15;
            case RARE -> 30;
            case LEGENDARY -> 50;
        };
        player.giveExperiencePoints(expAmount);

        // Show success message
        player.displayClientMessage(
                Component.literal("Caught: " + currentFishData.getName() + " (+" + expAmount + " XP)"), false);

        // CRITICAL: Clear player's fishing reference IMMEDIATELY to restore movement
        if (player.fishing != null) {
            player.fishing = null;
        }

        // Set phase to completed to stop game updates
        this.setPhase(FishingGameManager.GamePhase.COMPLETED);

        // THEN handle hook animation (after game state is cleared)
        if (fishingHook instanceof KisekiFishingHook kisekiHook && !kisekiHook.isRemoved()) {
            KisekiLegend.LOGGER.info("Starting hook animation with fish item");
            kisekiHook.animateReturnWithRewards(fishItem);
            // The hook will clean itself up after animation
        } else {
            // Fallback: give fish item directly if no hook animation possible
            KisekiLegend.LOGGER.info("No hook for animation, giving fish directly");
            if (!player.getInventory().add(fishItem)) {
                player.drop(fishItem, false);
            }

            // Force cleanup since no animation
            cleanup();
        }
    }
    private net.minecraft.world.entity.projectile.FishingHook serverHook;

    public void setServerHook(net.minecraft.world.entity.projectile.FishingHook hook) {
        this.serverHook = hook;
    }

    public net.minecraft.world.entity.projectile.FishingHook getServerHook() {
        return serverHook;
    }
    public void cleanup() {
        KisekiLegend.LOGGER.info("=== CLEANING UP FISHING STATE ===");

        try {
            // Clean up client fish
            if (currentFish != null && !currentFish.isRemoved()) {
                KisekiLegend.LOGGER.info("Removing client fish entity");
                currentFish.discard();
            }

            // Clean up server fish
            if (serverFish != null && !serverFish.isRemoved()) {
                KisekiLegend.LOGGER.info("Removing server fish entity");
                serverFish.discard();
            }

            // CRITICAL: Only remove hook if it's not currently animating a return
            if (fishingHook != null && !fishingHook.isRemoved()) {
                if (fishingHook instanceof KisekiFishingHook kisekiHook) {
                    if (!kisekiHook.isReturning()) {
                        KisekiLegend.LOGGER.info("Removing non-returning fishing hook");
                        fishingHook.discard();
                    } else {
                        KisekiLegend.LOGGER.info("Leaving returning hook to complete animation");
                    }
                } else {
                    KisekiLegend.LOGGER.info("Removing regular fishing hook");
                    fishingHook.discard();
                }
            }

            // Clean up server hook separately
            if (serverHook != null && !serverHook.isRemoved()) {
                if (serverHook instanceof KisekiFishingHook kisekiHook) {
                    if (!kisekiHook.isReturning()) {
                        KisekiLegend.LOGGER.info("Removing non-returning server hook");
                        serverHook.discard();
                    } else {
                        KisekiLegend.LOGGER.info("Leaving returning server hook to complete animation");
                    }
                } else {
                    KisekiLegend.LOGGER.info("Removing regular server hook");
                    serverHook.discard();
                }
            }

        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error during fishing cleanup", e);
        } finally {
            // Clear all references
            currentFish = null;
            serverFish = null;
            // Don't null out fishingHook if it's animating
            if (fishingHook instanceof KisekiFishingHook kisekiHook && !kisekiHook.isReturning()) {
                fishingHook = null;
            }
            if (serverHook instanceof KisekiFishingHook kisekiHook && !kisekiHook.isReturning()) {
                serverHook = null;
            }

            fishPosition = null;
            catchZonePosition = null;
            currentFishData = null;

            KisekiLegend.LOGGER.info("=== FISHING STATE CLEANUP COMPLETED ===");
        }
    }
    // Getters and setters
    public FishingGameManager.GamePhase getPhase() { return phase; }
    public void setPhase(FishingGameManager.GamePhase phase) { this.phase = phase; }

    public int getFishStamina() { return fishStamina; }
    public float getTension() { return tension; }
    public float getMaxTension() { return maxTension; }
    public void setReeling(boolean reeling) { this.isReeling = reeling; }
    public boolean isReeling() { return isReeling; }
    public boolean isFishInTensionState() { return fishInTensionState; }
    public Vec3 getFishPosition() { return fishPosition; }
    public Vec3 getCatchZonePosition() { return catchZonePosition; }
    public Vec3 getWaterPosition() { return waterPosition; }
    public int getBoundingBoxSize() { return boundingBoxSize; }
    public FishData getCurrentFishData() { return currentFishData; }
}