package net.JordanRiver.KisekiLegend;

import com.mojang.logging.LogUtils;
import net.JordanRiver.KisekiLegend.client.FishingInputHandler;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotRenderer;
import net.JordanRiver.KisekiLegend.client.renderer.block.QuartzMachineRenderer;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.client.ArtInputHandler;
import net.JordanRiver.KisekiLegend.client.ClientSetup;
import net.JordanRiver.KisekiLegend.client.renderers.FishingGameRenderer;
import net.JordanRiver.KisekiLegend.client.renderers.KisekiFishingRodRenderer;
import net.JordanRiver.KisekiLegend.client.screen.*;
import net.JordanRiver.KisekiLegend.client.AuraRenderer;
import net.JordanRiver.KisekiLegend.commands.RecipeProgressCommand;
import net.JordanRiver.KisekiLegend.crafting.QuartzRecipeManager;
import net.JordanRiver.KisekiLegend.datagen.ModItemTagProvider;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.events.FishSpawnHandler;
import net.JordanRiver.KisekiLegend.events.WeaponSyncHandler;
import net.JordanRiver.KisekiLegend.fishing.FishTypeRegistry;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.JordanRiver.KisekiLegend.datagen.ModDatapackEntries;
import net.JordanRiver.KisekiLegend.entity.ModEntities;
import net.JordanRiver.KisekiLegend.item.ModCreativeModeTabs;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.ModMenuTypes;
import net.JordanRiver.KisekiLegend.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.JordanRiver.KisekiLegend.menu.QuartzMachineMenu;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.JordanRiver.KisekiLegend.client.renderer.OrbalTableRenderer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.JordanRiver.KisekiLegend.client.renderers.UniversalFishRenderer;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import java.util.Optional;
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

        ModBlocks.BLOCKS.register(bus); // Register blocks FIRST
        ModBlocks.ITEMS.register(bus);  // Then register items
