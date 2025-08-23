package net.JordanRiver.KisekiLegend.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KisekiLegend.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ORBMENT_EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("orbment_equipment", () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.ORBMENT_ITEM.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.kisekilegend.orbment_equipment"))
                    .displayItems((parameters, output) -> {
                        // Add Orbment and all Quartz
                        output.accept(ModItems.ORBMENT_ITEM.get());
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
                        pOutput.accept(ModBlocks.ORBAL_TABLE.get());
                        pOutput.accept(ModBlocks.QUARTZ_MACHINE.get());



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
                    .build());

    public static final RegistryObject<CreativeModeTab> FISHING_TAB = CREATIVE_MODE_TABS.register("fishing_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.PROGRESS_ROD.get()))
                    .withTabsBefore(KISEKI_FOODS_TAB.getId())
                    .title(Component.translatable("creativetab.kisekilegend.fishing"))
                    .displayItems((pParameters, pOutput) -> {
                        // Fishing Rods
                        pOutput.accept(ModItems.PROGRESS_ROD.get());
                        pOutput.accept(ModItems.MARINE_STAR_ROD.get());
                        pOutput.accept(ModItems.PISCES_HEART.get());
                        pOutput.accept(ModItems.BAMBOO_FISHING_ROD.get());
                        pOutput.accept(ModItems.METAL_TRIDENT_ROD.get());
                        pOutput.accept(ModItems.LAKELORD_II.get());
                        pOutput.accept(ModItems.AQUA_MASTER.get());

                        // Bait Items
                        pOutput.accept(ModItems.EARTHWORM.get());
                        pOutput.accept(ModItems.POLYCHAETE.get());
                        pOutput.accept(ModItems.SHRIMPLET.get());
                        pOutput.accept(ModItems.DUMPLINGS.get());
                        pOutput.accept(ModItems.FROG.get());
                        pOutput.accept(ModItems.RED_FLIES.get());
                        pOutput.accept(ModItems.RIVER_BUG.get());
                        pOutput.accept(ModItems.RIVER_SNAIL.get());
                        pOutput.accept(ModItems.ROE.get());

                        // Fish Items
                        pOutput.accept(ModItems.DACE.get());
                        pOutput.accept(ModItems.YAMANY.get());
                        pOutput.accept(ModItems.CRAB.get());
                        pOutput.accept(ModItems.GOLD_ANGELFISH.get());
                        pOutput.accept(ModItems.LIBERL_CARP.get());
                        pOutput.accept(ModItems.KASAGO.get());
                        pOutput.accept(ModItems.VALLERIA_BASS.get());
                        pOutput.accept(ModItems.ROCKEATER.get());
                        pOutput.accept(ModItems.GREAT_BLACKFISH.get());
                        pOutput.accept(ModItems.CARP.get());
                        pOutput.accept(ModItems.OCTOPUS.get());
                        pOutput.accept(ModItems.RAINBOW_TROUT.get());
                        pOutput.accept(ModItems.TROUT.get());
                        pOutput.accept(ModItems.EEL.get());
                        pOutput.accept(ModItems.SALMON.get());
                        pOutput.accept(ModItems.CLAUDINE.get());
                        pOutput.accept(ModItems.SNAKEHEAD.get());
                        pOutput.accept(ModItems.PEARLGLASS.get());
                        pOutput.accept(ModItems.GARVELZE.get());
                        pOutput.accept(ModItems.SEA_BASS.get());
                        pOutput.accept(ModItems.GIGANGORA.get());
                        pOutput.accept(ModItems.MAHIMAHI.get());
                        pOutput.accept(ModItems.TIGER_ROCKFISH.get());
                        pOutput.accept(ModItems.GRANAKOR.get());
                        pOutput.accept(ModItems.BLUE_MARLIN.get());
                        pOutput.accept(ModItems.DYNATRAD.get());

                        // Fish Buckets - Add all fish buckets automatically
                        for (String fishType : Arrays.asList(
                                "carp", "liberl_carp", "crab", "dace", "eel", "kasago", "salmon",
                                "sea_bass", "valleria_bass", "trout", "rainbow_trout", "yamany",
                                "snakehead", "octopus", "granakor", "dynatrad", "garvelze", "gigangora",
                                "pearlglass", "blue_marlin", "mahimahi", "claudine", "tiger_rockfish",
                                "rockeater", "gold_angelfish", "great_blackfish"
                        )) {
                            Item bucket = ModItems.getFishBucket(fishType);
                            if (bucket != Items.AIR) {
                                pOutput.accept(bucket);
                            }
                        }
                    })
                    .build()
    );



    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }



}
