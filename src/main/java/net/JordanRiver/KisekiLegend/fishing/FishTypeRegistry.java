package net.JordanRiver.KisekiLegend.fishing;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.fish.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class FishTypeRegistry {
    public static final DeferredRegister<EntityType<?>> FISH_ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KisekiLegend.MOD_ID);

    // Store all fish entity types
    private static final Map<String, RegistryObject<? extends EntityType<? extends BaseFishEntity>>> FISH_ENTITY_TYPES = new HashMap<>();

    // Register all fish entities
    public static final RegistryObject<EntityType<DaceEntity>> DACE = registerFish("dace", DaceEntity::new);
    public static final RegistryObject<EntityType<YamanyEntity>> YAMANY = registerFish("yamany", YamanyEntity::new);
    public static final RegistryObject<EntityType<CrabEntity>> CRAB = registerFish("crab", CrabEntity::new);
    public static final RegistryObject<EntityType<KasagoEntity>> KASAGO = registerFish("kasago", KasagoEntity::new);
    public static final RegistryObject<EntityType<LiberlCarpEntity>> LIBERL_CARP = registerFish("liberl_carp", LiberlCarpEntity::new);
    public static final RegistryObject<EntityType<PearlglassEntity>> PEARLGLASS = registerFish("pearlglass", PearlglassEntity::new);
    public static final RegistryObject<EntityType<GarvelzeEntity>> GARVELZE = registerFish("garvelze", GarvelzeEntity::new);
    public static final RegistryObject<EntityType<SnakeheadEntity>> SNAKEHEAD = registerFish("snakehead", SnakeheadEntity::new);
    // Add these missing registrations:
    public static final RegistryObject<EntityType<GoldAngelfishEntity>> GOLD_ANGELFISH = registerFish("gold_angelfish", GoldAngelfishEntity::new);
    public static final RegistryObject<EntityType<ValleriaBassEntity>> VALLERIA_BASS = registerFish("valleria_bass", ValleriaBassEntity::new);
    public static final RegistryObject<EntityType<RockeaterEntity>> ROCKEATER = registerFish("rockeater", RockeaterEntity::new);
    public static final RegistryObject<EntityType<GreatBlackfishEntity>> GREAT_BLACKFISH = registerFish("great_blackfish", GreatBlackfishEntity::new);
    public static final RegistryObject<EntityType<CarpEntity>> CARP = registerFish("carp", CarpEntity::new);
    public static final RegistryObject<EntityType<OctopusEntity>> OCTOPUS = registerFish("octopus", OctopusEntity::new);
    public static final RegistryObject<EntityType<RainbowTroutEntity>> RAINBOW_TROUT = registerFish("rainbow_trout", RainbowTroutEntity::new);
    public static final RegistryObject<EntityType<TroutEntity>> TROUT = registerFish("trout", TroutEntity::new);
    public static final RegistryObject<EntityType<EelEntity>> EEL = registerFish("eel", EelEntity::new);
    public static final RegistryObject<EntityType<SalmonEntity>> SALMON = registerFish("salmon", SalmonEntity::new);
    public static final RegistryObject<EntityType<ClaudineEntity>> CLAUDINE = registerFish("claudine", ClaudineEntity::new);
    public static final RegistryObject<EntityType<SeaBassEntity>> SEA_BASS = registerFish("sea_bass", SeaBassEntity::new);
    public static final RegistryObject<EntityType<GigangoraEntity>> GIGANGORA = registerFish("gigangora", GigangoraEntity::new);
    public static final RegistryObject<EntityType<MahimahiEntity>> MAHIMAHI = registerFish("mahimahi", MahimahiEntity::new);
    public static final RegistryObject<EntityType<TigerRockfishEntity>> TIGER_ROCKFISH = registerFish("tiger_rockfish", TigerRockfishEntity::new);
    public static final RegistryObject<EntityType<GranakorEntity>> GRANAKOR = registerFish("granakor", GranakorEntity::new);
    public static final RegistryObject<EntityType<BlueMarlinEntity>> BLUE_MARLIN = registerFish("blue_marlin", BlueMarlinEntity::new);
    public static final RegistryObject<EntityType<DynatradEntity>> DYNATRAD = registerFish("dynatrad", DynatradEntity::new);

    private static <T extends BaseFishEntity> RegistryObject<EntityType<T>> registerFish(String name, EntityType.EntityFactory<T> factory) {
        RegistryObject<EntityType<T>> entityType = FISH_ENTITIES.register(name,
                () -> EntityType.Builder.of(factory, MobCategory.WATER_CREATURE)
                        .sized(0.5f, 0.3f)
                        .clientTrackingRange(64)  // Increased from 8
                        .updateInterval(3)        // Reduced from 20
                        .setShouldReceiveVelocityUpdates(true)
                        .build(name));

        FISH_ENTITY_TYPES.put(name, entityType);
        return entityType;
    }

    public static EntityType<? extends BaseFishEntity> getFishEntityType(String fishName) {
        var entityType = FISH_ENTITY_TYPES.get(fishName);
        return entityType != null ? entityType.get() : null;
    }
    public static void debugEntityTypes() {
        for (String fishName : FISH_ENTITY_TYPES.keySet()) {
            EntityType<?> type = FISH_ENTITY_TYPES.get(fishName).get();
            KisekiLegend.LOGGER.info("Registered entity: " + fishName + " -> " + type);
        }
    }
    public static BaseFishEntity createFishEntity(String fishName, Level level) {
        if (fishName == null || fishName.isEmpty() || level == null) {
            KisekiLegend.LOGGER.error("Invalid parameters: fishName=" + fishName + ", level=" + level);
            return null;
        }

        try {
            EntityType<? extends BaseFishEntity> entityType = getFishEntityType(fishName);
            if (entityType != null) {
                BaseFishEntity fish = entityType.create(level);
                if (fish != null) {
                    KisekiLegend.LOGGER.info("Successfully created fish entity: " + fishName);
                    return fish;
                } else {
                    KisekiLegend.LOGGER.error("Failed to create fish entity: " + fishName);
                }
            } else {
                KisekiLegend.LOGGER.error("No entity type found for: " + fishName);
            }
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Error creating fish entity: " + fishName, e);
        }
        return null;
    }
}