package net.JordanRiver.KisekiLegend.block;

import com.mojang.serialization.MapCodec;
import net.JordanRiver.KisekiLegend.block.core.DoubleHighBlock;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class QuartzMachineBlock extends DoubleHighBlock {
    public static final MapCodec<QuartzMachineBlock> CODEC = simpleCodec(QuartzMachineBlock::new);

    public QuartzMachineBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected MapCodec<? extends DoubleHighBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // This allows custom rendering
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        if (pState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new QuartzMachineBlockEntity(pPos, pState);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {
        System.out.println("Block clicked - Client side: " + pLevel.isClientSide());

        if (!pLevel.isClientSide()) {
            // For double blocks, always use the lower half
            BlockPos blockEntityPos = pPos;
            if (pState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                blockEntityPos = pPos.below();
            }

            BlockEntity entity = pLevel.getBlockEntity(blockEntityPos);
            System.out.println("Block entity at " + blockEntityPos + ": " + entity);

            if (entity instanceof QuartzMachineBlockEntity quartzEntity) {
                System.out.println("Found QuartzMachineBlockEntity");

                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    System.out.println("Opening menu for server player");
                    serverPlayer.openMenu(quartzEntity, quartzEntity::writeScreenOpeningData);
                }
            }
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.QUARTZ_MACHINE_BLOCK_ENTITY.get(), QuartzMachineBlockEntity::tick);
        }
        return null;
    }
}