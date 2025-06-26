package net.JordanRiver.KisekiLegend;

import com.mojang.logging.LogUtils;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentScreen;
import net.JordanRiver.KisekiLegend.item.ModCreativeModeTabs;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.ModMenuTypes;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KisekiLegend.MOD_ID)
public class KisekiLegend {
    public static final String MOD_ID = "kisekilegend";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KisekiLegend() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);

        ModCreativeModeTabs.register(bus);
        ModItems.register(bus);
        ModBlocks.register(bus);
        ModMenuTypes.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // No capability registration required in 1.21.1+
        LOGGER.info("KisekiLegend mod setup complete.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent ev) {
        if (ev.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ev.accept(ModItems.EARTH);
            ev.accept(ModItems.EARTH_MASS);
            ev.accept(ModItems.WATER);
            ev.accept(ModItems.WATER_MASS);
            ev.accept(ModItems.FIRE);
            ev.accept(ModItems.FIRE_MASS);
            ev.accept(ModItems.WIND);
            ev.accept(ModItems.WIND_MASS);
            ev.accept(ModItems.TIME);
            ev.accept(ModItems.TIME_MASS);
            ev.accept(ModItems.SPACE);
            ev.accept(ModItems.SPACE_MASS);
            ev.accept(ModItems.MIRAGE);
            ev.accept(ModItems.MIRAGE_MASS);
            ev.accept(ModItems.SEPITH_MASS);
        }
        if (ev.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            ev.accept(ModBlocks.EARTHVEIN_BLOCK);
            ev.accept(ModBlocks.FIREVEIN_BLOCK);
            ev.accept(ModBlocks.MIRAGEVEIN_BLOCK);
            ev.accept(ModBlocks.SPACEVEIN_BLOCK);
            ev.accept(ModBlocks.TIMEVEIN_BLOCK);
            ev.accept(ModBlocks.WATERVEIN_BLOCK);
            ev.accept(ModBlocks.WINDVEIN_BLOCK);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server is starting!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MenuScreens.register(ModMenuTypes.ORBMENT_MENU.get(), OrbmentScreen::new);
        }
    }
}
