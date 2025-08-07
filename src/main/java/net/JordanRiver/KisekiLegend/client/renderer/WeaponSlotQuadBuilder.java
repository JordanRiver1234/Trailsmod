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
    public static List<BakedQuad> create3DSlotQuads(WeaponSlotData.WeaponSlot slot, ItemDisplayContext context, float rotationY, float rotationX) {
        List<BakedQuad> quads = new ArrayList<>();

        // Determine which face this slot should be on based on its coordinates
        SlotFace face = determineSlotFace(slot.posX, slot.posY, slot.posZ);
        Vector3f slotPos = calculateSlotPosition(slot, face, context);

        float slotSize = getSlotSize(context);

        // Create slot quad facing the correct direction
        BakedQuad slotQuad = createSingleSlotQuad(slot.elementType, slotPos, slotSize);
        if (slotQuad != null) {
            quads.add(slotQuad);
        }

        // Add quartz if present
        if (slot.hasQuartz() && slot.quartzItem.getItem() instanceof QuartzItem quartzItem) {
            Vector3f quartzPos = calculateQuartzPosition(slotPos, face);
            BakedQuad quartzQuad = createSingleQuartzQuad(quartzItem.getQuartzId(), quartzPos, getQuartzSize(context));
            if (quartzQuad != null) {
                quads.add(quartzQuad);
            }
        }

        return quads;
    }
    private enum SlotFace {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }

    private static SlotFace determineSlotFace(float x, float y, float z) {
        System.out.println("=== DETERMINE SLOT FACE DEBUG ===");
        System.out.println("Input coordinates: x=" + x + ", y=" + y + ", z=" + z);

        SlotFace result;
        if (x > 0.5f) {
            result = SlotFace.RIGHT;
        } else if (x < -0.5f) {
            result = SlotFace.LEFT;
        } else if (y > 0.5f) {
            result = SlotFace.TOP;
        } else if (y < -0.5f) {
            result = SlotFace.BOTTOM;
        } else if (z > 0) {
            result = SlotFace.FRONT;
        } else {
            result = SlotFace.BACK;
        }

        System.out.println("Determined face: " + result);
        return result;
    }

    private static Vector3f calculateSlotPosition(WeaponSlotData.WeaponSlot slot, SlotFace face, ItemDisplayContext context) {
        System.out.println("=== CALCULATE SLOT POSITION DEBUG ===");
        System.out.println("Slot coordinates: x=" + slot.posX + ", y=" + slot.posY + ", z=" + slot.posZ);
        System.out.println("Face: " + face);

        // Keep your EXACT original positioning as base
        Vector3f pos = new Vector3f(
                slot.posX * 0.25f + 0.5f,
                -slot.posY * 0.3f + 0.5f,  // Increased from 0.23f to 0.3f for wider range
                0.56f + (slot.posZ * 0.03f) // Your original logic
        );

        System.out.println("Base position: " + pos);

        // Push slots outward from their respective faces, considering original Z position
        switch (face) {
            case FRONT -> pos.z += 0.01f;  // Push forward from front face
            case BACK -> pos.z -= 0.15f;   // Push backward from back face (MORE outward)
            case LEFT -> {
                pos.x -= 0.02f;   // Push left from left face
                // Adjust Z based on original slot Z position
                if (slot.posZ < 0) {
                    pos.z -= 0.15f;  // Back-positioned left slots
                } else {
                    pos.z += 0.01f;  // Front-positioned left slots
                }
            }
            case RIGHT -> {
                pos.x += 0.02f;  // Push right from right face
                // Adjust Z based on original slot Z position
                if (slot.posZ < 0) {
                    pos.z -= 0.15f;  // Back-positioned right slots
                } else {
                    pos.z += 0.01f;  // Front-positioned right slots
                }
            }
            case TOP -> {
                pos.y += 0.02f;    // Push up from top face
                // Adjust Z based on original slot Z position
                if (slot.posZ < 0) {
                    pos.z -= 0.15f;  // Back-positioned top slots
                } else {
                    pos.z += 0.01f;  // Front-positioned top slots
                }
            }
            case BOTTOM -> {
                pos.y -= 0.02f; // Push down from bottom face
                // Adjust Z based on original slot Z position
                if (slot.posZ < 0) {
                    pos.z -= 0.15f;  // Back-positioned bottom slots
                } else {
                    pos.z += 0.01f;  // Front-positioned bottom slots
                }
            }
        }

        System.out.println("Final position: " + pos);
        return pos;
    }

    private static Vector3f calculateQuartzPosition(Vector3f slotPos, SlotFace face) {
        Vector3f quartzPos = new Vector3f(slotPos);

        // Move quartz slightly outward from the slot based on face, with better alignment
        switch (face) {
            case FRONT -> quartzPos.z += 0.02f;  // More forward
            case BACK -> quartzPos.z -= 0.02f;   // Back face positioning
            case LEFT -> {
                quartzPos.x -= 0.015f;  // Less offset for better alignment
                // Check if this is a back-positioned left slot
                if (slotPos.z < 0.55f) {
                    quartzPos.z -= 0.03f;   // Back-positioned left slots
                } else {
                    quartzPos.z += 0.02f;   // Front-positioned left slots
                }
            }
            case RIGHT -> {
                quartzPos.x += 0.015f;  // Less offset for better alignment
                // Check if this is a back-positioned right slot
                if (slotPos.z < 0.55f) {
                    quartzPos.z -= 0.03f;   // Back-positioned right slots
                } else {
                    quartzPos.z += 0.02f;   // Front-positioned right slots
                }
            }
            case TOP -> {
                quartzPos.y += 0.015f;  // Less offset for better alignment
                // Check if this is a back-positioned top slot
                if (slotPos.z < 0.55f) {
                    quartzPos.z -= 0.03f;   // Back-positioned top slots
                } else {
                    quartzPos.z += 0.02f;   // Front-positioned top slots
                }
            }
            case BOTTOM -> {
                quartzPos.y -= 0.015f;  // Less offset for better alignment
                // Check if this is a back-positioned bottom slot
                if (slotPos.z < 0.55f) {
                    quartzPos.z -= 0.03f;   // Back-positioned bottom slots
                } else {
                    quartzPos.z += 0.02f;   // Front-positioned bottom slots
                }
            }
        }

        return quartzPos;
    }
    private static void createFaceSpecificSlotQuads(List<BakedQuad> quads, String elementType, Vector3f position, float size, SlotFace face) {
        Direction[] directions = getFaceDirections(face);

        for (Direction direction : directions) {
            BakedQuad quad = createDirectionalSlotQuad(elementType, position, size, direction);
            if (quad != null) {
                quads.add(quad);
            }
        }
    }

    private static void createFaceSpecificQuartzQuads(List<BakedQuad> quads, String quartzId, Vector3f position, float size, SlotFace face) {
        Direction[] directions = getFaceDirections(face);

        for (Direction direction : directions) {
            BakedQuad quad = createDirectionalQuartzQuad(quartzId, position, size, direction);
            if (quad != null) {
                quads.add(quad);
            }
        }
    }

    private static Direction[] getFaceDirections(SlotFace face) {
        return switch (face) {
            case FRONT -> new Direction[]{Direction.NORTH}; // Face forward
            case BACK -> new Direction[]{Direction.SOUTH}; // Face backward
            case LEFT -> new Direction[]{Direction.WEST}; // Face left
            case RIGHT -> new Direction[]{Direction.EAST}; // Face right
            case TOP -> new Direction[]{Direction.UP}; // Face up
            case BOTTOM -> new Direction[]{Direction.DOWN}; // Face down
        };
    }

    private static BakedQuad createSingleSlotQuad(String elementType, Vector3f position, float size) {
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

            if (sprite == null) return null;

            // Better logic: anything with Z < 0.55 came from back slots
            Direction face;
            if (position.z < 0.55f) {
                face = Direction.SOUTH;  // Back (includes pushed back slots)
                System.out.println("BACK quad - Z: " + position.z);
            } else {
                face = Direction.NORTH;  // Front
                System.out.println("FRONT quad - Z: " + position.z);
            }

            System.out.println("Position: " + position + " -> Face: " + face);

            return buildSingleQuad(sprite, position, size, face, 0xFFFFFFFF);

        } catch (Exception e) {
            System.err.println("Error creating single slot quad: " + e.getMessage());
            return null;
        }
    }

    private static BakedQuad createSingleQuartzQuad(String quartzId, Vector3f position, float size) {
        try {
            TextureAtlasSprite sprite = getQuartzSpriteEnhanced(quartzId);
            if (sprite != null) {
                // Determine face based on Z coordinate
                Direction face = position.z > 0.56f ? Direction.NORTH : Direction.SOUTH;
                return buildSingleQuad(sprite, position, size, face, 0xFFFFFFFF);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error creating single quartz quad: " + e.getMessage());
            return null;
        }
    }

    private static BakedQuad buildSingleQuad(TextureAtlasSprite sprite, Vector3f position, float size, Direction face, int color) {
        float halfSize = size / 2.0f;
        int[] vertexData = new int[32];

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        switch (face) {
            case SOUTH -> {
                // Back-facing quad
                putVertex(vertexData, 0, position.x - halfSize, position.y - halfSize, position.z, color, maxU, maxV, 0, 0, -1);
                putVertex(vertexData, 8, position.x - halfSize, position.y + halfSize, position.z, color, maxU, minV, 0, 0, -1);
                putVertex(vertexData, 16, position.x + halfSize, position.y + halfSize, position.z, color, minU, minV, 0, 0, -1);
                putVertex(vertexData, 24, position.x + halfSize, position.y - halfSize, position.z, color, minU, maxV, 0, 0, -1);
            }
            case EAST -> {
                // Right-facing quad
                putVertex(vertexData, 0, position.x, position.y - halfSize, position.z - halfSize, color, minU, maxV, 1, 0, 0);
                putVertex(vertexData, 8, position.x, position.y + halfSize, position.z - halfSize, color, minU, minV, 1, 0, 0);
                putVertex(vertexData, 16, position.x, position.y + halfSize, position.z + halfSize, color, maxU, minV, 1, 0, 0);
                putVertex(vertexData, 24, position.x, position.y - halfSize, position.z + halfSize, color, maxU, maxV, 1, 0, 0);
            }
            case WEST -> {
                // Left-facing quad
                putVertex(vertexData, 0, position.x, position.y - halfSize, position.z + halfSize, color, minU, maxV, -1, 0, 0);
                putVertex(vertexData, 8, position.x, position.y + halfSize, position.z + halfSize, color, minU, minV, -1, 0, 0);
                putVertex(vertexData, 16, position.x, position.y + halfSize, position.z - halfSize, color, maxU, minV, -1, 0, 0);
                putVertex(vertexData, 24, position.x, position.y - halfSize, position.z - halfSize, color, maxU, maxV, -1, 0, 0);
            }
            case UP -> {
                // Top-facing quad
                putVertex(vertexData, 0, position.x - halfSize, position.y, position.z - halfSize, color, minU, maxV, 0, 1, 0);
                putVertex(vertexData, 8, position.x - halfSize, position.y, position.z + halfSize, color, minU, minV, 0, 1, 0);
                putVertex(vertexData, 16, position.x + halfSize, position.y, position.z + halfSize, color, maxU, minV, 0, 1, 0);
                putVertex(vertexData, 24, position.x + halfSize, position.y, position.z - halfSize, color, maxU, maxV, 0, 1, 0);
            }
            case DOWN -> {
                // Bottom-facing quad
                putVertex(vertexData, 0, position.x + halfSize, position.y, position.z - halfSize, color, minU, maxV, 0, -1, 0);
                putVertex(vertexData, 8, position.x + halfSize, position.y, position.z + halfSize, color, minU, minV, 0, -1, 0);
                putVertex(vertexData, 16, position.x - halfSize, position.y, position.z + halfSize, color, maxU, minV, 0, -1, 0);
                putVertex(vertexData, 24, position.x - halfSize, position.y, position.z - halfSize, color, maxU, maxV, 0, -1, 0);
            }
            default -> {
                // Front-facing quad (NORTH)
                putVertex(vertexData, 0, position.x + halfSize, position.y - halfSize, position.z, color, minU, maxV, 0, 0, 1);
                putVertex(vertexData, 8, position.x + halfSize, position.y + halfSize, position.z, color, minU, minV, 0, 0, 1);
                putVertex(vertexData, 16, position.x - halfSize, position.y + halfSize, position.z, color, maxU, minV, 0, 0, 1);
                putVertex(vertexData, 24, position.x - halfSize, position.y - halfSize, position.z, color, maxU, maxV, 0, 0, 1);
            }
        }

        return new BakedQuad(vertexData, color, face, sprite, true);
    }

    private static void createQuartzQuadsAllSides(List<BakedQuad> quads, String quartzId, Vector3f position, float size) {
        // Create quartz quads facing multiple directions for 3D visibility
        Direction[] faces = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction face : faces) {
            BakedQuad quad = createDirectionalQuartzQuad(quartzId, position, size, face);
            if (quad != null) {
                quads.add(quad);
            }
        }
    }

    private static BakedQuad createDirectionalQuartzQuad(String quartzId, Vector3f position, float size, Direction face) {
        try {
            TextureAtlasSprite sprite = getQuartzSpriteEnhanced(quartzId);

            if (sprite != null) {
                return buildDirectionalQuad(sprite, position, size, face, 0xFFFFFFFF);
            } else {
                System.out.println("No sprite for directional quartz quad: " + quartzId);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error creating directional quartz quad: " + e.getMessage());
            return null;
        }
    }
    private static void createSlotQuadsAllSides(List<BakedQuad> quads, String elementType, Vector3f position, float size) {
        // Create quads facing multiple directions for 3D visibility
        Direction[] faces = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction face : faces) {
            BakedQuad quad = createDirectionalSlotQuad(elementType, position, size, face);
            if (quad != null) {
                quads.add(quad);
            }
        }
    }

    private static BakedQuad createDirectionalSlotQuad(String elementType, Vector3f position, float size, Direction face) {
        try {
            // Adjust position to place quad ON the surface of that face
            Vector3f adjustedPos = new Vector3f(position);
            switch (face) {
                case NORTH -> adjustedPos.z += 0.02f;  // Push forward from surface
                case SOUTH -> adjustedPos.z -= 0.02f;  // Push backward from surface
                case EAST -> adjustedPos.x += 0.02f;   // Push right from surface
                case WEST -> adjustedPos.x -= 0.02f;   // Push left from surface
                case UP -> adjustedPos.y += 0.02f;     // Push up from surface
                case DOWN -> adjustedPos.y -= 0.02f;   // Push down from surface
            }

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

            if (sprite == null) return null;

            return buildDirectionalQuad(sprite, adjustedPos, size, face, 0xFFFFFFFF);

        } catch (Exception e) {
            System.err.println("Error creating directional slot quad: " + e.getMessage());
            return null;
        }
    }

    private static BakedQuad buildDirectionalQuad(TextureAtlasSprite sprite, Vector3f position, float size, Direction face, int color) {
        float halfSize = size / 2.0f;
        int[] vertexData = new int[32];

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        Vector3f[] vertices = getDirectionalVertices(position, halfSize, face);

        putVertex(vertexData, 0, vertices[0].x, vertices[0].y, vertices[0].z, color, minU, maxV, face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ());
        putVertex(vertexData, 8, vertices[1].x, vertices[1].y, vertices[1].z, color, minU, minV, face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ());
        putVertex(vertexData, 16, vertices[2].x, vertices[2].y, vertices[2].z, color, maxU, minV, face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ());
        putVertex(vertexData, 24, vertices[3].x, vertices[3].y, vertices[3].z, color, maxU, maxV, face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ());
        return new BakedQuad(vertexData, color, face, sprite, true);
    }

    private static Vector3f[] getDirectionalVertices(Vector3f center, float halfSize, Direction face) {
        Vector3f[] vertices = new Vector3f[4];

        switch (face) {
            case NORTH -> {
                // Front face - facing toward viewer (outward from front)
                vertices[0] = new Vector3f(center.x + halfSize, center.y - halfSize, center.z);
                vertices[1] = new Vector3f(center.x + halfSize, center.y + halfSize, center.z);
                vertices[2] = new Vector3f(center.x - halfSize, center.y + halfSize, center.z);
                vertices[3] = new Vector3f(center.x - halfSize, center.y - halfSize, center.z);
            }
            case SOUTH -> {
                // Back face - vertices wound clockwise when viewed from back
                vertices[0] = new Vector3f(center.x - halfSize, center.y - halfSize, center.z);
                vertices[1] = new Vector3f(center.x + halfSize, center.y - halfSize, center.z);
                vertices[2] = new Vector3f(center.x + halfSize, center.y + halfSize, center.z);
                vertices[3] = new Vector3f(center.x - halfSize, center.y + halfSize, center.z);
            }
            case EAST -> {
                // Right face - facing outward from right side
                vertices[0] = new Vector3f(center.x, center.y - halfSize, center.z - halfSize);
                vertices[1] = new Vector3f(center.x, center.y + halfSize, center.z - halfSize);
                vertices[2] = new Vector3f(center.x, center.y + halfSize, center.z + halfSize);
                vertices[3] = new Vector3f(center.x, center.y - halfSize, center.z + halfSize);
            }
            case WEST -> {
                // Left face - facing outward from left side
                vertices[0] = new Vector3f(center.x, center.y - halfSize, center.z + halfSize);
                vertices[1] = new Vector3f(center.x, center.y + halfSize, center.z + halfSize);
                vertices[2] = new Vector3f(center.x, center.y + halfSize, center.z - halfSize);
                vertices[3] = new Vector3f(center.x, center.y - halfSize, center.z - halfSize);
            }
            case UP -> {
                // Top face - facing outward from top
                vertices[0] = new Vector3f(center.x - halfSize, center.y, center.z - halfSize);
                vertices[1] = new Vector3f(center.x - halfSize, center.y, center.z + halfSize);
                vertices[2] = new Vector3f(center.x + halfSize, center.y, center.z + halfSize);
                vertices[3] = new Vector3f(center.x + halfSize, center.y, center.z - halfSize);
            }
            case DOWN -> {
                // Bottom face - facing outward from bottom
                vertices[0] = new Vector3f(center.x + halfSize, center.y, center.z - halfSize);
                vertices[1] = new Vector3f(center.x + halfSize, center.y, center.z + halfSize);
                vertices[2] = new Vector3f(center.x - halfSize, center.y, center.z + halfSize);
                vertices[3] = new Vector3f(center.x - halfSize, center.y, center.z - halfSize);
            }
        }

        return vertices;
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