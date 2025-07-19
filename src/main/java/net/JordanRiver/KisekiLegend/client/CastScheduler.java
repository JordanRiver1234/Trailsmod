package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry.ArtDefinition;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.orbal.SpawnStyle;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private record PendingCast(UUID playerId, ArtDefinition art, int ticksLeft, long scheduleTime, AuraEntity aura, Vec3 targetPos) {}

    private static final Map<UUID, PendingCast> PENDING = new ConcurrentHashMap<>();
    private static final int MAX_SPELL_LIFETIME = 12000; // 10 minutes max
    private static int cleanupTimer = 0;

    public static void scheduleCast(Player player, ArtDefinition art, Vec3 targetPos) {
        if (player == null || art == null) {
            System.out.println("Failed to schedule cast: player or art is null");
            return;
        }
        if (!(player.level() instanceof ServerLevel)) {
            System.out.println("Cannot schedule cast: not on server side");
            return;
        }

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
        if (player.level() instanceof ServerLevel serverLevel) {
            aura = new AuraEntity(serverLevel, player);
            if (serverLevel.addFreshEntity(aura)) {
                System.out.println("Successfully added AuraEntity " + aura.getId() + " at " + aura.position());
            } else {
                System.out.println("Failed to add AuraEntity for player: " + player.getName().getString());

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

                targetPos // Store the target position for ground spells
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
                    if (pendingCast.aura != null && !pendingCast.aura.isAlive()) {
                        System.out.println("AuraEntity is not alive for player: " + pendingCast.playerId);
                    }

                }

                if (remainingTicks <= 0) {
                    iterator.remove();
                    if (pendingCast.aura != null) {
                        pendingCast.aura.discard();
                    }

                    executeCast(event.getServer(), pendingCast);
                } else {
                    entry.setValue(new PendingCast(
                            pendingCast.playerId,
                            pendingCast.art,
                            remainingTicks,
                            pendingCast.scheduleTime,
                            pendingCast.aura,
                            pendingCast.targetPos
                    ));
                }
            }
        }
    }

    private static void executeCast(net.minecraft.server.MinecraftServer server, PendingCast pendingCast) {
        ServerPlayer player = server.getPlayerList().getPlayer(pendingCast.playerId);
        if (player == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof OrbmentItem)) {
            return;
        }

        // --- DAMAGE BONUS LOGIC ---
        OrbmentComponent orbmentComponent = OrbmentItem.loadComponent(heldItem, level);
        float damageMultiplier = orbmentComponent.getArtDamageMultiplier(pendingCast.art.mainElement());

        int baseDamage = 1;
        try {
            String powerStr = pendingCast.art.power();
            if (powerStr != null && !powerStr.isEmpty() && !powerStr.equals("-")) {
                baseDamage = Integer.parseInt(powerStr);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid damage value for art: " + pendingCast.art.name());
        }

        int finalDamage = (int) (baseDamage * damageMultiplier);
        // --- END DAMAGE BONUS LOGIC ---


        String artKey = pendingCast.art.name().toLowerCase().replace(' ', '_');
        GeckoSpellEntity spell = new GeckoSpellEntity(level, player, finalDamage, artKey);

        Vec3 lookVector = player.getLookAngle();

        if (pendingCast.art.style() == SpawnStyle.GROUND) {
            Vec3 spawnPos;

            if (artKey.equals("volcanic_rave")) {
                spawnPos = player.position().add(lookVector.scale(4.0));
            } else if (pendingCast.targetPos != null) {
                spawnPos = pendingCast.targetPos;
            } else {
                spawnPos = player.position().add(lookVector.scale(2.5));
            }

            int blockX = Mth.floor(spawnPos.x);
            int blockZ = Mth.floor(spawnPos.z);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);

            spell.setPos(blockX + 0.5, groundY, blockZ + 0.5);
            spell.setDeltaMovement(Vec3.ZERO);
            spell.setNoGravity(true);

            Vec3 playerPos = player.position();
            Vec3 spellPos = new Vec3(blockX + 0.5, groundY, blockZ + 0.5);
            Vec3 direction = spellPos.subtract(playerPos).normalize();

            float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0 / Math.PI));

            if (artKey.equals("earth_lance")) {
                spell.setYRot(yaw);
            } else if (artKey.equals("petrify_breath")) {
                spell.setYRot(yaw + 180f);
            } else {
                spell.setYRot(player.getYRot());
            }

            spell.setXRot(0f);

            if (artKey.equals("petrify_breath")) {
                spell.setPositioned(true);
            }

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
        } else if (pendingCast.art.style() == SpawnStyle.STATIONARY) {
            Vec3 spawnPos = player.position().add(lookVector.scale(2.0)); // Move to 2 blocks in front
            int blockX = Mth.floor(spawnPos.x);
            int blockZ = Mth.floor(spawnPos.z);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);

            spell.setPos(blockX + 0.5, groundY, blockZ + 0.5);
            spell.setDeltaMovement(Vec3.ZERO);
            spell.setNoGravity(true);
            // FIXED: Calculate direction from player to spell position for proper orientation
            Vec3 playerPos = player.position();
            Vec3 spellPos = new Vec3(blockX + 0.5, groundY, blockZ + 0.5);
            Vec3 direction = spellPos.subtract(playerPos).normalize();

            // Convert direction to yaw (same calculation used for GROUND style)
            float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0 / Math.PI));
            spell.setYRot(yaw);
            spell.setXRot(0f);
        } else {
            Vec3 eyePos = player.getEyePosition();
            spell.setPos(eyePos.x, eyePos.y - 0.2, eyePos.z);
            double projectileSpeed;
            switch (artKey) {
                case "stone_hammer":
                case "aqua_bleed":
                case "fire_bolt":
                    projectileSpeed = 0.5;
                    break;
                case "fire_bolt_ex":
                default:
                    projectileSpeed = 0.8;
                    break;
            }
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
                    }
                } else if (entity instanceof AuraEntity aura) {
                    boolean isActive = PENDING.values().stream()
                            .anyMatch(pending -> pending.aura != null && pending.aura.equals(aura));
                    if (!isActive) {
                        aura.discard();
                        removedCount++;
                    }


                    }

            }
        }
    }

    private static void cleanupStaleCasts() {
        long currentTime = System.currentTimeMillis();

        synchronized (PENDING) {
            PENDING.entrySet().removeIf(entry -> {
                PendingCast cast = entry.getValue();
                if (currentTime - cast.scheduleTime > 30000) {
                    if (cast.aura != null) cast.aura.discard();
                    return true;
                }
                return false;
            });
        }
    }

    public static int getPendingCastCount() {
        return PENDING.size();
    }

    public static void clearAllPendingCasts() {
        PENDING.clear();
    }
}
