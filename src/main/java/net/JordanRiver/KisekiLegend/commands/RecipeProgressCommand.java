package net.JordanRiver.KisekiLegend.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.JordanRiver.KisekiLegend.capability.PlayerRecipeProgressProvider;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.SyncRecipeProgressPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

public class RecipeProgressCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recipeProgress")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetProgress(context, EntityArgument.getPlayers(context, "players"))))
                        .executes(context -> resetProgress(context, null))) // Reset for command sender
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> listProgress(context, EntityArgument.getPlayer(context, "player"))))
                        .executes(context -> listProgress(context, null)))); // List for command sender
    }

    private static int resetProgress(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        CommandSourceStack source = context.getSource();

        if (players == null) {
            // Reset for command sender
            if (source.getEntity() instanceof Player player) {
                resetPlayerProgress(player, source);
                return 1;
            } else {
                source.sendFailure(Component.literal("You must specify a player when running from console"));
                return 0;
            }
        } else {
            // Reset for specified players
            for (ServerPlayer player : players) {
                resetPlayerProgress(player, source);
            }
            final int count = players.size();
            source.sendSuccess(() -> Component.literal("Reset recipe progress for " + count + " player(s)"), true);
            return count;
        }
    }


    private static void resetPlayerProgress(Player player, CommandSourceStack source) {
        player.getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(progress -> {
            progress.clearProgress();
            source.sendSuccess(() -> Component.literal("Reset recipe progress for " + player.getName().getString()), true);

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("Your recipe progress has been reset!"));
                // ADD THIS LINE to sync the cleared progress to client
                NetworkHandler.sendToPlayer(new SyncRecipeProgressPacket(progress.getCompletedRecipes(), serverPlayer.getUUID()), serverPlayer);
            }
        });
    }
    private static int listProgress(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        CommandSourceStack source = context.getSource();

        Player player;
        if (targetPlayer == null) {
            if (source.getEntity() instanceof Player sourcePlayer) {
                player = sourcePlayer;
            } else {
                source.sendFailure(Component.literal("You must specify a player when running from console"));
                return 0;
            }
        } else {
            player = targetPlayer;
        }

        player.getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS).ifPresent(progress -> {
            var completedRecipes = progress.getCompletedRecipes();
            source.sendSuccess(() -> Component.literal("Recipe progress for " + player.getName().getString() + ":"), false);

            if (completedRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  No recipes completed"), false);
            } else {
                source.sendSuccess(() -> Component.literal("  Completed recipes (" + completedRecipes.size() + "):"), false);
                completedRecipes.forEach(recipeId -> {
                    source.sendSuccess(() -> Component.literal("    - " + recipeId.toString()), false);
                });
            }
        });

        return 1;
    }
}