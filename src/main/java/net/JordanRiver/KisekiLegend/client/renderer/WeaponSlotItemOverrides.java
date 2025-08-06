package net.JordanRiver.KisekiLegend.client.renderer;

import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeaponSlotItemOverrides extends ItemOverrides {

    private final WeaponSlotBakedModel weaponSlotModel;

    public WeaponSlotItemOverrides(WeaponSlotBakedModel weaponSlotModel) {
        this.weaponSlotModel = weaponSlotModel;
    }

    @Override
    public @Nullable BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {


        if (!WeaponSlotRenderer.hasWeaponSlots(stack)) {
            return weaponSlotModel.getBaseModel();
        }

        // FIXED: Skip cache completely for debugging

        // Determine context
        ItemDisplayContext context = determineContext(entity);

        // FIXED: Always create new combined model - no cache
        BakedModel combinedModel = weaponSlotModel.createCombinedModel(stack, context);

        return combinedModel;
    }

    private ItemDisplayContext determineContext(@Nullable LivingEntity entity) {
        if (entity == null) {
            return ItemDisplayContext.GUI;
        }

        Minecraft mc = Minecraft.getInstance();
        if (entity instanceof Player player && player == mc.player) {
            return ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        } else {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }
    }
}