package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtInputHandler {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final ConcurrentHashMap<UUID, CastingState> CASTING_PLAYERS = new ConcurrentHashMap<>();

    private static class CastingState {
        final long startTime;
        final long duration;
        final ArtsRegistry.ArtDefinition art;
        SimpleSoundInstance loopingSoundInstance;

        CastingState(long startTime, long duration, ArtsRegistry.ArtDefinition art) {
            this.startTime = startTime;
            this.duration = duration;
            this.art = art;
        }

        boolean isActive() {
            return System.currentTimeMillis() - startTime < duration;
        }

        float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0f, elapsed / (float) duration);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Player player = MC.player;
        if (player == null) return;
        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        int key = event.getKey();
        int action = event.getAction();

        if (key == ClientSetup.TOGGLE_ART_SELECT.getKey().getValue() && action == GLFW.GLFW_PRESS) {
            ClientSetup.artSelectMode = !ClientSetup.artSelectMode;
            ClientSetup.selectedArtIdx = 0;
            playSound(ModSoundEvents.ART_SELECT.get(), player, 0.7f, 1.0f);
            player.displayClientMessage(
                    Component.literal("Art Select " + (ClientSetup.artSelectMode ? "Enabled" : "Disabled")), true
            );
            return;
        }

        if (!ClientSetup.artSelectMode) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        List<ArtsRegistry.ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def -> def.elementCost().entrySet().stream()
                        .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue()))
                .collect(Collectors.toList());

        if (avail.isEmpty()) {
            player.displayClientMessage(Component.literal("No arts available."), true);
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.5f, 1.0f);
            return;
        }

        boolean changed = false;
        if (key == ClientSetup.ART_NEXT.getKey().getValue() && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx + 1) % avail.size();
            changed = true;
        } else if (key == ClientSetup.ART_PREV.getKey().getValue() && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx - 1 + avail.size()) % avail.size();
            changed = true;
        }

        if (changed) {
            ArtsRegistry.ArtDefinition current = avail.get(ClientSetup.selectedArtIdx);
            float pitch = 0.9f + (ClientSetup.selectedArtIdx * 0.1f) % 0.4f;
            playSound(ModSoundEvents.ART_SELECT.get(), player, 0.4f, pitch);
            MC.gui.setOverlayMessage(Component.literal("Selected Art: " + current.name()), false);
        }
    }

    @SubscribeEvent
    public static void onRightClick(RightClickItem ev) {
        Player player = ev.getEntity();
        if (player == null || ev.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return;

        if (ClientSetup.artSelectMode) {
            ClientSetup.artSelectMode = false;
            playSound(ModSoundEvents.ART_SELECT.get(), player, 0.5f, 0.8f);
            ev.setCanceled(true);
            return;
        }

        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        List<ArtsRegistry.ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def -> def.elementCost().entrySet().stream()
                        .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue()))
                .collect(Collectors.toList());

        if (avail.isEmpty()) {
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.7f, 1.0f);
            return;
        }

        ClientSetup.selectedArtIdx = Math.floorMod(ClientSetup.selectedArtIdx, avail.size());
        ArtsRegistry.ArtDefinition art = avail.get(ClientSetup.selectedArtIdx);

        int cost;
        try {
            cost = Integer.parseInt(art.epCost().split(" ")[0]);
        } catch (NumberFormatException x) {
            cost = 0;
        }

        if (!comp.useEP(cost)) {
            player.displayClientMessage(Component.literal("Not enough EP!"), true);
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.7f, 1.0f);
            ev.setCanceled(true);
            return;
        }

        player.swing(InteractionHand.MAIN_HAND);

        // Add casting sound effects and animation tracking
        if (ev.getLevel().isClientSide()) {
            int castDelayTicks = art.getCastDelayTicks();
            int animationDurationMs = castDelayTicks * 50;

            UUID playerId = player.getUUID();
            CastingState castState = new CastingState(System.currentTimeMillis(), animationDurationMs, art);
            CASTING_PLAYERS.put(playerId, castState);

            if (PlayerRenderHandler.class != null) {
                try {
                    PlayerRenderHandler.startPlayerCasting(player, animationDurationMs);
                } catch (Exception ignored) {
                    // PlayerRenderHandler might not exist, ignore
                }
            }

            startCastingSounds(player, castState, animationDurationMs);
            scheduleCompletionSound(player, animationDurationMs);
        }

        if (orb.getItem() instanceof GeoItem geoItem) {
            geoItem.triggerAnim(player, GeoItem.getId(orb), "cast_controller", "cast");
        }

        // Only schedule cast on the server
        if (!ev.getLevel().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) ev.getLevel();
            Vec3 lookVector = player.getLookAngle();
            Vec3 targetPos = player.position().add(lookVector.scale(2.5));
            CastScheduler.scheduleCast(player, art, targetPos);
        }

        OrbmentItem.saveComponent(orb, comp, player.level());
        ev.setCanceled(true);
    }

    private static void startCastingSounds(Player player, CastingState castState, long durationMs) {
        playSound(ModSoundEvents.CAST_START.get(), player, 0.8f, 1.0f);

        if (durationMs > 1000) {
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    if (castState.isActive() && MC.player != null) {
                        SimpleSoundInstance loopSound = new SimpleSoundInstance(
                                ModSoundEvents.CAST_LOOP.get().getLocation(), SoundSource.PLAYERS,
                                0.6f, 1.0f, player.getRandom(), true, 0,
                                SimpleSoundInstance.Attenuation.NONE, 0, 0, 0, true
                        );
                        castState.loopingSoundInstance = loopSound;
                        MC.getSoundManager().play(loopSound);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private static void scheduleCompletionSound(Player player, long durationMs) {
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                if (MC.player != null) {
                    playSound(ModSoundEvents.CAST_COMPLETE.get(), player, 0.9f, 1.0f);
                    CastingState state = CASTING_PLAYERS.remove(player.getUUID());
                    if (state != null && state.loopingSoundInstance != null) {
                        MC.getSoundManager().stop(state.loopingSoundInstance);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void playSound(SoundEvent soundEvent, Player player, float volume, float pitch) {
        if (MC.level == null || MC.getSoundManager() == null || soundEvent == null) return;
        MC.level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                soundEvent, SoundSource.PLAYERS,
                volume, pitch, false
        );
    }

    public static void stopCasting(Player player) {
        UUID playerId = player.getUUID();
        CastingState state = CASTING_PLAYERS.remove(playerId);
        if (state != null) {
            if (state.loopingSoundInstance != null) {
                MC.getSoundManager().stop(state.loopingSoundInstance);
            }
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.5f, 0.8f);
        }
    }

    public static boolean isPlayerCasting(Player player) {
        CastingState state = CASTING_PLAYERS.get(player.getUUID());
        if (state != null) {
            if (state.isActive()) {
                return true;
            }
            // Clean up expired state
            CASTING_PLAYERS.remove(player.getUUID());
            if (state.loopingSoundInstance != null) {
                MC.getSoundManager().stop(state.loopingSoundInstance);
            }
        }
        return false;
    }

    public static float getCastingProgress(Player player) {
        CastingState state = CASTING_PLAYERS.get(player.getUUID());
        return state != null && state.isActive() ? state.getProgress() : 0.0f;
    }
}