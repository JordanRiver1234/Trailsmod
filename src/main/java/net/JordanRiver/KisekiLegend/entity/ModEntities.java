package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KisekiLegend.MOD_ID);

    public static final RegistryObject<EntityType<GeckoSpellEntity>> SPELL = ENTITIES.register("gecko_spell",
            () -> EntityType.Builder.<GeckoSpellEntity>of(GeckoSpellEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("gecko_spell"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}