package net.JordanRiver.KisekiLegend.client.model;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OrbmentMachineModel extends GeoModel<OrbmentMachineBlockEntity> {

    @Override
    public ResourceLocation getModelResource(OrbmentMachineBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/orbment_machine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OrbmentMachineBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/block/orbment_machine.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OrbmentMachineBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/orbment_machine.animation.json");
    }
}