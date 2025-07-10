package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry.ArtDefinition;
import net.JordanRiver.KisekiLegend.orbal.SpawnStyle;
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
    private record PendingCast(UUID playerId, ArtDefinition art, int ticksLeft, long scheduleTime) {}

    private static final Map<UUID, PendingCast> PENDING = new ConcurrentHashMap<>();
    private static final int MAX_SPELL_LIFETIME = 12000; // 10 minutes max
    private static int cleanupTimer = 0;

    /** Queue a delayed cast */
    public static void scheduleCast(Player player, ArtDefinition art) {
        if (player == null || art == null) return;

        // Cancel any existing cast for this player
        cancelCast(player.getUUID());

        // Play casting sound
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                0.8f, 1.2f
        );

        PendingCast pendingCast = new PendingCast(
                player.getUUID(),
                art,
                Math.max(1, art.getCastDelayTicks()),
                System.currentTimeMillis()
        );

        PENDING.put(player.getUUID(), pendingCast);

        System.out.println("Scheduled cast for " + player.getName().getString() +
                ": " + art.name() + " in " + art.getCastDelayTicks() + " ticks");
    }

    /** Cancel a pending cast */
    public static void cancelCast(UUID playerId) {
        PendingCast removed = PENDING.remove(playerId);
        if (removed != null) {
            System.out.println("Cancelled pending cast for player: " + playerId);
        }
    }

    /** Check if player has a pending cast */
    public static boolean hasPendingCast(UUID playerId) {
        return PENDING.containsKey(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up pending casts when player disconnects
        cancelCast(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Cancel casts on respawn to prevent issues
        cancelCast(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Periodic cleanup every 5 seconds
        cleanupTimer++;
        if (cleanupTimer >= 100) {
            cleanupTimer = 0;
            cleanupOrphanedEntities(event.getServer());
            cleanupStaleCasts();
        }

        // Process pending casts
        Iterator<Map.Entry<UUID, PendingCast>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCast> entry = iterator.next();
            PendingCast pendingCast = entry.getValue();

            int remainingTicks = pendingCast.ticksLeft - 1;

            if (remainingTicks <= 0) {
                // Time to execute the cast
                iterator.remove();
                executeCast(event.getServer(), pendingCast);
            } else {
                // Update remaining ticks
                entry.setValue(new PendingCast(
                        pendingCast.playerId,
                        pendingCast.art,
                        remainingTicks,
                        pendingCast.scheduleTime
                ));
            }
        }
    }

    private static void executeCast(net.minecraft.server.MinecraftServer server, PendingCast pendingCast) {
        // Find the player across all levels
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

        // Verify player still has orbment equipped
        if (!(player.getMainHandItem().getItem() instanceof OrbmentItem)) {
            System.out.println("Cannot execute cast - player no longer has orbment equipped");
            return;
        }

        // Parse damage safely
        int damage = 1;
        try {
            String powerStr = pendingCast.art.power();
            if (powerStr != null && !powerStr.isEmpty()) {
                damage = Integer.parseInt(powerStr);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid damage value for art: " + pendingCast.art.name());
        }

        // Create the spell entity
        String artKey = pendingCast.art.name().toLowerCase().replace(' ', '_');
        GeckoSpellEntity spell = new GeckoSpellEntity(level, player, damage, artKey);

        // Position the spell based on type
        Vec3 lookVector = player.getLookAngle();

        if (pendingCast.art.style() == SpawnStyle.GROUND) {
            // Ground spell: spawn in front of player on ground
            Vec3 spawnPos = player.position().add(lookVector.scale(2.5));
            int groundX = Mth.floor(spawnPos.x);
            int groundZ = Mth.floor(spawnPos.z);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, groundX, groundZ);

            spell.setPos(spawnPos.x, groundY, spawnPos.z); // Lowered Y offset
            spell.setDeltaMovement(Vec3.ZERO);
            spell.setNoGravity(true);
            float playerYaw = Mth.wrapDegrees(player.getYRot());
            if (artKey.equals("earth_lance")) {
                // Conditional rotation: add 180 only for east/west facing to keep those correct, no add for north/south to fix reversal
                if (Math.abs(playerYaw) >= 45 && Math.abs(playerYaw) <= 135) { // Facing east or west (yaw between -135 to -45 or 45 to 135)
                    spell.setYRot(player.getYRot() + 180f);
                } else {
                    spell.setYRot(player.getYRot());
                }
            } else {
                spell.setYRot(player.getYRot());
            }
            spell.setXRot(0f);
        } else {
            // Projectile spell: spawn at player eye level
            Vec3 eyePos = player.getEyePosition();
            spell.setPos(eyePos.x, eyePos.y - 0.2, eyePos.z);
            double projectileSpeed = artKey.equals("stone_hammer") ? 1.0 : 2.0; // Decreased speed for stone_hammer
            spell.setDeltaMovement(lookVector.normalize().scale(projectileSpeed));
            spell.setRotationFromLook(lookVector);
        }

        // Play cast completion sound
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS,
                1.0f, 1.0f
        );

        // Add to world
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
                    // Remove spells that have been alive too long or are invalid
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
                }
            }
        }

        if (removedCount > 0) {
            System.out.println("Cleanup removed " + removedCount + " orphaned spell entities");
        }
    }

    private static void cleanupStaleCasts() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        Iterator<Map.Entry<UUID, PendingCast>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCast> entry = iterator.next();
            PendingCast cast = entry.getValue();

            // Remove casts that have been pending for more than 30 seconds
            if (currentTime - cast.scheduleTime > 30000) {
                iterator.remove();
                removedCount++;
                System.out.println("Removed stale cast for player: " + cast.playerId);
            }
        }

        if (removedCount > 0) {
            System.out.println("Cleanup removed " + removedCount + " stale pending casts");
        }
    }

    /** Debug method to get pending cast count */
    public static int getPendingCastCount() {
        return PENDING.size();
    }

    /** Debug method to clear all pending casts */
    public static void clearAllPendingCasts() {
        PENDING.clear();
        System.out.println("Cleared all pending casts");
    }
}