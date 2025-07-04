package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.item.ModItems;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class ModBlocks {
    public static final DeferredRegister<Block> BlOCKS =
            DeferredRegister.create(BLOCKS, KisekiLegend.MOD_ID);
    public static final RegistryObject<Block> ORBMENT_MACHINE = registerBlock(
            "orbment_machine",
            () -> new OrbmentMachineBlock(
                    BlockBehaviour.Properties
                            .of()    // <- here!
                            .strength(1f)
                            .noOcclusion()
            )
    );


    public static final RegistryObject<Block> EARTHVEIN_BLOCK =  registerBlock( "earthvein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.LODESTONE)));

    public static final RegistryObject<Block> FIREVEIN_BLOCK = registerBlock("firevein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.NETHER_WOOD)));

    public static final RegistryObject<Block> MIRAGEVEIN_BLOCK = registerBlock("miragevein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.CALCITE)));

    public static final RegistryObject<Block> SPACEVEIN_BLOCK = registerBlock("spacevein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.BONE_BLOCK)));

    public static final RegistryObject<Block> TIMEVEIN_BLOCK = registerBlock("timevein_block",
            ()-> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.CAVE_VINES)));

    public static final RegistryObject<Block> WATERVEIN_BLOCK = registerBlock("watervein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.WET_SPONGE)));

    public static final RegistryObject<Block> WINDVEIN_BLOCK = registerBlock("windvein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength( 2f ) .noOcclusion() .requiresCorrectToolForDrops().sound(SoundType.AZALEA_LEAVES)));

    public static final RegistryObject<Block> EARTH_ORE = registerBlock("earth_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> EARTH_DEEPSLATE_ORE = registerBlock("earth_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> FIRE_ORE = registerBlock("fire_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> FIRE_DEEPSLATE_ORE = registerBlock("fire_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> MIRAGE_ORE = registerBlock("mirage_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MIRAGE_DEEPSLATE_ORE = registerBlock("mirage_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> SPACE_ORE = registerBlock("space_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SPACE_DEEPSLATE_ORE = registerBlock("space_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> TIME_ORE = registerBlock("time_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> TIME_DEEPSLATE_ORE = registerBlock("time_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> WATER_ORE = registerBlock("water_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> WATER_DEEPSLATE_ORE = registerBlock("water_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> WIND_ORE = registerBlock("wind_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> WIND_DEEPSLATE_ORE = registerBlock("wind_deepslate_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(2f).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));












    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BlOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem (String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BlOCKS.register(eventBus);
    }
}
