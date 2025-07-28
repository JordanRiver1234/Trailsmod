package net.JordanRiver.KisekiLegend;

import com.mojang.logging.LogUtils;
import net.JordanRiver.KisekiLegend.client.renderer.block.QuartzMachineRenderer;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.client.ArtInputHandler;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentMachineRenderer;
import net.JordanRiver.KisekiLegend.client.AuraRenderer;
import net.JordanRiver.KisekiLegend.crafting.QuartzRecipeManager;
import net.JordanRiver.KisekiLegend.datagen.ModItemTagProvider;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentScreen;
import net.JordanRiver.KisekiLegend.datagen.ModDatapackEntries;
import net.JordanRiver.KisekiLegend.entity.ModEntities;
import net.JordanRiver.KisekiLegend.item.ModCreativeModeTabs;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.ModMenuTypes;
import net.JordanRiver.KisekiLegend.util.ModTags;
import net.minecraft.client.gui.screens.MenuScreens;
import net.JordanRiver.KisekiLegend.menu.QuartzMachineMenu;
import net.JordanRiver.KisekiLegend.client.screen.QuartzMachineScreen;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentMachineScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod(KisekiLegend.MOD_ID)
public class KisekiLegend {
    public static final String MOD_ID = "kisekilegend";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static QuartzRecipeManager getQuartzRecipeManager() {
        return quartzRecipeManager;
    }

    public KisekiLegend() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::gatherData);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);
        ModSoundEvents.register(bus);

        ModBlockEntities.register(bus);
        ModEntities.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModCreativeModeTabs.register(bus);
        ModItems.register(bus);
        ModBlocks.register(bus);
        ModMenuTypes.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public KisekiLegend(IEventBus modEventBus, ModContainer modContainer) {
        // ... other registrations ...

        // Register network packets
    }

    private static final QuartzRecipeManager quartzRecipeManager = new QuartzRecipeManager();

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
        ModTags.Items.init(); // Add this line

        // Debug: Check if all our tag files exist
        String[] tagFiles = {"jewel", "water_material", "fire_material", "earth_material",
                "wind_material", "time_material", "space_material", "mirage_material", "plant"};

        for (String tagName : tagFiles) {
            try {
                var resource = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("data/kisekilegend/tags/items/" + tagName + ".json");
                if (resource != null) {
                    // Read and log the content
                    String content = new String(resource.readAllBytes());
                    resource.close();
                }
            } catch (Exception e) {
            }
        }
    }
    private void gatherData(GatherDataEvent event) {
        if (event.includeServer()) {
            event.getGenerator().addProvider(
                    true,
                    new DatapackBuiltinEntriesProvider(
                            event.getGenerator().getPackOutput(),
                            event.getLookupProvider(),
                            ModDatapackEntries.BUILDER,
                            Set.of(MOD_ID)
                    )
            );

            // Simplified tag provider registration
            event.getGenerator().addProvider(true, new ModItemTagProvider(
                    event.getGenerator().getPackOutput(),
                    event.getLookupProvider(),
                    CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                    event.getExistingFileHelper()
            ));
        }
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
            ev.accept(ModBlocks.ORBMENT_MACHINE);
            ev.accept(ModBlocks.QUARTZ_MACHINE); // Add the new machine
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server is starting!");
    }
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(quartzRecipeManager);

        // Force tag reload - this ensures tags are loaded properly
        LOGGER.info("Adding reload listeners - tags should be available after this");
    }
    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        LOGGER.info("=== TAGS UPDATED EVENT ===");
        LOGGER.info("Update cause: " + event.getUpdateCause());

        // Force check our tags after they're loaded
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            LOGGER.info("Server data loaded - our tags should now be available");

        }
    }
    @Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // Register GUIs


                MenuScreens.register(ModMenuTypes.QUARTZ_MACHINE_MENU.get(), QuartzMachineScreen::new);
                MenuScreens.register(ModMenuTypes.ORBMENT_MACHINE.get(), OrbmentMachineScreen::new);
                MenuScreens.register(ModMenuTypes.ORBMENT_MENU.get(), OrbmentScreen::new);
                MinecraftForge.EVENT_BUS.register(new ArtInputHandler());

                // Register Block Entity Renderers
                BlockEntityRenderers.register(ModBlockEntities.QUARTZ_MACHINE_BLOCK_ENTITY.get(), QuartzMachineRenderer::new);
                BlockEntityRenderers.register(ModBlockEntities.ORBMENT_MACHINE.get(), OrbmentMachineRenderer::new);

                // Register Entity Renderers - CRITICAL: This must be in enqueueWork!
                EntityRenderers.register(ModEntities.AURA_ENTITY.get(), AuraRenderer::new);

                LOGGER.info("Registered AuraRenderer for: " + ModEntities.AURA_ENTITY.get().getDescriptionId());
            });

            // Register input handler (this can be outside enqueueWork)
            MinecraftForge.EVENT_BUS.register(ArtInputHandler.class);
        }
    }
}