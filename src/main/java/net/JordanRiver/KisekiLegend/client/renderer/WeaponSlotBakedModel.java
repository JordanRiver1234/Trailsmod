package net.JordanRiver.KisekiLegend.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WeaponSlotBakedModel implements BakedModel {

    private final BakedModel baseModel;
    private final Cache<String, BakedModel> modelCache;
    private final WeaponSlotItemOverrides itemOverrides;

    public WeaponSlotBakedModel(BakedModel baseModel) {
        this.baseModel = baseModel;
        this.modelCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.itemOverrides = new WeaponSlotItemOverrides(this);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        return baseModel.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return itemOverrides;
    }

    BakedModel getBaseModel() {
        return baseModel;
    }

    Cache<String, BakedModel> getModelCache() {
        return modelCache;
    }

    public static void invalidateWeaponModel(ItemStack weapon) {
        if (Minecraft.getInstance().level != null) {


            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.getItemRenderer().getItemModelShaper().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            }

        }
    }

    /**
     * FIXED: Always create new model for testing - disable caching temporarily
     */
    public BakedModel createCombinedModel(ItemStack itemStack, ItemDisplayContext context) {


        if (!WeaponSlotRenderer.hasWeaponSlots(itemStack)) {
            System.out.println("No weapon slots, returning base model");
            return baseModel;
        }

        WeaponSlotData slotData = WeaponSlotData.getOrCreate(itemStack);
        List<WeaponSlotData.WeaponSlot> activeSlots = new ArrayList<>();

        for (WeaponSlotData.WeaponSlot slot : slotData.getSlots()) {
            if (!slot.isClosed) {
                activeSlots.add(slot);

            }
        }

        if (activeSlots.isEmpty()) {
            return baseModel;
        }


        // FIXED: Always create new model - no caching for debugging
        return new CombinedWeaponSlotModel(baseModel, activeSlots, context);
    }

    public void invalidateCache() {
        modelCache.invalidateAll();
    }

    /**
     * Inner class that represents the final combined model
     */
    private static class CombinedWeaponSlotModel implements BakedModel {
        private final BakedModel baseModel;
        private final List<BakedQuad> combinedQuads;

        public CombinedWeaponSlotModel(BakedModel baseModel, List<WeaponSlotData.WeaponSlot> slots, ItemDisplayContext context) {
            this.baseModel = baseModel;
            this.combinedQuads = new ArrayList<>();

            // Start with base weapon quads
            List<BakedQuad> baseQuads = baseModel.getQuads(null, null, RandomSource.create());
            combinedQuads.addAll(baseQuads);

            // Add slot quads
            for (WeaponSlotData.WeaponSlot slot : slots) {


                List<BakedQuad> slotQuads = WeaponSlotQuadBuilder.create3DSlotQuads(slot, context, 0.0f, 15.0f);
                combinedQuads.addAll(slotQuads);


            }

        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            return side == null ? combinedQuads : List.of();
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, rand);
        }

        @Override
        public boolean useAmbientOcclusion() { return baseModel.useAmbientOcclusion(); }
        @Override
        public boolean isGui3d() { return baseModel.isGui3d(); }
        @Override
        public boolean usesBlockLight() { return baseModel.usesBlockLight(); }
        @Override
        public boolean isCustomRenderer() { return false; }
        @Override
        public @NotNull TextureAtlasSprite getParticleIcon() { return baseModel.getParticleIcon(); }
        @Override
        public @NotNull ItemTransforms getTransforms() { return baseModel.getTransforms(); }
        @Override
        public @NotNull ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}