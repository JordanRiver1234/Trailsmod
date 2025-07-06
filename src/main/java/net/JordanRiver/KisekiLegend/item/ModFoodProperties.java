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

}
