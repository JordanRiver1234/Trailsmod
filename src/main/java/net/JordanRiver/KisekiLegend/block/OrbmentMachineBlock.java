package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class OrbmentMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public OrbmentMachineBlock(Properties props) {
        super(props);
        registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public @NotNull InteractionResult use(@NotNull BlockState state,
                                          @NotNull Level level,
                                          @NotNull BlockPos pos,
                                          @NotNull Player player,
                                          @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OrbmentMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // (1) Remove Orbment with Shift + Empty hand
        if (player.isShiftKeyDown() && machine.hasOrbment() && held.isEmpty()) {
            ItemStack orb = machine.getOrbment().copy();

            // Use the new removal method to properly mark it as removed
            machine.removeOrbment();

            // Try to add to player inventory first
            if (!player.getInventory().add(orb)) {
                // If inventory is full, drop in world
                level.addFreshEntity(new ItemEntity(
                        level,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        orb
                ));
            }

            if (player instanceof ServerPlayer srv) {
                srv.sendSystemMessage(Component.literal("Removed Orbment"));
            }
            level.playSound(
                    null, pos,
                    SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS,
                    1f, 1f
            );
            return InteractionResult.CONSUME;
        }

        // (2) Insert Orbment on first right‐click
        if (!player.isShiftKeyDown()
                && held.getItem() instanceof OrbmentItem
                && !machine.hasOrbment()) {

            machine.setOrbment(held.copy());
            held.shrink(1);

            if (player instanceof ServerPlayer srv) {
                srv.sendSystemMessage(Component.literal("Inserted Orbment"));
            }
            level.playSound(
                    null, pos,
                    SoundEvents.ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS,
                    1f, 1f
            );

            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.HAPPY_VILLAGER, true,
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        0f, 0.1f, 0f, 0.05f, 10
                ));
            }
            return InteractionResult.CONSUME;
        }

        // (3) Open GUI on second right‐click
        if (!player.isShiftKeyDown() && machine.hasOrbment()) {
            if (player instanceof ServerPlayer srv) {
                srv.openMenu(machine, pos);
                level.playSound(
                        null, pos,
                        SoundEvents.ENDER_CHEST_OPEN,
                        SoundSource.BLOCKS,
                        0.8f, 1.2f
                );
                srv.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.END_ROD, true,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        0f, 0.2f, 0f, 0.05f, 15
                ));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state,
                                                     @NotNull Level level,
                                                     @NotNull BlockPos pos,
                                                     @NotNull Player player,
                                                     @NotNull BlockHitResult hit) {
        // Delegates to main logic for bare‐hand interaction
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrbmentMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof OrbmentMachineBlockEntity machine) {
                machine.tickClientParticles();
            }
        };
    }

    /**
     * When the block is broken or replaced, drop any stored Orbment
     * so that it never disappears.
     */
    @Override
    public void onRemove(@NotNull BlockState oldState,
                         @NotNull Level level,
                         @NotNull BlockPos pos,
                         @NotNull BlockState newState,
                         boolean isMoving) {
        // Only drop when actually removed, not on blockstate change
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OrbmentMachineBlockEntity machine) {
                ItemStack orb = machine.getOrbment();
                if (!orb.isEmpty()) {
                    // spawn it back in the world
                    level.addFreshEntity(new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            orb
                    ));
                }
            }
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }
}