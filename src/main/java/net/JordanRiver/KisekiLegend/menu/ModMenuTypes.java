package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, KisekiLegend.MOD_ID);

    public static final RegistryObject<MenuType<OrbmentMenu>> ORBMENT_MENU =
            MENUS.register("orbment_menu", () ->
                    IForgeMenuType.create((windowId, inv, buf) ->
                            new OrbmentMenu(windowId, inv)
                    )
            );

    public static final RegistryObject<MenuType<QuartzMachineMenu>> QUARTZ_MACHINE_MENU =
            MENUS.register("quartz_machine_menu", () -> IForgeMenuType.create(QuartzMachineMenu::new));

    public static final RegistryObject<MenuType<OrbmentMachineMenu>> ORBMENT_MACHINE =
            MENUS.register("orbment_machine", () ->
                    IForgeMenuType.create((windowId, inv, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        if (inv.player.level().getBlockEntity(pos) instanceof OrbmentMachineBlockEntity be) {
                            return new OrbmentMachineMenu(windowId, inv, be);
                        }
                        return null;
                    })
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}