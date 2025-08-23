package net.JordanRiver.KisekiLegend.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.KisekiFishingHook;
import net.JordanRiver.KisekiLegend.fishing.FishTypeRegistry;
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

    public static final RegistryObject<EntityType<KisekiFishingHook>> KISEKI_FISHING_HOOK = ENTITIES.register("kiseki_fishing_hook",
            () -> EntityType.Builder.<KisekiFishingHook>of(KisekiFishingHook::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .build("kiseki_fishing_hook"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}