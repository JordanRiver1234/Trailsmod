package net.JordanRiver.KisekiLegend.client;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.client.screen.OrbmentMachineRenderer;
import net.JordanRiver.KisekiLegend.entity.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEntityRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPELL.get(), GeckoSpellRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ORBMENT_MACHINE.get(),
                context -> new OrbmentMachineRenderer());
    }


}

