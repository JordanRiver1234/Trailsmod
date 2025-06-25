package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KisekiLegend.MOD_ID);

    public static final RegistryObject<Item> EARTH = ITEMS.register("earth", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EARTH_MASS = ITEMS.register("earth_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER = ITEMS.register("water", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER_MASS = ITEMS.register("water_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE = ITEMS.register("fire", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE_MASS = ITEMS.register("fire_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND = ITEMS.register("wind", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND_MASS = ITEMS.register("wind_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIME = ITEMS.register("time", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIME_MASS = ITEMS.register("time_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPACE = ITEMS.register("space", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPACE_MASS = ITEMS.register("space_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE = ITEMS.register("mirage", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE_MASS = ITEMS.register("mirage_mass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEPITH_MASS = ITEMS.register("sepith_mass", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ORBMENT_ITEM = ITEMS.register("orbment_item", () -> new OrbmentItem(new Item.Properties()));

    public static final Map<String, RegistryObject<Item>> QUARTZ = new HashMap<>();

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        registerQuartzItems();
    }

    private static void registerQuartz(String name, String mainElement, Map<String, Integer> sepith, String bonus, String effect) {
        RegistryObject<Item> item = ITEMS.register(name, () -> new QuartzItem(mainElement, sepith, bonus, effect, new Item.Properties()));
        QUARTZ.put(name, item);
    }

    private static void registerQuartzItems() {
        registerQuartz("defense_1", "earth", Map.of("earth", 1), "DEF+5%", "");
        registerQuartz("defense_2", "earth", Map.of("earth", 3), "DEF+10%", "");
        registerQuartz("defense_3", "earth", Map.of("earth", 5), "DEF+15%", "");
        registerQuartz("poison", "earth", Map.of("earth", 3), "", "10% chance to poison enemy");
        registerQuartz("mute", "earth", Map.of("earth", 3), "", "10% chance to mute enemy");
        registerQuartz("petrify", "earth", Map.of("earth", 3), "", "10% chance to petrify enemy");

        registerQuartz("hp_1", "water", Map.of("water", 1), "Max HP +5%", "");
        registerQuartz("hp_2", "water", Map.of("water", 3), "Max HP +10%", "");
        registerQuartz("hp_3", "water", Map.of("water", 5), "Max HP +15%", "");
        registerQuartz("mind_1", "water", Map.of("water", 1), "ATS +5%", "");
        registerQuartz("mind_2", "water", Map.of("water", 3), "ATS +10%", "");
        registerQuartz("mind_3", "water", Map.of("water", 5), "ATS +15%", "");
        registerQuartz("freeze", "water", Map.of("water", 3), "", "10% chance to freeze enemy");
        registerQuartz("heal", "water", Map.of("water", 3, "time", 2), "", "Recovers HP while walking");

        registerQuartz("attack_1", "fire", Map.of("fire", 1), "STR+5%/DEF-5%", "");
        registerQuartz("attack_2", "fire", Map.of("fire", 3), "STR+10%/DEF-10%", "");
        registerQuartz("attack_3", "fire", Map.of("fire", 5), "STR+15%/DEF-15%", "");
        registerQuartz("seal", "fire", Map.of("fire", 3), "", "10% chance to seal crafts");
        registerQuartz("confuse", "fire", Map.of("fire", 3), "", "10% chance to confuse enemy");
        registerQuartz("strike", "fire", Map.of("fire", 3), "", "10% chance to crit");

        registerQuartz("shield_1", "wind", Map.of("wind", 1), "ADF +5", "");
        registerQuartz("shield_2", "wind", Map.of("wind", 3), "ADF +15", "");
        registerQuartz("shield_3", "wind", Map.of("wind", 5), "ADF +30", "");
        registerQuartz("evade_1", "wind", Map.of("wind", 1), "AGL +1", "");
        registerQuartz("evade_2", "wind", Map.of("wind", 3), "AGL +2", "");
        registerQuartz("evade_3", "wind", Map.of("wind", 5), "AGL +3", "");
        registerQuartz("impede_1", "wind", Map.of("wind", 1), "", "Prevent arts 20%");
        registerQuartz("impede_2", "wind", Map.of("wind", 3), "", "Prevent arts 50%");
        registerQuartz("impede_3", "wind", Map.of("wind", 5), "", "Prevent arts 80%");
        registerQuartz("sleep", "wind", Map.of("wind", 3), "", "Put to sleep 10%");
        registerQuartz("scent", "wind", Map.of("wind", 3, "space", 2), "", "Enemies notice you");

        registerQuartz("action_1", "time", Map.of("time", 1), "SPD +10%", "");
        registerQuartz("action_2", "time", Map.of("time", 3), "SPD +20%", "");
        registerQuartz("action_3", "time", Map.of("time", 5), "SPD +30%", "");
        registerQuartz("blind", "time", Map.of("time", 3), "", "Blind enemy");
        registerQuartz("cast_1", "time", Map.of("time", 1), "", "Faster cast");
        registerQuartz("cast_2", "time", Map.of("time", 3), "", "Greatly faster cast");
        registerQuartz("deathblow_1", "time", Map.of("time", 3), "", "Kill 10% chance");
        registerQuartz("deathblow_2", "time", Map.of(), "", "100% kill, quartz breaks");

        registerQuartz("move_1", "space", Map.of("space", 1), "MOV +1", "");
        registerQuartz("move_2", "space", Map.of("space", 3), "MOV +2", "");
        registerQuartz("move_3", "space", Map.of("space", 5), "MOV +3", "");
        registerQuartz("ep_cut_1", "space", Map.of("space", 2, "time", 1, "mirage", 1), "", "EP cost -10%");
        registerQuartz("ep_cut_2", "space", Map.of("space", 3, "time", 2, "mirage", 2), "", "EP cost -25%");
        registerQuartz("ep_cut_3", "space", Map.of("space", 5, "time", 3, "mirage", 3), "", "EP cost -50%");
        registerQuartz("range_1", "space", Map.of("space", 3), "", "Range +1");
        registerQuartz("eagle_eye", "space", Map.of("space", 3, "mirage", 2), "", "See distant enemies");

        registerQuartz("ep_1", "mirage", Map.of("mirage", 2, "time", 1, "space", 1), "Max EP+5%", "");
        registerQuartz("ep_2", "mirage", Map.of("mirage", 3, "time", 2, "space", 2), "Max EP+10%", "");
        registerQuartz("ep_3", "mirage", Map.of("mirage", 5, "time", 3, "space", 2), "Max EP+15%", "");
        registerQuartz("hit_1", "mirage", Map.of("mirage", 1), "DEX +5", "");
        registerQuartz("hit_2", "mirage", Map.of("mirage", 3), "DEX +10", "");
        registerQuartz("hit_3", "mirage", Map.of("mirage", 5), "DEX +15", "");
        registerQuartz("information", "mirage", Map.of("mirage", 2), "", "See enemy info");
        registerQuartz("haze", "mirage", Map.of("mirage", 3, "earth", 2), "", "Avoid detection");
        registerQuartz("cloak", "mirage", Map.of("mirage", 3, "fire", 2), "", "No encounters");
    }
}
