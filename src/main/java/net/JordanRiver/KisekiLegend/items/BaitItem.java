package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.fishing.FishData;
import net.JordanRiver.KisekiLegend.fishing.FishRegistry;
import net.JordanRiver.KisekiLegend.fishing.RodType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class BaitItem extends Item {
    private final String baitType;

    public BaitItem(String baitType, Properties properties) {
        super(properties.stacksTo(64)); // Allow stacking to 64
        this.baitType = baitType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack baitStack = context.getItemInHand();

        // Check if clicking on a fishing rod
        ItemStack targetStack = context.getLevel().getBlockState(context.getClickedPos()).getBlock().asItem().getDefaultInstance();

        // Check player's hands for fishing rod
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack rodStack = null;
        if (mainHand.getItem() instanceof KisekiFishingRodItem && mainHand != baitStack) {
            rodStack = mainHand;
        } else if (offHand.getItem() instanceof KisekiFishingRodItem && offHand != baitStack) {
            rodStack = offHand;
        }

        if (rodStack != null) {
            return KisekiFishingRodItem.tryAttachBait(rodStack, baitStack, player) ?
                    InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Don't handle use in hand - only handle useOn for drag-and-drop style interaction
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.literal("Right-click while holding fishing rod to attach"));
        tooltipComponents.add(Component.literal("Bait Type: " + baitType));
    }

    public String getBaitType() {
        return baitType;
    }
}