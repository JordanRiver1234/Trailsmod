package net.JordanRiver.KisekiLegend.client.renderer.item;

import net.JordanRiver.KisekiLegend.client.model.item.OrbmentItemModel;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OrbmentItemRenderer extends GeoItemRenderer<OrbmentItem> {
    public OrbmentItemRenderer() {
        super(new OrbmentItemModel());
        System.out.println("GeoItemRenderer: OrbmentItemRenderer is loaded!");

    }
}
