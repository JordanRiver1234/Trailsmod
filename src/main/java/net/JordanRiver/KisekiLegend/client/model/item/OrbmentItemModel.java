package net.JordanRiver.KisekiLegend.client.model.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OrbmentItemModel extends GeoModel<OrbmentItem> {

    @Override
    public ResourceLocation getModelResource(OrbmentItem object) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/orbment.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OrbmentItem object) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/item/orbment_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OrbmentItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/orbment.animation.json");
    }
}