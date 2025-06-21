package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KisekiLegend.MOD_ID);

    public static final RegistryObject<Item> EARTH = ITEMS.register("earth",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EARTH_MASS = ITEMS.register("earth_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER = ITEMS.register("water",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WATER_MASS = ITEMS.register("water_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE = ITEMS.register("fire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIRE_MASS = ITEMS.register("fire_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND = ITEMS.register("wind",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND_MASS = ITEMS.register("wind_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIME = ITEMS.register("time",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIME_MASS = ITEMS.register("time_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPACE = ITEMS.register("space",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPACE_MASS = ITEMS.register("space_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE = ITEMS.register("mirage",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MIRAGE_MASS = ITEMS.register("mirage_mass",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEPITH_MASS = ITEMS.register("sepith_mass",
            () -> new Item(new Item.Properties()));





    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
