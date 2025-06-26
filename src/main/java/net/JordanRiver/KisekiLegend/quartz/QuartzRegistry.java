package net.JordanRiver.KisekiLegend.quartz;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSources;

import java.util.*;
import java.util.function.BiConsumer;

public class QuartzRegistry {
    private static final Map<String, QuartzDefinition> MAP = new HashMap<>();

    static {
        // ──────────────── EARTH ────────────────// mute
        MAP.put("defense_1", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 0, 40, null));
        MAP.put("defense_2", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 1, 40, null));
        MAP.put("defense_3", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 2, 40, null));
        MAP.put("poison", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.POISON, 60, 0, false, true, true
                ));
            }
        }));
        MAP.put("petrify", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true, true
                ));
            }
        }));

        // ──────────────── WATER ────────────────
        MAP.put("hp_1", new QuartzDefinition(MobEffects.ABSORPTION, 0, 40, null));
        MAP.put("hp_2", new QuartzDefinition(MobEffects.ABSORPTION, 1, 40, null));
        MAP.put("hp_3", new QuartzDefinition(MobEffects.ABSORPTION, 2, 40, null));
        MAP.put("mind_1", new QuartzDefinition(MobEffects.DAMAGE_BOOST, 0, 40, null));
        MAP.put("mind_2", new QuartzDefinition(MobEffects.DAMAGE_BOOST, 1, 40, null));
        MAP.put("mind_3", new QuartzDefinition(MobEffects.DAMAGE_BOOST, 2, 40, null));
        MAP.put("freeze", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                // apply freeze damage type (no HP loss) to trigger relevant events
                target.hurt(
                        attacker.damageSources().freeze(),
                        0.0F
                );
                // then apply slowness I for 2 seconds
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true
                ));
            }
        }));
            MAP.put("heal", new QuartzDefinition(MobEffects.REGENERATION, 0, 40, null));

        // ──────────────── FIRE ────────────────
        MAP.put("attack_1", new QuartzDefinition(
                MobEffects.DAMAGE_BOOST, 0, 40,
                (target, attacker) -> attacker.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 40, 0, false, false, true
                ))
        ));
        MAP.put("attack_2", new QuartzDefinition(
                MobEffects.DAMAGE_BOOST, 1, 40,
                (target, attacker) -> attacker.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 40, 1, false, false, true
                ))
        ));
        MAP.put("attack_3", new QuartzDefinition(
                MobEffects.DAMAGE_BOOST, 2, 40,
                (target, attacker) -> attacker.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 40, 2, false, false, true
                ))
        ));
        MAP.put("seal", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN, 40, 1, false, true, true
                ));
            }
        }));
        MAP.put("confuse", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.CONFUSION, 40, 0, false, true, true
                ));
            }
        }));
        MAP.put("strike", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE, 40, 0, false, true, true
                ));
            }
        }));


        // ──────────────── WIND ────────────────
        MAP.put("shield_1", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 0, 40, null));
        MAP.put("shield_2", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 1, 40, null));
        MAP.put("shield_3", new QuartzDefinition(MobEffects.DAMAGE_RESISTANCE, 2, 40, null));
        MAP.put("evade_1", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 0, 40, null));
        MAP.put("evade_2", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 1, 40, null));
        MAP.put("evade_3", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 2, 40, null));

        // ──────────────── TIME ────────────────
        MAP.put("action_1", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 0, 40, null));
        MAP.put("action_2", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 1, 40, null));
        MAP.put("action_3", new QuartzDefinition(MobEffects.MOVEMENT_SPEED, 2, 40, null));
        MAP.put("blind", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS, 40, 0, false, true, true
                ));
            }
        }));
        MAP.put("cast_1", new QuartzDefinition(MobEffects.DIG_SPEED, 0, 40, null));
        MAP.put("cast_2", new QuartzDefinition(MobEffects.DIG_SPEED, 1, 40, null));

        // ──────────────── DEATHBLOW ────────────────
        MAP.put("deathblow_1", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            if (Math.random() < 0.10) {
                target.hurt(
                        attacker.damageSources().playerAttack(attacker),
                        target.getHealth()
                );
            }
        }));
        MAP.put("deathblow_2", new QuartzDefinition(null, 0, 0, (target, attacker) -> {
            target.hurt(
                    attacker.damageSources().genericKill(),
                    Float.MAX_VALUE
            );
        }));

        // ──────────────── SPACE ────────────────ep cut, range, eagle eye
        MAP.put("move_1", new QuartzDefinition(MobEffects.JUMP, 0, 40, null));
        MAP.put("move_2", new QuartzDefinition(MobEffects.JUMP, 1, 40, null));
        MAP.put("move_3", new QuartzDefinition(MobEffects.JUMP, 2, 40, null));

        // ──────────────── MIRAGE ────────────────information, haze * cloak
        MAP.put("ep_1", new QuartzDefinition(MobEffects.ABSORPTION, 0, 40, null));
        MAP.put("ep_2", new QuartzDefinition(MobEffects.ABSORPTION, 1, 40, null));
        MAP.put("ep_3", new QuartzDefinition(MobEffects.ABSORPTION, 2, 40, null));
        MAP.put("hit_1", new QuartzDefinition(MobEffects.DIG_SPEED, 0, 40, null));
        MAP.put("hit_2", new QuartzDefinition(MobEffects.DIG_SPEED, 1, 40, null));
        MAP.put("hit_3", new QuartzDefinition(MobEffects.DIG_SPEED, 2, 40, null));
        MAP.put("scent", new QuartzDefinition(MobEffects.GLOWING, 0, 100, null));

        // additional
        MAP.put("range_1", new QuartzDefinition(
                null, 0, 0,
                (target, wearer) -> {
                    // We won’t use this; we’ll do it on right-click instead.

    }));
        MAP.put("eagle_eye", new QuartzDefinition(null, 0, 0, (target, wearer) -> {
            // reveal enemies on map; implement in your map overlay code
        }));
        MAP.put("haze", new QuartzDefinition(null, 0, 0, (target, wearer) -> {
            wearer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, true, false, false));
        }));
        MAP.put("cloak", new QuartzDefinition(null, 0, 0, (target, wearer) -> {
            wearer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, true, false, false));
        }));

    }


    /** Lookup by your quartz ID (e.g. "poison") */
    public static QuartzDefinition get(String id) {
        return MAP.get(id);
    }

    public static Collection<QuartzDefinition> all() {
        return Collections.unmodifiableCollection(MAP.values());
    }
}
