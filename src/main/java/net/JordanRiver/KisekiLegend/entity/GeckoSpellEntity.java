package net.JordanRiver.KisekiLegend.entity;
import net.minecraft.core.Direction;
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
import net.minecraft.util.Mth;

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

    public GeckoSpellEntity(EntityType<? extends GeckoSpellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true; // Prevent culling issues
    }

    public GeckoSpellEntity(Level level, LivingEntity shooter, int damage, String artName) {
        this(ModEntities.SPELL.get(), level);
        this.ownerUUID = shooter.getUUID();
        setDamage(damage);
        setArtName(artName);

        // Set appropriate lifetime based on spell type
        var artDef = getArtDefinition();
        if (artDef != null) {
            this.maxLifetimeTicks = artDef.style() == SpawnStyle.GROUND ? 80 : 120; // 4s ground, 6s projectile
        }
        // Special positioning for petrify breath
        if ("petrify_breath".equals(artName) && shooter instanceof Player player) {
            positionPetrifyBreath(player);
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

        // Safety check - remove if no art name
        if (getArtName().isEmpty()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        var artDef = getArtDefinition();
        if (artDef == null) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        lifeTimer++;

        // Hard timeout - prevent entities from living forever
        if (lifeTimer > maxLifetimeTicks) {
            if (!level().isClientSide) {
                setShouldRemove(true);
                discard();
            }
            return;
        }

        // Handle removal flag
        if (shouldRemove()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        // Add trailing particles every few ticks
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel && random.nextInt(3) == 0) {
            String art = getArtName();
            switch (art) {
                case "stone_hammer" ->
                        serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "earth_lance" ->
                        serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "stone_impact" ->
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                default ->
                        serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Handle different spell behaviors
        // Handle different spell behaviors
        if (artDef.style() == SpawnStyle.GROUND) {
            if ("petrify_breath".equals(getArtName())) {
                handlePetrifyBreathSpell();
            } else {
                handleGroundSpell();
            }
        } else if (artDef.style() == SpawnStyle.AOE_CENTERED) {
            handleAOECenteredSpell();
        } else if (artDef.style() == SpawnStyle.BOUNCING_PROJECTILE) {
            handleBouncingProjectileSpell();
        } else {
            handleProjectileSpell();
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
                // For earth_lance, damage in a straight line
                Vec3 direction = Vec3.directionFromRotation(0, getYRot()).normalize();
                double lineLength = 5.0;
                double lineWidth = 1.5; // Half-width for the box

                Vec3 start = position();
                Vec3 end = start.add(direction.scale(lineLength));

                // Create a thin long AABB along the line
                AABB lineBox = new AABB(
                        Math.min(start.x, end.x) - lineWidth,
                        Math.min(start.y, end.y) - lineWidth,
                        Math.min(start.z, end.z) - lineWidth,
                        Math.max(start.x, end.x) + lineWidth,
                        Math.max(start.y, end.y) + lineWidth,
                        Math.max(start.z, end.z) + lineWidth
                );

                level().getEntities(this, lineBox).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .forEach(target -> {
                            onHitEntity(new EntityHitResult(target));
                        });
            } else {
                // Default for other ground spells
                AABB box = getBoundingBox().inflate(1.0);
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
    private void positionPetrifyBreath(Player owner) {
        // Get the block position directly in front of the player based on their facing direction
        Vec3 playerPos = owner.position();
        Direction facing = owner.getDirection(); // Gets the cardinal direction the player is facing

        // Get the block position 1 block in front of the player
        Vec3 offsetPos = switch (facing) {
            case NORTH -> playerPos.add(0, 0, -1);
            case SOUTH -> playerPos.add(0, 0, 1);
            case WEST -> playerPos.add(-1, 0, 0);
            case EAST -> playerPos.add(1, 0, 0);
            default -> playerPos.add(0, 0, -1);
        };

        // Snap to block grid center
        int blockX = Mth.floor(offsetPos.x);
        int blockZ = Mth.floor(offsetPos.z);
        int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);

        // Apply to entity - centered on the block
        setPos(blockX + 0.5, groundY, blockZ + 0.5);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setPositioned(true);

        // Set the entity's rotation to match the player's facing direction
        setYRot(owner.getYRot());

        // Trigger instant impact
        impactOccurred = true;
        setHit(true);
    }


    private void handlePetrifyBreathSpell() {
        // For petrify breath, we position immediately in constructor
        // so we just need to handle the animation and damage timing

        if (!impactOccurred) {
            // This should not happen for petrify breath as it's positioned immediately
            // But keep as fallback
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
                // Apply 2x2 AOE damage
                AABB aoeBox = getBoundingBox().inflate(1.0); // 2x2 area
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
            // Apply movement
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Apply air friction
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.95, 0.99)); // Slightly more friction on Y for arc

            // Check for block collision
            if (this.onGround() || this.horizontalCollision || this.verticalCollision) {
                impactOccurred = true;
                setHit(true);
                setDeltaMovement(Vec3.ZERO);
                if (!level().isClientSide) {
                    playImpactEffects();
                }
                deathTimer = 0;
            }

            // Check for entity collision - single target for projectiles like stone_hammer
            AABB box = getBoundingBox().inflate(0.2);
            level().getEntities(this, box).stream()
                    .filter(entity -> entity instanceof LivingEntity target &&
                            !target.getUUID().equals(ownerUUID))
                    .findFirst()
                    .ifPresent(target -> {
                        onHitEntity(new EntityHitResult(target));
                        impactOccurred = true;
                        if (!level().isClientSide) {
                            playImpactEffects();
                        }
                        deathTimer = 0;
                    });
        }

        // After impact, start countdown
        if (impactOccurred) {
            deathTimer++;
            if (!level().isClientSide && deathTimer > 60) { // 3 seconds after impact
                setShouldRemove(true);
                discard();
            }
        }

        // Projectile timeout - shorter for stone_hammer
        int maxFlightTicks = "stone_hammer".equals(getArtName()) ? 20 : 40;
        if (lifeTimer > maxFlightTicks && !impactOccurred) { // Shorter range for stone_hammer
            if (!level().isClientSide) {
                setShouldRemove(true);
                discard();
            }
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
                if (!groundImpacted) {
                    // First block impact - just bounce, no explosion
                    halfAOE = false;
                    bounceToGround();
                    // Don't set impactOccurred = true here, let it continue moving
                } else {
                    // Second block impact - explode
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
            }

            // Check for entity collision
            if (!impactOccurred && !groundImpacted) {
                AABB box = getBoundingBox().inflate(0.2);
                level().getEntities(this, box).stream()
                        .filter(entity -> entity instanceof LivingEntity target &&
                                !target.getUUID().equals(ownerUUID))
                        .findFirst()
                        .ifPresent(target -> {
                            // Hit entity - cause half damage, then bounce to ground
                            onHitEntity(new EntityHitResult(target));
                            halfAOE = true;
                            bounceToGround();
                            impactOccurred = true; // End the projectile phase after entity hit
                        });
            }
        }

        // Handle the lingering AOE effect after impact
        if (impactOccurred && groundImpacted) {
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

    private void bounceToGround() {
        Vec3 hitPos = position();
        int gx = Mth.floor(hitPos.x);
        int gz = Mth.floor(hitPos.z);
        int gy = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);

        setPos(hitPos.x, gy, hitPos.z);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);

        // Play bounce effects
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // If this bounce was after hitting an entity, apply half AOE damage
        if (halfAOE) {
            int aoeDamage = getDamage() / 2;
            AABB aoeBox = getBoundingBox().inflate(1.5); // 3x3 blocks
            level().getEntities(this, aoeBox).stream()
                    .filter(entity -> entity instanceof LivingEntity target &&
                            !target.getUUID().equals(ownerUUID))
                    .forEach(target -> {
                        LivingEntity owner = getOwner();
                        if (owner != null) {
                            target.hurt(level().damageSources().indirectMagic(this, owner), aoeDamage);
                        } else {
                            target.hurt(level().damageSources().magic(), aoeDamage);
                        }
                    });

            // Play AOE effects for entity bounce
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 10, 1.0, 0.5, 1.0, 0.1);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20, 1.0, 0.5, 1.0, 0.05);
                }
            }
        }

        groundImpacted = true;
        deathTimer = 0;
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

    private void playImpactEffects() {
        if (level().isClientSide) return;

        String art = getArtName();
        SoundEvent sound = switch (art) {
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

    // New method specifically for stone_impact final explosion
    private void playFinalImpactEffects() {
        if (level().isClientSide) return;

        String art = getArtName();
        if ("stone_impact".equals(art)) {
            // Play stone impact explosion sound
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);

            // Add explosion particles for stone impact
            if (level() instanceof ServerLevel serverLevel) {
                // Main explosion effect
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 15, 1.0, 0.5, 1.0, 0.1);
                // Stone debris
                serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 25, 1.5, 1.0, 1.5, 0.2);
                // Falling dust for stone chunks
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY(), getZ(), 30, 1.0, 1.0, 1.0, 0.1);
                // Smoke cloud
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20, 1.0, 0.5, 1.0, 0.05);
            }
        } else {
            // Fallback to regular impact effects for other spells
            playImpactEffects();
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
            if (art.isEmpty()) {
                return PlayState.STOP;
            }

            var def = getArtDefinition();
            if (def == null) {
                return PlayState.STOP;
            }

            // Force start animation if not started
            if (!animationStarted) {
                String animKey = "animation." + KisekiLegend.MOD_ID + "." + art + "_cast";
                Animation.LoopType loopType = ("earth_lance".equals(art) || "titanic_roar".equals(art) || "stone_impact".equals(art) || "petrify_breath".equals(art)) ? HOLD_ON_LAST_FRAME : PLAY_ONCE;
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

        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
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

        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
    }


    // Spawn data for client-server sync
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf(getArtName(), 32);
        buffer.writeInt(getDamage());
        buffer.writeBoolean(isHit());
        buffer.writeBoolean(shouldRemove());
        buffer.writeBoolean(isPositioned());
        buffer.writeInt(lifeTimer);
        buffer.writeBoolean(impactOccurred);
        buffer.writeInt(damageDelayTimer);
        buffer.writeBoolean(groundImpacted);
        buffer.writeBoolean(halfAOE);

        // Write owner UUID
        if (ownerUUID != null) {
            buffer.writeBoolean(true);
            buffer.writeUUID(ownerUUID);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setArtName(buffer.readUtf(32));
        setDamage(buffer.readInt());
        setHit(buffer.readBoolean());
        setShouldRemove(buffer.readBoolean());
        setPositioned(buffer.readBoolean());
        lifeTimer = buffer.readInt();
        impactOccurred = buffer.readBoolean();
        damageDelayTimer = buffer.readInt();
        groundImpacted = buffer.readBoolean();
        halfAOE = buffer.readBoolean();

        // Read owner UUID
        if (buffer.readBoolean()) {
            ownerUUID = buffer.readUUID();
        }
    }
}