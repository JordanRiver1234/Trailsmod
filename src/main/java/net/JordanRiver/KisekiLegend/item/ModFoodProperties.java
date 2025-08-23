package net.JordanRiver.KisekiLegend.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {

    public static final FoodProperties POT_O_MEAT = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(1.5f)
            .effect(new MobEffectInstance(MobEffects.HEAL, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1), 1.0f)
            .build();
    public static final FoodProperties BOUILLABAISSE = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 100), 1.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400), 0.5f) // Simulated CP
            .build();
    public static final FoodProperties CHEFS_CURRY = new FoodProperties.Builder()
            .nutrition(9)
            .saturationModifier(1.3f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 160), 1.0f)
            .build();
    public static final FoodProperties WILD_VEGGIE_POT = new FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(1.1f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 120), 1.0f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200), 0.4f) // CP-like bonus
            .build();
    public static final FoodProperties SALUBRIOUS_OATMEAL = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.6f)
            .effect(new MobEffectInstance(MobEffects.HEAL, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 80), 1.0f)
            .build();
    public static final FoodProperties JENIS_LUNCH = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.9f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 100), 1.0f)
            .build();
    public static final FoodProperties LIBERL_OMELET = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 80), 1.0f)
            .build();
    public static final FoodProperties CHEESE_RISOTTO = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.9f)
            .effect(new MobEffectInstance(MobEffects.HEAL, 1), 1.0f)
            .build();
    public static final FoodProperties ABADDON_POTLUCK = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .effect(new MobEffectInstance(MobEffects.HARM, 1), 0.5f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 200), 0.5f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400), 0.5f)
            .build();
    public static final FoodProperties WHOLESOME_PASTA = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 60), 1.0f)
            .build();
    public static final FoodProperties DIEHARD_PAELLA = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.7f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 70), 1.0f)
            .build();



    // Fish food properties
    public static final FoodProperties DACE = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600), 0.3f)
            .build();

    public static final FoodProperties YAMANY = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.4f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400), 0.4f)
            .build();

    public static final FoodProperties CRAB = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.6f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 800), 0.5f)
            .build();

    public static final FoodProperties KASAGO = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200), 0.7f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 200), 0.3f)
            .build();

    public static final FoodProperties LIBERL_CARP = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.0f)
            .effect(new MobEffectInstance(MobEffects.LUCK, 2400), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 400), 0.5f)
            .build();

    public static final FoodProperties PEARLGLASS = new FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(1.2f)
            .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600), 1.0f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600), 1.0f)
            .build();

    public static final FoodProperties GARVELZE = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(2.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0f)
            .build();

    public static final FoodProperties SNAKEHEAD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(1.5f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 400), 0.8f)
            .build();
    public static final FoodProperties GOLD_ANGELFISH = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.5f)
            .effect(new MobEffectInstance(MobEffects.LUCK, 1200), 0.6f)
            .build();

    public static final FoodProperties VALLERIA_BASS = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.2f)
            .effect(new MobEffectInstance(MobEffects.HUNGER, 200), 0.8f)
            .build();

    public static final FoodProperties ROCKEATER = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.7f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600), 0.4f)
            .build();

    public static final FoodProperties GREAT_BLACKFISH = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.9f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 300), 0.6f)
            .build();

    public static final FoodProperties CARP = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.6f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 200), 0.4f)
            .build();

    public static final FoodProperties OCTOPUS = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.1f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1800), 0.8f)
            .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200), 0.5f)
            .build();

    public static final FoodProperties RAINBOW_TROUT = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.LUCK, 1800), 0.7f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 300), 0.4f)
            .build();

    public static final FoodProperties TROUT = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.7f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 250), 0.5f)
            .build();

    public static final FoodProperties EEL = new FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(1.3f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400), 0.8f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800), 0.6f)
            .build();

    public static final FoodProperties SALMON = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 400), 0.7f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200), 0.4f)
            .build();

    public static final FoodProperties CLAUDINE = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(1.4f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 1), 0.9f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 500), 0.6f)
            .build();

    public static final FoodProperties SEA_BASS = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.9f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200), 0.6f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 300), 0.4f)
            .build();

    public static final FoodProperties GIGANGORA = new FoodProperties.Builder()
            .nutrition(12)
            .saturationModifier(2.5f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 2), 1.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 800, 1), 1.0f)
            .build();

    public static final FoodProperties MAHIMAHI = new FoodProperties.Builder()
            .nutrition(11)
            .saturationModifier(2.2f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4200, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 4800), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 700, 1), 0.9f)
            .build();

    public static final FoodProperties TIGER_ROCKFISH = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.1f)
            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400), 0.8f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 400), 0.6f)
            .build();

    public static final FoodProperties GRANAKOR = new FoodProperties.Builder()
            .nutrition(13)
            .saturationModifier(2.8f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4800, 2), 1.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 900, 2), 1.0f)
            .build();

    public static final FoodProperties BLUE_MARLIN = new FoodProperties.Builder()
            .nutrition(14)
            .saturationModifier(3.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 2), 1.0f)
            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 6000), 1.0f)
            .effect(new MobEffectInstance(MobEffects.LUCK, 4800, 1), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 1000, 2), 1.0f)
            .build();

    public static final FoodProperties DYNATRAD = new FoodProperties.Builder()
            .nutrition(15)
            .saturationModifier(3.5f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 7200, 3), 1.0f)
            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 2), 1.0f)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 3), 1.0f)
            .effect(new MobEffectInstance(MobEffects.LUCK, 6000, 2), 1.0f)
            .build();
}
