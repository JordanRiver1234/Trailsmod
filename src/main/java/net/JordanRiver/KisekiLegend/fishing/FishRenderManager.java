package net.JordanRiver.KisekiLegend.fishing;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FishRenderManager {
    private static boolean allowFishRendering = false;

    public static void enableFishRendering() {
        allowFishRendering = true;
    }

    public static void disableFishRendering() {
        allowFishRendering = false;
    }

    public static boolean shouldRenderFish() {
        return allowFishRendering && FishingGameManager.isActive();
    }
}