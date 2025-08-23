package net.JordanRiver.KisekiLegend.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.fishing.FishTypeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class SpawnFishCommand {

    private static final List<String> FISH_TYPES = Arrays.asList(
            "carp", "liberl_carp", "crab", "dace", "eel", "kasago", "salmon",
            "sea_bass", "valleria_bass", "trout", "rainbow_trout", "yamany",
            "snakehead", "octopus", "granakor", "dynatrad", "garvelze", "gigangora", "great_blackfish",
            "pearlglass", "blue_marlin", "mahimahi", "claudine", "tiger_rockfish", "rockeater", "gold_angelfish"
    );

    private static final SuggestionProvider<CommandSourceStack> FISH_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(FISH_TYPES, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnfish")
                .requires(source -> source.hasPermission(2)) // OP level 2
                .then(Commands.argument("fish_type", StringArgumentType.string())
                        .suggests(FISH_TYPE_SUGGESTIONS)
                        .executes(SpawnFishCommand::spawnFishAtSelf)
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(SpawnFishCommand::spawnFishAtPlayer))));
    }

    private static int spawnFishAtSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        return spawnFish(context, player);
    }

    private static int spawnFishAtPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        return spawnFish(context, targetPlayer);
    }

    private static int spawnFish(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        String fishType = StringArgumentType.getString(context, "fish_type");

        if (!FISH_TYPES.contains(fishType)) {
            context.getSource().sendFailure(Component.literal("Unknown fish type: " + fishType));
            return 0;
        }

        try {
            BaseFishEntity fish = FishTypeRegistry.createFishEntity(fishType, targetPlayer.level());
            if (fish == null) {
                context.getSource().sendFailure(Component.literal("Failed to create fish entity for type: " + fishType));
                return 0;
            }

            // Position fish in front of player
            Vec3 playerPos = targetPlayer.position();
            Vec3 lookVec = targetPlayer.getLookAngle();
            Vec3 spawnPos = playerPos.add(lookVec.scale(3.0)).add(0, 1.0, 0);

            fish.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            fish.setPersistenceRequired();
            fish.setNoGravity(true);
            fish.setGlowingTag(true);
            fish.setCustomName(Component.literal(fishType)); // Name it to prevent despawn

            // Add to world
            boolean spawned = targetPlayer.serverLevel().addFreshEntity(fish);

            if (spawned) {
                context.getSource().sendSuccess(() ->
                        Component.literal("Spawned " + fishType + " at " + targetPlayer.getName().getString() + "'s location"), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Failed to spawn fish"));
                return 0;
            }

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error spawning fish: " + e.getMessage()));
            return 0;
        }
    }
}