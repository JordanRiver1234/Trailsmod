package net.JordanRiver.KisekiLegend.commands;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID)
public class ModCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        // Register your commands here
        FillEpCommand.register(event.getDispatcher());
        ResetQuartzMachineCommand.register(event.getDispatcher());
        SpawnFishCommand.register(event.getDispatcher()); // ADD THIS LINE

        // This is for the default /config command
        ConfigCommand.register(event.getDispatcher());
    }
}
