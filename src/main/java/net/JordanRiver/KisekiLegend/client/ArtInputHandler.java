package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtInputHandler {
    private static final Minecraft MC = Minecraft.getInstance();

    public static void castArt(Player player, ArtsRegistry.ArtDefinition art, OrbmentComponent comp) {
        if (player == null || art == null) return;

        ItemStack orb = findOrbment(player);
        if (orb.isEmpty()) return;

        int cost;
        try {
            cost = Integer.parseInt(art.epCost().split(" ")[0]);
        } catch (NumberFormatException e) {
            cost = 0;
        }

        if (!comp.useEP(cost)) {
            player.displayClientMessage(Component.literal("Not enough EP!"), true);
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.7f, 1.0f);
            return;
        }

        OrbmentItem.saveComponent(orb, comp, player.level());

        playSound(ModSoundEvents.CAST_START.get(), player, 0.8f, 1.0f);

        player.swing(InteractionHand.MAIN_HAND);
        if (orb.getItem() instanceof GeoItem geoItem) {
            geoItem.triggerAnim(player, GeoItem.getId(orb), "cast_controller", "cast");
        }

        if (!player.level().isClientSide()) {
            Vec3 targetPos = player.position().add(player.getLookAngle().scale(2.5));
            CastScheduler.scheduleCast(player, art, targetPos);
        }
    }
    @SubscribeEvent
    public static void onRightClick(RightClickItem ev) {
        Player player = ev.getEntity();
        if (player.level().isClientSide() || ev.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack orb = findOrbment(player);
        if (!(orb.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent comp = OrbmentItem.loadComponent(orb, player.level());
        comp.recalculate();
        List<ArtsRegistry.ArtDefinition> availableArts = getAvailableArts(comp);

        String lastArtName = comp.getSelectedArt();
        ArtsRegistry.ArtDefinition artToCast = null;
        if (lastArtName != null && !lastArtName.isEmpty()) {
            artToCast = ArtsRegistry.ALL_ARTS.stream()
                    .filter(art -> art.name().equals(lastArtName))
                    .findFirst()
                    .orElse(null);
        }
        if (artToCast == null || !availableArts.contains(artToCast)) {
            if (artToCast != null) {
                player.displayClientMessage(Component.literal("Cannot cast " + artToCast.name() + ": requirements not met."), true);
            }
            playSound(ModSoundEvents.CAST_FAIL.get(), player, 0.7f, 1.0f);
            ev.setCanceled(true);
            return;
        }
        castArt(player, artToCast, comp);
        ev.setCanceled(true);
    }
    private static List<ArtsRegistry.ArtDefinition> getAvailableArts(OrbmentComponent comp) {
        return ArtsRegistry.ALL_ARTS.stream()
                .filter(def -> def.elementCost().entrySet().stream()
                        .allMatch(e -> comp.getSepithCounts()[OrbmentComponent.ELEMENT_INDEX.get(e.getKey())] >= e.getValue()))
                .sorted(Comparator.comparing(art -> art.name())) // Sort alphabetically for consistency
                .collect(Collectors.toList());
    }
    private static ItemStack findOrbment(Player player) {
        if (player.getMainHandItem().getItem() instanceof OrbmentItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof OrbmentItem) return player.getOffhandItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) return stack;
        }
        return ItemStack.EMPTY;
    }
    private static void playSound(SoundEvent soundEvent, Player player, float volume, float pitch) {
        if (player.level() == null || soundEvent == null) return;
        player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                soundEvent, SoundSource.PLAYERS,
                volume, pitch, false
        );
    }
}