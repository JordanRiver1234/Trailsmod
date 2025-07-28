package net.JordanRiver.KisekiLegend.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ResetQuartzMachineCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kiseki")
                .then(Commands.literal("resetmachine")
                        .requires(source -> source.hasPermission(2)) // Operator-only
                        .executes(context -> resetMachine(context.getSource()))));
    }

    private static int resetMachine(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();

        HitResult result = player.pick(10, 0, false); // Raycast 10 blocks
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) result).getBlockPos();
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof QuartzMachineBlockEntity machine) {
                // Clear the state on the server
                machine.returnStoredItemsToPlayer(player);
                machine.setActiveRecipe(null); // This will clear items, nodes, and sync

                source.sendSuccess(() -> Component.literal("Quartz Machine at " + pos.toShortString() + " has been reset."), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("Block you are looking at is not a Quartz Machine."));
                return 0;
            }
        } else {
            source.sendFailure(Component.literal("You are not looking at a block."));
            return 0;
        }
    }
}