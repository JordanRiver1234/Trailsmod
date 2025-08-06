package net.JordanRiver.KisekiLegend.client.model;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OrbalTableModel extends GeoModel<OrbalTableBlockEntity> {

    @Override
    public ResourceLocation getModelResource(OrbalTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/orbal_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OrbalTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/block/orbal_table.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OrbalTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/orbal_table.animation.json");
    }
}