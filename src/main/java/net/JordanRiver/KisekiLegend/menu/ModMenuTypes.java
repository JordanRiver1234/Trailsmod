package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, KisekiLegend.MOD_ID);

    public static final RegistryObject<MenuType<OrbmentMenu>> ORBMENT_MENU =
            MENUS.register("orbment_menu",
                    () -> IForgeMenuType.create(      // ← factory must match (int,Inventory)
                            (windowId, inv, buf) -> // buf is ignored under the hood since we never wrote extra data
                                    new OrbmentMenu(windowId, inv)
                    )
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}