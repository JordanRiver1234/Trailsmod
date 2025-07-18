package net.JordanRiver.KisekiLegend.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.SpawnStyle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

import static software.bernie.geckolib.animation.Animation.LoopType.*;

public class GeckoSpellEntity extends Entity implements GeoEntity, ItemSupplier, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<String> DATA_ART =
            SynchedEntityData.defineId(GeckoSpellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE =
            SynchedEntityData.defineId(GeckoSpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HIT =
            SynchedEntityData.defineId(GeckoSpellEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHOULD_REMOVE =
            SynchedEntityData.defineId(GeckoSpellEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_POSITIONED =
            SynchedEntityData.defineId(GeckoSpellEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private AnimationController<GeckoSpellEntity> controller;

    private int lifeTimer = 0;
    private int deathTimer = 0;
    private boolean animationStarted = false;
    private boolean impactOccurred = false;
    private UUID ownerUUID;
    private int maxLifetimeTicks = 100; // Default 5 seconds
    private int damageDelayTimer = 0; // For delayed damage
    private boolean groundImpacted = false;
    private boolean halfAOE = false;

    // For Diamond Dust
    private int spellPhase = 0; // 0: inactive, 1: travelling, 2: dropping, 3: swelling, 4: popping
    private Vec3 playerStartPos; // Player's position when casting
    private Vec3 direction;
    private float initialYaw; // Player's yaw when casting

    public GeckoSpellEntity(EntityType<? extends GeckoSpellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true; // Prevent culling issues
    }

    public GeckoSpellEntity(Level level, LivingEntity shooter, int damage, String artName) {
        this(ModEntities.SPELL.get(), level);
        this.ownerUUID = shooter.getUUID();
        setDamage(damage);
        setArtName(artName);

        var artDef = getArtDefinition();
        if (artDef != null) {
            if (artDef.style() == SpawnStyle.PROJECTILE_TRAIL) {
                this.maxLifetimeTicks = 100; // 5 seconds for Diamond Dust
                this.playerStartPos = shooter.position(); // Use player's base position
                this.direction = shooter.getLookAngle().normalize();
                this.initialYaw = shooter.getYRot();
                this.setYRot(this.initialYaw); // Set rotation immediately
                this.spellPhase = 1; // Start travelling
            } else {
                this.maxLifetimeTicks = artDef.style() == SpawnStyle.GROUND ? 80 : 120; // 4s ground, 6s projectile
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ART, "");
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_HIT, false);
        builder.define(DATA_SHOULD_REMOVE, false);
        builder.define(DATA_POSITIONED, false);
    }

    // Getters and setters
    public void setArtName(String name) {
        this.entityData.set(DATA_ART, name != null ? name : "");
    }

    public String getArtName() {
        return this.entityData.get(DATA_ART);
    }

    public void setDamage(int damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public int getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setHit(boolean hit) {
        this.entityData.set(DATA_HIT, hit);
    }

    public boolean isHit() {
        return this.entityData.get(DATA_HIT);
    }

    public void setShouldRemove(boolean remove) {
        this.entityData.set(DATA_SHOULD_REMOVE, remove);
    }

    public boolean shouldRemove() {
        return this.entityData.get(DATA_SHOULD_REMOVE);
    }
    public void setPositioned(boolean positioned) {
        this.entityData.set(DATA_POSITIONED, positioned);
    }

    public boolean isPositioned() {
        return this.entityData.get(DATA_POSITIONED);
    }
    private ArtsRegistry.ArtDefinition getArtDefinition() {
        String artName = getArtName();
        if (artName.isEmpty()) return null;

        return ArtsRegistry.ALL_ARTS.stream()
                .filter(a -> a.name().equalsIgnoreCase(artName.replace('_', ' ')))
                .findFirst()
                .orElse(null);
    }

    public LivingEntity getOwner() {
        if (ownerUUID == null) return null;
        if (level().isClientSide) return null;

        Entity entity = ((net.minecraft.server.level.ServerLevel) level()).getEntity(ownerUUID);
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (getArtName().isEmpty() || shouldRemove()) {
            if (!level().isClientSide) discard();
            return;
        }

        var artDef = getArtDefinition();
        if (artDef == null) {
            if (!level().isClientSide) discard();
            return;
        }

        lifeTimer++;
        if (lifeTimer > maxLifetimeTicks) {
            if (!level().isClientSide) discard();
            return;
        }

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel && random.nextInt(3) == 0) {
            String art = getArtName();
            switch (art) {
                case "stone_hammer" -> serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "earth_lance" -> serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "stone_impact" -> serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "aqua_bleed" -> serverLevel.sendParticles(ParticleTypes.RAIN, getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.01);
                case "blue_impact" -> serverLevel.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "diamond_dust" -> {} // Handled in its own logic
                case "fire_bolt" -> serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.0);
                case "flare_arrow" -> {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
                }
                case "napalm_breath" -> {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 3, 0.2, 0.2, 0.2, 0.01);
                    serverLevel.sendParticles(ParticleTypes.FALLING_LAVA, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
                }
                case "fire_bolt_ex" -> serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0.01);
                case "spiral_flare", "volcanic_rave" -> {} // Custom particle handling
                default -> serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Handle different spell behaviors based on SpawnStyle
        switch (artDef.style()) {
            case GROUND -> {
                if ("petrify_breath".equals(getArtName())) {
                    handlePetrifyBreathSpell();
                } else if ("volcanic_rave".equals(getArtName())) {
                    handleVolcanicRaveSpell();
                }
                else {
                    handleGroundSpell();
                }
            }
            case AOE_CENTERED -> handleAOECenteredSpell();
            case BOUNCING_PROJECTILE -> handleBouncingProjectileSpell();
            case PROJECTILE_SPREAD -> handleProjectileSpreadSpell();
            case PROJECTILE_TRAIL -> handleProjectileTrailSpell();
            case STATIONARY -> handleStationarySpell();
            default -> handleProjectileSpell(); // PROJECTILE
        }
    }

    private void handleGroundSpell() {
        // Check for ground impact
        if (!impactOccurred && (onGround() || lifeTimer > 20)) { // Hit ground or 1 second timeout
            impactOccurred = true;
            setHit(true);
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            deathTimer = 0;

            if (getArtName().equals("earth_lance")) {
                // For earth_lance, damage in a straight line based on the entity's rotation
                // FIXED: Convert yaw to radians and use proper direction calculation
                float yawRad = (float) Math.toRadians(getYRot());
                Vec3 direction = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();

                double lineLength = 5.0;
                double lineWidth = 1.0; // Half-width for the damage box

                Vec3 start = position();
                Vec3 end = start.add(direction.scale(lineLength));

                // Create multiple damage points along the line for better hit detection
                for (int i = 0; i <= 10; i++) {
                    double t = i / 10.0;
                    Vec3 checkPoint = start.add(direction.scale(lineLength * t));

                    AABB lineSegment = new AABB(
                            checkPoint.x - lineWidth,
                            checkPoint.y - 0.5,
                            checkPoint.z - lineWidth,
                            checkPoint.x + lineWidth,
                            checkPoint.y + 2.0,
                            checkPoint.z + lineWidth
                    );

                    level().getEntities(this, lineSegment).stream()
                            .filter(entity -> entity instanceof LivingEntity target &&
                                    !target.getUUID().equals(ownerUUID))
                            .forEach(target -> {
                                onHitEntity(new EntityHitResult(target));
                            });
                }
            } else {
                // Default for other ground spells
                double inflateAmount = "petrify_breath".equals(getArtName()) ? 1.5 : 1.0;
                AABB box = getBoundingBox().inflate(inflateAmount);
                level().getEntities(this, box).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .forEach(target -> {
                            onHitEntity(new EntityHitResult(target));
                        });
            }

            if (!level().isClientSide) {
                playImpactEffects();
            }
        }

        // After impact, start countdown
        if (impactOccurred) {
            deathTimer++;
            if (!level().isClientSide && deathTimer > 60) { // 3 seconds after impact
                setShouldRemove(true);
                discard();
            }
        }
    }
    private void handlePetrifyBreathSpell() {
        // Petrify breath now uses the same ground spell logic but with different timing
        if (!impactOccurred) {
            // Immediately trigger impact for petrify breath
            impactOccurred = true;
            setHit(true);
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            deathTimer = 0;

            if (!level().isClientSide) {
                playPetrifyBreathEffects();
            }
        }

        // After impact, start countdown and apply damage after animation delay
        if (impactOccurred) {
            deathTimer++;

            // Play effects at start of animation
            if (deathTimer == 1 && !level().isClientSide) {
                playPetrifyBreathEffects();
            }

            // Apply damage after 1 second delay (20 ticks) to sync with animation
            if (deathTimer == 20) {
                // Apply 3x3 AOE damage
                AABB aoeBox = getBoundingBox().inflate(1.5); // 3x3 area
                level().getEntities(this, aoeBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .forEach(target -> {
                            // Apply damage
                            onHitEntity(new EntityHitResult(target));

                            // Apply petrify effect (20% chance of slowness)
                            if (random.nextFloat() < 0.2f) { // 20% chance
                                if (target instanceof net.minecraft.world.entity.player.Player player) {
                                    // For players, apply slowness effect
                                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                                            100, // 5 seconds at 20 TPS
                                            1 // Level 2 slowness (20% slower)
                                    ));
                                } else if (target instanceof net.minecraft.world.entity.Mob mob) {
                                    // For mobs, apply slowness effect
                                    mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                                            100, // 5 seconds
                                            1 // Level 2 slowness
                                    ));
                                }
                            }
                        });
            }

            if (!level().isClientSide && deathTimer > 80) { // 4 seconds after impact
                setShouldRemove(true);
                discard();
            }
        }
    }

    private void playPetrifyBreathEffects() {
        if (level().isClientSide) return;

        // Play petrify breath sound effect
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.7f);

        // Add poison/gas-like particles for petrify breath
        if (level() instanceof ServerLevel serverLevel) {
            // Main petrify cloud effect
            serverLevel.sendParticles(ParticleTypes.MYCELIUM, getX(), getY(), getZ(), 30, 1.0, 0.5, 1.0, 0.1);
            // Spore particles for petrification effect
            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(), getZ(), 20, 1.0, 0.5, 1.0, 0.05);
            // Dust particles for stone effect
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 15, 1.0, 0.5, 1.0, 0.02);
            // Ambient particles
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 10, 0.8, 0.3, 0.8, 0.03);
        }
    }

    private void handleAOECenteredSpell() {
        // For AOE_CENTERED like Titanic Roar: hover, then drop to ground
        if (!impactOccurred) {
            // Simulate drop after initial hover
            if (lifeTimer > 20) { // After 1 second, start dropping
                setDeltaMovement(getDeltaMovement().add(0, -0.1, 0)); // Gravity-like drop
                move(MoverType.SELF, getDeltaMovement());
            }

            // Check for ground impact
            if (onGround() || lifeTimer > 40) { // Hit ground or timeout after 2 seconds
                impactOccurred = true;
                setHit(true);
                // Set position to ground level
                int groundX = Mth.floor(getX());
                int groundZ = Mth.floor(getZ());
                int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, groundX, groundZ);
                setPos(getX(), groundY, getZ());
                setDeltaMovement(Vec3.ZERO);
                setNoGravity(true);
                damageDelayTimer = 0; // Reset delay timer for damage
                if (!level().isClientSide) {
                    playImpactEffects();
                }
            }
        }

        if (impactOccurred) {
            damageDelayTimer++;
            if (damageDelayTimer == 5) { // 0.25 seconds (5 ticks) delay
                // Apply damage in large AOE
                AABB aoeBox = getBoundingBox().inflate(10.0); // Large radius
                level().getEntities(this, aoeBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .forEach(target -> {
                            onHitEntity(new EntityHitResult(target));
                        });

                // Additional particles for large AOE
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 20, 5.0, 1.0, 5.0, 0.1);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 50, 5.0, 1.0, 5.0, 0.05);
                }

                // Play sound effect
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            deathTimer++;
            if (!level().isClientSide && deathTimer > 100) { // 5 seconds after impact
                setShouldRemove(true);
                discard();
            }
        }
    }
    private void handleProjectileSpell() {
        if (!impactOccurred) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.95, 0.99));

            // For Fire Bolt EX, apply damage in its path without stopping on entity hit
            if ("fire_bolt_ex".equals(getArtName())) {
                // The 3x1 line is represented by an inflated bounding box
                AABB damageBox = getBoundingBox().inflate(1.0, 0.5, 1.0);
                level().getEntities(this, damageBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                        .forEach(target -> onHitEntity(new EntityHitResult(target))); // Deal damage but don't stop
            }

            if (this.onGround() || this.horizontalCollision || this.verticalCollision) {
                impactOccurred = true;
                setHit(true);
                setDeltaMovement(Vec3.ZERO);
                setNoGravity(true); // Ensure it stops moving after impact
                if (!level().isClientSide) {
                    playImpactEffects();
                }
                deathTimer = 0;
            }

            // Standard single-target hit check for other projectiles
            if (!"fire_bolt_ex".equals(getArtName())) {
                AABB box = getBoundingBox().inflate(0.2);
                level().getEntities(this, box).stream()
                        .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                        .findFirst()
                        .ifPresent(target -> {
                            onHitEntity(new EntityHitResult(target));
                            impactOccurred = true;
                            setHit(true);
                            setDeltaMovement(Vec3.ZERO);
                            setNoGravity(true);
                            if (!level().isClientSide) {
                                if ("aqua_bleed".equals(getArtName())) playWaterImpactEffects(); else playImpactEffects();
                            }
                            deathTimer = 0;
                        });
            }
        }

        if (impactOccurred) {
            deathTimer++;
            if ("aqua_bleed".equals(getArtName()) && deathTimer >= 5 && deathTimer <= 10) {
                AABB damageBox = getBoundingBox().inflate(0.5D);
                level().getEntities(this, damageBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                        .forEach(target -> onHitEntity(new EntityHitResult(target)));
            }

            if (!level().isClientSide && deathTimer > 60) {
                setShouldRemove(true);
                discard();
            }
        }

        int maxFlightTicks = "stone_hammer".equals(getArtName()) || "fire_bolt".equals(getArtName()) ||"aqua_bleed".equals(getArtName()) ? 20 : 40;
        if (lifeTimer > maxFlightTicks && !impactOccurred) {
            if (!level().isClientSide) discard();
        }
    }
    private void handleBouncingProjectileSpell() {
        if (!impactOccurred) {
            // Apply movement
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Apply air friction
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.95, 0.99)); // Slightly more friction on Y for arc

            // Check for block collision
            if (this.onGround() || this.horizontalCollision || this.verticalCollision) {
                impactOccurred = true;
                setHit(true);
                setDeltaMovement(Vec3.ZERO);
                setNoGravity(true);

                // Apply AOE damage at full power (since it's block impact)
                AABB aoeBox = getBoundingBox().inflate(1.5); // 3x3 blocks
                level().getEntities(this, aoeBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .forEach(target -> {
                            LivingEntity owner = getOwner();
                            if (owner != null) {
                                target.hurt(level().damageSources().indirectMagic(this, owner), getDamage());
                            } else {
                                target.hurt(level().damageSources().magic(), getDamage());
                            }
                        });

                // Play AOE effects for the final explosion
                if (!level().isClientSide) {
                    playFinalImpactEffects();
                }

                deathTimer = 0;
            }

            // Check for entity collision
            if (!impactOccurred) {
                AABB box = getBoundingBox().inflate(0.2);
                level().getEntities(this, box).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .findFirst()
                        .ifPresent(target -> {
                            // Hit entity - cause full damage and stop
                            onHitEntity(new EntityHitResult(target));
                            impactOccurred = true;
                            setHit(true);
                            setDeltaMovement(Vec3.ZERO);
                            setNoGravity(true);

                            // Apply AOE damage
                            AABB aoeBox = getBoundingBox().inflate(1.5); // 3x3 blocks
                            level().getEntities(this, aoeBox).stream()
                                    .filter(e -> e instanceof LivingEntity t &&
                                            !t.getUUID().equals(ownerUUID))
                                    .forEach(t -> {
                                        LivingEntity owner = getOwner();
                                        if (owner != null) {
                                            t.hurt(level().damageSources().indirectMagic(this, owner), getDamage());
                                        } else {
                                            t.hurt(level().damageSources().magic(), getDamage());
                                        }
                                    });

                            // Play AOE effects
                            if (!level().isClientSide) {
                                playFinalImpactEffects();
                            }
                            deathTimer = 0;
                        });
            }
        }

        // Handle the lingering AOE effect after impact
        if (impactOccurred) {
            deathTimer++;
            if (!level().isClientSide && deathTimer > 60) { // 3 seconds linger
                setShouldRemove(true);
                discard();
            }
        }

        // Projectile timeout
        int maxFlightTicks = 80;
        if (lifeTimer > maxFlightTicks && !impactOccurred) {
            if (!level().isClientSide) {
                setShouldRemove(true);
                discard();
            }
        }
    }

    private void handleProjectileSpreadSpell() { // For Blue Impact
        if (!impactOccurred) {
            // Standard projectile movement
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.99, 0.95, 0.99));

            // Check for collision
            if (onGround() || horizontalCollision || verticalCollision) {
                impactOccurred = true;
                setHit(true);
                setDeltaMovement(Vec3.ZERO);
                if (!level().isClientSide) playWaterImpactEffects();
                deathTimer = 0;
            }

            AABB box = getBoundingBox().inflate(0.2);
            level().getEntities(this, box).stream()
                    .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                    .findFirst()
                    .ifPresent(target -> {
                        impactOccurred = true;
                        setHit(true);
                        setDeltaMovement(Vec3.ZERO);
                        if (!level().isClientSide) playWaterImpactEffects();
                        deathTimer = 0;
                    });
        } else {
            // After impact, start the bubble animation timer
            deathTimer++;
            if (deathTimer == 25) { // 1.25 seconds
                if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
                    // Cause suffocation damage
                    AABB damageBox = getBoundingBox().inflate(1.0);
                    level().getEntities(this, damageBox).stream()
                            .filter(e -> e instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                            .forEach(e -> e.hurt(level().damageSources().drown(), getDamage()));

                    // Spawn secondary projectiles
                    spawnSecondaryProjectiles(serverLevel);

                    // Burst effects
                    serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY(), getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                }
            }

            if (!level().isClientSide && deathTimer > 100) { // 5 seconds total lifetime
                discard();
            }
        }
    }

    private void spawnSecondaryProjectiles(ServerLevel level) {
        Vec3[] directions = {
                new Vec3(1, 0, 1).normalize(),   // NE
                new Vec3(1, 0, -1).normalize(),  // SE
                new Vec3(-1, 0, -1).normalize(), // SW
                new Vec3(-1, 0, 1).normalize()   // NW
        };

        for (Vec3 dir : directions) {
            GeckoSpellEntity secondary = new GeckoSpellEntity(level, getOwner(), getDamage() / 4, "aqua_bleed"); // Use a simple projectile
            secondary.setPos(getX(), getY(), getZ());
            secondary.setDeltaMovement(dir.scale(0.6)); // Slower speed
            level.addFreshEntity(secondary);
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void handleProjectileTrailSpell() { // For Diamond Dust
        if (level().isClientSide || playerStartPos == null || direction == null) return;
        ServerLevel serverLevel = (ServerLevel) level();

        setYRot(initialYaw + 180.0F);

        switch (spellPhase) {
            case 1: // Travelling phase (0 to 3.75s, or 75 ticks)
                if (lifeTimer <= 75) {
                    double progress = lifeTimer / 75.0;
                    double travelDistance = progress * 6.0;

                    // --- NEW ARC LOGIC ---
                    // 1. Define the parameters of the arc.
                    double startHeight = 3.0; // Starts 3 blocks above the ground.
                    double arcPeakHeight = 2.5; // It will go an additional 2.5 blocks up at its peak.

                    // 2. Calculate the vertical offset using a parabolic equation.
                    // This formula creates an arc that is 0 at the start and end of the travel time.
                    double time = lifeTimer;
                    double travelDuration = 75.0;
                    double yOffset = -4 * arcPeakHeight / (travelDuration * travelDuration) * time * (time - travelDuration);
                    // --- END NEW ARC LOGIC ---

                    // 3. Calculate the ground position for the particle trail (this remains the same)
                    Vec3 groundTrailPos = playerStartPos.add(direction.scale(travelDistance));
                    int trailGroundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(groundTrailPos.x), Mth.floor(groundTrailPos.z));

                    // 4. The visual model's position now includes the starting height and the arc offset.
                    Vec3 modelCurrentPos = new Vec3(groundTrailPos.x, trailGroundY + startHeight + yOffset, groundTrailPos.z);

                    // Collision check for the model, now using the arcing path
                    Vec3 nextModelPos = modelCurrentPos.add(direction.scale(0.2));
                    BlockPos collisionCheckPos = new BlockPos(Mth.floor(nextModelPos.x), Mth.floor(nextModelPos.y), Mth.floor(nextModelPos.z));
                    if (!level().getBlockState(collisionCheckPos).isAir()) {
                        setPos(getX(), trailGroundY, getZ()); // Drop to ground on collision
                        setHit(true);
                        spellPhase = 2;
                        lifeTimer = 76;
                        break;
                    }

                    setPos(modelCurrentPos);

                    // 5. Spawn particles on the ground below the arcing spell
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, groundTrailPos.x, trailGroundY + 0.1, groundTrailPos.z, 5, 0.2, 0.1, 0.2, 0.0);
                    AABB trailBox = new AABB(groundTrailPos.x - 0.5, trailGroundY, groundTrailPos.z - 0.5, groundTrailPos.x + 0.5, trailGroundY + 1.0, groundTrailPos.z + 0.5);
                    level().getEntitiesOfClass(LivingEntity.class, trailBox, e -> !e.getUUID().equals(ownerUUID))
                            .forEach(e -> e.setTicksFrozen(140));

                } else {
                    // End of travel: Position entity on the ground at the 6-block mark
                    Vec3 finalPos = playerStartPos.add(direction.scale(6.0));
                    int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(finalPos.x), Mth.floor(finalPos.z));
                    setPos(finalPos.x, groundY, finalPos.z);
                    setDeltaMovement(Vec3.ZERO);
                    setNoGravity(true);
                    setHit(true);
                    spellPhase = 2;
                }
                break;

            case 2: // Waiting for the first impact at 4s (tick 80)
                if (lifeTimer >= 80) {
                    playIceImpactEffects(1.0f);
                    AABB damageBox = getBoundingBox().inflate(0.5);
                    level().getEntities(this, damageBox, e -> e instanceof LivingEntity && !e.getUUID().equals(ownerUUID))
                            .forEach(e -> {
                                e.hurt(level().damageSources().magic(), getDamage());
                                if (e instanceof LivingEntity target) {
                                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                                }
                            });
                    spellPhase = 3;
                }
                break;

            case 3: // Waiting for the final pop at 5s (tick 100)
                if (lifeTimer >= 100) {
                    playIceImpactEffects(1.5f);
                    AABB aoeBox = getBoundingBox().inflate(1.5);
                    level().getEntities(this, aoeBox, e -> e instanceof LivingEntity && !e.getUUID().equals(ownerUUID))
                            .forEach(e -> e.hurt(level().damageSources().magic(), getDamage() / 2.0f));

                    spellPhase = 4;
                    setShouldRemove(true);
                }
                break;

            case 4: // Spell finished, waiting for removal
                if (lifeTimer > 101) {
                    discard();
                }
                break;
        }
    }

    private void handleStationarySpell() { // For Spiral Flare
        if (impactOccurred) { // Use flag to mean "finished"
            deathTimer++;
            if (deathTimer > 40) { // 2 second linger
                if (!level().isClientSide) {
                    setShouldRemove(true);
                    discard();
                }
            }
            return;
        }

        lifeTimer++;

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            if (lifeTimer % 20 == 1) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 1.5f);
            }

            // The model faces North (-Z) in Blockbench. The entity's rotation is set to the player's rotation.
            // To get the laser to fire from the model's front, we need the entity's forward vector.
            Vec3 direction = Vec3.directionFromRotation(0, this.getYRot());

            // The model is 2 blocks long. The entity's position is the model's origin (at its back).
            // The mouth is at the front of the model, so we move forward by the model's length.
            Vec3 mouthPos = position().add(0, 0.5, 0).add(direction.scale(2.0));

            double laserLength = 5.0;
            double spiralRadius = 0.5;
            double spiralFrequency = 4.0; // How many turns over the length

            // Start the laser just in front of the mouth (d=0.2)
            for (double d = 0.2; d < laserLength; d += 0.2) {
                Vec3 forward = direction.scale(d);
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 side = direction.cross(up).normalize();

                // Calculate spiral offset, animating over time
                double angle = (d * spiralFrequency) + (lifeTimer * 0.5);
                Vec3 offset = side.scale(Math.cos(angle) * spiralRadius).add(up.scale(Math.sin(angle) * spiralRadius));
                Vec3 particlePos = mouthPos.add(forward).add(offset);

                serverLevel.sendParticles(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);

                AABB damageBox = new AABB(particlePos.subtract(0.2, 0.2, 0.2), particlePos.add(0.2, 0.2, 0.2));
                level().getEntities(this, damageBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                        .forEach(target -> onHitEntity(new EntityHitResult(target)));
            }
        }

        if (lifeTimer > 100) { // Spell ends after 5 AT (100 ticks)
            impactOccurred = true;
            deathTimer = 0;
        }
    }

    private void handleVolcanicRaveSpell() {
        if (!impactOccurred) {
            impactOccurred = true;
            setHit(true);
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            deathTimer = 0;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 2.0f, 0.5f);
        }

        deathTimer++;
        if (!level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level();
            Vec3 origin = position();
            float yawRad = (float) Math.toRadians(getYRot());
            boolean lavaHitOccurred = false;

            // Time windows for lava eruptions (in ticks)
            if (deathTimer >= 5 && deathTimer < 10) { // 0.25 - 0.5s
                applyVolcanicDamage(serverLevel, origin, 0, 0, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 10 && deathTimer < 15) { // 0.5 - 0.75s
                applyVolcanicDamage(serverLevel, origin, -1, 1, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 15 && deathTimer < 20) { // 0.75 - 1.0s
                applyVolcanicDamage(serverLevel, origin, -2, 3, getDamage() / 4f, yawRad);
                applyVolcanicDamage(serverLevel, origin, -1, 3, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 20 && deathTimer < 25) { // 1.0 - 1.25s
                applyVolcanicDamage(serverLevel, origin, 0, 4, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 25 && deathTimer < 30) { // 1.25 - 1.5s
                applyVolcanicDamage(serverLevel, origin, 1, 3, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 30 && deathTimer < 35) { // 1.5 - 1.75s
                applyVolcanicDamage(serverLevel, origin, 2, 3, getDamage() / 4f, yawRad);
                applyVolcanicDamage(serverLevel, origin, 1, 1, getDamage() / 4f, yawRad);
                lavaHitOccurred = true;
            }
            if (deathTimer >= 35 && deathTimer < 40) { // 1.75 - 2.0s
                applyVolcanicDamage(serverLevel, origin, 2, 2, getDamage(), yawRad); // Final hit
                lavaHitOccurred = true;
            }

            // Apply movement slowdown inside the rhombus area if no lava is active
            if (!lavaHitOccurred) {
                AABB effectArea = getBoundingBox().inflate(3.0, 1.0, 5.0);
                level().getEntities(this, effectArea).stream()
                        .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                        .forEach(target -> ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1)));
            }
        }

        if (deathTimer > 100) { // Total duration and linger
            if (!level().isClientSide) {
                setShouldRemove(true);
                discard();
            }
        }
    }

    private Vec3 rotatePoint(Vec3 point, float yawRad) {
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double newX = point.x * cos - point.z * sin;
        double newZ = point.x * sin + point.z * cos;
        return new Vec3(newX, point.y, newZ);
    }

    private void applyVolcanicDamage(ServerLevel level, Vec3 origin, double relX, double relZ, float damage, float yawRad) {
        // Rotate the relative X/Z coordinates based on the entity's yaw
        Vec3 rotatedOffset = rotatePoint(new Vec3(relX, 0, relZ), -yawRad);
        Vec3 lavaPos = origin.add(rotatedOffset);

        level.sendParticles(ParticleTypes.LAVA, lavaPos.x, lavaPos.y, lavaPos.z, 20, 0.5, 0.2, 0.5, 1);
        level.sendParticles(ParticleTypes.SMOKE, lavaPos.x, lavaPos.y, lavaPos.z, 10, 0.5, 0.5, 0.5, 0.1);

        AABB damageBox = new AABB(lavaPos.subtract(0.5, 0, 0.5), lavaPos.add(0.5, 1, 0.5));
        level.getEntities(this, damageBox).stream()
                .filter(entity -> entity instanceof LivingEntity target && !target.getUUID().equals(ownerUUID))
                .forEach(target -> {
                    LivingEntity owner = getOwner();
                    if(owner != null) {
                        target.hurt(level.damageSources().indirectMagic(this, owner), damage);
                    } else {
                        target.hurt(level.damageSources().lava(), damage);
                    }
                });
    }

    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                target.hurt(level().damageSources().indirectMagic(this, owner), getDamage());
            } else {
                target.hurt(level().damageSources().magic(), getDamage());
            }
        }
    }

    private void playFireImpactEffects() {
        if (level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level();
        String art = getArtName();

        switch (art) {
            case "fire_bolt" -> {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
                serverLevel.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
            case "flare_arrow" -> {
                // "Showers an enemy with searing flames" effect
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.5f);
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 50, 1.0, 1.0, 1.0, 0.15);
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 25, 1.0, 1.0, 1.0, 0.1);
            }
            case "fire_bolt_ex" -> {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.5, 0.5, 0.5, 0.1);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 30, 1.0, 1.0, 1.0, 0.15);
            }
        }
    }

    // Now, MODIFY the existing playImpactEffects method to call our new one.
    private void playImpactEffects() {
        if (level().isClientSide) return;

        String art = getArtName();
        // --- ADD THIS BLOCK to the top of the method ---
        if (art.startsWith("fire_")) {
            playFireImpactEffects();
            return;
        }
        // --- END OF ADDITION ---

        // Check for water spells
        if (art.equals("aqua_bleed") || art.equals("blue_impact")) { //
            playWaterImpactEffects();
            return;
        }

        SoundEvent sound = switch (art) { //

            case "stone_hammer" -> SoundEvents.ANVIL_BREAK;
            case "earth_lance" -> SoundEvents.GLASS_BREAK;
            case "titanic_roar" -> SoundEvents.ROOTED_DIRT_BREAK; // Custom sound for roar
            default -> SoundEvents.GENERIC_HURT;
        };

        level().playSound(null, getX(), getY(), getZ(), sound,
                SoundSource.PLAYERS, 1.0f, 1.0f);

        // Add vanilla particle effects on impact
        if (level() instanceof ServerLevel serverLevel) {
            switch (art) {
                case "stone_hammer" -> {
                    // Dust and smoke for stone impact
                    serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                case "earth_lance" -> {
                    // Crit and ash for lance
                    serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 30, 0.5, 1.0, 0.5, 0.2);
                    serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), 15, 0.4, 0.4, 0.4, 0.1);
                }
                case "titanic_roar" -> {
                    // Explosion particles for large AOE
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 50, 5.0, 1.0, 5.0, 0.1);
                }
                default -> {
                    // Default explosion particles
                    serverLevel.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }

    private void playWaterImpactEffects() {
        if (level().isClientSide) return;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS, 1.0f, 1.2f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 30, 0.5, 0.5, 0.5, 0.2);
            serverLevel.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private void playIceImpactEffects(float volume) {
        if (level().isClientSide) return;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, volume, 1.5f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 50, 1.0, 1.0, 1.0, 0.1);
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }
    }

    private void playFinalImpactEffects() {
        if (level().isClientSide) return;

        String art = getArtName();
        // MODIFY the 'if' condition
        if ("stone_impact".equals(art) || "napalm_breath".equals(art)) {
            // --- ADD THIS 'if' BLOCK for Napalm Breath ---
            if ("napalm_breath".equals(art)) {
                // "Crimson flames that erupt from below" effect
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.7f);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 2, 0, 0, 0, 0);
                    // Spawn particles at ground level that move upwards
                    for (int i = 0; i < 50; i++) {
                        double radius = 2.5;
                        double x = getX() + random.nextGaussian() * radius;
                        double z = getZ() + random.nextGaussian() * radius;
                        serverLevel.sendParticles(ParticleTypes.FLAME, x, getY() + 0.1, z, 1, 0, 0.5, 0, 0.05);
                    }
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 40, 2.5, 1.0, 2.5, 0.05);
                }
            } else { // stone_impact
                playImpactEffects();
            }
        }
    }

    public void setRotationFromLook(Vec3 look) {
        double horizontalDistance = Math.sqrt(look.x * look.x + look.z * look.z);
        float yaw = (float) (Math.atan2(-look.x, look.z) * (180.0 / Math.PI));
        float pitch = (float) (Math.atan2(-look.y, horizontalDistance) * (180.0 / Math.PI));

        setYRot(yaw);
        setXRot(pitch);
        setRot(yaw, pitch);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar regs) {
        controller = new AnimationController<>(this, "spellController", 2, state -> {
            String art = getArtName();
            if (art.isEmpty()) return PlayState.STOP;
            var def = getArtDefinition();
            if (def == null) return PlayState.STOP;

            if (!animationStarted) {
                String animKey = "animation." + KisekiLegend.MOD_ID + "." + art + "_cast";
                // MODIFY this switch statement
                boolean hold = switch (art) {
                    case "earth_lance", "titanic_roar", "stone_impact", "petrify_breath",
                         "aqua_bleed", "blue_impact", "diamond_dust" -> true; //
                    // ADD THESE CASES
                    case "fire_bolt", "flare_arrow", "napalm_breath", "fire_bolt_ex", "spiral_flare" -> true;
                    default -> false;
                };
                Animation.LoopType loopType = hold ? HOLD_ON_LAST_FRAME : PLAY_ONCE;
                state.getController().setAnimation(RawAnimation.begin().then(animKey, loopType));
                animationStarted = true;
            }

            return PlayState.CONTINUE;
        });

        regs.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object entity) {
        return this.tickCount;
    }

    @Override
    public ItemStack getItem() {
        return Items.FIRE_CHARGE.getDefaultInstance(); // Visual placeholder
    }

    // NBT serialization
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("ArtName", getArtName());
        tag.putInt("Damage", getDamage());
        tag.putBoolean("Hit", isHit());
        tag.putBoolean("ShouldRemove", shouldRemove());
        tag.putBoolean("Positioned", isPositioned());
        tag.putInt("LifeTimer", lifeTimer);
        tag.putInt("DeathTimer", deathTimer);
        tag.putInt("MaxLifetime", maxLifetimeTicks);
        tag.putBoolean("AnimationStarted", animationStarted);
        tag.putBoolean("ImpactOccurred", impactOccurred);
        tag.putInt("DamageDelayTimer", damageDelayTimer);
        tag.putBoolean("GroundImpacted", groundImpacted);
        tag.putBoolean("HalfAOE", halfAOE);
        tag.putInt("SpellPhase", spellPhase);
        if (playerStartPos != null) {
            tag.putDouble("PlayerStartX", playerStartPos.x);
            tag.putDouble("PlayerStartY", playerStartPos.y);
            tag.putDouble("PlayerStartZ", playerStartPos.z);
        }
        if (direction != null) {
            tag.putDouble("DirX", direction.x);
            tag.putDouble("DirY", direction.y);
            tag.putDouble("DirZ", direction.z);
        }
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if ("diamond_dust".equals(getArtName())) {
            tag.putFloat("InitialYaw", initialYaw);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setArtName(tag.getString("ArtName"));
        setDamage(tag.getInt("Damage"));
        setHit(tag.getBoolean("Hit"));
        setShouldRemove(tag.getBoolean("ShouldRemove"));
        setPositioned(tag.getBoolean("Positioned"));
        lifeTimer = tag.getInt("LifeTimer");
        deathTimer = tag.getInt("DeathTimer");
        maxLifetimeTicks = tag.getInt("MaxLifetime");
        animationStarted = tag.getBoolean("AnimationStarted");
        impactOccurred = tag.getBoolean("ImpactOccurred");
        damageDelayTimer = tag.getInt("DamageDelayTimer");
        groundImpacted = tag.getBoolean("GroundImpacted");
        halfAOE = tag.getBoolean("HalfAOE");
        spellPhase = tag.getInt("SpellPhase");
        if (tag.contains("PlayerStartX")) {
            playerStartPos = new Vec3(tag.getDouble("PlayerStartX"), tag.getDouble("PlayerStartY"), tag.getDouble("PlayerStartZ"));
        }
        if (tag.contains("DirX")) {
            direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        }
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        if ("diamond_dust".equals(getArtName())) {
            initialYaw = tag.getFloat("InitialYaw");
        }
    }

    // Spawn data for client-server sync
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf(getArtName(), 32);
        buffer.writeInt(getDamage());
        if (ownerUUID != null) {
            buffer.writeBoolean(true);
            buffer.writeUUID(ownerUUID);
        } else {
            buffer.writeBoolean(false);
        }
        if (getArtName().equals("diamond_dust")) {
            if (playerStartPos != null) {
                buffer.writeBoolean(true);
                buffer.writeDouble(playerStartPos.x);
                buffer.writeDouble(playerStartPos.y);
                buffer.writeDouble(playerStartPos.z);
            } else {
                buffer.writeBoolean(false);
            }
            if (direction != null) {
                buffer.writeBoolean(true);
                buffer.writeDouble(direction.x);
                buffer.writeDouble(direction.y);
                buffer.writeDouble(direction.z);
            } else {
                buffer.writeBoolean(false);
            }
            buffer.writeFloat(initialYaw);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setArtName(buffer.readUtf(32));
        setDamage(buffer.readInt());
        if (buffer.readBoolean()) {
            ownerUUID = buffer.readUUID();
        }
        if (getArtName().equals("diamond_dust")) {
            if (buffer.readBoolean()) {
                playerStartPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            }
            if (buffer.readBoolean()) {
                direction = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            }
            initialYaw = buffer.readFloat();
        }
    }
}
