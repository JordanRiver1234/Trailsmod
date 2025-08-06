package net.JordanRiver.KisekiLegend.client.renderer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.JordanRiver.KisekiLegend.client.renderer.WeaponSlotItemOverrides;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class WeaponSlotGeometryLoader implements IGeometryLoader<WeaponSlotGeometryLoader.WeaponSlotGeometry> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "weapon_slot");

    @Override
    public WeaponSlotGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new WeaponSlotGeometry();
    }

    // Package-private class
    static class WeaponSlotGeometry implements IUnbakedGeometry<WeaponSlotGeometry> {

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            // FIXED: Determine base model from context
            ResourceLocation baseModelLocation = determineBaseModel(context);

            try {
                UnbakedModel baseModel = baker.getModel(baseModelLocation);
                BakedModel bakedBaseModel = baseModel.bake(baker, spriteGetter, modelState);

                return new WeaponSlotBakedModel(bakedBaseModel);

            } catch (Exception e) {
                System.err.println("WeaponSlotGeometry: Failed to bake base model " + baseModelLocation + ": " + e.getMessage());

                // Fallback to default handheld model
                try {
                    UnbakedModel fallbackModel = baker.getModel(ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld"));
                    BakedModel bakedFallbackModel = fallbackModel.bake(baker, spriteGetter, modelState);
                    System.out.println("WeaponSlotGeometry: Using fallback handheld model");
                    return new WeaponSlotBakedModel(bakedFallbackModel);
                } catch (Exception fallbackException) {
                    System.err.println("WeaponSlotGeometry: Even fallback model failed: " + fallbackException.getMessage());

                    // Ultimate fallback - return a basic generated model
                    return baker.bake(ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld"), modelState);
                }
            }
        }

        private ResourceLocation determineBaseModel(IGeometryBakingContext context) {
            // Try to determine the base model from context
            try {
                // Attempt to get model location from context (method may vary by Forge version)
                String contextString = context.toString().toLowerCase();

                if (contextString.contains("sword")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
                } else if (contextString.contains("axe")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
                } else if (contextString.contains("pickaxe")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
                } else if (contextString.contains("shovel")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
                } else if (contextString.contains("hoe")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
                } else if (contextString.contains("bow")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/bow");
                } else if (contextString.contains("crossbow")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/crossbow_standby");
                } else if (contextString.contains("trident")) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "item/trident");
                }

                // Default fallback
                return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");

            } catch (Exception e) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "item/handheld");
            }
        }
    }

    /**
     * Item overrides class for weapon slot models
     */

}