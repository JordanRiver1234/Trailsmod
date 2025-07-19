package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlayerRenderHandler {

    // Track casting state per player
    private static final Map<UUID, CastingData> CASTING_PLAYERS = new HashMap<>();

    private static class CastingData {
        long startTime;
        long duration;

        CastingData(long startTime, long duration) {
            this.startTime = startTime;
            this.duration = duration;
        }

        boolean isActive() {
            return System.currentTimeMillis() - startTime < duration;
        }

        float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0f, elapsed / (float) duration);
        }
    }

    /**
     * Call this from your ArtInputHandler when casting starts
     */
    public static void startPlayerCasting(Player player, long durationMs) {
        CASTING_PLAYERS.put(player.getUUID(), new CastingData(System.currentTimeMillis(), durationMs));
        System.out.println("Started casting for player: " + player.getName().getString() + " for " + durationMs + "ms");
    }

    /**
     * Check if a player is currently casting
     */
    public static boolean isPlayerCasting(Player player) {
        CastingData data = CASTING_PLAYERS.get(player.getUUID());
        if (data == null || !data.isActive()) {
            if (data != null) {
                System.out.println("Casting finished for player: " + player.getName().getString());
            }
            CASTING_PLAYERS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();

        System.out.println("Rendering player: " + player.getName().getString() +
                " | Has Orbment: " + (mainHand.getItem() instanceof OrbmentItem) +
                " | Is Casting: " + isPlayerCasting(player));

        // Only apply casting animation if holding an Orbment
        if (!(mainHand.getItem() instanceof OrbmentItem)) return;

        CastingData castingData = CASTING_PLAYERS.get(player.getUUID());
        if (castingData == null || !castingData.isActive()) {
            CASTING_PLAYERS.remove(player.getUUID());
            return;
        }

        // Apply casting pose to the player model
        PlayerModel<? extends Player> model = event.getRenderer().getModel();
        applyCastingPose(model, castingData.getProgress());

        System.out.println("Applied casting pose with progress: " + castingData.getProgress());
    }

    private static void applyCastingPose(PlayerModel<? extends Player> model, float progress) {
        // Smooth animation curve - starts slow, peaks in middle, ends slow
        float intensity = (float) (Math.sin(progress * Math.PI) * 0.8 + 0.2); // Min 0.2, max 1.0

        // More dramatic casting pose - both arms raised as if channeling magic
        model.rightArm.xRot = -2.0f * intensity; // Raise right arm higher
        model.rightArm.yRot = -0.5f * intensity; // Point more inward
        model.rightArm.zRot = 0.4f * intensity;  // More outward angle

        model.leftArm.xRot = -1.8f * intensity;  // Raise left arm high
        model.leftArm.yRot = 0.5f * intensity;   // Point more inward
        model.leftArm.zRot = -0.4f * intensity;  // More inward angle

        // Body modifications for more dramatic effect
        model.body.xRot = -0.2f * intensity; // Lean back more

        // Add subtle oscillation for magical energy effect
        float oscillation = (float) Math.sin(System.currentTimeMillis() * 0.02) * 0.1f;
        model.body.yRot = oscillation * intensity; // Subtle magical sway

        // Head looks up while casting
        model.head.xRot = -0.4f * intensity + oscillation * 0.5f;

        // Legs spread for stability and power stance
        model.rightLeg.zRot = 0.15f * intensity;
        model.leftLeg.zRot = -0.15f * intensity;

        // Add slight forward lean to legs
        model.rightLeg.xRot = 0.1f * intensity;
        model.leftLeg.xRot = 0.1f * intensity;
    }
}