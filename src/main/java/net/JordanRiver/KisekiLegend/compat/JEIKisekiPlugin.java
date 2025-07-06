package net.JordanRiver.KisekiLegend.compat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JeiPlugin
public class JEIKisekiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FoodRecipeCategory(
                        registration.getJeiHelpers().getGuiHelper()
                ),
                new QuartzRecipeCategory(
                        registration.getJeiHelpers().getGuiHelper()
                ),
                new MiscRecipeCategory(
                        registration.getJeiHelpers().getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1) Foods: explicitly list your food items
        List<SimpleItemRecipe> foods = List.of(
                        ModItems.POT_O_MEAT.get(),
                        ModItems.BOUILLABAISSE.get(),
                        ModItems.CHEFS_CURRY.get(),
                        ModItems.WILD_VEGGIE_POT.get(),
                        ModItems.SALUBRIOUS_OATMEAL.get(),
                        ModItems.JENIS_LUNCH.get(),
                        ModItems.LIBERL_OMELET.get(),
                        ModItems.CHEESE_RISOTTO.get(),
                        ModItems.ABADDON_POTLUCK.get(),
                        ModItems.WHOLESOME_PASTA.get(),
                        ModItems.DIEHARD_PAELLA.get()
                ).stream()
                .map(item -> new SimpleItemRecipe(item.getDefaultInstance()))
                .toList();
        registration.addRecipes(FoodRecipeCategory.TYPE, foods);

        // 2) Quartz: still from your QUARTZ map
        List<SimpleItemRecipe> quartz = ModItems.QUARTZ.values().stream()
                .map(ro -> new SimpleItemRecipe(ro.get().getDefaultInstance()))
                .toList();
        registration.addRecipes(QuartzRecipeCategory.TYPE, quartz);

        // 3) Misc: everything in your mod namespace except Foods & Quartz
        Set<net.minecraft.world.item.Item> excluded = Stream.concat(
                foods.stream().map(r -> r.getResultItem().getItem()),
                ModItems.QUARTZ.values().stream().map(RegistryObject::get)
        ).collect(Collectors.toSet());

        List<SimpleItemRecipe> misc = ForgeRegistries.ITEMS.getValues().stream()
                .filter(i -> ForgeRegistries.ITEMS.getKey(i).getNamespace().equals(KisekiLegend.MOD_ID))
                .filter(i -> !excluded.contains(i))
                .map(i -> new SimpleItemRecipe(i.getDefaultInstance()))
                .toList();
        registration.addRecipes(MiscRecipeCategory.TYPE, misc);
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.POT_O_MEAT.get()),
                FoodRecipeCategory.TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.ORBMENT_ITEM.get()),
                QuartzRecipeCategory.TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ORBMENT_MACHINE.get().asItem()),
                MiscRecipeCategory.TYPE
        );
    }
}
