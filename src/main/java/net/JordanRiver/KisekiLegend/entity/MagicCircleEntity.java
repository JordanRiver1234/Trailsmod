package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class MagicCircleEntity extends Entity implements GeoEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final RawAnimation CAST_ANIM = RawAnimation.begin().then("cast", Animation.LoopType.LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUUID;
    private int lifespan = 0;
    private static final int MAX_LIFESPAN = 200; // 10 seconds at 20 TPS

    public MagicCircleEntity(EntityType<? extends MagicCircleEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        LOGGER.info("MagicCircleEntity created with EntityType: {}", entityType.getDescriptionId());
    }

    public MagicCircleEntity(ServerLevel level, Player owner) {
        this(ModEntities.MAGIC_CIRCLE_ENTITY.get(), level);
        this.ownerUUID = owner.getUUID();

        // Position the magic circle at the player's feet, centered on the ground
        Vec3 playerPos = owner.position();
        double groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) playerPos.x, (int) playerPos.z);

        this.setPos(playerPos.x, groundY + 0.1, playerPos.z); // Slightly above ground to avoid z-fighting
        this.setYRot(0); // Always face north for consistency
        this.setXRot(0); // Flat on the ground

        LOGGER.info("MagicCircleEntity positioned at: {} for owner: {}", this.position(), owner.getName().getString());
    }

    @Override
    public void tick() {
        super.tick();

        lifespan++;

        // Remove if too old or owner is gone
        if (lifespan > MAX_LIFESPAN || (ownerUUID != null && level().getPlayerByUUID(ownerUUID) == null)) {
            LOGGER.info("MagicCircleEntity {} removing due to lifespan or missing owner", this.getId());
            this.discard();
            return;
        }

        // Keep the circle centered on the owner's feet
        if (ownerUUID != null && level().getPlayerByUUID(ownerUUID) instanceof Player owner) {
            Vec3 ownerPos = owner.position();
            double groundY = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) ownerPos.x, (int) ownerPos.z);

            // Smoothly move the circle to follow the player
            Vec3 targetPos = new Vec3(ownerPos.x, groundY + 0.1, ownerPos.z);
            Vec3 currentPos = this.position();
            Vec3 lerpedPos = currentPos.lerp(targetPos, 0.2); // Smooth following

            this.setPos(lerpedPos.x, lerpedPos.y, lerpedPos.z);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Empty implementation for 1.21.1 compatibility
        // Add any synched data definitions here if needed
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        this.lifespan = compound.getInt("Lifespan");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        compound.putInt("Lifespan", this.lifespan);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "magic_circle_controller", 0, state -> {
            state.getController().setAnimation(CAST_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public int getLifespan() {
        return lifespan;
    }

    @Override
    public boolean isAlive() {
        return super.isAlive() && lifespan < MAX_LIFESPAN;
    }
}