// Register fish entities
        FishTypeRegistry.FISH_ENTITIES.register(bus);
        ModBlockEntities.register(bus);
        ModEntities.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModCreativeModeTabs.register(bus);
        ModItems.register(bus);
        ModMenuTypes.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FishSpawnHandler.class);

    }

    public KisekiLegend(IEventBus modEventBus, ModContainer modContainer) {
        // ... other registrations ...

        // Register network packets
    }

    private static final QuartzRecipeManager quartzRecipeManager = new QuartzRecipeManager();

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();

            // Debug entity registration
            KisekiLegend.LOGGER.info("=== CHECKING ENTITY REGISTRATION ===");
            KisekiLegend.LOGGER.info("Hook entity type: " + ModEntities.KISEKI_FISHING_HOOK.get());
            KisekiLegend.LOGGER.info("Carp entity type: " + FishTypeRegistry.CARP.get());
            KisekiLegend.LOGGER.info("Entity registry size: " + ForgeRegistries.ENTITY_TYPES.getKeys().size());
        });
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
            ev.accept(ModBlocks.ORBAL_TABLE);
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
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RecipeProgressCommand.register(event.getDispatcher());
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
    @Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            // Small fish (1-2 health, slower)
            event.put(FishTypeRegistry.DACE.get(), createFishAttributes(1.0D, 0.15D));
            event.put(FishTypeRegistry.YAMANY.get(), createFishAttributes(1.0D, 0.15D));
            event.put(FishTypeRegistry.KASAGO.get(), createFishAttributes(1.5D, 0.2D));

            // Medium fish (2-4 health, normal speed)
            event.put(FishTypeRegistry.LIBERL_CARP.get(), createFishAttributes(3.0D, 0.25D));
            event.put(FishTypeRegistry.CARP.get(), createFishAttributes(3.0D, 0.25D));
            event.put(FishTypeRegistry.TROUT.get(), createFishAttributes(2.5D, 0.3D));
            event.put(FishTypeRegistry.RAINBOW_TROUT.get(), createFishAttributes(2.5D, 0.3D));
            event.put(FishTypeRegistry.SALMON.get(), createFishAttributes(3.0D, 0.35D));
            event.put(FishTypeRegistry.VALLERIA_BASS.get(), createFishAttributes(3.5D, 0.3D));
            event.put(FishTypeRegistry.SEA_BASS.get(), createFishAttributes(3.5D, 0.3D));
            event.put(FishTypeRegistry.ROCKEATER.get(), createFishAttributes(2.0D, 0.1D)); // Very cautious

            // Large aggressive fish (4-6 health, fast)
            event.put(FishTypeRegistry.SNAKEHEAD.get(), createFishAttributes(4.0D, 0.4D));
            event.put(FishTypeRegistry.EEL.get(), createFishAttributes(3.5D, 0.35D));
            event.put(FishTypeRegistry.BLUE_MARLIN.get(), createFishAttributes(6.0D, 0.5D));
            event.put(FishTypeRegistry.MAHIMAHI.get(), createFishAttributes(6.0D, 0.45D));

            // Large bottom dwellers (5-8 health, slower)
            event.put(FishTypeRegistry.GARVELZE.get(), createFishAttributes(5.0D, 0.2D));
            event.put(FishTypeRegistry.GIGANGORA.get(), createFishAttributes(8.0D, 0.15D));
            event.put(FishTypeRegistry.GREAT_BLACKFISH.get(), createFishAttributes(4.0D, 0.25D));

            // Special creatures
            event.put(FishTypeRegistry.OCTOPUS.get(), createFishAttributes(3.0D, 0.3D));
            event.put(FishTypeRegistry.CRAB.get(), createFishAttributes(2.0D, 0.2D));
            event.put(FishTypeRegistry.GRANAKOR.get(), createFishAttributes(10.0D, 0.15D)); // Giant crab

            // Rare/mysterious fish
            event.put(FishTypeRegistry.CLAUDINE.get(), createFishAttributes(4.0D, 0.25D));
            event.put(FishTypeRegistry.PEARLGLASS.get(), createFishAttributes(3.0D, 0.2D)); // Graceful
            event.put(FishTypeRegistry.DYNATRAD.get(), createFishAttributes(15.0D, 0.4D)); // King of lake

            // Remaining fish
            event.put(FishTypeRegistry.GOLD_ANGELFISH.get(), createFishAttributes(1.5D, 0.25D));
            event.put(FishTypeRegistry.TIGER_ROCKFISH.get(), createFishAttributes(2.5D, 0.25D));
        }

        private static AttributeSupplier createFishAttributes(double health, double speed) {
            return PathfinderMob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, health)
                    .add(Attributes.MOVEMENT_SPEED, speed)
                    .build();
        }
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        KisekiLegend.LOGGER.info("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
        KisekiLegend.LOGGER.info("OpenGL Vendor: " + GL11.glGetString(GL11.GL_VENDOR));
        KisekiLegend.LOGGER.info("OpenGL Renderer: " + GL11.glGetString(GL11.GL_RENDERER));

        // Check available memory
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        KisekiLegend.LOGGER.info("Max Memory: " + (maxMemory / 1024 / 1024) + "MB");
        KisekiLegend.LOGGER.info("Total Memory: " + (totalMemory / 1024 / 1024) + "MB");
        KisekiLegend.LOGGER.info("Free Memory: " + (freeMemory / 1024 / 1024) + "MB");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Clean up any fish entities when player joins
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();

            // Remove any existing fish entities near spawn
            level.getEntitiesOfClass(BaseFishEntity.class,
                            new AABB(player.blockPosition()).inflate(64))
                    .stream()
                    .filter(fish -> !fish.fromBucket() && !fish.hasCustomName())
                    .forEach(Entity::discard);

            LOGGER.info("Cleaned up fish entities for joining player: " + player.getName().getString());
        }
    }


    // Also add this cleanup method:
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Clean up when server stops
        try {
            FishingGameManager.endFishing();
            System.gc();
        } catch (Exception e) {
            // Ignore cleanup errors during shutdown
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
                MenuScreens.register(ModMenuTypes.ORBAL_TABLE_MENU.get(), OrbalTableScreen::new);
                MinecraftForge.EVENT_BUS.register(WeaponSyncHandler.class);
                EntityRenderers.register(ModEntities.KISEKI_FISHING_HOOK.get(), FishingHookRenderer::new);

                // Register Block Entity Renderers
                BlockEntityRenderers.register(ModBlockEntities.QUARTZ_MACHINE_BLOCK_ENTITY.get(), QuartzMachineRenderer::new);
                BlockEntityRenderers.register(ModBlockEntities.ORBAL_TABLE.get(), OrbalTableRenderer::new);

                MinecraftForge.EVENT_BUS.register(net.JordanRiver.KisekiLegend.events.FishingRestrictionHandler.class);
                MinecraftForge.EVENT_BUS.register(net.JordanRiver.KisekiLegend.client.FishingInputHandler.class);
                // Register Entity Renderers - CRITICAL: This must be in enqueueWork!
                EntityRenderers.register(ModEntities.AURA_ENTITY.get(), AuraRenderer::new);
                LOGGER.info("Registered WeaponSlotRenderer");

                LOGGER.info("Registered AuraRenderer for: " + ModEntities.AURA_ENTITY.get().getDescriptionId());

                // Register all fish renderers
                EntityRenderers.register(FishTypeRegistry.DACE.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.YAMANY.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.CRAB.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.KASAGO.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.LIBERL_CARP.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.PEARLGLASS.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.GARVELZE.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.SNAKEHEAD.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.GOLD_ANGELFISH.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.VALLERIA_BASS.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.ROCKEATER.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.GREAT_BLACKFISH.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.CARP.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.OCTOPUS.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.RAINBOW_TROUT.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.TROUT.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.EEL.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.SALMON.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.CLAUDINE.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.SEA_BASS.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.GIGANGORA.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.MAHIMAHI.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.TIGER_ROCKFISH.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.GRANAKOR.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.BLUE_MARLIN.get(), UniversalFishRenderer::new);
                EntityRenderers.register(FishTypeRegistry.DYNATRAD.get(), UniversalFishRenderer::new);

                // Register the fishing game renderer
            });

            // Register input handler (this can be outside enqueueWork)
            MinecraftForge.EVENT_BUS.register(ArtInputHandler.class);
        }
    }

}