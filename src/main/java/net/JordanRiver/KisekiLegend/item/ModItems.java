package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.fishing.FishData;
import net.JordanRiver.KisekiLegend.fishing.FishRegistry;
import net.JordanRiver.KisekiLegend.fishing.FishTypeRegistry;
import net.JordanRiver.KisekiLegend.items.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.geckolib.animatable.GeoItem;
import net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem;
import net.JordanRiver.KisekiLegend.fishing.RodType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {
    public static final Map<String, RegistryObject<Item>> QUARTZ = new HashMap<>();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KisekiLegend.MOD_ID);

    public static final RegistryObject<Item> EARTH = ITEMS.register("earth", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER = ITEMS.register("water", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE = ITEMS.register("fire", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND = ITEMS.register("wind", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIME = ITEMS.register("time", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPACE = ITEMS.register("space", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE = ITEMS.register("mirage", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEPITH_MASS = ITEMS.register("sepith_mass", () ->
            new SepithMassItem(new Item.Properties()));

    public static final RegistryObject<Item> EARTH_MASS = ITEMS.register("earth_mass", () ->
            new ElementalMassItem("earth", new Item.Properties()));
    public static final RegistryObject<Item> FIRE_MASS = ITEMS.register("fire_mass", () ->
            new ElementalMassItem("fire", new Item.Properties()));
    public static final RegistryObject<Item> WATER_MASS = ITEMS.register("water_mass", () ->
            new ElementalMassItem("water", new Item.Properties()));
    public static final RegistryObject<Item> WIND_MASS = ITEMS.register("wind_mass", () ->
            new ElementalMassItem("wind", new Item.Properties()));
    public static final RegistryObject<Item> TIME_MASS = ITEMS.register("time_mass", () ->
            new ElementalMassItem("time", new Item.Properties()));
    public static final RegistryObject<Item> SPACE_MASS = ITEMS.register("space_mass", () ->
            new ElementalMassItem("space", new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE_MASS = ITEMS.register("mirage_mass", () ->
            new ElementalMassItem("mirage", new Item.Properties()));

    public static final RegistryObject<Item> POT_O_MEAT = ITEMS.register("pot_o_meat",
            () -> new Item(new Item.Properties().food(ModFoodProperties.POT_O_MEAT)));
    public static final RegistryObject<Item> BOUILLABAISSE = ITEMS.register("bouillabaisse", () ->
            new Item(new Item.Properties().food(ModFoodProperties.BOUILLABAISSE)));
    public static final RegistryObject<Item> CHEFS_CURRY = ITEMS.register("chefs_curry", () ->
            new Item(new Item.Properties().food(ModFoodProperties.CHEFS_CURRY)));
    public static final RegistryObject<Item> WILD_VEGGIE_POT = ITEMS.register("wild_veggie_pot",
            () -> new Item(new Item.Properties().food(ModFoodProperties.WILD_VEGGIE_POT)));
    public static final RegistryObject<Item> SALUBRIOUS_OATMEAL = ITEMS.register("salubrious_oatmeal",
            () -> new Item(new Item.Properties().food(ModFoodProperties.SALUBRIOUS_OATMEAL)));
    public static final RegistryObject<Item> JENIS_LUNCH = ITEMS.register("jenis_lunch",
            () -> new Item(new Item.Properties().food(ModFoodProperties.JENIS_LUNCH)));
    public static final RegistryObject<Item> LIBERL_OMELET = ITEMS.register("liberl_omelet",
            () -> new Item(new Item.Properties().food(ModFoodProperties.LIBERL_OMELET)));
    public static final RegistryObject<Item> CHEESE_RISOTTO = ITEMS.register("cheese_risotto",
            () -> new Item(new Item.Properties().food(ModFoodProperties.CHEESE_RISOTTO)));
    public static final RegistryObject<Item> ABADDON_POTLUCK = ITEMS.register("abaddon_potluck",
            () -> new Item(new Item.Properties().food(ModFoodProperties.ABADDON_POTLUCK)));
    public static final RegistryObject<Item> WHOLESOME_PASTA = ITEMS.register("wholesome_pasta",
            () -> new Item(new Item.Properties().food(ModFoodProperties.WHOLESOME_PASTA)));
    public static final RegistryObject<Item> DIEHARD_PAELLA = ITEMS.register("diehard_paella",
            () -> new Item(new Item.Properties().food(ModFoodProperties.DIEHARD_PAELLA)));

    // Regular bait items - all stackable to 64
    public static final RegistryObject<Item> EARTHWORM = ITEMS.register("earthworm",
            () -> new BaitItem("earthworm", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> POLYCHAETE = ITEMS.register("polychaete", () ->
            new BaitItem("polychaete", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SHRIMPLET = ITEMS.register("shrimplet", () ->
            new BaitItem("shrimplet", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> DUMPLINGS = ITEMS.register("dumplings", () ->
            new BaitItem("dumplings", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> FROG = ITEMS.register("frog", () ->
            new BaitItem("frog", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> RED_FLIES = ITEMS.register("red_flies", () ->
            new BaitItem("red_flies", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> RIVER_BUG = ITEMS.register("river_bug", () ->
            new BaitItem("river_bug", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> RIVER_SNAIL = ITEMS.register("river_snail", () ->
            new BaitItem("river_snail", new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> ROE = ITEMS.register("roe", () ->
            new BaitItem("roe", new Item.Properties().stacksTo(64)));

    // Add fishing rods
    public static final RegistryObject<Item> PROGRESS_ROD = ITEMS.register("progress_rod", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(64), RodType.PROGRESS_ROD));
    public static final RegistryObject<Item> MARINE_STAR_ROD = ITEMS.register("marine_star_rod", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(80), RodType.MARINE_STAR_ROD));
    public static final RegistryObject<Item> PISCES_HEART = ITEMS.register("pisces_heart", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(96), RodType.PISCES_HEART));
    public static final RegistryObject<Item> BAMBOO_FISHING_ROD = ITEMS.register("bamboo_fishing_rod", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(112), RodType.BAMBOO_FISHING_ROD));
    public static final RegistryObject<Item> METAL_TRIDENT_ROD = ITEMS.register("metal_trident_rod", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(128), RodType.METAL_TRIDENT_ROD));
    public static final RegistryObject<Item> LAKELORD_II = ITEMS.register("lakelord_ii", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(144), RodType.LAKELORD_II));
    public static final RegistryObject<Item> AQUA_MASTER = ITEMS.register("aqua_master", () ->
            new KisekiFishingRodItem(new Item.Properties().durability(200), RodType.AQUA_MASTER));

    // Fish items with custom food properties


    public static final RegistryObject<Item> GOLD_ANGELFISH = ITEMS.register("gold_angelfish", () ->
            new FishItem("gold_angelfish", new Item.Properties().food(ModFoodProperties.GOLD_ANGELFISH)));
    public static final RegistryObject<Item> LIBERL_CARP = ITEMS.register("liberl_carp", () ->
            new FishItem("liberl_carp", new Item.Properties().food(ModFoodProperties.LIBERL_CARP)));

    public static final RegistryObject<Item> VALLERIA_BASS = ITEMS.register("valleria_bass", () ->
            new FishItem("valleria_bass", new Item.Properties().food(ModFoodProperties.VALLERIA_BASS)));
    public static final RegistryObject<Item> ROCKEATER = ITEMS.register("rockeater", () ->
            new FishItem("rockeater", new Item.Properties().food(ModFoodProperties.ROCKEATER)));
    public static final RegistryObject<Item> GREAT_BLACKFISH = ITEMS.register("great_blackfish", () ->
            new FishItem("great_blackfish", new Item.Properties().food(ModFoodProperties.GREAT_BLACKFISH)));

    public static final RegistryObject<Item> OCTOPUS = ITEMS.register("octopus", () ->
            new FishItem("octopus", new Item.Properties().food(ModFoodProperties.OCTOPUS)));
    public static final RegistryObject<Item> RAINBOW_TROUT = ITEMS.register("rainbow_trout", () ->
            new FishItem("rainbow_trout", new Item.Properties().food(ModFoodProperties.RAINBOW_TROUT)));


    public static final RegistryObject<Item> CLAUDINE = ITEMS.register("claudine", () ->
            new FishItem("claudine", new Item.Properties().food(ModFoodProperties.CLAUDINE)));
    public static final RegistryObject<Item> SNAKEHEAD = ITEMS.register("snakehead", () ->
            new FishItem("snakehead", new Item.Properties().food(ModFoodProperties.SNAKEHEAD)));
    public static final RegistryObject<Item> PEARLGLASS = ITEMS.register("pearlglass", () ->
            new FishItem("pearlglass", new Item.Properties().food(ModFoodProperties.PEARLGLASS)));
    public static final RegistryObject<Item> GARVELZE = ITEMS.register("garvelze", () ->
            new FishItem("garvelze", new Item.Properties().food(ModFoodProperties.GARVELZE)));

    public static final RegistryObject<Item> GIGANGORA = ITEMS.register("gigangora", () ->
            new FishItem("gigangora", new Item.Properties().food(ModFoodProperties.GIGANGORA)));
    public static final RegistryObject<Item> MAHIMAHI = ITEMS.register("mahimahi", () ->
            new FishItem("mahimahi", new Item.Properties().food(ModFoodProperties.MAHIMAHI)));
    public static final RegistryObject<Item> TIGER_ROCKFISH = ITEMS.register("tiger_rockfish", () ->
            new FishItem("tiger_rockfish", new Item.Properties().food(ModFoodProperties.TIGER_ROCKFISH)));
    public static final RegistryObject<Item> GRANAKOR = ITEMS.register("granakor", () ->
            new FishItem("granakor", new Item.Properties().food(ModFoodProperties.GRANAKOR)));
    public static final RegistryObject<Item> BLUE_MARLIN = ITEMS.register("blue_marlin", () ->
            new FishItem("blue_marlin", new Item.Properties().food(ModFoodProperties.BLUE_MARLIN)));

    public static final RegistryObject<Item> DYNATRAD = ITEMS.register("dynatrad", () ->
            new FishItem("dynatrad", new Item.Properties().food(ModFoodProperties.DYNATRAD)));


    // Fish items that can be bait - stackable to 64
    public static final RegistryObject<Item> CRAB = ITEMS.register("crab", () ->
            new FishItem("crab", new Item.Properties().food(ModFoodProperties.CRAB).stacksTo(64)));
    public static final RegistryObject<Item> DACE = ITEMS.register("dace", () ->
            new FishItem("dace", new Item.Properties().food(ModFoodProperties.DACE).stacksTo(64)));
    public static final RegistryObject<Item> CARP = ITEMS.register("carp", () ->
            new FishItem("carp", new Item.Properties().food(ModFoodProperties.CARP).stacksTo(64)));
    public static final RegistryObject<Item> EEL = ITEMS.register("eel", () ->
            new FishItem("eel", new Item.Properties().food(ModFoodProperties.EEL).stacksTo(64)));
    public static final RegistryObject<Item> KASAGO = ITEMS.register("kasago", () ->
            new FishItem("kasago", new Item.Properties().food(ModFoodProperties.KASAGO).stacksTo(64)));
    public static final RegistryObject<Item> SALMON = ITEMS.register("salmon", () ->
            new FishItem("salmon", new Item.Properties().food(ModFoodProperties.SALMON).stacksTo(64)));
    public static final RegistryObject<Item> SEA_BASS = ITEMS.register("sea_bass", () ->
            new FishItem("sea_bass", new Item.Properties().food(ModFoodProperties.SEA_BASS).stacksTo(64)));
    public static final RegistryObject<Item> TROUT = ITEMS.register("trout", () ->
            new FishItem("trout", new Item.Properties().food(ModFoodProperties.TROUT).stacksTo(64)));
    public static final RegistryObject<Item> YAMANY = ITEMS.register("yamany", () ->
            new FishItem("yamany", new Item.Properties().food(ModFoodProperties.YAMANY).stacksTo(64)));




    // Helper methods
    private net.minecraft.world.item.Item getBaitItem(String baitName) {
        // Check regular bait items first
        Item regularBait = switch (baitName) {
            case "earthworm" -> net.JordanRiver.KisekiLegend.item.ModItems.EARTHWORM.get();
            case "polychaete" -> net.JordanRiver.KisekiLegend.item.ModItems.POLYCHAETE.get();
            case "shrimplet" -> net.JordanRiver.KisekiLegend.item.ModItems.SHRIMPLET.get();
            case "dumplings" -> net.JordanRiver.KisekiLegend.item.ModItems.DUMPLINGS.get();
            case "red_flies" -> net.JordanRiver.KisekiLegend.item.ModItems.RED_FLIES.get();
            case "river_bug" -> net.JordanRiver.KisekiLegend.item.ModItems.RIVER_BUG.get();
            case "roe" -> net.JordanRiver.KisekiLegend.item.ModItems.ROE.get();
            case "river_snail" -> net.JordanRiver.KisekiLegend.item.ModItems.RIVER_SNAIL.get();
            case "frog" -> net.JordanRiver.KisekiLegend.item.ModItems.FROG.get();
            default -> null;
        };

        if (regularBait != null) return regularBait;

        // Check fish items that can be bait
        return switch (baitName) {
            case "carp" -> net.JordanRiver.KisekiLegend.item.ModItems.CARP.get();
            case "crab" -> net.JordanRiver.KisekiLegend.item.ModItems.CRAB.get();
            case "dace" -> net.JordanRiver.KisekiLegend.item.ModItems.DACE.get();
            case "eel" -> net.JordanRiver.KisekiLegend.item.ModItems.EEL.get();
            case "kasago" -> net.JordanRiver.KisekiLegend.item.ModItems.KASAGO.get();
            case "salmon" -> net.JordanRiver.KisekiLegend.item.ModItems.SALMON.get();
            case "sea_bass" -> net.JordanRiver.KisekiLegend.item.ModItems.SEA_BASS.get();
            case "trout" -> net.JordanRiver.KisekiLegend.item.ModItems.TROUT.get();
            case "yamany" -> net.JordanRiver.KisekiLegend.item.ModItems.YAMANY.get();
            default -> net.minecraft.world.item.Items.AIR;
        };
    }

    public static Item getFishItem(String fishName) {
        return switch (fishName) {
            case "dace" -> DACE.get();
            case "crab" -> CRAB.get();
            case "gold_angelfish" -> GOLD_ANGELFISH.get();
            case "liberl_carp" -> LIBERL_CARP.get();
            case "kasago" -> KASAGO.get();
            case "valleria_bass" -> VALLERIA_BASS.get();
            case "rockeater" -> ROCKEATER.get();
            case "great_blackfish" -> GREAT_BLACKFISH.get();
            case "carp" -> CARP.get();
            case "octopus" -> OCTOPUS.get();
            case "rainbow_trout" -> RAINBOW_TROUT.get();
            case "trout" -> TROUT.get();
            case "eel" -> EEL.get();
            case "salmon" -> SALMON.get();
            case "claudine" -> CLAUDINE.get();
            case "snakehead" -> SNAKEHEAD.get();
            case "pearlglass" -> PEARLGLASS.get();
            case "garvelze" -> GARVELZE.get();
            case "sea_bass" -> SEA_BASS.get();
            case "gigangora" -> GIGANGORA.get();
            case "mahimahi" -> MAHIMAHI.get();
            case "tiger_rockfish" -> TIGER_ROCKFISH.get();
            case "granakor" -> GRANAKOR.get();
            case "blue_marlin" -> BLUE_MARLIN.get();
            case "yamany" -> YAMANY.get();
            case "dynatrad" -> DYNATRAD.get();

            default -> null;
        };
    }
    public static String getFishBaitType(ItemStack fishStack) {
        // Check if it's a FishItem
        if (fishStack.getItem() instanceof FishItem fishItem) {
            String fishType = fishItem.getFishType();
            FishData fishData = FishRegistry.getFishData(fishType);

            // Only return the fish type if it can be used as bait
            if (fishData != null && fishData.canBeBait()) {
                return fishType;
            }
        }

        return null; // Not a fish that can be used as bait
    }

    public static boolean canFishBeUsedAsBait(ItemStack fishStack) {
        return getFishBaitType(fishStack) != null;
    }

    public static final Map<String, RegistryObject<Item>> FISH_BUCKETS = new HashMap<>();

    // List of all fish types that need buckets
    private static final String[] FISH_TYPES = {
            "carp", "liberl_carp", "crab", "dace", "eel", "kasago", "salmon",
            "sea_bass", "valleria_bass", "trout", "rainbow_trout", "yamany",
            "snakehead", "octopus", "granakor", "dynatrad", "garvelze", "gigangora",
            "pearlglass", "blue_marlin", "mahimahi", "claudine", "tiger_rockfish",
            "rockeater", "gold_angelfish", "great_blackfish"
    };


    public static Item getSepithItem(String sepithType) {
        return switch (sepithType) {
            case "earth" -> EARTH.get();
            case "water" -> WATER.get();
            case "fire" -> FIRE.get();
            case "wind" -> WIND.get();
            case "time" -> TIME.get();
            case "space" -> SPACE.get();
            case "mirage" -> MIRAGE.get();
            default -> null;
        };
    }





        public static final RegistryObject<Item> ORBMENT_ITEM =
                ITEMS.register("orbment", () -> new OrbmentItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

        // Register fish buckets automatically
        registerFishBuckets();

        registerQuartzItems();
    }
    private static void registerFishBuckets() {
        for (String fishType : FISH_TYPES) {
            String bucketName = fishType + "_bucket";
            RegistryObject<Item> bucket = ITEMS.register(bucketName,
                    () -> new FishBucketItem(fishType, new Item.Properties().stacksTo(1)));
            FISH_BUCKETS.put(fishType, bucket);
        }
    }

    // ADD helper method to get fish bucket:
    public static Item getFishBucket(String fishType) {
        RegistryObject<Item> bucket = FISH_BUCKETS.get(fishType);
        return bucket != null ? bucket.get() : Items.AIR;
    }

    private static void registerQuartz(String name, String mainElement, Map<String, Integer> sepith) {
        RegistryObject<Item> item = ITEMS.register(name, () -> new QuartzItem(mainElement, sepith, new Item.Properties()));
        QUARTZ.put(name, item);
    }

    private static void registerQuartzItems() {
        registerQuartz("defense_1", "earth", Map.of("earth", 1));
        registerQuartz("defense_2", "earth", Map.of("earth", 3));
        registerQuartz("defense_3", "earth", Map.of("earth", 5));
        registerQuartz("poison", "earth", Map.of("earth", 3));
        registerQuartz("mute", "earth", Map.of("earth", 3));
        registerQuartz("petrify", "earth", Map.of("earth", 3));

        registerQuartz("hp_1", "water", Map.of("water", 1));
        registerQuartz("hp_2", "water", Map.of("water", 3));
        registerQuartz("hp_3", "water", Map.of("water", 5));
        registerQuartz("mind_1", "water", Map.of("water", 1));
        registerQuartz("mind_2", "water", Map.of("water", 3));
        registerQuartz("mind_3", "water", Map.of("water", 5));
        registerQuartz("freeze", "water", Map.of("water", 3));
        registerQuartz("heal", "water", Map.of("water", 3, "time", 2));

        registerQuartz("attack_1", "fire", Map.of("fire", 1));
        registerQuartz("attack_2", "fire", Map.of("fire", 3));
        registerQuartz("attack_3", "fire", Map.of("fire", 5));
        registerQuartz("seal", "fire", Map.of("fire", 3));
        registerQuartz("confuse", "fire", Map.of("fire", 3));
        registerQuartz("strike", "fire", Map.of("fire", 3));

        registerQuartz("shield_1", "wind", Map.of("wind", 1));
        registerQuartz("shield_2", "wind", Map.of("wind", 3));
        registerQuartz("shield_3", "wind", Map.of("wind", 5));
        registerQuartz("evade_1", "wind", Map.of("wind", 1));
        registerQuartz("evade_2", "wind", Map.of("wind", 3));
        registerQuartz("evade_3", "wind", Map.of("wind", 5));
        registerQuartz("impede_1", "wind", Map.of("wind", 1));
        registerQuartz("impede_2", "wind", Map.of("wind", 3));
        registerQuartz("impede_3", "wind", Map.of("wind", 5));
        registerQuartz("sleep", "wind", Map.of("wind", 3));
        registerQuartz("scent", "wind", Map.of("wind", 3, "space", 2));

        registerQuartz("action_1", "time", Map.of("time", 1));
        registerQuartz("action_2", "time", Map.of("time", 3));
        registerQuartz("action_3", "time", Map.of("time", 5));
        registerQuartz("blind", "time", Map.of("time", 3));
        registerQuartz("cast_1", "time", Map.of("time", 1));
        registerQuartz("cast_2", "time", Map.of("time", 3));
        registerQuartz("deathblow_1", "time", Map.of("time", 3));
        registerQuartz("deathblow_2", "time", Map.of());

        registerQuartz("move_1", "space", Map.of("space", 1));
        registerQuartz("move_2", "space", Map.of("space", 3));
        registerQuartz("move_3", "space", Map.of("space", 5));
        registerQuartz("ep_cut_1", "space", Map.of("space", 2, "time", 1, "mirage", 1));
        registerQuartz("ep_cut_2", "space", Map.of("space", 3, "time", 2, "mirage", 2));
        registerQuartz("ep_cut_3", "space", Map.of("space", 5, "time", 3, "mirage", 3));
        registerQuartz("range_1", "space", Map.of("space", 3));
        registerQuartz("eagle_eye", "space", Map.of("space", 3, "mirage", 2));

        registerQuartz("ep_1", "mirage", Map.of("mirage", 2, "time", 1, "space", 1));
        registerQuartz("ep_2", "mirage", Map.of("mirage", 3, "time", 2, "space", 2));
        registerQuartz("ep_3", "mirage", Map.of("mirage", 5, "time", 3, "space", 2));
        registerQuartz("hit_1", "mirage", Map.of("mirage", 1));
        registerQuartz("hit_2", "mirage", Map.of("mirage", 3));
        registerQuartz("hit_3", "mirage", Map.of("mirage", 5));
        registerQuartz("information", "mirage", Map.of("mirage", 2));
        registerQuartz("haze", "mirage", Map.of("mirage", 3, "earth", 2));
        registerQuartz("cloak", "mirage", Map.of("mirage", 3, "fire", 2));
    }
}