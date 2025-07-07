package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = KisekiLegend.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientSetup {
    // toggle‐HUD key
    public static final KeyMapping TOGGLE_EP_HUD = new KeyMapping(
            "key.kisekilegend.toggle_ep",
            GLFW.GLFW_KEY_P,
            "key.categories.kisekilegend"
    );

    public static boolean showEP = true;

    /** Forge now fires a RegisterKeyMappingsEvent for key‐binding registration. */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent ev) {
        ev.register(TOGGLE_EP_HUD);
    }
}
