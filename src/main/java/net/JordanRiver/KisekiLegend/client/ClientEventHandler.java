// src/main/java/net/JordanRiver/KisekiLegend/client/ClientEventHandler.java

package net.JordanRiver.KisekiLegend.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentMachineScreen;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentScreen;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.animatable.GeoItem;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Handles various client-side events, including HUD toggling and custom cursor management.
 */
@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public class ClientEventHandler {

    private static long customCursor = 0L;
    private static boolean isCustomCursorSet = false;

    /**
     * Loads the custom cursor image and creates a GLFW cursor object.
     * This should be called once during client setup.
     */
    public static void initializeCustomCursor() {
        ResourceLocation cursorTexture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/cursor.png");
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(cursorTexture);
            try (InputStream inputStream = resource.open(); NativeImage nativeImage = NativeImage.read(inputStream)) {

                int width = nativeImage.getWidth();
                int height = nativeImage.getHeight();

                // Manually allocate a ByteBuffer since asByteBuffer() was removed.
                ByteBuffer byteBuffer = MemoryUtil.memAlloc(width * height * 4);

                // Iterate over each pixel to copy it to the buffer.
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // getPixelRGBA() returns color in ABGR format (Alpha, Blue, Green, Red).
                        int color = nativeImage.getPixelRGBA(x, y);

                        // Extract color components.
                        byte alpha = (byte) ((color >> 24) & 0xFF);
                        byte blue = (byte) ((color >> 16) & 0xFF);
                        byte green = (byte) ((color >> 8) & 0xFF);
                        byte red = (byte) (color & 0xFF);

                        // Put components into the buffer in RGBA order, which GLFW expects.
                        byteBuffer.put(red);
                        byteBuffer.put(green);
                        byteBuffer.put(blue);
                        byteBuffer.put(alpha);
                    }
                }
                // Prepare the buffer for reading.
                byteBuffer.flip();

                // Create a GLFWImage to hold the pixel data.
                GLFWImage glfwImage = GLFWImage.create();
                glfwImage.width(width);
                glfwImage.height(height);
                glfwImage.pixels(byteBuffer);

                // Create the cursor, with the "hotspot" at the top-left corner (0,0).
                customCursor = GLFW.glfwCreateCursor(glfwImage, 0, 0);

                // Free the manually allocated buffer now that the cursor is created.
                MemoryUtil.memFree(byteBuffer);

                // Add a shutdown hook to clean up the cursor when the game closes.
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (customCursor != 0L) {
                        GLFW.glfwDestroyCursor(customCursor);
                    }
                }, "KisekiLegend Cursor Cleanup"));

                KisekiLegend.LOGGER.info("Custom cursor loaded successfully.");
            }
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Failed to load custom cursor", e);
        }
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        // Toggle HUD
        if (ClientSetup.TOGGLE_EP_HUD.consumeClick()) {
            ClientSetup.showEP = !ClientSetup.showEP;
        }

        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null) return; // Removed level null check as it is not used before the cursor logic.

        // --- Custom Cursor Logic ---
        boolean shouldShowCustomCursor = mc.screen instanceof OrbmentScreen || mc.screen instanceof OrbmentMachineScreen;

        if (shouldShowCustomCursor) {
            // If the custom cursor should be shown but isn't, set it.
            if (!isCustomCursorSet && customCursor != 0L) {
                GLFW.glfwSetCursor(mc.getWindow().getWindow(), customCursor);
                isCustomCursorSet = true;
            }
        } else {
            // If the custom cursor should NOT be shown but currently is, revert to the default.
            if (isCustomCursorSet) {
                GLFW.glfwSetCursor(mc.getWindow().getWindow(), 0L); // 0L sets the default arrow cursor
                isCustomCursorSet = false;
            }
        }

        // --- Original Animation Logic ---
        if (level == null) return; // Re-add null check here before it's used.

        boolean hasAuraNearby = level.getEntitiesOfClass(AuraEntity.class, player.getBoundingBox().inflate(2.0))
                .stream().anyMatch(e -> {
                    var uuid = e.getOwnerUUID(); // You must implement this in AuraEntity
                    return uuid != null && uuid.equals(player.getUUID());
                });

        if (!hasAuraNearby) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GeoItem geoItem) {
                geoItem.triggerAnim(player, GeoItem.getId(stack), "cast_controller", "idle");
            }
        }
    }
}