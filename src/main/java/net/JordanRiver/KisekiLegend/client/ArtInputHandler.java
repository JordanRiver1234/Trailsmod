// src/main/java/net/JordanRiver/KisekiLegend/client/ArtInputHandler.java
package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.CastScheduler;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry.ArtDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtInputHandler {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Player player = MC.player;
        if (player == null) return;
        // Must be holding the orbment
        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        int key    = event.getKey();
        int action = event.getAction();

        // 1) Toggle art-select mode on R
        if (key == ClientSetup.TOGGLE_ART_SELECT.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.artSelectMode  = !ClientSetup.artSelectMode;
            ClientSetup.selectedArtIdx = 0;
            player.displayClientMessage(
                    Component.literal("Art Select " + (ClientSetup.artSelectMode ? "Enabled" : "Disabled")),
                    true
            );
            return;
        }

        // 2) If we’re not in art-select, ignore further keys
        if (!ClientSetup.artSelectMode) return;

        // 3) Load & recalc the real orbment component from item NBT
        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        // 4) Build the list of available arts
        List<ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def ->
                        def.elementCost().entrySet().stream()
                                .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue())
                ).collect(Collectors.toList());
        if (avail.isEmpty()) {
            player.displayClientMessage(Component.literal("No arts available."), true);
            return;
        }

        boolean changed = false;
        // 5) Cycle forward on '.'
        if (key == ClientSetup.ART_NEXT.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx + 1) % avail.size();
            changed = true;
        }
        // 6) Cycle backward on ','
        else if (key == ClientSetup.ART_PREV.getKey().getValue()
                && action == GLFW.GLFW_PRESS) {
            ClientSetup.selectedArtIdx = (ClientSetup.selectedArtIdx - 1 + avail.size()) % avail.size();
            changed = true;
        }

        if (changed) {
            ArtDefinition current = avail.get(ClientSetup.selectedArtIdx);
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

        // If in art-select mode, close it
        if (ClientSetup.artSelectMode) {
            ClientSetup.artSelectMode = false;
            ev.setCanceled(true);
            return;
        }

        // Otherwise cast the currently selected art with delay
        ItemStack orb = player.getMainHandItem();
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        // Re-load component and recalc
        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();

        List<ArtDefinition> avail = ArtsRegistry.ALL_ARTS.stream()
                .filter(def ->
                        def.elementCost().entrySet().stream()
                                .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue())
                ).collect(Collectors.toList());
        if (avail.isEmpty()) return;

        // Clamp index, charge EP
        ClientSetup.selectedArtIdx = Math.floorMod(ClientSetup.selectedArtIdx, avail.size());
        ArtDefinition art = avail.get(ClientSetup.selectedArtIdx);

        int cost;
        try {
            cost = Integer.parseInt(art.epCost().split(" ")[0]);
        } catch (NumberFormatException x) {
            cost = 0;
        }
        if (!comp.useEP(cost)) {
            player.displayClientMessage(
                    Component.translatable("message.kisekilegend.not_enough_ep"),
                    true
            );
            ev.setCanceled(true);
            return;
        }

        // Swing hand for casting motion
        player.swing(InteractionHand.MAIN_HAND);

        // Schedule the delayed projectile spawn
        CastScheduler.scheduleCast(player, art);

        // Persist EP & inventory back to the item
        OrbmentItem.saveComponent(orb, comp, player.level());

        ev.setCanceled(true);
    }
}
