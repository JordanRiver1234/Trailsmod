package net.JordanRiver.KisekiLegend.quartz;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.BiConsumer;

public class QuartzRegistry {
    private static final Map<String, QuartzDefinition> MAP = new HashMap<>();

    static {
        // ──────────────── EARTH ────────────────
        // Only on-hit effects are defined here; sepith values are in ModItems
        MAP.put("defense_1", new QuartzDefinition()); // No special effect
        MAP.put("defense_2", new QuartzDefinition()); // No special effect
        MAP.put("defense_3", new QuartzDefinition()); // No special effect

        MAP.put("poison", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.POISON, 60, 0, false, true, true
                ));
            }
        }));

        MAP.put("petrify", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true, true
                ));
            }
        }));

        // ──────────────── WATER ────────────────
        MAP.put("hp_1", new QuartzDefinition());
        MAP.put("hp_2", new QuartzDefinition());
        MAP.put("hp_3.json", new QuartzDefinition());
        MAP.put("mind_1", new QuartzDefinition());
        MAP.put("mind_2", new QuartzDefinition());
        MAP.put("mind_3", new QuartzDefinition());

        MAP.put("freeze", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.hurt(attacker.damageSources().freeze(), 0.0F);
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true
                ));
            }
        }));

        MAP.put("heal", new QuartzDefinition());

        // ──────────────── FIRE ────────────────
        MAP.put("attack_1", new QuartzDefinition((target, attacker) -> {
            // Apply weakness to attacker as trade-off for damage boost
            attacker.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 40, 0, false, false, true
            ));
        }));

        MAP.put("attack_2", new QuartzDefinition((target, attacker) -> {
            attacker.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 40, 1, false, false, true
            ));
        }));

        MAP.put("attack_3", new QuartzDefinition((target, attacker) -> {
            attacker.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 40, 2, false, false, true
            ));
        }));

        MAP.put("seal", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN, 40, 1, false, true, true
                ));
            }
        }));

        MAP.put("confuse", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.CONFUSION, 40, 0, false, true, true
                ));
            }
        }));

        MAP.put("strike", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE, 40, 0, false, true, true
                ));
            }
        }));

        // ──────────────── WIND ────────────────
        MAP.put("shield_1", new QuartzDefinition());
        MAP.put("shield_2", new QuartzDefinition());
        MAP.put("shield_3", new QuartzDefinition());
        MAP.put("evade_1", new QuartzDefinition());
        MAP.put("evade_2", new QuartzDefinition());
        MAP.put("evade_3", new QuartzDefinition());

        // ──────────────── TIME ────────────────
        MAP.put("action_1", new QuartzDefinition());
        MAP.put("action_2", new QuartzDefinition());
        MAP.put("action_3", new QuartzDefinition());

        MAP.put("blind", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS, 40, 0, false, true, true
                ));
            }
        }));

        MAP.put("cast_1", new QuartzDefinition());
        MAP.put("cast_2", new QuartzDefinition());

        // ──────────────── DEATHBLOW ────────────────
        MAP.put("deathblow_1", new QuartzDefinition((target, attacker) -> {
            if (Math.random() < 0.10) {
                target.hurt(attacker.damageSources().playerAttack(attacker), target.getHealth());
            }
        }));

        MAP.put("deathblow_2", new QuartzDefinition((target, attacker) -> {
            target.hurt(attacker.damageSources().genericKill(), Float.MAX_VALUE);
        }));

        // ──────────────── SPACE ────────────────
        MAP.put("move_1", new QuartzDefinition());
        MAP.put("move_2", new QuartzDefinition());
        MAP.put("move_3", new QuartzDefinition());

        // ──────────────── MIRAGE ────────────────
        MAP.put("ep_1", new QuartzDefinition());
        MAP.put("ep_2", new QuartzDefinition());
        MAP.put("ep_3", new QuartzDefinition());
        MAP.put("hit_1", new QuartzDefinition());
        MAP.put("hit_2", new QuartzDefinition());
        MAP.put("hit_3", new QuartzDefinition());

        MAP.put("scent", new QuartzDefinition());

        // Additional special quartz
        MAP.put("range_1", new QuartzDefinition((target, wearer) -> {
            // Range enhancement - implement in weapon/tool logic
        }));

        MAP.put("eagle_eye", new QuartzDefinition((target, wearer) -> {
            // Reveal enemies - implement in map overlay
        }));

        MAP.put("haze", new QuartzDefinition((target, wearer) -> {
            wearer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, true, false, false));
        }));

        MAP.put("cloak", new QuartzDefinition((target, wearer) -> {
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