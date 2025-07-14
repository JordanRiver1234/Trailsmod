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

public class MagicCircleEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public MagicCircleEntity(EntityType<? extends MagicCircleEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public MagicCircleEntity(Level level, Player player) {
        this(ModEntities.MAGIC_CIRCLE_ENTITY.get(), level);
        this.entityData.set(OWNER_UUID, player.getUUID().toString());
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)player.getX(), (int)player.getZ());
        setPos(player.getX(), groundY, player.getZ());
        System.out.println("MagicCircleEntity created for player: " + player.getName().getString() + " at " + this.position());
    }

    public MagicCircleEntity(Level level, double x, double y, double z) {
        super(ModEntities.MAGIC_CIRCLE_ENTITY.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, "");
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            // Client-side: Just follow the owner without expensive calculations
            String ownerUuidStr = entityData.get(OWNER_UUID);
            if (!ownerUuidStr.isEmpty()) {
                try {
                    UUID ownerUuid = UUID.fromString(ownerUuidStr);
                    Player owner = level().getPlayerByUUID(ownerUuid);
                    if (owner != null) {
                        // Simple position following on client
                        setPos(owner.getX(), owner.getY(), owner.getZ());
                        setRot(owner.getYRot(), 0);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid UUID string for MagicCircleEntity: " + ownerUuidStr);
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
            System.out.println("MagicCircleEntity discarding - owner: " + (owner != null) + ", hasPendingCast: " + (ownerUuid != null && CastScheduler.hasPendingCast(ownerUuid)));
            discard();
            return;
        }

        // Update position to follow owner (server authoritative)
        int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)owner.getX(), (int)owner.getZ());
        setPos(owner.getX(), groundY, owner.getZ());
        setRot(owner.getYRot(), 0);

        // Force synchronization to client
        if (tickCount % 5 == 0) { // Sync every 5 ticks
            level().broadcastEntityEvent(this, (byte) 1);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar regs) {
        regs.add(new AnimationController<>(this, "circleController", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.kisekilegend.magic_circle", Animation.LoopType.LOOP));
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

    // Add visibility and culling methods
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0; // Render within 64 blocks
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}