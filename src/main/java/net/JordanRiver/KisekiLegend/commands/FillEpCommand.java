package net.JordanRiver.KisekiLegend.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FillEpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kiseki")
                .then(Commands.literal("fillep")
                        .requires((source) -> source.hasPermission(2)) // Requires operator level 2
                        .executes((context) -> {
                            return fillPlayerEp(context.getSource());
                        })
                )
        );
    }

    private static int fillPlayerEp(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack orbmentStack = ItemStack.EMPTY;

        // Search inventory for an OrbmentItem
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) {
                orbmentStack = stack;
                break;
            }
        }

        if (orbmentStack.isEmpty()) {
            source.sendFailure(Component.literal("No Orbment found in your inventory."));
            return 0;
        }

        // Load the component, fill EP, and save it back
        OrbmentComponent component = OrbmentItem.loadComponent(orbmentStack, player.level(), player);
        component.fillToMaxEP();
        OrbmentItem.saveComponent(orbmentStack, component, player.level(), player);

        source.sendSuccess(() -> Component.literal("Your EP has been restored to maximum!"), true);
        return 1;
    }
}
