package net.JordanRiver.KisekiLegend.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class PlayerRecipeProgressCapability implements INBTSerializable<CompoundTag> {
    private final Set<ResourceLocation> completedRecipes = new HashSet<>();

    public void markRecipeCompleted(ResourceLocation recipeId) {
        completedRecipes.add(recipeId);
        System.out.println("Player completed recipe: " + recipeId);
    }

    public boolean hasCompletedRecipe(ResourceLocation recipeId) {
        return completedRecipes.contains(recipeId);
    }
    public boolean hasCompletedRecipeUnlock(String recipeId) {
        // This should only return true if the recipe was completed via a recipe_morph node
        // For now, using the same logic but you can extend this to track unlock sources
        return hasCompletedRecipe(recipeId);
    }
    public boolean hasCompletedRecipe(String recipeId) {
        return completedRecipes.stream().anyMatch(id -> id.getPath().equals(recipeId));
    }

    public Set<ResourceLocation> getCompletedRecipes() {
        return new HashSet<>(completedRecipes);
    }

    public void clearProgress() {
        completedRecipes.clear();
        System.out.println("Player recipe progress cleared");
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag completedList = new ListTag();

        for (ResourceLocation recipeId : completedRecipes) {
            completedList.add(StringTag.valueOf(recipeId.toString()));
        }

        tag.put("completed_recipes", completedList);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        completedRecipes.clear();

        if (tag.contains("completed_recipes")) {
            ListTag completedList = tag.getList("completed_recipes", Tag.TAG_STRING);
            for (int i = 0; i < completedList.size(); i++) {
                try {
                    ResourceLocation recipeId = ResourceLocation.parse(completedList.getString(i));
                    completedRecipes.add(recipeId);
                } catch (Exception e) {
                    System.err.println("Failed to parse recipe ID: " + completedList.getString(i));
                }
            }
        }
    }
}