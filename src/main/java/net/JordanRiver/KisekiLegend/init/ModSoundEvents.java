package net.JordanRiver.KisekiLegend.init;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KisekiLegend.MOD_ID);

    // Casting sounds
    public static final RegistryObject<SoundEvent> CAST_START = registerSoundEvent("cast_start");
    public static final RegistryObject<SoundEvent> CAST_LOOP = registerSoundEvent("cast_loop");
    public static final RegistryObject<SoundEvent> CAST_COMPLETE = registerSoundEvent("cast_complete");
    public static final RegistryObject<SoundEvent> CAST_FAIL = registerSoundEvent("cast_fail");

    // UI sounds
    public static final RegistryObject<SoundEvent> ART_SELECT = registerSoundEvent("art_select");

    // Orbment UI Sounds
    public static final RegistryObject<SoundEvent> ORBMENT_MENU_OPEN = registerSoundEvent("ui.orbment.open");
    public static final RegistryObject<SoundEvent> ORBMENT_MENU_CLOSE = registerSoundEvent("ui.orbment.close");
    public static final RegistryObject<SoundEvent> ORBMENT_SLOT_UNLOCK = registerSoundEvent("ui.orbment.unlock");
    public static final RegistryObject<SoundEvent> ORBMENT_SLOT_LOCKED = registerSoundEvent("ui.orbment.locked");


    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}