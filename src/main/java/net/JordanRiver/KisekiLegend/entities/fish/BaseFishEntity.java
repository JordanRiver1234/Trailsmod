package net.JordanRiver.KisekiLegend.entities.fish;

import net.JordanRiver.KisekiLegend.fishing.FishData;
import net.JordanRiver.KisekiLegend.fishing.FishRegistry;
import net.JordanRiver.KisekiLegend.fishing.FishTypeRegistry;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.items.FishBucketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluids;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

public abstract class BaseFishEntity extends PathfinderMob implements GeoEntity, Bucketable {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(BaseFishEntity.class, EntityDataSerializers.BOOLEAN);

    public BaseFishEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnReason) {
        // Only allow spawning from spawn eggs, buckets, or fishing games
        return spawnReason == MobSpawnType.SPAWNER ||
                spawnReason == MobSpawnType.BUCKET ||
                spawnReason == MobSpawnType.SPAWN_EGG ||
                FishingGameManager.isActive();
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return false;
    }

    @Override
    public void tick() {
        if (this.isRemoved() || this.level() == null) {
            return;
        }

        super.tick();

        // MEMORY SAVER: Only update every 30 ticks (1.5 seconds)
        if (this.tickCount % 30 != 0) {
            return;
        }

        // During fishing game - fish are controlled by FishingGameState
        if (FishingGameManager.isActive()) {
            this.setAirSupply(this.getMaxAirSupply());
            this.setGlowingTag(true);
            return; // No autonomous movement during fishing
        }

        // Basic survival behavior for spawned fish
        boolean inWater = this.level().getFluidState(this.blockPosition()).is(Fluids.WATER);

        if (inWater) {
            this.setNoGravity(true);
            this.setAirSupply(this.getMaxAirSupply());

            // Very minimal movement for spawn egg fish
            if (this.fromBucket() || this.isPersistenceRequired()) {
                // Spawned fish have minimal movement
                if (this.random.nextInt(300) == 0) { // Very rare movement
                    double angle = this.random.nextFloat() * 2 * Math.PI;
                    this.setDeltaMovement(
                            Math.cos(angle) * 0.005, // Very slow
                            0,
                            Math.sin(angle) * 0.005
                    );
                }
            }
        } else {
            this.setNoGravity(false);

            // Simple flop for spawned fish
            if (this.onGround() && this.random.nextInt(100) == 0) {
                this.setDeltaMovement(0, 0.1, 0);
            }

            // Air management - only affect spawned fish if they don't have persistence
            if (!this.isPersistenceRequired() && !this.fromBucket()) {
                int currentAir = this.getAirSupply();
                if (currentAir > -20) {
                    this.setAirSupply(currentAir - 1);
                }
                if (currentAir <= -20) {
                    this.hurt(this.damageSources().drown(), 1.0F);
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D) // Slightly more health for spawned fish
                .add(Attributes.MOVEMENT_SPEED, 0.15D); // Slower movement to save performance
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<BaseFishEntity> animationState) {
        if (!this.isInWater() && this.onGround()) {
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("flop"));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("swim"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public abstract String getFishType();

    // NO CACHING - fetch fresh to prevent memory leaks
    public FishData getFishData() {
        return FishRegistry.getFishData(getFishType());
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected int decreaseAirSupply(int currentAir) {
        if (FishingGameManager.isActive() || this.isPersistenceRequired()) {
            return this.getMaxAirSupply();
        }
        return currentAir - 1;
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, compoundTag -> {
            compoundTag.putString("FishType", getFishType());
            compoundTag.putBoolean("FromBucket", true);
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        this.setFromBucket(true);
        this.setPersistenceRequired();
        this.setNoGravity(true);
    }

    @Override
    public ItemStack getBucketItemStack() {
        Item fishBucketItem = ModItems.getFishBucket(getFishType());
        if (fishBucketItem == Items.AIR) {
            return new ItemStack(Items.WATER_BUCKET);
        }

        ItemStack bucket = new ItemStack(fishBucketItem);
        saveToBucketTag(bucket);
        return bucket;
    }

    public static void spawnFromBucket(Level level, ItemStack bucket, BlockPos waterPos) {
        if (bucket.has(DataComponents.BUCKET_ENTITY_DATA)) {
            CompoundTag tag = bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag();
            if (tag.contains("FishType")) {
                String fishType = tag.getString("FishType");
                BaseFishEntity fish = FishTypeRegistry.createFishEntity(fishType, level);
                if (fish != null) {
                    fish.setPos(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);
                    fish.loadFromBucketTag(tag);
                    level.addFreshEntity(fish);
                }
            }
        } else if (bucket.getItem() instanceof FishBucketItem fishBucket) {
            String fishType = fishBucket.getFishType();
            BaseFishEntity fish = FishTypeRegistry.createFishEntity(fishType, level);
            if (fish != null) {
                fish.setPos(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);
                fish.setFromBucket(true);
                fish.setPersistenceRequired();
                fish.setNoGravity(false);
                level.addFreshEntity(fish);
            }
        }
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    public boolean isInvulnerable() {
        return this.isPersistenceRequired() || FishingGameManager.isActive() || super.isInvulnerable();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (FishingGameManager.isActive()) {
            return InteractionResult.FAIL;
        }

        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.WATER_BUCKET) && this.isAlive() && this.isInWater()) {
            return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean requiresCustomPersistence() {
        // All fish from spawn eggs or buckets persist
        return this.fromBucket() || this.hasCustomName() || super.requiresCustomPersistence();
    }

    public boolean isValidForRendering() {
        return !this.isRemoved() && this.level() != null;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (FishingGameManager.isActive()) {
            return false;
        }
        return super.hurt(damageSource, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Fish from spawn eggs or buckets never despawn
        return !this.fromBucket() && !this.hasCustomName() && !this.isPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROM_BUCKET, false);
    }
}