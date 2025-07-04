package net.JordanRiver.KisekiLegend.menu;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, KisekiLegend.MOD_ID);

    // Orbment Item GUI
    public static final RegistryObject<MenuType<OrbmentMenu>> ORBMENT_MENU =
            MENUS.register("orbment_menu", () ->
                    IForgeMenuType.create((windowId, inv, buf) ->
                            new OrbmentMenu(windowId, inv)
                    )
            );

    // Orbment Machine GUI (reads BlockPos from buf safely)
    public static final RegistryObject<MenuType<OrbmentMachineMenu>> ORBMENT_MACHINE =
            MENUS.register("orbment_machine", () ->
                    IForgeMenuType.create((windowId, inv, buf) -> {
                        if (buf == null) throw new IllegalStateException("Missing BlockPos buffer for Orbment Machine!");
                        BlockPos pos = buf.readBlockPos();
                        if (!(inv.player.level().getBlockEntity(pos) instanceof OrbmentMachineBlockEntity be)) {
                            throw new IllegalStateException("No valid OrbmentMachineBlockEntity at " + pos);
                        }
                        return new OrbmentMachineMenu(windowId, inv, be);
                    })
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
