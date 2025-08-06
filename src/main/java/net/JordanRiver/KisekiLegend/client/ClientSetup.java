package net.JordanRiver.KisekiLegend.client;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotBakedModel;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotGeometryLoader;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotRenderer;
import net.JordanRiver.KisekiLegend.particle.BlueFlowParticle;
import net.JordanRiver.KisekiLegend.particle.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.client.event.TextureStitchEvent;
// OR if that doesn't exist:
import net.minecraftforge.client.event.ModelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotRenderer.hasWeaponSlots;

@Mod.EventBusSubscriber(
        modid = KisekiLegend.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping RANGE_TELEPORT;
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
        RANGE_TELEPORT = new KeyMapping("key.kisekilegend.range_teleport", GLFW.GLFW_KEY_G, "key.categories.kisekilegend");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent ev) {
        ev.register(OPEN_RADIAL_MENU);
        ev.register(TOGGLE_EP_HUD);
        ev.register(TOGGLE_ART_SELECT_LEGACY);
        ev.register(ART_NEXT_LEGACY);
        ev.register(ART_PREV_LEGACY);
        ev.register(RANGE_TELEPORT);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLUE_FLOW.get(), BlueFlowParticle.Provider::new);
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientEventHandler::initializeCustomCursor);


    }
    // Add this to your ClientSetup class

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        System.out.println("=== WRAPPING ALL WEAPON MODELS UNIVERSALLY ===");

        Map<ModelResourceLocation, BakedModel> modelRegistry = event.getModels();
        Map<ModelResourceLocation, BakedModel> newModels = new HashMap<>();

        // Debug: Check what models exist
        System.out.println("Total models in registry: " + modelRegistry.size());
        int itemModelCount = 0;
        for (ModelResourceLocation location : modelRegistry.keySet()) {
            if (location.id().getPath().startsWith("item/")) {
                itemModelCount++;
                if (itemModelCount <= 5) { // Show first 5 for debugging
                    System.out.println("Found item model: " + location);
                }
            }
        }
        System.out.println("Total item models found: " + itemModelCount);

        // Get all registered items and wrap weapon/tool models
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ItemStack testStack = new ItemStack(item);

            if (WeaponSlotRenderer.isWeaponOrTool(testStack)) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId != null) {
                    // Try multiple possible model location formats
                    ModelResourceLocation[] possibleLocations = {
                            new ModelResourceLocation(itemId, "inventory"),
                            new ModelResourceLocation(itemId.withPrefix("item/"), "inventory"),
                            new ModelResourceLocation(itemId, "")
                    };

                    for (ModelResourceLocation modelLocation : possibleLocations) {
                        BakedModel originalModel = modelRegistry.get(modelLocation);
                        if (originalModel != null) {
                            System.out.println("Wrapping weapon/tool: " + itemId + " at location: " + modelLocation);
                            newModels.put(modelLocation, new WeaponSlotBakedModel(originalModel));
                            break; // Found it, no need to try other formats
                        }
                    }
                }
            }
        }

        // Apply all wrapped models
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : newModels.entrySet()) {
            modelRegistry.put(entry.getKey(), entry.getValue());
        }

        System.out.println("Successfully wrapped " + newModels.size() + " weapon/tool models!");
    }
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        // Force texture loading by creating a custom reload listener
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                System.out.println("Slot texture reload listener triggered");
            }
        });
    }

    private static boolean isLikelyWeaponModel(String path) {
        // Check for common weapon/tool names
        return path.contains("sword") ||
                path.contains("axe") ||
                path.contains("pickaxe") ||
                path.contains("shovel") ||
                path.contains("hoe") ||
                path.contains("bow") ||
                path.contains("crossbow") ||
                path.contains("trident") ||
                path.contains("weapon") ||
                path.contains("blade") ||
                path.contains("knife") ||
                path.contains("dagger") ||
                path.contains("staff") ||
                path.contains("wand");
    }



    // For EP bar and legacy mode toggle
    public static boolean showEP = true;
    public static boolean artSelectMode = false;

    // This should sync with the actual selected art from the orbment
    public static int selectedArtIdx = 0;

    // Method to sync the index with the actual selected art
    public static void syncSelectedArtIndex(String selectedArtName, java.util.List<String> availableArts) {
        if (selectedArtName != null && availableArts != null) {
            int index = availableArts.indexOf(selectedArtName);
            if (index >= 0) {
                selectedArtIdx = index;
            }
        }
    }
}