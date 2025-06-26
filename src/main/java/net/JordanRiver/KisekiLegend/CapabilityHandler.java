package net.JordanRiver.KisekiLegend;

import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityHandler {
    @SubscribeEvent
    public static void attachPlayerCaps(AttachCapabilitiesEvent<Entity> ev) {
        if (!(ev.getObject() instanceof Player)) return;

        var provider = new ICapabilityProvider() {
            private final LazyOptional<OrbmentComponent> inst = LazyOptional.of(OrbmentComponent::new);
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, net.minecraft.core.Direction side) {
                return cap == OrbmentComponent.CAPABILITY ? inst.cast() : LazyOptional.empty();
            }
        };

        ev.addCapability(
                ResourceLocation.parse(KisekiLegend.MOD_ID + ":orbment"),
                provider
        );
    }
}
