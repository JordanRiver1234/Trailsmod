// GeckoSpellRenderer.java
package net.JordanRiver.KisekiLegend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.model.GeckoSpellModel;
import net.JordanRiver.KisekiLegend.entity.GeckoSpellEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GeckoSpellRenderer extends GeoEntityRenderer<GeckoSpellEntity> {
    public GeckoSpellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeckoSpellModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public ResourceLocation getTextureLocation(GeckoSpellEntity e) {
        String art = e.getArtName();
        if (art.isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath(
                    KisekiLegend.MOD_ID, "textures/entity/stone_hammer.png");
        }
        // Note: vanilla expects the "textures/" prefix + ".png"
        return ResourceLocation.fromNamespaceAndPath(
                KisekiLegend.MOD_ID, "textures/entity/" + art + ".png");
    }

    @Override
    public RenderType getRenderType(GeckoSpellEntity a, ResourceLocation tex,
                                    MultiBufferSource buf, float pt) {
        return RenderType.entityCutoutNoCull(tex);
    }

    @Override
    public void render(GeckoSpellEntity e, float yaw, float pt,
                       PoseStack ms, MultiBufferSource buf, int light) {
        String artName = e.getArtName();
        if (artName.isEmpty()) {
            System.out.println("WARNING: Rendering spell with empty art name!");
            return;
        }

        System.out.println("Rendering spell - Art: " + artName +
                " | Position: " + e.position() +
                " | TickCount: " + e.tickCount +
                " | Hit: " + e.isHit());

        super.render(e, yaw, pt, ms, buf, light);
    }

    // Fixed: Changed parameter type from Camera to Frustum for 1.21.1
    @Override
    public boolean shouldRender(GeckoSpellEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        // Always render if entity has a valid art name
        return !entity.getArtName().isEmpty() && super.shouldRender(entity, frustum, camX, camY, camZ);
    }
}