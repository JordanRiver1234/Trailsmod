package net.JordanRiver.KisekiLegend.block;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KisekiLegend.MOD_ID);

    public static final RegistryObject<BlockEntityType<OrbmentMachineBlockEntity>> ORBMENT_MACHINE =
            BLOCK_ENTITIES.register("orbment_machine",
                    () -> BlockEntityType.Builder
                            .of(OrbmentMachineBlockEntity::new, ModBlocks.ORBMENT_MACHINE.get())
                            .build(null)  // ✅ this is correct for Forge 1.21.1
            );


    public static final RegistryObject<BlockEntityType<QuartzMachineBlockEntity>> QUARTZ_MACHINE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("quartz_machine",
                    () -> BlockEntityType.Builder
                            .of(QuartzMachineBlockEntity::new, ModBlocks.QUARTZ_MACHINE.get())
                            .build(null)
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
