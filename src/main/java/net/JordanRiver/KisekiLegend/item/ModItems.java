package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.JordanRiver.KisekiLegend.items.ElementalMassItem;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.items.SepithMassItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.geckolib.animatable.GeoItem;

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



        public static final RegistryObject<Item> ORBMENT_ITEM =
                ITEMS.register("orbment", () -> new OrbmentItem(new Item.Properties()));

        public static void register(IEventBus eventBus) {
            ITEMS.register(eventBus);



            registerQuartzItems();
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