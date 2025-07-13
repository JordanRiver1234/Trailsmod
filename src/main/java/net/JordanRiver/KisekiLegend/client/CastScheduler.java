package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.JordanRiver.KisekiLegend.entity.MagicCircleEntity;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry.ArtDefinition;
import net.JordanRiver.KisekiLegend.orbal.SpawnStyle;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CastScheduler {
    private record PendingCast(UUID playerId, ArtDefinition art, int ticksLeft, long scheduleTime, AuraEntity aura, MagicCircleEntity circle) {}

    private static final Map<UUID, PendingCast> PENDING = new ConcurrentHashMap<>();
    private static final int MAX_SPELL_LIFETIME = 12000; // 10 minutes max
    private static int cleanupTimer = 0;

    public static void scheduleCast(Player player, ArtDefinition art) {
        if (player == null || art == null) {
            System.out.println("Failed to schedule cast: player or art is null");
            return;
        }
        if (!(player.level() instanceof ServerLevel)) {
            System.out.println("Cannot schedule cast: not on server side");
            return;
        }
        // Rest of the method remains unchanged

        // Cancel any existing cast
        cancelCast(player.getUUID());

        // Play casting sound
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                0.8f, 1.2f
        );

        // Spawn aura and magic circle entities
        AuraEntity aura = null;
        MagicCircleEntity circle = null;
        if (player.level() instanceof ServerLevel serverLevel) {
            aura = new AuraEntity(serverLevel, player);
            circle = new MagicCircleEntity(serverLevel, player);
            if (serverLevel.addFreshEntity(aura)) {
                System.out.println("Successfully added AuraEntity " + aura.getId() + " at " + aura.position());
            } else {
                System.out.println("Failed to add AuraEntity for player: " + player.getName().getString());
            }
            if (serverLevel.addFreshEntity(circle)) {
                System.out.println("Successfully added MagicCircleEntity " + circle.getId() + " at " + circle.position());
            } else {
                System.out.println("Failed to add MagicCircleEntity for player: " + player.getName().getString());
            }
        } else {
            System.out.println("Failed to spawn entities: level is not ServerLevel");
            return;
        }

        PendingCast pendingCast = new PendingCast(
                player.getUUID(),
                art,
                Math.max(1, art.getCastDelayTicks()),
                System.currentTimeMillis(),
                aura,
                circle
        );

        PENDING.put(player.getUUID(), pendingCast);

        System.out.println("Scheduled cast for " + player.getName().getString() +
                ": " + art.name() + " in " + art.getCastDelayTicks() + " ticks");
    }
    public static void cancelCast(UUID playerId) {
        synchronized (PENDING) {
            PendingCast removed = PENDING.remove(playerId);
            if (removed != null) {
                if (removed.aura != null) {
                    removed.aura.discard();
                    System.out.println("Discarded AuraEntity for player: " + playerId);
                }
                if (removed.circle != null) {
                    removed.circle.discard();
                    System.out.println("Discarded MagicCircleEntity for player: " + playerId);
                }
                System.out.println("Cancelled pending cast for player: " + playerId);
            }
        }
    }

    public static boolean hasPendingCast(UUID playerId) {
        return PENDING.containsKey(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cancelCast(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        cancelCast(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        cleanupTimer++;
        if (cleanupTimer >= 100) {
            cleanupTimer = 0;
            cleanupOrphanedEntities(event.getServer());
            cleanupStaleCasts();
        }

        synchronized (PENDING) {
            Iterator<Map.Entry<UUID, PendingCast>> iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, PendingCast> entry = iterator.next();
                PendingCast pendingCast = entry.getValue();

                int remainingTicks = pendingCast.ticksLeft - 1;

                ServerPlayer player = event.getServer().getPlayerList().getPlayer(pendingCast.playerId);
                if (player != null && player.serverLevel() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 5; i++) {
                        double offsetX = serverLevel.random.nextGaussian() * 0.2;
                        double offsetY = 0.5 + serverLevel.random.nextGaussian() * 0.1;
                        double offsetZ = serverLevel.random.nextGaussian() * 0.2;
                        serverLevel.sendParticles(ModParticles.BLUE_FLOW.get(), player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ, 1, 0.0, 0.1, 0.0, 0.0);
                    }
                    // Verify entities are still alive
                    if (pendingCast.aura != null && !pendingCast.aura.isAlive()) {
                        System.out.println("AuraEntity is not alive for player: " + pendingCast.playerId);
                    }
                    if (pendingCast.circle != null && !pendingCast.circle.isAlive()) {
                        System.out.println("MagicCircleEntity is not alive for player: " + pendingCast.playerId);
                    }
                    System.out.println("Processing cast for player: " + pendingCast.playerId + ", ticks left: " + remainingTicks + ", aura alive: " + (pendingCast.aura != null && pendingCast.aura.isAlive()) + ", circle alive: " + (pendingCast.circle != null && pendingCast.circle.isAlive()));
                } else {
                    System.out.println("Player not found or invalid level for pending cast: " + pendingCast.playerId);
                }

                if (remainingTicks <= 0) {
                    iterator.remove();
                    if (pendingCast.aura != null) {
                        pendingCast.aura.discard();
                        System.out.println("Discarded AuraEntity at cast execution for player: " + pendingCast.playerId);
                    }
                    if (pendingCast.circle != null) {
                        pendingCast.circle.discard();
                        System.out.println("Discarded MagicCircleEntity at cast execution for player: " + pendingCast.playerId);
                    }
                    executeCast(event.getServer(), pendingCast);
                } else {
                    entry.setValue(new PendingCast(
                            pendingCast.playerId,
                            pendingCast.art,
                            remainingTicks,
                            pendingCast.scheduleTime,
                            pendingCast.aura,
                            pendingCast.circle
                    ));
                }
            }
        }
    }

    private static void executeCast(net.minecraft.server.MinecraftServer server, PendingCast pendingCast) {
        ServerPlayer player = server.getPlayerList().getPlayer(pendingCast.playerId);
        if (player == null) {
            System.out.println("Cannot execute cast - player not found: " + pendingCast.playerId);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level == null) {
            System.out.println("Cannot execute cast - player level is null");
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof OrbmentItem)) {
            System.out.println("Cannot execute cast - player no longer has orbment equipped");
            return;
        }

        int damage = 1;
        try {
            String powerStr = pendingCast.art.power();
            if (powerStr != null && !powerStr.isEmpty()) {
                damage = Integer.parseInt(powerStr);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid damage value for art: " + pendingCast.art.name());
        }

        String artKey = pendingCast.art.name().toLowerCase().replace(' ', '_');
        GeckoSpellEntity spell = new GeckoSpellEntity(level, player, damage, artKey);

        Vec3 lookVector = player.getLookAngle();

        if (pendingCast.art.style() == SpawnStyle.GROUND) {
            Vec3 spawnPos = player.position().add(lookVector.scale(2.5));
            int groundX = Mth.floor(spawnPos.x);
            int groundZ = Mth.floor(spawnPos.z);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, groundX, groundZ);

            spell.setPos(spawnPos.x, groundY, spawnPos.z);
            spell.setDeltaMovement(Vec3.ZERO);
            spell.setNoGravity(true);
            float playerYaw = Mth.wrapDegrees(player.getYRot());
            if (artKey.equals("earth_lance")) {
                if (Math.abs(playerYaw) >= 45 && Math.abs(playerYaw) <= 135) {
                    spell.setYRot(player.getYRot() + 180f);
                } else {
                    spell.setYRot(player.getYRot());
                }
            } else {
                spell.setYRot(player.getYRot());
            }
            spell.setXRot(0f);
        } else if (pendingCast.art.style() == SpawnStyle.AOE_CENTERED) {
            Vec3 spawnPos = player.position().add(0, 5.0, 0);
            spell.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            spell.setDeltaMovement(Vec3.ZERO);
            spell.setNoGravity(true);
            spell.setYRot(player.getYRot());
            spell.setXRot(0f);
        } else if (pendingCast.art.style() == SpawnStyle.BOUNCING_PROJECTILE) {
            Vec3 eyePos = player.getEyePosition();
            spell.setPos(eyePos.x, eyePos.y - 0.2, eyePos.z);
            spell.setDeltaMovement(lookVector.normalize().scale(2.0));
            spell.setRotationFromLook(lookVector);
        } else {
            Vec3 eyePos = player.getEyePosition();
            spell.setPos(eyePos.x, eyePos.y - 0.2, eyePos.z);
            double projectileSpeed = artKey.equals("stone_hammer") ? 0.5 : 1.0;
            spell.setDeltaMovement(lookVector.normalize().scale(projectileSpeed));
            spell.setRotationFromLook(lookVector);
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS,
                1.0f, 1.0f
        );

        level.addFreshEntity(spell);

        System.out.println("Successfully spawned spell: " + artKey +
                " at " + spell.position() +
                " for player: " + player.getName().getString());
    }

    private static void cleanupOrphanedEntities(net.minecraft.server.MinecraftServer server) {
        int removedCount = 0;

        for (ServerLevel level : server.getAllLevels()) {
            var entities = level.getAllEntities();
            for (var entity : entities) {
                if (entity instanceof GeckoSpellEntity spell) {
                    boolean shouldRemove = false;
                    String reason = "";
                    if (spell.tickCount > MAX_SPELL_LIFETIME) {
                        shouldRemove = true;
                        reason = "exceeded max lifetime";
                    } else if (spell.getArtName().isEmpty()) {
                        shouldRemove = true;
                        reason = "empty art name";
                    } else if (spell.shouldRemove()) {
                        shouldRemove = true;
                        reason = "marked for removal";
                    }
                    if (shouldRemove) {
                        spell.discard();
                        removedCount++;
                        System.out.println("Removed orphaned spell entity: " + reason);
                    }
                } else if (entity instanceof AuraEntity aura) {
                    // Only discard AuraEntity if not tied to an active PendingCast
                    boolean isActive = PENDING.values().stream()
                            .anyMatch(pending -> pending.aura != null && pending.aura.equals(aura));
                    if (!isActive) {
                        aura.discard();
                        removedCount++;
                        System.out.println("Removed orphaned AuraEntity: " + aura.getType().getDescriptionId());
                    }
                } else if (entity instanceof MagicCircleEntity circle) {
                    // Only discard MagicCircleEntity if not tied to an active PendingCast
                    boolean isActive = PENDING.values().stream()
                            .anyMatch(pending -> pending.circle != null && pending.circle.equals(circle));
                    if (!isActive) {
                        circle.discard();
                        removedCount++;
                        System.out.println("Removed orphaned MagicCircleEntity: " + circle.getType().getDescriptionId());
                    }
                }
            }
        }

        if (PENDING.size() > 10) { // Arbitrary limit, adjust as needed
            cancelCast(PENDING.keySet().iterator().next());
            System.out.println("Removed oldest cast due to entity limit");
        }

        if (removedCount > 0) {
            System.out.println("Cleanup removed " + removedCount + " orphaned entities");
        }
    }

    private static void cleanupStaleCasts() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        synchronized (PENDING) {
            Iterator<Map.Entry<UUID, PendingCast>> iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, PendingCast> entry = iterator.next();
                PendingCast cast = entry.getValue();

                if (currentTime - cast.scheduleTime > 30000) {
                    if (cast.aura != null) {
                        cast.aura.discard();
                        System.out.println("Discarded stale AuraEntity for player: " + cast.playerId);
                    }
                    if (cast.circle != null) {
                        cast.circle.discard();
                        System.out.println("Discarded stale MagicCircleEntity for player: " + cast.playerId);
                    }
                    iterator.remove();
                    removedCount++;
                    System.out.println("Removed stale cast for player: " + cast.playerId);
                }
            }
        }

        if (removedCount > 0) {
            System.out.println("Cleanup removed " + removedCount + " stale pending casts");
        }
    }

    public static int getPendingCastCount() {
        return PENDING.size();
    }

    public static void clearAllPendingCasts() {
        PENDING.clear();
        System.out.println("Cleared all pending casts");
    }
}