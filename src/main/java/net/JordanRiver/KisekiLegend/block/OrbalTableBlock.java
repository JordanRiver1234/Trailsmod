package net.JordanRiver.KisekiLegend.block;

import com.mojang.serialization.MapCodec;
import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrbalTableBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Cross-shaped hitbox (plus symbol)
    private static final VoxelShape SHAPE = Shapes.or(
            // Horizontal arm of the cross
            Block.box(2.0D, 0.0D, 6.0D, 14.0D, 4.0D, 10.0D),
            // Vertical arm of the cross
            Block.box(6.0D, 0.0D, 2.0D, 10.0D, 4.0D, 14.0D),
            // Center piece (slightly raised)
            Block.box(6.0D, 4.0D, 6.0D, 10.0D, 6.0D, 10.0D)
    );

    public OrbalTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        System.out.println("=== CREATING ORBAL TABLE BLOCK ENTITY ===");
        return new OrbalTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        System.out.println("=== ORBAL TABLE USE ITEM ON CALLED ===");

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OrbalTableBlockEntity orbalTable)) {
            System.out.println("Block entity is not OrbalTableBlockEntity: " + be);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Try to insert weapon directly
        if (!stack.isEmpty() && orbalTable.isWeaponOrTool(stack) && !orbalTable.hasWeapon()) {
            System.out.println("Attempting to insert weapon: " + stack.getDisplayName().getString());

            if (orbalTable.insertWeapon(stack)) {
                stack.shrink(1);
                player.sendSystemMessage(Component.literal("Weapon inserted into Orbal Table!"));

                // CRITICAL: Force immediate sync to client
                if (level instanceof ServerLevel serverLevel) {
                    System.out.println("=== FORCING BLOCK UPDATE AFTER WEAPON INSERT ===");

                    // Force block update
                    BlockState currentState = level.getBlockState(pos);
                    level.sendBlockUpdated(pos, currentState, currentState, Block.UPDATE_ALL);

                    // Force block entity sync
                    orbalTable.setChanged();
                    orbalTable.syncToClients();

                    // Force chunk update
                    serverLevel.getChunkSource().blockChanged(pos);

                    System.out.println("=== BLOCK SYNC FORCED ===");
                }

                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OrbalTableBlockEntity orbalTable)) {
            return InteractionResult.PASS;
        }

        // If crouching and there's a weapon, remove it
        if (player.isCrouching() && orbalTable.hasWeapon()) {
            ItemStack weapon = orbalTable.removeWeapon();
            if (!weapon.isEmpty()) {
                player.addItem(weapon);
                player.sendSystemMessage(Component.literal("Weapon removed from Orbal Table!"));
                return InteractionResult.CONSUME;
            }
        }

        // Otherwise open GUI
        if (player instanceof ServerPlayer serverPlayer) {
            orbalTable.openScreen(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.ORBAL_TABLE.get()) {
            return (BlockEntityTicker<T>) new OrbalTableBlockEntity.Ticker();
        }
        return null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof OrbalTableBlockEntity blockEntity) {
                // Drop stored weapon
                ItemStack weapon = blockEntity.getWeaponItem();
                if (!weapon.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), weapon);
                }
                // Drop inventory contents
                blockEntity.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}