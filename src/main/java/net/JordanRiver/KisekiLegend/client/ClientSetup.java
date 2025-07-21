package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.particle.BlueFlowParticle;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = KisekiLegend.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ClientSetup {

    public static final KeyMapping OPEN_RADIAL_MENU;
    public static final KeyMapping TOGGLE_EP_HUD;
    public static final KeyMapping TOGGLE_ART_SELECT_LEGACY;
    public static final KeyMapping ART_NEXT_LEGACY;
    public static final KeyMapping ART_PREV_LEGACY;

    static {
        OPEN_RADIAL_MENU = new KeyMapping("key.kisekilegend.open_radial_menu", GLFW.GLFW_KEY_R, "key.categories.kisekilegend");
        TOGGLE_EP_HUD = new KeyMapping("key.kisekilegend.toggle_ep", GLFW.GLFW_KEY_P, "key.categories.kisekilegend");
        TOGGLE_ART_SELECT_LEGACY = new KeyMapping("key.kisekilegend.art_select_legacy", GLFW.GLFW_KEY_C, "key.categories.kisekilegend");
        ART_NEXT_LEGACY = new KeyMapping("key.kisekilegend.art_next_legacy", GLFW.GLFW_KEY_PERIOD, "key.categories.kisekilegend");
        ART_PREV_LEGACY = new KeyMapping("key.kisekilegend.art_prev_legacy", GLFW.GLFW_KEY_COMMA, "key.categories.kisekilegend");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent ev) {
        ev.register(OPEN_RADIAL_MENU);
        ev.register(TOGGLE_EP_HUD);
        ev.register(TOGGLE_ART_SELECT_LEGACY);
        ev.register(ART_NEXT_LEGACY);
        ev.register(ART_PREV_LEGACY);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLUE_FLOW.get(), BlueFlowParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientEventHandler::initializeCustomCursor);
    }

    // For EP bar and legacy mode toggle
    public static boolean showEP = true;
    public static boolean artSelectMode = false;

    // Legacy index is no longer the source of truth, but can be kept for UI hints if desired
    public static int selectedArtIdx = 0;
}