package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtInputHandler {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Player player = MC.player;
        if (player == null) return;
        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        int key = event.getKey();
        int action = event.getAction();

        if (key == ClientSetup.TOGGLE_ART_SELECT.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.artSelectMode = !ClientSetup.artSelectMode;
            ClientSetup.selectedArtIdx = 0;
            player.displayClientMessage(
                    Component.literal("Art Select " + (ClientSetup.artSelectMode ? "Enabled" : "Disabled")),
                    true
            );
            return;
        }

        if (!ClientSetup.artSelectMode) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        List<ArtsRegistry.ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def ->
                        def.elementCost().entrySet().stream()
                                .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue())
                ).collect(Collectors.toList());
        if (avail.isEmpty()) {
            player.displayClientMessage(Component.literal("No arts available."), true);
            return;
        }

        boolean changed = false;
        if (key == ClientSetup.ART_NEXT.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx + 1) % avail.size();
            changed = true;
        }
        else if (key == ClientSetup.ART_PREV.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx - 1 + avail.size()) % avail.size();
            changed = true;
        }

        if (changed) {
            ArtsRegistry.ArtDefinition current = avail.get(ClientSetup.selectedArtIdx);
            MC.gui.setOverlayMessage(
                    Component.literal("Selected Art: " + current.name()),
                    false
            );
        }
    }

    @SubscribeEvent
    public static void onRightClick(RightClickItem ev) {
        Player player = ev.getEntity();
        if (player == null
                || ev.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown())
            return;

        if (ClientSetup.artSelectMode) {
            ClientSetup.artSelectMode = false;
            ev.setCanceled(true);
            return;
        }

        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        List<ArtsRegistry.ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def ->
                        def.elementCost().entrySet().stream()
                                .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue())
                ).collect(Collectors.toList());
        if (avail.isEmpty()) return;

        ClientSetup.selectedArtIdx = Math.floorMod(ClientSetup.selectedArtIdx, avail.size());
        ArtsRegistry.ArtDefinition art = avail.get(ClientSetup.selectedArtIdx);

        int cost;
        try {
            cost = Integer.parseInt(art.epCost().split(" ")[0]);
        } catch (NumberFormatException x) {
            cost = 0;
        }
        if (!comp.useEP(cost)) {
            player.displayClientMessage(
                    Component.translatable("Not enough EP!"),
                    true
            );
            ev.setCanceled(true);
            return;
        }

        player.swing(InteractionHand.MAIN_HAND);

        // Only schedule cast on server side
        // Always trigger animation client-side
        if (orb.getItem() instanceof GeoItem geoItem) {
            if (art.name().equals("Flare Arrow")) {
                // If the art is Flare Arrow, trigger the bow draw animation
                geoItem.triggerAnim(player, GeoItem.getId(orb), "cast_controller", "flare_arrow_draw");
            } else {
                // Otherwise, trigger the default cast animation
                geoItem.triggerAnim(player, GeoItem.getId(orb), "cast_controller", "cast");
            }
        }


        // Only schedule cast on the server
        if (!ev.getLevel().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) ev.getLevel();

            // Calculate target position for ground spells
            Vec3 lookVector = player.getLookAngle();
            Vec3 targetPos = player.position().add(lookVector.scale(2.5));

            CastScheduler.scheduleCast(player, art, targetPos);
        }

        OrbmentItem.saveComponent(orb, comp, player.level());
        ev.setCanceled(true);
    }
}