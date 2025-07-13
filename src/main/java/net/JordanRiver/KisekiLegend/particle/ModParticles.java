package net.JordanRiver.KisekiLegend.particle;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KisekiLegend.MOD_ID);

    public static final RegistryObject<SimpleParticleType> BLUE_FLOW =
            PARTICLES.register("blue_flow", () -> new SimpleParticleType(true));
}
