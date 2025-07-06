package net.JordanRiver.KisekiLegend.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MiscRecipeCategory implements IRecipeCategory<SimpleItemRecipe> {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "misc");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                    "textures/item/orbment_machine.png");
    public static final RecipeType<SimpleItemRecipe> TYPE =
            new RecipeType<>(UID, SimpleItemRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MiscRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 100, 18);
        this.icon = helper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ORBMENT_MACHINE.get().asItem())
        );
    }

    @Override
    public RecipeType<SimpleItemRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.kisekilegend.misc");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          SimpleItemRecipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 40, 1)
                .addItemStack(recipe.getResultItem());
    }
}

