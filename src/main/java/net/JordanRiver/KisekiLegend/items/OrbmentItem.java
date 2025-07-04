// OrbmentItem.java
package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.JordanRiver.KisekiLegend.items.SizedItemStackHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class OrbmentItem extends Item {
    public OrbmentItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Only open GUI if player is not right-clicking on a block
        HitResult target = player.pick(5.0D, 0.0F, false);
        if (!level.isClientSide() && target.getType() == HitResult.Type.MISS) {
            player.openMenu(new SimpleMenuProvider(
                    (windowId, inv, plyr) -> new OrbmentMenu(windowId, inv),
                    Component.literal("Orbment")
            ));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public static void saveInventory(ItemStack stack, SizedItemStackHandler handler, int unlockedSlots, Level level) {
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = existing.copyTag();
        tag.put("orbment_inventory", handler.serializeNBT(level.registryAccess()));
        tag.putInt("orbment_unlocked", unlockedSlots);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static OrbmentComponent loadComponent(ItemStack stack, Level level) {
        OrbmentComponent component = new OrbmentComponent();
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();
            if (tag.contains("orbment_unlocked", Tag.TAG_INT)) {
                component.setUnlockedSlots(tag.getInt("orbment_unlocked"));
            }
            if (tag.contains("orbment_inventory", Tag.TAG_COMPOUND)) {
                SizedItemStackHandler handler = new SizedItemStackHandler(OrbmentMenu.ORBMENT_SLOT_COUNT);
                handler.deserializeNBT(level.registryAccess(), tag.getCompound("orbment_inventory"));
                component.setInventory(handler);
            }
        }
        return component;
    }
}
