package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public class FishBucketItem extends Item {
    private final String fishType;

    public FishBucketItem(String fishType, Properties properties) {
        super(properties);
        this.fishType = fishType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        BlockPos placePos = pos;

        // If clicking on a non-replaceable block, try to place adjacent
        if (!level.getBlockState(pos).canBeReplaced()) {
            placePos = pos.relative(context.getClickedFace());
        }

        // Check if we can place water at the target position
        if (level.getBlockState(placePos).canBeReplaced() || level.getFluidState(placePos).is(Fluids.WATER)) {
            if (!level.isClientSide) {
                // Place water block if not already water
                if (!level.getFluidState(placePos).is(Fluids.WATER)) {
                    level.setBlock(placePos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
                    // Play water placement sound
                    level.playSound(null, placePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                // Then spawn the fish in the water
                BaseFishEntity.spawnFromBucket(level, stack, placePos);

                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                    if (player.getInventory().getFreeSlot() >= 0) {
                        player.getInventory().add(new ItemStack(Items.BUCKET));
                    } else {
                        player.drop(new ItemStack(Items.BUCKET), false);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private boolean isWaterNearby(Level level, BlockPos pos) {
        // Check immediate position and surrounding area for water
        BlockPos[] positions = {
                pos, pos.above(), pos.below(),
                pos.north(), pos.south(), pos.east(), pos.west(),
                pos.above().north(), pos.above().south(),
                pos.above().east(), pos.above().west()
        };

        for (BlockPos checkPos : positions) {
            if (level.getFluidState(checkPos).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }
    public String getFishType() {
        return fishType;
    }
}