package net.JordanRiver.KisekiLegend.entity;

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
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private AnimationController<GeckoSpellEntity> controller;

    private int lifeTimer = 0;
    private int deathTimer = 0;
    private boolean animationStarted = false;
    private boolean impactOccurred = false;
    private UUID ownerUUID;
    private int maxLifetimeTicks = 100; // Default 5 seconds

    public GeckoSpellEntity(EntityType<? extends GeckoSpellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true; // Prevent culling issues
    }

    public GeckoSpellEntity(Level level, LivingEntity shooter, int damage, String artName) {
        this(KisekiLegend.SPELL.get(), level);
        this.ownerUUID = shooter.getUUID();
        setDamage(damage);
        setArtName(artName);

        // Set appropriate lifetime based on spell type
        var artDef = getArtDefinition();
        if (artDef != null) {
            this.maxLifetimeTicks = artDef.style() == SpawnStyle.GROUND ? 80 : 120; // 4s ground, 6s projectile
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ART, "");
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_HIT, false);
        builder.define(DATA_SHOULD_REMOVE, false);
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
                case "stone_hammer" -> serverLevel.sendParticles(ParticleTypes.DUST_PLUME, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                case "earth_lance" -> serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                default -> serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Handle different spell behaviors
        if (artDef.style() == SpawnStyle.GROUND) {
            handleGroundSpell();
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
        int maxFlightTicks = "stone_hammer".equals(getArtName()) ? 40 : 80;
        if (lifeTimer > maxFlightTicks && !impactOccurred) { // Shorter range for stone_hammer
            if (!level().isClientSide) {
                setShouldRemove(true);
                discard();
            }
        }
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
            default -> SoundEvents.GENERIC_HURT ;
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
                default -> {
                    // Default explosion particles
                    serverLevel.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                }
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
                Animation.LoopType loopType = "earth_lance".equals(art) ? HOLD_ON_LAST_FRAME : PLAY_ONCE;
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
        tag.putInt("LifeTimer", lifeTimer);
        tag.putInt("DeathTimer", deathTimer);
        tag.putInt("MaxLifetime", maxLifetimeTicks);
        tag.putBoolean("AnimationStarted", animationStarted);
        tag.putBoolean("ImpactOccurred", impactOccurred);

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
        lifeTimer = tag.getInt("LifeTimer");
        deathTimer = tag.getInt("DeathTimer");
        maxLifetimeTicks = tag.getInt("MaxLifetime");
        animationStarted = tag.getBoolean("AnimationStarted");
        impactOccurred = tag.getBoolean("ImpactOccurred");

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
        buffer.writeInt(lifeTimer);
        buffer.writeBoolean(impactOccurred);

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
        lifeTimer = buffer.readInt();
        impactOccurred = buffer.readBoolean();

        // Read owner UUID
        if (buffer.readBoolean()) {
            ownerUUID = buffer.readUUID();
        }
    }
}