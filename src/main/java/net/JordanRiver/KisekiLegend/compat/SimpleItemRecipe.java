package net.JordanRiver.KisekiLegend.compat;

import net.minecraft.world.item.ItemStack;

public class SimpleItemRecipe {
    private final ItemStack result;

    public SimpleItemRecipe(ItemStack result) {
        this.result = result;
    }

    public ItemStack getResultItem() {
        return result;
    }
}