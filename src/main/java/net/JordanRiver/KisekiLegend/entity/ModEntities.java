package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

    public static final RegistryObject<EntityType<AuraEntity>> AURA_ENTITY = ENTITIES.register("aura_entity",
            () -> EntityType.Builder.<AuraEntity>of(AuraEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(32) // Increased tracking range
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false) // Aura doesn't need velocity updates
                    .build(ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "aura_entity").toString()));

    public static final RegistryObject<EntityType<MagicCircleEntity>> MAGIC_CIRCLE_ENTITY = ENTITIES.register("magic_circle_entity",
            () -> EntityType.Builder.<MagicCircleEntity>of(MagicCircleEntity::new, MobCategory.MISC)
                    .sized(2.0f, 0.1f)
                    .clientTrackingRange(32) // Increased tracking range
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false) // Magic circle doesn't need velocity updates
                    .build(ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "magic_circle_entity").toString()));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}