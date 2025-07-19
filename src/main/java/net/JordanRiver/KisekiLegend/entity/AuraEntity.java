package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
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

public class AuraEntity extends Entity implements GeoEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final RawAnimation PULSE_ANIM = RawAnimation.begin().then("pulse", Animation.LoopType.LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUUID;
    private int lifespan = 0;
    private static final int MAX_LIFESPAN = 200; // 10 seconds at 20 TPS

    public AuraEntity(EntityType<? extends AuraEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        LOGGER.info("AuraEntity created with EntityType: {}", entityType.getDescriptionId());
    }

    public AuraEntity(ServerLevel level, Player owner) {
        this(ModEntities.AURA_ENTITY.get(), level);
        this.ownerUUID = owner.getUUID();

        // Position the aura around the player's torso
        Vec3 playerPos = owner.position();
        this.setPos(playerPos.x, playerPos.y - 0, playerPos.z); // At chest level
        this.setYRot(owner.getYRot());
        this.setXRot(0);

        LOGGER.info("AuraEntity positioned at: {} for owner: {}", this.position(), owner.getName().getString());
    }

    @Override
    public void tick() {
        super.tick();

        lifespan++;

        // Remove if too old or owner is gone
        if (lifespan > MAX_LIFESPAN || (ownerUUID != null && level().getPlayerByUUID(ownerUUID) == null)) {
            LOGGER.info("AuraEntity {} removing due to lifespan or missing owner", this.getId());
            this.discard();
            return;
        }

        // Keep the aura centered on the owner's torso
        if (ownerUUID != null && level().getPlayerByUUID(ownerUUID) instanceof Player owner) {
            Vec3 ownerPos = owner.position();
            Vec3 targetPos = new Vec3(ownerPos.x, ownerPos.y - 0, ownerPos.z);
            Vec3 currentPos = this.position();
            Vec3 lerpedPos = currentPos.lerp(targetPos, 0.3); // Smooth following

            this.setPos(lerpedPos.x, lerpedPos.y, lerpedPos.z);
            this.setYRot(owner.getYRot());

            // Spawn particles around the aura on server side
            if (level() instanceof ServerLevel serverLevel) {
                // Spawn particles in a circle around the aura
                for (int i = 0; i < 3; i++) {
                    double angle = (System.currentTimeMillis() / 500.0 + i * 2.0943951023931953) % (2 * Math.PI); // 120 degrees apart
                    double radius = 0.8 + Math.sin(System.currentTimeMillis() / 1000.0) * 0.2; // Pulsing radius

                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double offsetY = Math.sin(System.currentTimeMillis() / 800.0) * 0.3; // Floating motion

                    serverLevel.sendParticles(
                            ModParticles.BLUE_FLOW.get(),
                            lerpedPos.x + offsetX,
                            lerpedPos.y + offsetY,
                            lerpedPos.z + offsetZ,
                            1,
                            0.0, 0.05, 0.0,
                            0.02
                    );
                }

                // Additional central particles
                serverLevel.sendParticles(
                        ModParticles.BLUE_FLOW.get(),
                        lerpedPos.x,
                        lerpedPos.y,
                        lerpedPos.z,
                        2,
                        0.2, 0.2, 0.2,
                        0.01
                );
            }
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
        controllers.add(new AnimationController<>(this, "aura_controller", 0, state -> {
            // Force the animation to play
            state.getController().setAnimation(PULSE_ANIM);
            LOGGER.debug("Setting pulse animation for AuraEntity {}", this.getId());
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

    // Trigger animations manually for debugging
    public void triggerAnimations() {
        if (level().isClientSide()) {
            this.triggerAnim("aura_controller", "pulse");
        }
    }
}