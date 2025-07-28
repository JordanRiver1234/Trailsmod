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
        // Register your command here
        FillEpCommand.register(event.getDispatcher());
        ResetQuartzMachineCommand.register(event.getDispatcher());

        // This is for the default /config command, you can leave it
        ConfigCommand.register(event.getDispatcher());
    }
}
