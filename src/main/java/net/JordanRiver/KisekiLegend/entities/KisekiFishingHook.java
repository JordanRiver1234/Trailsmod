package net.JordanRiver.KisekiLegend.entities;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

public class KisekiFishingHook extends FishingHook {
    private Vec3 staticPosition = null;
    private boolean isStaticMode = false;
    private boolean isReturning = false;
    private boolean hasGivenReward = false; // CRITICAL: Prevent duplicate rewards
    private ItemStack rewardItem = ItemStack.EMPTY;
    private int animationTimer = 0;

    public KisekiFishingHook(EntityType<? extends FishingHook> entityType, Level level) {
        super(entityType, level);
    }

    public KisekiFishingHook(Player player, Level level, int luck, int lureSpeed) {
        super(player, level, luck, lureSpeed);
    }

    @Override
    public void tick() {
        // Handle return animation with rewards
        if (isReturning && !rewardItem.isEmpty() && !hasGivenReward) {
            handleReturnAnimation();
            return;
        }

        // Static positioning during fishing game
        if (FishingGameManager.isActive() && isStaticMode && staticPosition != null && !isReturning) {
            // Force static position
            this.setPos(staticPosition.x, staticPosition.y, staticPosition.z);
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);

            // Update interpolation to prevent jittering
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            return;
        }

        // Use vanilla behavior when not in game or returning
        if (!FishingGameManager.isActive() || !isStaticMode) {
            super.tick();
        }
    }

    private void handleReturnAnimation() {
        Player owner = this.getPlayerOwner();
        if (owner == null) {
            this.discard();
            return;
        }

        animationTimer++;

        // FIRST tick - spawn the visual reward item and play sound
        if (animationTimer == 1) {
            spawnVisualRewardItem(owner, rewardItem);

            // Play retrieve sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Mark reward as given to prevent duplicates
            hasGivenReward = true;
        }

        // Add some particles during the first few ticks
        if (animationTimer <= 20 && this.level().isClientSide && animationTimer % 3 == 0) {
            this.level().addParticle(ParticleTypes.SPLASH,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2,
                    0.1,
                    (this.random.nextDouble() - 0.5) * 0.2);
        }

        // Remove hook after short delay (let the item do the flying)
        if (animationTimer > 20) { // 1 second
            KisekiLegend.LOGGER.info("Removing hook after return animation");
            this.discard();
        }
    }

    // FIXED: Simple visual reward item spawning (like vanilla)
    private void spawnVisualRewardItem(Player player, ItemStack itemStack) {
        if (this.level().isClientSide) return; // Server only

        KisekiLegend.LOGGER.info("Spawning visual reward item: " + itemStack);

        // Create item entity at hook position
        net.minecraft.world.entity.item.ItemEntity itemEntity =
                new net.minecraft.world.entity.item.ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack.copy());

        // EXACTLY like vanilla Minecraft fishing - copy from FishingHook.retrieve()
        double d0 = player.getX() - this.getX();
        double d1 = player.getY() - this.getY();
        double d2 = player.getZ() - this.getZ();
        double d3 = 0.1; // Vanilla speed multiplier

        itemEntity.setDeltaMovement(
                d0 * d3,
                d1 * d3 + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08,
                d2 * d3
        );

        itemEntity.setPickUpDelay(10); // Short pickup delay
        itemEntity.setGlowingTag(true);

        this.level().addFreshEntity(itemEntity);

        KisekiLegend.LOGGER.info("Visual reward item spawned successfully");
    }

    public void setStaticPosition(Vec3 position) {
        if (position == null) {
            this.staticPosition = null;
            this.isStaticMode = false;
            this.setNoGravity(false);
            return;
        }

        this.staticPosition = position;
        this.isStaticMode = true;

        // Immediate position update
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);

        // Force interpolation
        this.xo = position.x;
        this.yo = position.y;
        this.zo = position.z;
    }

    public boolean isReturning() {
        return isReturning;
    }

    // CRITICAL: Only call this ONCE when fishing completes successfully
    public void animateReturnWithRewards(ItemStack fishItem) {
        if (isReturning || hasGivenReward || fishItem.isEmpty()) {
            KisekiLegend.LOGGER.info("Return animation blocked - returning: " + isReturning +
                    ", hasGivenReward: " + hasGivenReward + ", empty: " + fishItem.isEmpty());
            return;
        }

        KisekiLegend.LOGGER.info("Starting return animation with item: " + fishItem);

        this.isReturning = true;
        this.rewardItem = fishItem.copy();
        this.isStaticMode = false;
        this.staticPosition = null;
        this.animationTimer = 0;
        this.hasGivenReward = false; // Will be set to true when item is spawned

        // Keep gravity off but allow movement
        this.setNoGravity(true);
    }

    @Override
    public int retrieve(ItemStack stack) {
        // CRITICAL: Block vanilla retrieve completely during fishing game
        if (FishingGameManager.isActive()) {
            KisekiLegend.LOGGER.info("Blocked vanilla retrieve - fishing game active");
            return 0;
        }
        return super.retrieve(stack);
    }

    @Override
    public void remove(RemovalReason reason) {
        KisekiLegend.LOGGER.info("KisekiFishingHook removed - Reason: " + reason +
                ", Returning: " + isReturning + ", HasGivenReward: " + hasGivenReward);
        super.remove(reason);
    }
}