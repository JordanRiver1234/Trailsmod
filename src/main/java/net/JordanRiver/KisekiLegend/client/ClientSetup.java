package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderer.item.OrbmentItemRenderer;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = KisekiLegend.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ClientSetup {

    public static final KeyMapping TOGGLE_EP_HUD;
    public static final KeyMapping TOGGLE_ART_SELECT;
    public static final KeyMapping ART_NEXT;
    public static final KeyMapping ART_PREV;

    static {
        TOGGLE_EP_HUD = new KeyMapping("key.kisekilegend.toggle_ep", GLFW.GLFW_KEY_P, "key.categories.kisekilegend");
        TOGGLE_ART_SELECT = new KeyMapping("key.kisekilegend.art_select", GLFW.GLFW_KEY_R, "key.categories.kisekilegend");
        ART_NEXT = new KeyMapping("key.kisekilegend.art_next", GLFW.GLFW_KEY_PERIOD, "key.categories.kisekilegend");
        ART_PREV = new KeyMapping("key.kisekilegend.art_prev", GLFW.GLFW_KEY_COMMA, "key.categories.kisekilegend");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent ev) {
        ev.register(TOGGLE_EP_HUD);
        ev.register(TOGGLE_ART_SELECT);
        ev.register(ART_NEXT);
        ev.register(ART_PREV);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Register any layer definitions here if needed
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize GeckoLib item renderers
            initializeItemRenderers();
        });
    }

    private static void initializeItemRenderers() {
        // This ensures the renderer is properly initialized
        System.out.println("Initializing GeckoLib item renderers...");
    }

    // For art selector + EP bar
    public static boolean showEP = true;
    public static boolean artSelectMode = false;
    public static int selectedArtIdx = 0;
}