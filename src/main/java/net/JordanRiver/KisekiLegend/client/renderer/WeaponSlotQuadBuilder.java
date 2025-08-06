package net.JordanRiver.KisekiLegend.client.renderer;

import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WeaponSlotQuadBuilder {

    /**
     * Creates BakedQuads for a 3D rounded slot (multiple quads for depth)
     */
    public static List<BakedQuad> create3DSlotQuads(WeaponSlotData.WeaponSlot slot, ItemDisplayContext context) {
        List<BakedQuad> quads = new ArrayList<>();

        Vector3f slotPos = new Vector3f(
                slot.posX * 0.25f + 0.5f,  // Negative X to flip horizontally
                -slot.posY * 0.23f + 0.5f,   // Keep Y as is
                0.57f
        );

        float slotSize = getSlotSize(context);



        // Create 3D depth effect with multiple layers
        for (int layer = 0; layer < 4; layer++) {
            float layerZ = slotPos.z + (layer * 0.01f); // FIXED: Much larger Z spacing for visibility
            float layerSize = slotSize * (1.0f - layer * 0.1f); // FIXED: More size variation

            Vector3f layerPos = new Vector3f(slotPos.x, slotPos.y, layerZ);

            BakedQuad slotQuad = createRounded3DSlotQuad(slot.elementType, layerPos, layerSize, layer);
            if (slotQuad != null) {
                quads.add(slotQuad);
            } else {
                System.out.println("Failed to create slot layer " + layer);
            }
        }

        // FIXED: Position quartz slightly above slot to reduce overlap
        if (slot.hasQuartz() && slot.quartzItem.getItem() instanceof QuartzItem quartzItem) {
            Vector3f quartzPos = new Vector3f(slotPos.x, slotPos.y, slotPos.z + 0.028f);


            List<BakedQuad> quartzQuads = create3DQuartzQuads(quartzItem.getQuartzId(), quartzPos, getQuartzSize(context));
            if (!quartzQuads.isEmpty()) {
                quads.addAll(quartzQuads);
            } else {
                System.out.println("No quartz quads created!");
            }

        }

        return quads;
    }

    private static float getSlotSize(ItemDisplayContext context) {
        return switch (context) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> 0.25f;
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> 0.22f;
            case GUI -> 0.20f;
            case GROUND, FIXED -> 0.30f;
            default -> 0.22f;
        };
    }

    private static float getQuartzSize(ItemDisplayContext context) {
        return getSlotSize(context) * 0.85f;
    }

    private static BakedQuad createRounded3DSlotQuad(String elementType, Vector3f position, float size, int layer) {
        try {
            ResourceLocation slotTexture = WeaponSlotRenderer.getSlotTexture(elementType);
            String texturePath = slotTexture.getPath();
            if (texturePath.startsWith("textures/")) {
                texturePath = texturePath.substring("textures/".length());
            }
            if (texturePath.endsWith(".png")) {
                texturePath = texturePath.substring(0, texturePath.length() - 4);
            }

            ResourceLocation atlasTexture = ResourceLocation.fromNamespaceAndPath(slotTexture.getNamespace(), texturePath);
            TextureAtlasSprite sprite = getSprite(atlasTexture);

            if (sprite == null) {
                System.err.println("Failed to get slot sprite for: " + atlasTexture);
                return null;
            }

            // FIXED: Use white tint, no element colors
            int layerTint = 0xFFFFFFFF;

            return buildRounded3DQuad(sprite, position, size, layerTint, layer);

        } catch (Exception e) {
            System.err.println("Error creating rounded 3D slot quad: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static List<BakedQuad> create3DQuartzQuads(String quartzId, Vector3f position, float size) {
        List<BakedQuad> quads = new ArrayList<>();



        // Create 3 layers for quartz depth
        for (int layer = 0; layer < 3; layer++) {
            float layerZ = position.z + (layer * 0.005f); // FIXED: Larger spacing
            float layerSize = size * (1.0f - layer * 0.05f);

            Vector3f layerPos = new Vector3f(position.x, position.y, layerZ);

            BakedQuad quartzQuad = create3DQuartzQuad(quartzId, layerPos, layerSize, layer);
            if (quartzQuad != null) {
                quads.add(quartzQuad);
            } else {
                System.out.println("Failed to create quartz layer " + layer);
            }
        }

        return quads;
    }

    private static BakedQuad create3DQuartzQuad(String quartzId, Vector3f position, float size, int layer) {
        try {

            TextureAtlasSprite sprite = getQuartzSpriteEnhanced(quartzId);

            if (sprite != null) {
                // FIXED: Use -1 (0xFFFFFFFF) which is pure white with full alpha
                // Remove any color processing that might cause red tinting
                int layerTint = -1; // This is 0xFFFFFFFF in two's complement


                return buildRounded3DQuad(sprite, position, size, layerTint, layer);
            } else {
                System.out.println("No sprite for 3D quartz layer " + layer);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error creating 3D quartz quad layer " + layer + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static TextureAtlasSprite getQuartzSpriteEnhanced(String quartzId) {
        try {


            Minecraft mc = Minecraft.getInstance();

            // Try multiple texture path variations
            ResourceLocation[] possibleTextures = {
                    ResourceLocation.fromNamespaceAndPath("kisekilegend", "item/" + quartzId),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/" + quartzId),
                    ResourceLocation.fromNamespaceAndPath("kisekilegend", "items/" + quartzId), // Alternative path
                    ResourceLocation.fromNamespaceAndPath("kisekilegend", "quartz/" + quartzId)  // Alternative path
            };

            for (ResourceLocation texture : possibleTextures) {
                System.out.println("Trying texture: " + texture);
                TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);

                if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                    return sprite;
                } else {
                    System.out.println("FAILED: Missing texture for " + texture);
                }
            }

            System.err.println("All texture attempts failed for quartz ID: " + quartzId);
            // Fallback to a known working texture
            return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/diamond"));

        } catch (Exception e) {
            System.err.println("Exception in getQuartzSpriteEnhanced: " + e.getMessage());
            e.printStackTrace();
            // Ultimate fallback
            Minecraft mc = Minecraft.getInstance();
            return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/iron_ingot"));
        }
    }

    private static TextureAtlasSprite getSprite(ResourceLocation texture) {
        try {

            Minecraft mc = Minecraft.getInstance();
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);

            if (sprite != null) {
                String spriteName = sprite.contents().name().toString();

                if (!spriteName.contains("missingno")) {
                    return sprite;
                } else {
                    System.err.println("ERROR: Got missingno texture for: " + texture);
                }
            } else {
                System.err.println("ERROR: Sprite is null for: " + texture);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Exception loading sprite " + texture + ": " + e.getMessage());
            return null;
        }
    }

    private static BakedQuad buildRounded3DQuad(TextureAtlasSprite sprite, Vector3f position, float size, int tint, int layer) {
        float halfSize = size / 2.0f;
        int[] vertexData = new int[32]; // 4 vertices * 8 ints per vertex

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        float z = position.z;

        // Create rounded corners
        float uvInset = 0.1f + (layer * 0.02f);
        float adjustedMinU = minU + (maxU - minU) * uvInset;
        float adjustedMaxU = maxU - (maxU - minU) * uvInset;
        float adjustedMinV = minV + (maxV - minV) * uvInset;
        float adjustedMaxV = maxV - (maxV - minV) * uvInset;

        float roundingFactor = 0.9f - (layer * 0.02f);
        float roundedHalfSize = halfSize * roundingFactor;

        // FIXED: Force white color (no tinting)
        int vertexColor = 0xFFFFFFFF; // Pure white, ignore the tint parameter for quartz

        // Counter-clockwise vertex winding
        putVertex(vertexData, 0,
                position.x + roundedHalfSize, position.y - roundedHalfSize, z,
                vertexColor, adjustedMinU, adjustedMaxV, 0, 0, 1);

        putVertex(vertexData, 8,
                position.x + roundedHalfSize, position.y + roundedHalfSize, z,
                vertexColor, adjustedMinU, adjustedMinV, 0, 0, 1);

        putVertex(vertexData, 16,
                position.x - roundedHalfSize, position.y + roundedHalfSize, z,
                vertexColor, adjustedMaxU, adjustedMinV, 0, 0, 1);

        putVertex(vertexData, 24,
                position.x - roundedHalfSize, position.y - roundedHalfSize, z,
                vertexColor, adjustedMaxU, adjustedMaxV, 0, 0, 1);



        // FIXED: Pass white color to BakedQuad constructor, not the tint parameter
        return new BakedQuad(vertexData, 0xFFFFFFFF, Direction.NORTH, sprite, true);
    }
    private static void putVertex(int[] data, int offset, float x, float y, float z, int color, float u, float v, float nx, float ny, float nz) {
        data[offset] = Float.floatToRawIntBits(x);
        data[offset + 1] = Float.floatToRawIntBits(y);
        data[offset + 2] = Float.floatToRawIntBits(z);
        data[offset + 3] = color;
        data[offset + 4] = Float.floatToRawIntBits(u);
        data[offset + 5] = Float.floatToRawIntBits(v);
        data[offset + 6] = Float.floatToRawIntBits(nx);
        data[offset + 7] = 0;
    }
}