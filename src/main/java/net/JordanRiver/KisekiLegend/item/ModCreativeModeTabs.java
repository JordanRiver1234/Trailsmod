package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KisekiLegend.MOD_ID);

    // In ModCreativeModeTabs.java, update the ORBMENT_EQUIPMENT_TAB registration:
    public static final RegistryObject<CreativeModeTab> ORBMENT_EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("orbment_equipment", () -> CreativeModeTab.builder()
                    .icon(() -> {
                        ItemStack stack = new ItemStack(ModItems.ORBMENT_ITEM.get());
                        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
                        return stack;
                    })
                    .title(Component.translatable("itemGroup.kisekilegend.orbment_equipment"))
                    .displayItems((parameters, output) -> {
                        // Add 3D Orbment
                        ItemStack orbmentStack = new ItemStack(ModItems.ORBMENT_ITEM.get());
                        orbmentStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
                        output.accept(orbmentStack);

                        // Add all Quartz
                        for (RegistryObject<Item> item : ModItems.QUARTZ.values()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> SEPITH_ITEMS_TAB = CREATIVE_MODE_TABS.register("sepith_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SEPITH_MASS.get()))
                    .title(Component.translatable("creativetab.kisekilegend.sepith_items"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.SEPITH_MASS.get());
                        pOutput.accept(ModItems.EARTH_MASS.get());
                        pOutput.accept(ModItems.FIRE_MASS.get());
                        pOutput.accept(ModItems.MIRAGE_MASS.get());
                        pOutput.accept(ModItems.SPACE_MASS.get());
                        pOutput.accept(ModItems.WATER_MASS.get());
                        pOutput.accept(ModItems.WIND_MASS.get());
                        pOutput.accept(ModItems.TIME_MASS.get());

                        pOutput.accept(ModItems.EARTH.get());
                        pOutput.accept(ModItems.WATER.get());
                        pOutput.accept(ModItems.FIRE.get());
                        pOutput.accept(ModItems.WIND.get());
                        pOutput.accept(ModItems.TIME.get());
                        pOutput.accept(ModItems.SPACE.get());
                        pOutput.accept(ModItems.MIRAGE.get());

                        // Orbment Machine BlockItem (must be registered!)

                    }).build());

    public static final RegistryObject<CreativeModeTab> SEPITH_BLOCKS_TAB = CREATIVE_MODE_TABS.register("sepith_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.MIRAGEVEIN_BLOCK.get()))
                    .withTabsBefore(SEPITH_ITEMS_TAB.getId())
                    .title(Component.translatable("creativetab.kisekilegend.sepith_blocks"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModBlocks.EARTHVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.FIREVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.MIRAGEVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.SPACEVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.TIMEVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.WATERVEIN_BLOCK.get());
                        pOutput.accept(ModBlocks.WINDVEIN_BLOCK.get());

                        pOutput.accept(ModBlocks.EARTH_ORE.get());
                        pOutput.accept(ModBlocks.EARTH_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.FIRE_ORE.get());
                        pOutput.accept(ModBlocks.FIRE_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.MIRAGE_ORE.get());
                        pOutput.accept(ModBlocks.MIRAGE_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.SPACE_ORE.get());
                        pOutput.accept(ModBlocks.SPACE_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.TIME_ORE.get());
                        pOutput.accept(ModBlocks.TIME_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.WATER_ORE.get());
                        pOutput.accept(ModBlocks.WATER_DEEPSLATE_ORE.get());
                        pOutput.accept(ModBlocks.WIND_ORE.get());
                        pOutput.accept(ModBlocks.WIND_DEEPSLATE_ORE.get());

                        pOutput.accept(ModBlocks.ORBMENT_MACHINE.get());



                    }).build());

    public static final RegistryObject<CreativeModeTab> KISEKI_FOODS_TAB = CREATIVE_MODE_TABS.register("kiseki_foods_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.POT_O_MEAT.get().getDefaultInstance()) // Use any food item as icon
                    .title(Component.translatable("creativetab.kisekilegend.kiseki_foods"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.POT_O_MEAT.get());
                        output.accept(ModItems.BOUILLABAISSE.get());
                        output.accept(ModItems.CHEFS_CURRY.get());
                        output.accept(ModItems.WILD_VEGGIE_POT.get());
                        output.accept(ModItems.SALUBRIOUS_OATMEAL.get());
                        output.accept(ModItems.JENIS_LUNCH.get());
                        output.accept(ModItems.LIBERL_OMELET.get());
                        output.accept(ModItems.DIEHARD_PAELLA.get());
                        output.accept(ModItems.CHEESE_RISOTTO.get());
                        output.accept(ModItems.WHOLESOME_PASTA.get());
                        output.accept(ModItems.ABADDON_POTLUCK.get());
                    })
                    .build()
    );




    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }



}
