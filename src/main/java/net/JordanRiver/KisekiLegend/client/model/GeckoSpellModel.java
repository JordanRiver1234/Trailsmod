package net.JordanRiver.KisekiLegend.client.model;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class GeckoSpellModel extends GeoModel<GeckoSpellEntity> {
    private static final ResourceLocation DEF_MODEL =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/stone_hammer.geo.json");
    private static final ResourceLocation DEF_ANIM  =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/stone_hammer.animation.json");
    private static final ResourceLocation DEF_TEX   =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "entity/stone_hammer");

    @Override
    public ResourceLocation getModelResource(GeckoSpellEntity e) {
        String art = e.getArtName();
        if (art.isEmpty()) return DEF_MODEL;
        return ResourceLocation.fromNamespaceAndPath(
                KisekiLegend.MOD_ID, "geo/" + art + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeckoSpellEntity e) {
        String art = e.getArtName();
        if (art.isEmpty()) return DEF_TEX;
        return ResourceLocation.fromNamespaceAndPath(
                KisekiLegend.MOD_ID, "entity/" + art);
    }

    @Override
    public ResourceLocation getAnimationResource(GeckoSpellEntity e) {
        String art = e.getArtName();
        if (art.isEmpty()) return DEF_ANIM;
        return ResourceLocation.fromNamespaceAndPath(
                KisekiLegend.MOD_ID, "animations/" + art + ".animation.json");
    }

    @Override
    public void setCustomAnimations(GeckoSpellEntity a, long id, AnimationState<GeckoSpellEntity> st) {
        super.setCustomAnimations(a, id, st);
        var root = getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setRotY((float) Math.toRadians(a.getYRot()));
            root.setRotX((float) Math.toRadians(a.getXRot()));
        }
    }
}
