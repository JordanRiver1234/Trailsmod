package net.JordanRiver.KisekiLegend.util;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> WATER_MATERIAL = bind("water_material");
        public static final TagKey<Item> JEWEL = bind("jewel");
        public static final TagKey<Item> PLANT = bind("plant");
        public static final TagKey<Item> FIRE_MATERIAL = bind("fire_material");
        public static final TagKey<Item> EARTH_MATERIAL = bind("earth_material");
        public static final TagKey<Item> WIND_MATERIAL = bind("wind_material");
        public static final TagKey<Item> TIME_MATERIAL = bind("time_material");
        public static final TagKey<Item> SPACE_MATERIAL = bind("space_material");
        public static final TagKey<Item> MIRAGE_MATERIAL = bind("mirage_material");
        public static final TagKey<Item> ACCESSORY = bind("accessory");
        public static final TagKey<Item> BOMB = bind("bomb");
        public static final TagKey<Item> COOKING = bind("cooking");
        public static final TagKey<Item> DESSERT = bind("dessert");
        public static final TagKey<Item> ELIXIR = bind("elixir");
        public static final TagKey<Item> FOOD = bind("food");
        public static final TagKey<Item> GUNPOWDER = bind("gunpowder");
        public static final TagKey<Item> INGOT = bind("ingot");
        public static final TagKey<Item> LIQUID = bind("liquid");
        public static final TagKey<Item> MAGIC_TOOL = bind("magic_tool");
        public static final TagKey<Item> MEDICINAL = bind("medicinal");
        public static final TagKey<Item> MYSTERY = bind("mystery");
        public static final TagKey<Item> OIL = bind("oil");
        public static final TagKey<Item> ORE = bind("ore");
        public static final TagKey<Item> POISON = bind("poison");
        public static final TagKey<Item> SPICE = bind("spice");
        public static final TagKey<Item> SUNDRY = bind("sundry");
        public static final TagKey<Item> SUPPLEMENT = bind("supplement");
        public static final TagKey<Item> THREADS = bind("threads");
        public static final TagKey<Item> WOOL = bind("wool");
        public static final TagKey<Item> MEDICINE = bind("medicine");
        public static final TagKey<Item> QUARTZ = bind("quartz");
        public static final TagKey<Item> CLOTH = bind("cloth");

        private static TagKey<Item> bind(String name) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name));
        }

        public static void init() {
            KisekiLegend.LOGGER.info("ModTags initialized with ForgeRegistries");
        }
    }
}