package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.CastScheduler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.UUID;

public class AuraEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(AuraEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public AuraEntity(EntityType<? extends AuraEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public AuraEntity(Level level, Player player) {
        this(ModEntities.AURA_ENTITY.get(), level);
        this.entityData.set(OWNER_UUID, player.getUUID().toString());
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)player.getX(), (int)player.getZ());
        setPos(player.getX(), groundY, player.getZ());
        System.out.println("AuraEntity created for player: " + player.getName().getString() + " at " + this.position());
    }

    public AuraEntity(Level level, double x, double y, double z) {
        super(ModEntities.AURA_ENTITY.get(), level);
        this.setPos(x, y, z);
    }

    public UUID getOwnerUUID() {
        String str = this.getEntityData().get(OWNER_UUID);
        return str.isEmpty() ? null : UUID.fromString(str);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, "");
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            // On client side, just update position to follow owner
            String ownerUuidStr = entityData.get(OWNER_UUID);
            if (!ownerUuidStr.isEmpty()) {
                try {
                    UUID ownerUuid = UUID.fromString(ownerUuidStr);
                    Player owner = level().getPlayerByUUID(ownerUuid);
                    if (owner != null) {
                        int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)owner.getX(), (int)owner.getZ());
                        setPos(owner.getX(), groundY + 0.1, owner.getZ());
                        setRot(owner.getYRot(), 0);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid UUID string for AuraEntity: " + ownerUuidStr);
                }
            }
            return;
        }

        // Server side logic
        if (level() == null || isRemoved()) {
            return;
        }

        String ownerUuidStr = entityData.get(OWNER_UUID);
        UUID ownerUuid = ownerUuidStr.isEmpty() ? null : UUID.fromString(ownerUuidStr);
        Player owner = ownerUuid != null ? level().getPlayerByUUID(ownerUuid) : null;

        if (owner == null || !CastScheduler.hasPendingCast(ownerUuid)) {
            System.out.println("AuraEntity discarding - owner: " + (owner != null) + ", hasPendingCast: " + (ownerUuid != null && CastScheduler.hasPendingCast(ownerUuid)));
            discard();
            return;
        }

        // Update position to follow owner
        double ownerX = owner.getX();
        double ownerZ = owner.getZ();
        if (!Double.isNaN(ownerX) && !Double.isNaN(ownerZ)) {
            int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)ownerX, (int)ownerZ);
            setPos(owner.getX(), groundY + 0.1, owner.getZ());
            setRot(owner.getYRot(), 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar regs) {
        regs.add(new AnimationController<>(this, "auraController", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.kisekilegend.aura_pulse", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("OwnerUUID")) {
            entityData.set(OWNER_UUID, tag.getString("OwnerUUID"));
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putString("OwnerUUID", entityData.get(OWNER_UUID));
    }

    // Add this method to ensure proper spawning
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (OWNER_UUID.equals(key)) {
            System.out.println("AuraEntity owner UUID synced: " + entityData.get(OWNER_UUID));
        }
    }
}