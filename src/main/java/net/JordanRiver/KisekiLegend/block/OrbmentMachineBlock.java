package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class OrbmentMachineBlock extends Block implements EntityBlock {
    public OrbmentMachineBlock(Properties props) {
        super(props);
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
            ItemStack orb = machine.getOrbment();
            machine.setOrbment(ItemStack.EMPTY);
            if (!player.getInventory().add(orb.copy())) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, orb));
            }
            if (player instanceof ServerPlayer srv) {
                srv.sendSystemMessage(Component.literal("Removed Orbment"));
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.CONSUME;
        }

        // (2) Insert Orbment on first right-click
        if (!player.isShiftKeyDown() && held.getItem() instanceof OrbmentItem && !machine.hasOrbment()) {
            machine.setOrbment(held.copy());
            held.shrink(1);
            if (player instanceof ServerPlayer srv) {
                srv.sendSystemMessage(Component.literal("Inserted Orbment"));
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.HAPPY_VILLAGER,
                        true,
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        0f, 0.1f, 0f, 0.05f, 10
                ));
            }
            return InteractionResult.CONSUME;
        }

        // (3) Open GUI on second right-click (orbment already inserted)
        if (!player.isShiftKeyDown() && machine.hasOrbment()) {
            if (player instanceof ServerPlayer srv) {
                srv.openMenu(machine, pos); // ✅ Sends BlockPos to the client
                level.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 1.2f);
                srv.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.END_ROD,
                        true,
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
        // Delegates to main logic for bare hand
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrbmentMachineBlockEntity(pos, state);
    }
}
