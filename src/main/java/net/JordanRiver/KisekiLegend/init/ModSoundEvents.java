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

    // Radial Menu UI sounds
    public static final RegistryObject<SoundEvent> UI_CLOCK_OPEN = registerSoundEvent("ui.clock_open");
    public static final RegistryObject<SoundEvent> UI_CLOCK_CLOSE = registerSoundEvent("ui.clock_close");
    public static final RegistryObject<SoundEvent> UI_ELEMENT_HOVER_TICK = registerSoundEvent("ui.element_tick");
    public static final RegistryObject<SoundEvent> UI_RINGS_ENGAGE = registerSoundEvent("ui.rings_engage");
    public static final RegistryObject<SoundEvent> UI_SPIRAL_TICK = registerSoundEvent("ui.spiral_tick");
    public static final RegistryObject<SoundEvent> UI_ART_SELECT_CHIME = registerSoundEvent("ui.art_select");
    public static final RegistryObject<SoundEvent> UI_FAVORITE_SET = registerSoundEvent("ui.favorite_set");

    // UI sounds
    public static final RegistryObject<SoundEvent> ART_SELECT = registerSoundEvent("art_select");

    // Orbment UI Sounds
    public static final RegistryObject<SoundEvent> ORBMENT_MENU_OPEN = registerSoundEvent("ui.orbment.open");
    public static final RegistryObject<SoundEvent> ORBMENT_MENU_CLOSE = registerSoundEvent("ui.orbment.close");
    public static final RegistryObject<SoundEvent> ORBMENT_SLOT_UNLOCK = registerSoundEvent("ui.orbment.unlock");
    public static final RegistryObject<SoundEvent> ORBMENT_SLOT_LOCKED = registerSoundEvent("ui.orbment.locked");


    // Loading screen sounds
    public static final RegistryObject<SoundEvent> QUARTZ_STARTUP = registerSoundEvent("quartz_startup");
    public static final RegistryObject<SoundEvent> QUARTZ_PROCESSING = registerSoundEvent("quartz_processing");
    public static final RegistryObject<SoundEvent> QUARTZ_COMPLETE = registerSoundEvent("quartz_complete");
    public static final RegistryObject<SoundEvent> QUARTZ_ERROR = registerSoundEvent("quartz_error");
    // Item merge and convergence sounds
// Synthesis sounds for different tiers
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER1_START = registerSoundEvent("synthesis.tier1.start");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER1_LOOP = registerSoundEvent("synthesis.tier1.loop");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER1_COMPLETE = registerSoundEvent("synthesis.tier1.complete");

    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER2_START = registerSoundEvent("synthesis.tier2.start");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER2_LOOP = registerSoundEvent("synthesis.tier2.loop");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER2_COMPLETE = registerSoundEvent("synthesis.tier2.complete");

    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER3_START = registerSoundEvent("synthesis.tier3.start");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER3_LOOP = registerSoundEvent("synthesis.tier3.loop");
    public static final RegistryObject<SoundEvent> SYNTHESIS_TIER3_COMPLETE = registerSoundEvent("synthesis.tier3.complete");

    public static final RegistryObject<SoundEvent> SYNTHESIS_EFFECT_START = registerSoundEvent("synthesis.effect.start");
    public static final RegistryObject<SoundEvent> SYNTHESIS_EFFECT_LOOP = registerSoundEvent("synthesis.effect.loop");
    public static final RegistryObject<SoundEvent> SYNTHESIS_EFFECT_COMPLETE = registerSoundEvent("synthesis.effect.complete");

    // Add these lines to the existing ModSoundEvents class
    public static final RegistryObject<SoundEvent> FISHING_EXCLAMATION = registerSoundEvent("fishing.exclamation");
    public static final RegistryObject<SoundEvent> FISHING_TENSION_START = registerSoundEvent("fishing.tension_start");
    public static final RegistryObject<SoundEvent> FISHING_TENSION_END = registerSoundEvent("fishing.tension_end");
    public static final RegistryObject<SoundEvent> FISHING_REEL = registerSoundEvent("fishing.reel");
    public static final RegistryObject<SoundEvent> FISHING_HIT_EFFECT = registerSoundEvent("fishing.hit_effect");
    public static final RegistryObject<SoundEvent> FISHING_SUCCESS = registerSoundEvent("fishing.success");
    public static final RegistryObject<SoundEvent> FISHING_FAIL = registerSoundEvent("fishing.fail");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}