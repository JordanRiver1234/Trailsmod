package net.JordanRiver.KisekiLegend.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.screen.ArtSelectionScreen;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentMachineScreen;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentScreen;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.item.enhancement.ItemEnhancementSystem;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.SetSelectedArtPacket;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.animatable.GeoItem;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public class ClientEventHandler {

    private static long customCursor = 0L;
    private static boolean isCustomCursorSet = false;

    public static void initializeCustomCursor() {
        ResourceLocation cursorTexture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/cursor.png");
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(cursorTexture);
            try (InputStream inputStream = resource.open(); NativeImage nativeImage = NativeImage.read(inputStream)) {
                ByteBuffer byteBuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
                for (int y = 0; y < nativeImage.getHeight(); y++) {
                    for (int x = 0; x < nativeImage.getWidth(); x++) {
                        int color = nativeImage.getPixelRGBA(x, y);
                        byte a = (byte) ((color >> 24) & 0xFF);
                        byte b = (byte) ((color >> 16) & 0xFF);
                        byte g = (byte) ((color >> 8) & 0xFF);
                        byte r = (byte) (color & 0xFF);
                        byteBuffer.put(r).put(g).put(b).put(a);
                    }
                }
                byteBuffer.flip();
                GLFWImage glfwImage = GLFWImage.create().width(nativeImage.getWidth()).height(nativeImage.getHeight()).pixels(byteBuffer);
                customCursor = GLFW.glfwCreateCursor(glfwImage, 0, 0);
                MemoryUtil.memFree(byteBuffer);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (customCursor != 0L) GLFW.glfwDestroyCursor(customCursor);
                }));
                KisekiLegend.LOGGER.info("Custom cursor loaded successfully.");
            }
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Failed to load custom cursor", e);
        }
    }
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack item = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // Add enhancement information to tooltip
        List<Component> enhancements = ItemEnhancementSystem.getEnhancementTooltip(item);
        if (!enhancements.isEmpty()) {
            tooltip.addAll(enhancements);
        }
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Handle custom cursor logic
        handleCustomCursor(mc);

        // Handle radial menu and other inputs ONLY when no screen is open
        if (mc.screen == null) {
            handleRadialMenuInput(mc, player);
            handleInGameInput(player);
        }

        // Handle animation logic
        handleAnimationLogic(mc, player);
    }

    private static void handleCustomCursor(Minecraft mc) {
        boolean shouldShowCustomCursor = mc.screen instanceof OrbmentScreen || mc.screen instanceof OrbmentMachineScreen;
        if (shouldShowCustomCursor) {
            if (!isCustomCursorSet && customCursor != 0L) {
                GLFW.glfwSetCursor(mc.getWindow().getWindow(), customCursor);
                isCustomCursorSet = true;
            }
        } else {
            if (isCustomCursorSet) {
                GLFW.glfwSetCursor(mc.getWindow().getWindow(), 0L);
                isCustomCursorSet = false;
            }
        }
    }

    private static void handleRadialMenuInput(Minecraft mc, Player player) {
        // Only handle radial menu input when no screen is open
        if (ClientSetup.OPEN_RADIAL_MENU.consumeClick()) {
            ItemStack orbment = findOrbmentInInventory(player);
            if (orbment.getItem() instanceof OrbmentItem) {
                mc.setScreen(new ArtSelectionScreen());
                player.playSound(ModSoundEvents.UI_CLOCK_OPEN.get(), 0.8f, 1.0f);
            }
        }
    }

    private static void handleAnimationLogic(Minecraft mc, Player player) {
        ClientLevel level = mc.level;
        if (level == null) return;

        boolean hasAuraNearby = level.getEntitiesOfClass(AuraEntity.class, player.getBoundingBox().inflate(2.0))
                .stream().anyMatch(e -> {
                    var uuid = e.getOwnerUUID();
                    return uuid != null && uuid.equals(player.getUUID());
                });

        if (!hasAuraNearby) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GeoItem geoItem) {
                geoItem.triggerAnim(player, GeoItem.getId(stack), "cast_controller", "idle");
            }
        }
    }

    private static ItemStack findOrbmentInInventory(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof OrbmentItem) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof OrbmentItem) return offHand;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void syncArtSelectionIfNeeded(Player player, ItemStack orb) {
        if (!(orb.getItem() instanceof OrbmentItem)) return;
        OrbmentComponent comp = OrbmentItem.loadComponentClientSide(orb, player.level());
        String currentSelection = comp.getSelectedArt();
        if (!currentSelection.isEmpty() && !comp.isArtAvailable(currentSelection)) {
            List<ArtsRegistry.ArtDefinition> availableArts = ArtsRegistry.ALL_ARTS.stream()
                    .filter(def -> comp.isArtAvailable(def.name()))
                    .sorted(Comparator.comparing(ArtsRegistry.ArtDefinition::name))
                    .collect(Collectors.toList());
            if (!availableArts.isEmpty()) {
                comp.setSelectedArt(availableArts.get(0).name());
// Send packet to server instead of saving directly
                NetworkHandler.sendToServer(new SetSelectedArtPacket(availableArts.get(0).name()));
            } else {
                comp.setSelectedArt("");
// Send packet to server instead of saving directly
                NetworkHandler.sendToServer(new SetSelectedArtPacket(""));
            }
        }
    }

    private static void handleInGameInput(Player player) {
        // Handle EP HUD toggle
        if (ClientSetup.TOGGLE_EP_HUD.consumeClick()) {
            ClientSetup.showEP = !ClientSetup.showEP;
        }

        // Handle legacy art select toggle
        if (ClientSetup.TOGGLE_ART_SELECT_LEGACY.consumeClick()) {
            ClientSetup.artSelectMode = !ClientSetup.artSelectMode;
            player.playSound(ModSoundEvents.ART_SELECT.get(), 0.7f, 1.0f);
            player.displayClientMessage(
                    Component.literal("Legacy Art Select " + (ClientSetup.artSelectMode ? "Enabled" : "Disabled")), true
            );
        }

        ItemStack orb = findOrbmentInInventory(player);
        if (player.level() != null) {
            syncArtSelectionIfNeeded(player, orb);
        }

        // Handle legacy art selection only if mode is enabled AND no screen is open
        if (ClientSetup.artSelectMode && Minecraft.getInstance().screen == null) {
            handleLegacyArtSelection(player, orb);
        }
    }

    private static void handleLegacyArtSelection(Player player, ItemStack orb) {
        if (!(orb.getItem() instanceof OrbmentItem) || player.level() == null) {
            return;
        }

        OrbmentComponent comp = OrbmentItem.loadComponentClientSide(orb, player.level());
        comp.recalculate();
        List<ArtsRegistry.ArtDefinition> availableArts = ArtsRegistry.ALL_ARTS.stream()
                .filter(def -> comp.isArtAvailable(def.name()))
                .sorted(Comparator.comparing(ArtsRegistry.ArtDefinition::name))
                .collect(Collectors.toList());

        if (availableArts.isEmpty()) return;

        String currentArtName = comp.getSelectedArt();
        int currentIndex = -1;
        for (int i = 0; i < availableArts.size(); i++) {
            if (availableArts.get(i).name().equals(currentArtName)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1 && !availableArts.isEmpty()) {
            currentIndex = 0;
        } else if (currentIndex == -1) {
            return;
        }

        // Sync the ClientSetup index with actual selection
        ClientSetup.selectedArtIdx = currentIndex;

        boolean changed = false;
        if (ClientSetup.ART_NEXT_LEGACY.consumeClick()) {
            currentIndex = (currentIndex + 1) % availableArts.size();
            changed = true;
        } else if (ClientSetup.ART_PREV_LEGACY.consumeClick()) {
            currentIndex = (currentIndex - 1 + availableArts.size()) % availableArts.size();
            changed = true;
        }

        if (changed) {
            ArtsRegistry.ArtDefinition newArt = availableArts.get(currentIndex);
            comp.setSelectedArt(newArt.name());

            // Update ClientSetup index and send to server
            ClientSetup.selectedArtIdx = currentIndex;
            NetworkHandler.sendToServer(new SetSelectedArtPacket(newArt.name()));

            float pitch = 0.9f + (currentIndex * 0.1f) % 0.4f;
            player.playSound(ModSoundEvents.ART_SELECT.get(), 0.4f, pitch);
            Minecraft.getInstance().gui.setOverlayMessage(Component.literal("Selected Art: " + newArt.name()), false);
        }
    }
}