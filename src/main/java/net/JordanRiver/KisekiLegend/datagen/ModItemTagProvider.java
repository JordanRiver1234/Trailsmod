package net.JordanRiver.KisekiLegend.datagen;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.concurrent.CompletableFuture;

import static net.JordanRiver.KisekiLegend.item.ModItems.QUARTZ;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, KisekiLegend.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.JEWEL)
                .add(Items.DIAMOND.builtInRegistryHolder().key())
                .add(Items.EMERALD.builtInRegistryHolder().key())
                .add(Items.LAPIS_LAZULI.builtInRegistryHolder().key())
                .add(Items.AMETHYST_SHARD.builtInRegistryHolder().key())
                .add(Items.QUARTZ.builtInRegistryHolder().key())
                .add(Items.PRISMARINE_SHARD.builtInRegistryHolder().key())
                .add(Items.PRISMARINE_CRYSTALS.builtInRegistryHolder().key());

        IntrinsicTagAppender<Item> quartzTag = tag(ModTags.Items.QUARTZ);
        for (RegistryObject<Item> quartzItem : QUARTZ.values()) {
            quartzTag.add(quartzItem.get());
        }
        tag(ModTags.Items.WATER_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "water"))
                .add(Items.WATER_BUCKET.builtInRegistryHolder().key())
                .add(Items.ICE.builtInRegistryHolder().key())
                .add(Items.PACKED_ICE.builtInRegistryHolder().key())
                .add(Items.BLUE_ICE.builtInRegistryHolder().key())
                .add(Items.SNOWBALL.builtInRegistryHolder().key())
                .add(Items.SNOW_BLOCK.builtInRegistryHolder().key())
                .add(Items.KELP.builtInRegistryHolder().key())
                .add(Items.SEAGRASS.builtInRegistryHolder().key())
                .add(Items.SEA_PICKLE.builtInRegistryHolder().key())
                .add(Items.NAUTILUS_SHELL.builtInRegistryHolder().key())
                .add(Items.HEART_OF_THE_SEA.builtInRegistryHolder().key())
                .add(Items.PRISMARINE_SHARD.builtInRegistryHolder().key())
                .add(Items.PRISMARINE_CRYSTALS.builtInRegistryHolder().key())
                .add(Items.SPONGE.builtInRegistryHolder().key())
                .add(Items.WET_SPONGE.builtInRegistryHolder().key())
                .addTag(net.minecraft.tags.ItemTags.FISHES);

        tag(ModTags.Items.EARTH_MATERIAL)
                .addTag(ModTags.Items.ORE)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "earth"))
                .add(Items.DIRT.builtInRegistryHolder().key())
                .add(Items.COBBLESTONE.builtInRegistryHolder().key())
                .add(Items.SAND.builtInRegistryHolder().key())
                .add(Items.GRAVEL.builtInRegistryHolder().key())
                .add(Items.CLAY.builtInRegistryHolder().key());

        tag(ModTags.Items.FIRE_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "fire"))
                .add(Items.LAVA_BUCKET.builtInRegistryHolder().key())
                .add(Items.FLINT_AND_STEEL.builtInRegistryHolder().key())
                .add(Items.BLAZE_ROD.builtInRegistryHolder().key())
                .add(Items.BLAZE_POWDER.builtInRegistryHolder().key())
                .add(Items.MAGMA_BLOCK.builtInRegistryHolder().key())
                .add(Items.MAGMA_CREAM.builtInRegistryHolder().key())
                .add(Items.FIRE_CHARGE.builtInRegistryHolder().key())
                .addTag(net.minecraft.tags.ItemTags.COALS);

        tag(ModTags.Items.SPACE_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "space"))
                .add(Items.END_STONE.builtInRegistryHolder().key())
                .add(Items.PURPUR_BLOCK.builtInRegistryHolder().key())
                .add(Items.CHORUS_FRUIT.builtInRegistryHolder().key())
                .add(Items.CHORUS_FLOWER.builtInRegistryHolder().key())
                .add(Items.SHULKER_SHELL.builtInRegistryHolder().key())
                .add(Items.OBSIDIAN.builtInRegistryHolder().key())
                .add(Items.CRYING_OBSIDIAN.builtInRegistryHolder().key());

        tag(ModTags.Items.WIND_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "wind"))
                .add(Items.FEATHER.builtInRegistryHolder().key())
                .add(Items.PHANTOM_MEMBRANE.builtInRegistryHolder().key())
                .add(Items.ELYTRA.builtInRegistryHolder().key())
                .add(Items.SPECTRAL_ARROW.builtInRegistryHolder().key())
                .add(Items.TIPPED_ARROW.builtInRegistryHolder().key())
                .addTag(net.minecraft.tags.ItemTags.ARROWS)
                .addTag(net.minecraft.tags.ItemTags.LEAVES);

        tag(ModTags.Items.TIME_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "time"))
                .add(Items.CLOCK.builtInRegistryHolder().key())
                .add(Items.REDSTONE.builtInRegistryHolder().key())
                .add(Items.REDSTONE_BLOCK.builtInRegistryHolder().key())
                .add(Items.SAND.builtInRegistryHolder().key())
                .add(Items.SANDSTONE.builtInRegistryHolder().key())
                .add(Items.SOUL_SAND.builtInRegistryHolder().key())
                .add(Items.SOUL_SOIL.builtInRegistryHolder().key());

        tag(ModTags.Items.MIRAGE_MATERIAL)
                .addOptional(ResourceLocation.fromNamespaceAndPath("kisekilegend", "mirage"))
                .addTag(ModTags.Items.JEWEL)
                .add(Items.GLASS.builtInRegistryHolder().key())
                .add(Items.GLASS_PANE.builtInRegistryHolder().key())
                .add(Items.ENDER_PEARL.builtInRegistryHolder().key())
                .add(Items.ENDER_EYE.builtInRegistryHolder().key())
                .add(Items.PHANTOM_MEMBRANE.builtInRegistryHolder().key())
                .add(Items.GHAST_TEAR.builtInRegistryHolder().key());

        tag(ModTags.Items.MYSTERY)
                .add(Items.DRAGON_EGG.builtInRegistryHolder().key())
                .add(Items.NETHER_STAR.builtInRegistryHolder().key())
                .add(Items.WITHER_SKELETON_SKULL.builtInRegistryHolder().key())
                .add(Items.SKELETON_SKULL.builtInRegistryHolder().key())
                .add(Items.ZOMBIE_HEAD.builtInRegistryHolder().key())
                .add(Items.CREEPER_HEAD.builtInRegistryHolder().key())
                .add(Items.PLAYER_HEAD.builtInRegistryHolder().key())
                .add(Items.PIGLIN_HEAD.builtInRegistryHolder().key())
                .add(Items.HEART_OF_THE_SEA.builtInRegistryHolder().key())
                .add(Items.NAUTILUS_SHELL.builtInRegistryHolder().key())
                .add(Items.ECHO_SHARD.builtInRegistryHolder().key())
                .add(Items.MUSIC_DISC_5.builtInRegistryHolder().key())
                .add(Items.MUSIC_DISC_11.builtInRegistryHolder().key())
                .add(Items.MUSIC_DISC_13.builtInRegistryHolder().key())
                .add(Items.MUSIC_DISC_RELIC.builtInRegistryHolder().key())
                .add(Items.GOAT_HORN.builtInRegistryHolder().key());

        tag(ModTags.Items.ACCESSORY)
                .add(Items.SPYGLASS.builtInRegistryHolder().key())
                .add(Items.CLOCK.builtInRegistryHolder().key())
                .add(Items.COMPASS.builtInRegistryHolder().key())
                .add(Items.RECOVERY_COMPASS.builtInRegistryHolder().key())
                .add(Items.TOTEM_OF_UNDYING.builtInRegistryHolder().key());

        tag(ModTags.Items.BOMB)
                .add(Items.TNT.builtInRegistryHolder().key())
                .add(Items.TNT_MINECART.builtInRegistryHolder().key())
                .add(Items.ENDER_PEARL.builtInRegistryHolder().key())
                .add(Items.ENDER_EYE.builtInRegistryHolder().key());

        tag(ModTags.Items.COOKING)
                .add(Items.BOWL.builtInRegistryHolder().key())
                .add(Items.BUCKET.builtInRegistryHolder().key())
                .add(Items.WATER_BUCKET.builtInRegistryHolder().key())
                .add(Items.LAVA_BUCKET.builtInRegistryHolder().key())
                .add(Items.MILK_BUCKET.builtInRegistryHolder().key())
                .add(Items.POWDER_SNOW_BUCKET.builtInRegistryHolder().key())
                .add(Items.AXOLOTL_BUCKET.builtInRegistryHolder().key())
                .add(Items.COD_BUCKET.builtInRegistryHolder().key())
                .add(Items.PUFFERFISH_BUCKET.builtInRegistryHolder().key())
                .add(Items.SALMON_BUCKET.builtInRegistryHolder().key())
                .add(Items.TADPOLE_BUCKET.builtInRegistryHolder().key())
                .add(Items.TROPICAL_FISH_BUCKET.builtInRegistryHolder().key());

        tag(ModTags.Items.DESSERT)
                .add(Items.CAKE.builtInRegistryHolder().key())
                .add(Items.PUMPKIN_PIE.builtInRegistryHolder().key())
                .add(Items.COOKIE.builtInRegistryHolder().key())
                .add(Items.HONEY_BOTTLE.builtInRegistryHolder().key())
                .add(Items.SWEET_BERRIES.builtInRegistryHolder().key())
                .add(Items.GLOW_BERRIES.builtInRegistryHolder().key())
                .add(Items.APPLE.builtInRegistryHolder().key())
                .add(Items.GOLDEN_APPLE.builtInRegistryHolder().key())
                .add(Items.ENCHANTED_GOLDEN_APPLE.builtInRegistryHolder().key())
                .add(Items.MELON_SLICE.builtInRegistryHolder().key());

        tag(ModTags.Items.ELIXIR)
                .add(Items.POTION.builtInRegistryHolder().key())
                .add(Items.SPLASH_POTION.builtInRegistryHolder().key())
                .add(Items.LINGERING_POTION.builtInRegistryHolder().key())
                .add(Items.EXPERIENCE_BOTTLE.builtInRegistryHolder().key())
                .add(Items.DRAGON_BREATH.builtInRegistryHolder().key());

        tag(ModTags.Items.FOOD)
                .addTag(net.minecraft.tags.ItemTags.FISHES)
                .add(Items.BEEF.builtInRegistryHolder().key())
                .add(Items.PORKCHOP.builtInRegistryHolder().key())
                .add(Items.MUTTON.builtInRegistryHolder().key())
                .add(Items.CHICKEN.builtInRegistryHolder().key())
                .add(Items.RABBIT.builtInRegistryHolder().key())
                .add(Items.COD.builtInRegistryHolder().key())
                .add(Items.SALMON.builtInRegistryHolder().key())
                .add(Items.TROPICAL_FISH.builtInRegistryHolder().key())
                .add(Items.PUFFERFISH.builtInRegistryHolder().key())
                .add(Items.COOKED_BEEF.builtInRegistryHolder().key())
                .add(Items.COOKED_PORKCHOP.builtInRegistryHolder().key())
                .add(Items.COOKED_MUTTON.builtInRegistryHolder().key())
                .add(Items.COOKED_CHICKEN.builtInRegistryHolder().key())
                .add(Items.COOKED_RABBIT.builtInRegistryHolder().key())
                .add(Items.COOKED_COD.builtInRegistryHolder().key())
                .add(Items.COOKED_SALMON.builtInRegistryHolder().key())
                .add(Items.BREAD.builtInRegistryHolder().key())
                .add(Items.BAKED_POTATO.builtInRegistryHolder().key())
                .add(Items.CARROT.builtInRegistryHolder().key())
                .add(Items.GOLDEN_CARROT.builtInRegistryHolder().key())
                .add(Items.BEETROOT.builtInRegistryHolder().key())
                .add(Items.BEETROOT_SOUP.builtInRegistryHolder().key())
                .add(Items.MUSHROOM_STEW.builtInRegistryHolder().key())
                .add(Items.RABBIT_STEW.builtInRegistryHolder().key())
                .add(Items.SUSPICIOUS_STEW.builtInRegistryHolder().key());

        tag(ModTags.Items.GUNPOWDER)
                .add(Items.GUNPOWDER.builtInRegistryHolder().key())
                .add(Items.FIRE_CHARGE.builtInRegistryHolder().key())
                .add(Items.BLAZE_POWDER.builtInRegistryHolder().key());

        tag(ModTags.Items.INGOT)
                .addTag(net.minecraft.tags.ItemTags.COALS)
                .add(Items.IRON_INGOT.builtInRegistryHolder().key())
                .add(Items.GOLD_INGOT.builtInRegistryHolder().key())
                .add(Items.COPPER_INGOT.builtInRegistryHolder().key())
                .add(Items.NETHERITE_INGOT.builtInRegistryHolder().key())
                .add(Items.NETHER_BRICK.builtInRegistryHolder().key())
                .add(Items.BRICK.builtInRegistryHolder().key());

        tag(ModTags.Items.LIQUID)
                .add(Items.WATER_BUCKET.builtInRegistryHolder().key())
                .add(Items.LAVA_BUCKET.builtInRegistryHolder().key())
                .add(Items.MILK_BUCKET.builtInRegistryHolder().key())
                .add(Items.POTION.builtInRegistryHolder().key())
                .add(Items.HONEY_BOTTLE.builtInRegistryHolder().key());

        tag(ModTags.Items.MAGIC_TOOL)
                .add(Items.BOOK.builtInRegistryHolder().key())
                .add(Items.WRITABLE_BOOK.builtInRegistryHolder().key())
                .add(Items.WRITTEN_BOOK.builtInRegistryHolder().key())
                .add(Items.ENCHANTED_BOOK.builtInRegistryHolder().key())
                .add(Items.KNOWLEDGE_BOOK.builtInRegistryHolder().key())
                .add(Items.PAPER.builtInRegistryHolder().key())
                .add(Items.MAP.builtInRegistryHolder().key())
                .add(Items.FILLED_MAP.builtInRegistryHolder().key())
                .add(Items.ENCHANTING_TABLE.builtInRegistryHolder().key())
                .add(Items.BREWING_STAND.builtInRegistryHolder().key())
                .add(Items.CAULDRON.builtInRegistryHolder().key())
                .add(Items.BELL.builtInRegistryHolder().key());

        tag(ModTags.Items.MEDICINAL)
                .add(Items.GLISTERING_MELON_SLICE.builtInRegistryHolder().key())
                .add(Items.GOLDEN_CARROT.builtInRegistryHolder().key())
                .add(Items.GOLDEN_APPLE.builtInRegistryHolder().key())
                .add(Items.ENCHANTED_GOLDEN_APPLE.builtInRegistryHolder().key())
                .add(Items.PUFFERFISH.builtInRegistryHolder().key())
                .add(Items.SPIDER_EYE.builtInRegistryHolder().key())
                .add(Items.FERMENTED_SPIDER_EYE.builtInRegistryHolder().key())
                .add(Items.BLAZE_POWDER.builtInRegistryHolder().key())
                .add(Items.MAGMA_CREAM.builtInRegistryHolder().key())
                .add(Items.GHAST_TEAR.builtInRegistryHolder().key())
                .add(Items.PHANTOM_MEMBRANE.builtInRegistryHolder().key());

        tag(ModTags.Items.MEDICINE)
                .add(Items.POTION.builtInRegistryHolder().key())
                .add(Items.MILK_BUCKET.builtInRegistryHolder().key())
                .add(Items.SUSPICIOUS_STEW.builtInRegistryHolder().key())
                .add(Items.HONEY_BOTTLE.builtInRegistryHolder().key());

        tag(ModTags.Items.OIL)
                .add(Items.INK_SAC.builtInRegistryHolder().key())
                .add(Items.GLOW_INK_SAC.builtInRegistryHolder().key())
                .add(Items.BLACK_DYE.builtInRegistryHolder().key())
                .add(Items.SLIME_BALL.builtInRegistryHolder().key())
                .add(Items.MAGMA_CREAM.builtInRegistryHolder().key());

        tag(ModTags.Items.ORE)
                .addTag(net.minecraft.tags.ItemTags.COAL_ORES)
                .addTag(net.minecraft.tags.ItemTags.IRON_ORES)
                .addTag(net.minecraft.tags.ItemTags.GOLD_ORES)
                .addTag(net.minecraft.tags.ItemTags.COPPER_ORES)
                .addTag(net.minecraft.tags.ItemTags.DIAMOND_ORES)
                .addTag(net.minecraft.tags.ItemTags.EMERALD_ORES)
                .addTag(net.minecraft.tags.ItemTags.LAPIS_ORES)
                .addTag(net.minecraft.tags.ItemTags.REDSTONE_ORES)
                .add(Items.NETHER_QUARTZ_ORE.builtInRegistryHolder().key())
                .add(Items.NETHER_GOLD_ORE.builtInRegistryHolder().key())
                .add(Items.GILDED_BLACKSTONE.builtInRegistryHolder().key())
                .add(Items.RAW_IRON.builtInRegistryHolder().key())
                .add(Items.RAW_GOLD.builtInRegistryHolder().key())
                .add(Items.RAW_COPPER.builtInRegistryHolder().key());

        tag(ModTags.Items.PLANT)
                .addTag(net.minecraft.tags.ItemTags.SAPLINGS)
                .addTag(net.minecraft.tags.ItemTags.LEAVES)
                .addTag(net.minecraft.tags.ItemTags.FLOWERS)
                .add(Items.SHORT_GRASS.builtInRegistryHolder().key())
                .add(Items.TALL_GRASS.builtInRegistryHolder().key())
                .add(Items.FERN.builtInRegistryHolder().key())
                .add(Items.DEAD_BUSH.builtInRegistryHolder().key())
                .add(Items.DANDELION.builtInRegistryHolder().key())
                .add(Items.POPPY.builtInRegistryHolder().key())
                .add(Items.SUGAR_CANE.builtInRegistryHolder().key())
                .add(Items.KELP.builtInRegistryHolder().key())
                .add(Items.MOSS_BLOCK.builtInRegistryHolder().key())
                .add(Items.MOSS_CARPET.builtInRegistryHolder().key())
                .add(Items.VINE.builtInRegistryHolder().key())
                .add(Items.LILY_PAD.builtInRegistryHolder().key())
                .add(Items.CACTUS.builtInRegistryHolder().key())
                .add(Items.WHEAT.builtInRegistryHolder().key())
                .add(Items.CARROT.builtInRegistryHolder().key())
                .add(Items.POTATO.builtInRegistryHolder().key())
                .add(Items.BEETROOT.builtInRegistryHolder().key())
                .add(Items.NETHER_WART.builtInRegistryHolder().key())
                .add(Items.COCOA_BEANS.builtInRegistryHolder().key())
                .add(Items.PUMPKIN.builtInRegistryHolder().key())
                .add(Items.MELON.builtInRegistryHolder().key());

        tag(ModTags.Items.POISON)
                .add(Items.POISONOUS_POTATO.builtInRegistryHolder().key())
                .add(Items.PUFFERFISH.builtInRegistryHolder().key())
                .add(Items.SPIDER_EYE.builtInRegistryHolder().key())
                .add(Items.ROTTEN_FLESH.builtInRegistryHolder().key());

        tag(ModTags.Items.SPICE)
                .add(Items.SUGAR.builtInRegistryHolder().key())
                .add(Items.COCOA_BEANS.builtInRegistryHolder().key())
                .add(Items.BROWN_MUSHROOM.builtInRegistryHolder().key())
                .add(Items.RED_MUSHROOM.builtInRegistryHolder().key())
                .add(Items.NETHER_WART.builtInRegistryHolder().key());

        tag(ModTags.Items.SUNDRY)
                .add(Items.STICK.builtInRegistryHolder().key())
                .add(Items.FLINT.builtInRegistryHolder().key())
                .add(Items.FEATHER.builtInRegistryHolder().key())
                .add(Items.STRING.builtInRegistryHolder().key())
                .add(Items.BONE.builtInRegistryHolder().key())
                .add(Items.CLAY_BALL.builtInRegistryHolder().key())
                .add(Items.PAPER.builtInRegistryHolder().key())
                .add(Items.LEATHER.builtInRegistryHolder().key())
                .add(Items.RABBIT_HIDE.builtInRegistryHolder().key())
                .add(Items.EGG.builtInRegistryHolder().key())
                .add(Items.SNOWBALL.builtInRegistryHolder().key())
                .add(Items.CHARCOAL.builtInRegistryHolder().key());

        tag(ModTags.Items.SUPPLEMENT)
                .add(Items.GLOWSTONE_DUST.builtInRegistryHolder().key())
                .add(Items.REDSTONE.builtInRegistryHolder().key())
                .add(Items.GUNPOWDER.builtInRegistryHolder().key())
                .add(Items.SUGAR.builtInRegistryHolder().key())
                .add(Items.BLAZE_POWDER.builtInRegistryHolder().key())
                .add(Items.FERMENTED_SPIDER_EYE.builtInRegistryHolder().key());

        tag(ModTags.Items.THREADS)
                .add(Items.STRING.builtInRegistryHolder().key())
                .addTag(net.minecraft.tags.ItemTags.WOOL);

        tag(ModTags.Items.WOOL)
                .addTag(net.minecraft.tags.ItemTags.WOOL)
                .addTag(net.minecraft.tags.ItemTags.WOOL_CARPETS)
                .add(Items.LEATHER.builtInRegistryHolder().key())
                .add(Items.RABBIT_HIDE.builtInRegistryHolder().key());
        tag(ModTags.Items.CLOTH)
                .addTag(net.minecraft.tags.ItemTags.WOOL) // includes all wool blocks
                .add(Items.LEATHER.builtInRegistryHolder().key())
                .add(Items.ROTTEN_FLESH.builtInRegistryHolder().key())
                .add(Items.RABBIT_HIDE.builtInRegistryHolder().key());
    }
}