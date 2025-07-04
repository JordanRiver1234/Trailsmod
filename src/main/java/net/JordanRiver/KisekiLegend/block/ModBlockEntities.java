package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KisekiLegend.MOD_ID);

    public static final RegistryObject<BlockEntityType<OrbmentMachineBlockEntity>> ORBMENT_MACHINE =
            BLOCK_ENTITIES.register("orbment_machine",
                    () -> BlockEntityType.Builder
                            .of(OrbmentMachineBlockEntity::new,
                                    ModBlocks.ORBMENT_MACHINE.get())
                            .build(null)
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
