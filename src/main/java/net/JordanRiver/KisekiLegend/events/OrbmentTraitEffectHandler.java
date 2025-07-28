package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.item.enhancement.ItemEnhancementSystem;
import net.JordanRiver.KisekiLegend.item.enhancement.MaterialQualitySystem;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import java.util.*;

@Mod.EventBusSubscriber(modid = KisekiLegend.MOD_ID)
public class OrbmentTraitEffectHandler {

    private static final Random RANDOM = new Random();

    /**
     * Handles passive effects that occur every second. ⏱️
     * Every effect now has its own unique case for maximum granularity.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || player.level().getGameTime() % 20 != 0) {
            return;
        }

        Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);
        if (activeEffects.isEmpty()) return;

        for (MaterialQualitySystem.EffectData effect : activeEffects.values()) {
            int amplifier = Math.max(0, (int) (effect.getValue() - 1));
            int duration = 45;

            switch (effect.getName()) {
                //<editor-fold desc="HP Effects">
                case "HP Gain XS": player.heal(effect.getValue() * 0.1f); break;
                case "HP Gain S": player.heal(effect.getValue() * 0.25f); break;
                case "HP Gain M": player.heal(effect.getValue() * 0.5f); break;
                case "HP Gain L": player.heal(effect.getValue() * 1.0f); break;
                case "HP Gain XL": player.heal(effect.getValue() * 2.0f); break;
                case "HP Regen S": player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0)); break;
                case "HP Regen M": player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1)); break;
                case "HP Regen L": player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2)); break;
                //</editor-fold>

                //<editor-fold desc="Attack/Damage Effects">
                case "ATK Up S": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0)); break;
                case "ATK Up M": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1)); break;
                case "ATK Down S": player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0)); break;
                case "Physical Damage XS": break; // Handled in attack events
                case "Physical Damage M": break; // Handled in attack events
                case "Fire Damage L": break; // Handled in attack events
                case "Fire Damage XL": break; // Handled in attack events
                case "Ice Damage S": break; // Handled in attack events
                case "Lightning Damage S": break; // Handled in attack events
                case "Poison Damage XS": break; // Handled in attack events
                //</editor-fold>

                //<editor-fold desc="Defense Effects">
                case "DEF Up S": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0)); break;
                case "Guardian Mirror S": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1)); break;
                case "Defense Veil": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, (int)(effect.getValue() - 1))); break;
                case "Reduce Damage -3%": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0)); break;
                case "Reduce Damage -10%": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1)); break;
                case "Fire Resist Up": player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0)); break;
                case "Fire Resist Up+": player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 1)); break;
                //</editor-fold>

                //<editor-fold desc="Speed Effects">
                case "SPD Up S": player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0)); break;
                case "SPD Up M": player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1)); break;
                case "SPD Down S": player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0)); break;
                case "Wind Rider":
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
                    break;
                //</editor-fold>

                //<editor-fold desc="Special Resistances & Protections">
                case "Water Breathing": player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0)); break;
                case "Night Vision": player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0)); break;
                case "Fire Resistance": player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0)); break;
                case "Levitation": player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, 0)); break;
                case "Slow Falling": player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0)); break;
                //</editor-fold>

                //<editor-fold desc="Debuff Infliction Effects">
                case "Inflict Burn S": if (RANDOM.nextInt(20) == 0) player.setRemainingFireTicks(60); break;
                case "Inflict Burn M": if (RANDOM.nextInt(15) == 0) player.setRemainingFireTicks(100); break;
                case "Inflict Burn L": if (RANDOM.nextInt(10) == 0) player.setRemainingFireTicks(140); break;
                case "Inflict Poison S": if (RANDOM.nextInt(25) == 0) player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); break;
                case "Inflict Poison M": if (RANDOM.nextInt(20) == 0) player.addEffect(new MobEffectInstance(MobEffects.POISON, 150, 0)); break;
                case "Inflict Poison L": if (RANDOM.nextInt(15) == 0) player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1)); break;
                case "Inflict Frostbite S": if (RANDOM.nextInt(20) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)); break;
                case "Inflict Frostbite M": if (RANDOM.nextInt(15) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 150, 1)); break;
                case "Inflict Thorn S": if (RANDOM.nextInt(30) == 0) player.hurt(player.damageSources().cactus(), 1.0f); break;
                case "Inflict Curse S": if (RANDOM.nextInt(25) == 0) player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 200, 0)); break;
                case "Inflict Curse M": if (RANDOM.nextInt(20) == 0) player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 300, 0)); break;
                case "Inflict Curse L": if (RANDOM.nextInt(15) == 0) player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 400, 1)); break;
                case "Inflict Slow S": if (RANDOM.nextInt(25) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)); break;
                case "All Stats Down S": if (RANDOM.nextInt(40) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0));
                } break;
                //</editor-fold>

                //<editor-fold desc="Utility & Cleansing Effects">
                case "Remove Debuffs":
                    if (RANDOM.nextInt(100) == 0) {
                        player.getActiveEffects().stream()
                                .filter(e -> !e.getEffect().value().isBeneficial())
                                .forEach(e -> player.removeEffect(e.getEffect()));
                    }
                    break;
                case "Remove Ailments":
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(MobEffects.CONFUSION);
                    break;
                case "Poison Cure": player.removeEffect(MobEffects.POISON); break;
                //</editor-fold>

                //<editor-fold desc="Food & Saturation Effects">
                case "Healing Taste S": player.getFoodData().eat(1, 0.2f); break;
                case "Healing Taste M": player.getFoodData().eat(2, 0.4f); break;
                case "Healing Taste L": player.getFoodData().eat(3, 0.6f); break;
                case "Feeling Full S": player.getFoodData().eat(1, 0.5f); break;
                case "Mild Sweetness": player.getFoodData().eat(1, 0.1f); break;
                case "Sweetness": player.getFoodData().eat(2, 0.2f); break;
                //</editor-fold>

                //<editor-fold desc="Skill & Enhancement Effects">
                case "Enhance Skills +3%": break; // Passive bonus
                case "Enhance Skills +7%": break; // Passive bonus
                case "Enhance Skills +10%": break; // Passive bonus
                case "Enhance Critical +20%": break; // Handled in attack events
                case "Critical Rate Up S": break; // Handled in attack events
                case "Enhance Items +5%": break; // Tool enhancement bonus
                case "Weaken Items +3%": break; // Tool durability penalty
                //</editor-fold>

                //<editor-fold desc="Special Utility Effects">
                case "Magic Veil": player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0)); break;
                case "Money Magnet": break; // Custom implementation needed
                case "Eye for Materials": break; // Custom mining bonus
                case "Treasure Hunter": break; // Custom loot bonus
                case "Dragon Slayer": break; // Massive damage bonus vs dragons
                case "All Stats Up L":
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
                    break;
                case "Energy Surge L":
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 1));
                    break;
                case "Light Blessing S": player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0)); break;
                case "Thunderclap S":
                    if (RANDOM.nextInt(100) < 5) {
                        AABB area = player.getBoundingBox().inflate(8.0);
                        player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player)
                                .forEach(e -> {
                                    e.hurt(player.damageSources().lightningBolt(), 3.0f);
                                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                                });
                    }
                    break;
                //</editor-fold>

                //<editor-fold desc="Recovery Effects">
                case "KO Recovery S": break; // Handled in death event
                case "KO Recovery M": break; // Handled in death event
                case "Resist KO +10%": break; // Handled in death event
                //</editor-fold>

                //<editor-fold desc="Harmful/Risky Effects">
                case "Self Harm": if (RANDOM.nextInt(200) == 0) player.hurt(player.damageSources().magic(), 1.0f); break;
                case "Surprise! S":
                    if (RANDOM.nextInt(500) == 0) {
                        int randomEffect = RANDOM.nextInt(6);
                        switch (randomEffect) {
                            case 0 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                            case 1 -> player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));
                            case 2 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                            case 3 -> player.setRemainingFireTicks(60);
                            case 4 -> player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0));
                            case 5 -> teleportPlayerRandomly(player, 16);
                        }
                    }
                    break;
                case "Fire Vulnerability":
                    if (player.isOnFire()) player.hurt(player.damageSources().onFire(), 0.5f);
                    break;
                //</editor-fold>

                //<editor-fold desc="Random & Chaos Effects">
                case "Random Effect":
                    if (RANDOM.nextInt(200) == 0) {
                        Holder<MobEffect> randomEffect = RANDOM.nextBoolean() ?
                                MobEffects.MOVEMENT_SPEED : MobEffects.JUMP;
                        player.addEffect(new MobEffectInstance(randomEffect, 200, 0));
                    }
                    break;
                case "Random Teleport":
                    if (RANDOM.nextInt(400) == 0) teleportPlayerRandomly(player, 16);
                    break;
                case "Explosive":
                    if (RANDOM.nextInt(1000) == 0) {
                        player.level().explode(player, player.getX(), player.getY(), player.getZ(),
                                2.0f * effect.getValue(), Level.ExplosionInteraction.NONE);
                    }
                    break;
                //</editor-fold>

                //<editor-fold desc="XP & Learning Effects">
                case "XP Gain": break; // Handled in XP pickup event
                //</editor-fold>
            }
        }
    }

    /**
     * Handles defensive/reactive traits and effects that trigger upon taking damage. 🛡️
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        Map<String, MaterialQualitySystem.TraitData> activeTraits = getPlayerActiveTraits(player);
        Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);
        float damage = event.getAmount();

        // --- TRAIT-BASED DEFENSES ---
        for (MaterialQualitySystem.TraitData trait : activeTraits.values()) {
            switch (trait.getName()) {
                //<editor-fold desc="Healing & Recovery Traits">
                case "Healing": damage *= (1.0f - (trait.getLevel() * 0.02f)); break;
                case "Healing+": damage *= (1.0f - (trait.getLevel() * 0.04f)); break;
                case "Healing++": damage *= (1.0f - (trait.getLevel() * 0.06f)); break;
                case "Terrific Healing":
                    damage *= (1.0f - (trait.getLevel() * 0.08f));
                    if (RANDOM.nextInt(10) == 0) player.heal(1.0f);
                    break;
                case "Natural Medicine":
                    if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
                    break;
                //</editor-fold>

                //<editor-fold desc="Defense & Protection Traits">
                case "Defense Charge": damage *= (1.0f - (trait.getLevel() * 0.03f)); break;
                case "Steel Protection": damage *= (1.0f - (trait.getLevel() * 0.05f)); break;
                case "Dragonscale Protection": damage *= (1.0f - (trait.getLevel() * 0.08f)); break;
                case "Indestructible Shield": damage *= (1.0f - (trait.getLevel() * 0.10f)); break;
                case "Quality": damage *= (1.0f - (trait.getLevel() * 0.01f)); break;
                case "Quality+": damage *= (1.0f - (trait.getLevel() * 0.02f)); break;
                case "Quality++": damage *= (1.0f - (trait.getLevel() * 0.03f)); break;
                case "High Quality": damage *= (1.0f - (trait.getLevel() * 0.025f)); break;
                case "Best Quality": damage *= (1.0f - (trait.getLevel() * 0.04f)); break;
                //</editor-fold>

                //<editor-fold desc="Elemental & Special Protections">
                case "Sponge": if (player.isInWater()) damage *= 0.7f; break;
                case "Icy Echo": if (event.getSource().is(DamageTypes.IN_FIRE)) damage *= 0.8f; break;
                case "Perpetual Ice S":
                    if (event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE))
                        damage *= 0.6f;
                    break;
                case "Secret Rainbow": if (event.getSource().is(DamageTypes.MAGIC)) damage *= 0.9f; break;
                case "Soft Texture": if (event.getSource().is(DamageTypes.FALL)) damage *= 0.5f; break;
                case "Mystic Life": if (event.getSource().is(DamageTypes.WITHER)) damage *= 0.7f; break;
                case "Resonant": if (player.getMainHandItem().isEnchanted()) damage *= 0.9f; break;
                case "Fantasy Spore": if (RANDOM.nextInt(20) == 0) damage = 0; break; // Dodge chance
                case "Infinite Energy": player.causeFoodExhaustion(-0.1f); break; // Restore hunger
                case "Glittering Darkness": if (player.level().isNight()) damage *= 0.8f; break;
                //</editor-fold>

                //<editor-fold desc="Legendary & Ultimate Traits">
                case "Primordial Power":
                    damage *= (1.0f - (trait.getLevel() * 0.06f));
                    if (RANDOM.nextInt(5) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    }
                    break;
                case "Glorious Soul":
                    damage *= (1.0f - (trait.getLevel() * 0.05f));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 0));
                    break;
                case "Divine Petal":
                    damage *= (1.0f - (trait.getLevel() * 0.04f));
                    if (RANDOM.nextInt(10) == 0) player.removeAllEffects();
                    break;
                case "Stats Power":
                    damage *= (1.0f - (trait.getLevel() * 0.03f));
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
                    break;
                case "War God's Power":
                    damage *= (1.0f - (trait.getLevel() * 0.02f));
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                        attacker.hurt(player.damageSources().thorns(player), trait.getLevel() * 1.5f);
                    }
                    break;
                case "Rarest": damage *= (1.0f - (trait.getLevel() * 0.08f)); break;
                case "Speed of Light":
                    damage *= (1.0f - (trait.getLevel() * 0.02f));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
                    break;
                //</editor-fold>

                //<editor-fold desc="Reactive Traits">
                case "Light Glow":
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    }
                    break;
                case "Smoldering Lunacy":
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                        attacker.setRemainingFireTicks(trait.getLevel() * 40);
                    }
                    break;
                case "Dissolving Heat S":
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                        attacker.setRemainingFireTicks(120);
                        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    }
                    break;
                case "Thunder Burn":
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                        attacker.hurt(player.damageSources().lightningBolt(), trait.getLevel() * 2.0f);
                    }
                    break;
                case "Thunder Current S":
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker.isInWaterOrRain()) {
                        attacker.hurt(player.damageSources().lightningBolt(), trait.getLevel() * 3.0f);
                    }
                    break;
                //</editor-fold>

                //<editor-fold desc="Negative Traits">
                case "Sticky Goo S": if (RANDOM.nextInt(10) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)); break;
                case "Sticky Goo M": if (RANDOM.nextInt(8) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); break;
                case "Power Throw": damage *= (1.0f + (trait.getLevel() * 0.05f)); break;
                case "Power Throw+": damage *= (1.0f + (trait.getLevel() * 0.08f)); break;
                case "Explosive":
                    if (RANDOM.nextInt(50) == 0) {
                        player.level().explode(player, player.getX(), player.getY(), player.getZ(),
                                1.0f, Level.ExplosionInteraction.NONE);
                    }
                    break;
                case "Rapid": damage *= (1.0f + (trait.getLevel() * 0.03f)); break;
                case "Rapid+": damage *= (1.0f + (trait.getLevel() * 0.05f)); break;
                case "Slowdown S": if (RANDOM.nextInt(15) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)); break;
                case "Slowdown M": if (RANDOM.nextInt(12) == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1)); break;
                case "Curse Strength": if (RANDOM.nextInt(20) == 0) player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 200, 0)); break;
                case "Curse Protection": if (RANDOM.nextInt(25) == 0) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); break;
                case "Assassin Poison S": if (RANDOM.nextInt(15) == 0) player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1)); break;
                case "Grievous Wound S": damage *= (1.0f + (trait.getLevel() * 0.1f)); break;
                case "Twilight Invitation S": if (RANDOM.nextInt(20) == 0) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)); break;
                case "Hazy Outline S": if (RANDOM.nextInt(25) == 0) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0)); break;
                case "Expensive": damage *= (1.0f + (trait.getLevel() * 0.02f)); break;
                case "Expensive+": damage *= (1.0f + (trait.getLevel() * 0.03f)); break;
                case "Expensive++": damage *= (1.0f + (trait.getLevel() * 0.04f)); break;
                //</editor-fold>

                //<editor-fold desc="Charge & Boost Traits">
                case "HP Charge": if (player.getHealth() < player.getMaxHealth() / 2) damage *= 0.9f; break;
                case "Attack Charge": break; // Handled in attack events
                case "Speed Charge": break; // Passive speed bonus
                case "Stats Charge+": break; // Passive stat bonus
                case "Skill Charge": break; // Skill enhancement bonus
                case "Skill Charge+": break; // Enhanced skill bonus
                //</editor-fold>

                //<editor-fold desc="Special & Utility Traits">
                case "Flowing Wisdom": if (player.isInWater()) damage *= 0.8f; break;
                case "Sharp Edge S": break; // Handled in attack events
                case "Rich Flavor": if (player.getFoodData().getFoodLevel() > 15) damage *= 0.9f; break;
                case "Area Bonus": break; // AoE enhancement
                case "Critical": break; // Handled in attack events
                case "Critical+": break; // Handled in attack events
                case "Critical++": break; // Handled in attack events
                case "Free Soul": if (RANDOM.nextInt(15) == 0) teleportPlayerRandomly(player, 8); break;
                case "Overflowing Courage":
                    if (player.getHealth() < player.getMaxHealth() / 4) {
                        damage *= 0.7f;
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
                    }
                    break;
                case "Healing Taste S": player.getFoodData().eat(1, 0.1f); break;
                case "Healing Taste M": player.getFoodData().eat(2, 0.2f); break;
                case "Clear Head S": break; // Mental clarity bonus
                case "Reverse Hour Hand":
                    if (RANDOM.nextInt(100) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                    }
                    break;
                case "Destructive": break; // Handled in attack events
                case "Destructive+": break; // Handled in attack events
                case "Destructive++": break; // Handled in attack events
                //</editor-fold>
            }
        }

        event.setAmount(Math.max(0, damage));
    }

    /**
     * Handles traits that affect outgoing attacks. ⚔️
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getTarget() instanceof LivingEntity target)) return;

        Map<String, MaterialQualitySystem.TraitData> activeTraits = getPlayerActiveTraits(player);
        Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);
        final float[] damageBonus = {0};

        // Handle trait-based attack bonuses
        for (MaterialQualitySystem.TraitData trait : activeTraits.values()) {
            switch (trait.getName()) {
                //<editor-fold desc="Attack Enhancement Traits">
                case "Attack Charge": damageBonus[0] += trait.getLevel() * 0.5f; break;
                case "Sharp Edge S": damageBonus[0] += trait.getLevel() * 0.8f; break;
                case "Smoldering Lunacy":
                    damageBonus[0] += trait.getLevel() * 0.6f;
                    target.setRemainingFireTicks(trait.getLevel() * 60);
                    break;
                case "Dissolving Heat S":
                    damageBonus[0] += trait.getLevel() * 0.7f;
                    target.setRemainingFireTicks(120);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    break;
                case "Destructive": damageBonus[0] += trait.getLevel() * 0.4f; break;
                case "Destructive+": damageBonus[0] += trait.getLevel() * 0.6f; break;
                case "Destructive++": damageBonus[0] += trait.getLevel() * 0.8f; break;
                case "War God's Power": damageBonus[0] += trait.getLevel() * 1.0f; break;
                case "Thunder Burn":
                    damageBonus[0] += trait.getLevel() * 0.5f;
                    if (target.isInWaterOrRain()) damageBonus[0] += trait.getLevel() * 0.5f;
                    break;
                case "Thunder Current S":
                    if (target.isInWaterOrRain()) damageBonus[0] += trait.getLevel() * 1.5f;
                    break;
                //</editor-fold>

                //<editor-fold desc="Critical Hit Traits">
                case "Critical":
                    if (RANDOM.nextInt(100) < (trait.getLevel() * 15)) {
                        damageBonus[0] += target.getMaxHealth() * 0.1f;
                    }
                    break;
                case "Critical+":
                    if (RANDOM.nextInt(100) < (trait.getLevel() * 20)) {
                        damageBonus[0] += target.getMaxHealth() * 0.15f;
                    }
                    break;
                case "Critical++":
                    if (RANDOM.nextInt(100) < (trait.getLevel() * 25)) {
                        damageBonus[0] += target.getMaxHealth() * 0.2f;
                    }
                    break;
                //</editor-fold>

                //<editor-fold desc="Special Attack Traits">
                case "Grievous Wound S":
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                    break;
                case "Assassin Poison S":
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, trait.getLevel() - 1));
                    break;
                case "Area Bonus":
                    if (trait.getLevel() >= 2) {
                        AABB area = target.getBoundingBox().inflate(3.0);
                        player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e != player)
                                .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damageBonus[0] * 0.5f));
                    }
                    break;
                case "Primordial Power":
                    damageBonus[0] += trait.getLevel() * 0.8f;
                    if (RANDOM.nextInt(10) == 0) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    }
                    break;
                //</editor-fold>
            }
        }

        // Handle effect-based attack bonuses
        for (MaterialQualitySystem.EffectData effect : activeEffects.values()) {
            switch (effect.getName()) {
                case "Physical Damage XS": damageBonus[0] += effect.getValue() * 0.5f; break;
                case "Physical Damage M": damageBonus[0] += effect.getValue() * 1.0f; break;
                case "Fire Damage L":
                    damageBonus[0] += effect.getValue() * 1.5f;
                    target.setRemainingFireTicks(100);
                    break;
                case "Fire Damage XL":
                    damageBonus[0] += effect.getValue() * 2.0f;
                    target.setRemainingFireTicks(160);
                    break;
                case "Ice Damage S":
                    damageBonus[0] += effect.getValue() * 0.8f;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                    break;
                case "Lightning Damage S":
                    damageBonus[0] += effect.getValue() * 1.2f;
                    if (target.isInWaterOrRain()) damageBonus[0] += effect.getValue() * 0.5f;
                    break;
                case "Poison Damage XS":
                    damageBonus[0] += effect.getValue() * 0.3f;
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    break;
                case "Dragon Slayer":
                    if (target.getType().toString().toLowerCase().contains("dragon")) {
                        damageBonus[0] += effect.getValue() * 10.0f;
                    }
                    break;
                case "Enhance Critical +20%":
                    if (RANDOM.nextInt(100) < 20) {
                        damageBonus[0] += target.getMaxHealth() * (effect.getValue() * 0.05f);
                    }
                    break;
            }
        }

        if (damageBonus[0] > 0) {
            target.hurt(player.damageSources().playerAttack(player), damageBonus[0]);
        }
    }

    /**
     * Handles traits that affect mining speed. ⛏️
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Map<String, MaterialQualitySystem.TraitData> activeTraits = getPlayerActiveTraits(player);
        Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);

        float speedMultiplier = 1.0f;

        // Trait-based mining bonuses
        for (MaterialQualitySystem.TraitData trait : activeTraits.values()) {
            switch (trait.getName()) {
                case "Sharp Edge S": speedMultiplier *= (1 + (trait.getLevel() * 0.1f)); break;
                case "Destructive": speedMultiplier *= (1 + (trait.getLevel() * 0.05f)); break;
                case "Destructive+": speedMultiplier *= (1 + (trait.getLevel() * 0.08f)); break;
                case "Destructive++": speedMultiplier *= (1 + (trait.getLevel() * 0.12f)); break;
                case "Thunder Burn": speedMultiplier *= (1 + (trait.getLevel() * 0.07f)); break;
            }
        }

        // Effect-based mining bonuses
        for (MaterialQualitySystem.EffectData effect : activeEffects.values()) {
            switch (effect.getName()) {
                case "Eye for Materials": speedMultiplier *= (1 + (effect.getValue() * 0.15f)); break;
                case "Enhance Items +5%": speedMultiplier *= 1.05f; break;
                case "Weaken Items +3%": speedMultiplier *= 0.97f; break;
            }
        }

        event.setNewSpeed(event.getNewSpeed() * speedMultiplier);
    }

    /**
     * Handles death prevention and recovery effects. 💀➡️💖
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);
            Map<String, MaterialQualitySystem.TraitData> activeTraits = getPlayerActiveTraits(player);

            boolean canRecover = false;
            float recoveryValue = 1.0f;

            // Check for recovery effects
            if (activeEffects.containsKey("KO Recovery S")) {
                canRecover = true;
                recoveryValue = activeEffects.get("KO Recovery S").getValue();
            } else if (activeEffects.containsKey("KO Recovery M")) {
                canRecover = true;
                recoveryValue = activeEffects.get("KO Recovery M").getValue() * 2.0f;
            } else if (activeEffects.containsKey("Resist KO +10%")) {
                if (RANDOM.nextInt(100) < 10) {
                    canRecover = true;
                    recoveryValue = 2.0f;
                }
            }

            // Check for trait-based recovery
            if (activeTraits.containsKey("Divine Petal") && RANDOM.nextInt(100) < (activeTraits.get("Divine Petal").getLevel() * 20)) {
                canRecover = true;
                recoveryValue = activeTraits.get("Divine Petal").getLevel();
            }

            if (canRecover) {
                event.setCanceled(true);
                player.setHealth(Math.min(player.getMaxHealth() * 0.5f, recoveryValue * 4.0f));
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
            }
        }
    }

    /**
     * Handles XP modification effects. 🧠✨
     */
    @SubscribeEvent
    public static void onXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        Map<String, MaterialQualitySystem.EffectData> activeEffects = getPlayerActiveEffects(player);
        Map<String, MaterialQualitySystem.TraitData> activeTraits = getPlayerActiveTraits(player);

        float multiplier = 1.0f;

        // Effect-based XP bonuses
        if (activeEffects.containsKey("XP Gain")) {
            multiplier += (activeEffects.get("XP Gain").getValue() * 0.2f);
        }

        // Trait-based XP bonuses
        for (MaterialQualitySystem.TraitData trait : activeTraits.values()) {
            switch (trait.getName()) {
                case "Flowing Wisdom": multiplier += (trait.getLevel() * 0.1f); break;
                case "Clear Head S": multiplier += (trait.getLevel() * 0.05f); break;
                case "Skill Charge": multiplier += (trait.getLevel() * 0.08f); break;
                case "Skill Charge+": multiplier += (trait.getLevel() * 0.12f); break;
                case "Best Quality": multiplier += (trait.getLevel() * 0.06f); break;
                case "Glorious Soul": multiplier += (trait.getLevel() * 0.04f); break;
            }
        }

        if (multiplier > 1.0f) {
            event.getOrb().value = (int)(event.getOrb().value * multiplier);
        }
    }


    // --- HELPER METHODS ---
    private static Map<String, MaterialQualitySystem.EffectData> getPlayerActiveEffects(Player player) {
        Map<String, MaterialQualitySystem.EffectData> effectsMap = new HashMap<>();
        if (player.level().isClientSide()) return effectsMap;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) {
                OrbmentComponent component = OrbmentItem.loadComponentClientSide(stack, player.level());
                if (component == null) continue;

                for (int slot = 0; slot < OrbmentComponent.MAX_SLOTS; slot++) {
                    if (!component.isSlotUnlocked(slot)) continue;
                    ItemStack quartzStack = component.getInventory().getStackInSlot(slot);
                    if (quartzStack.isEmpty()) continue;

                    // Get effects from the ItemStack itself (synthesis results)
                    List<ItemEnhancementSystem.ItemEffect> stackEffects = ItemEnhancementSystem.Effects.getAllEffects(quartzStack);
                    for (ItemEnhancementSystem.ItemEffect effect : stackEffects) {
                        MaterialQualitySystem.EffectData effectData = new MaterialQualitySystem.EffectData(
                                effect.name(), effect.value(), effect.duration(),
                                getEffectType(effect.name())
                        );
                        effectsMap.merge(effectData.getName(), effectData,
                                (oldEffect, newEffect) -> newEffect.getValue() > oldEffect.getValue() ? newEffect : oldEffect);
                    }

                    // Also get base material effects
                    Map<String, MaterialQualitySystem.EffectData> materialEffects = MaterialQualitySystem.getMaterialEffects(quartzStack.getItem());
                    for (MaterialQualitySystem.EffectData newEffect : materialEffects.values()) {
                        effectsMap.merge(newEffect.getName(), newEffect,
                                (oldEffect, nEffect) -> nEffect.getValue() > oldEffect.getValue() ? nEffect : oldEffect);
                    }
                }
            }
        }
        return effectsMap;
    }

    private static Map<String, MaterialQualitySystem.TraitData> getPlayerActiveTraits(Player player) {
        Map<String, MaterialQualitySystem.TraitData> traitsMap = new HashMap<>();
        if (player.level().isClientSide()) return traitsMap;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) {
                OrbmentComponent component = OrbmentItem.loadComponentClientSide(stack, player.level());
                if (component == null) continue;

                for (int slot = 0; slot < OrbmentComponent.MAX_SLOTS; slot++) {
                    if (!component.isSlotUnlocked(slot)) continue;
                    ItemStack quartzStack = component.getInventory().getStackInSlot(slot);
                    if (quartzStack.isEmpty()) continue;

                    // Get traits from the ItemStack itself (synthesis results)
                    Map<String, Integer> stackTraits = ItemEnhancementSystem.Traits.getAllTraits(quartzStack);
                    for (Map.Entry<String, Integer> trait : stackTraits.entrySet()) {
                        MaterialQualitySystem.TraitData traitData = new MaterialQualitySystem.TraitData(
                                trait.getKey(), trait.getValue(),
                                getTraitType(trait.getKey())
                        );
                        traitsMap.merge(traitData.getName(), traitData,
                                (oldTrait, newTrait) -> newTrait.getLevel() > oldTrait.getLevel() ? newTrait : oldTrait);
                    }

                    // Also get base material traits
                    Map<String, MaterialQualitySystem.TraitData> materialTraits = MaterialQualitySystem.getMaterialTraits(quartzStack.getItem());
                    for (MaterialQualitySystem.TraitData newTrait : materialTraits.values()) {
                        traitsMap.merge(newTrait.getName(), newTrait,
                                (oldTrait, nTrait) -> nTrait.getLevel() > oldTrait.getLevel() ? nTrait : oldTrait);
                    }
                }
            }
        }
        return traitsMap;
    }
    private static MaterialQualitySystem.EffectType getEffectType(String effectName) {
        return switch (effectName.toLowerCase()) {
            case "hp gain m", "hp gain s", "hp gain l", "hp gain xl", "hp gain xs", "water breathing",
                 "night vision", "fire resistance", "atk up m", "atk up s", "def up s", "guardian mirror s",
                 "levitation", "slow falling", "enhance critical +20%", "money magnet", "magic veil",
                 "enhance skills +10%", "enhance skills +3%", "enhance skills +7%", "healing taste s",
                 "healing taste m", "healing taste l", "spd up s", "spd up m", "poison cure", "remove debuffs",
                 "remove ailments", "hp regen l", "hp regen s", "hp regen m", "ko recovery s", "ko recovery m",
                 "wind rider", "lightning damage s", "eye for materials", "dragon slayer", "all stats up l",
                 "defense veil", "feeling full s", "xp gain", "treasure hunter", "resist ko +10%",
                 "explosive", "mild sweetness", "sweetness", "critical rate up s", "reduce damage -10%",
                 "reduce damage -3%", "fire resist up+", "fire resist up", "weaken items +3%",
                 "enhance items +5%", "evasion up s", "light blessing s", "thunderclap s", "energy surge l",
                 "fire damage l", "fire damage xl", "physical damage m", "physical damage xs",
                 "ice damage s" -> MaterialQualitySystem.EffectType.POSITIVE;

            case "inflict burn m", "inflict burn s", "inflict burn l", "self harm", "spd down s",
                 "atk down s", "inflict frostbite s", "inflict frostbite m", "inflict poison m",
                 "inflict poison s", "inflict poison l", "inflict thorn s", "inflict curse s",
                 "inflict curse m", "inflict curse l", "surprise! s", "inflict slow s", "poison damage xs",
                 "all stats down s", "fire vulnerability" -> MaterialQualitySystem.EffectType.NEGATIVE;

            case "random effect", "random teleport" -> MaterialQualitySystem.EffectType.NEUTRAL;

            default -> MaterialQualitySystem.EffectType.NEUTRAL;
        };
    }

    private static MaterialQualitySystem.TraitType getTraitType(String traitName) {
        return switch (traitName.toLowerCase()) {
            case "healing", "healing+", "healing++", "natural medicine", "light glow",
                 "smoldering lunacy", "dissolving heat s", "destructive", "destructive+", "destructive++",
                 "defense charge", "steel protection", "speed charge", "free soul", "best quality",
                 "expensive+", "skill charge", "skill charge+", "mild sweetness", "overflowing courage",
                 "terrific healing", "high quality", "quality+", "quality++", "sharp edge s",
                 "flowing wisdom", "healing taste s", "healing taste m", "rich flavor", "area bonus",
                 "critical", "critical+", "critical++", "attack charge", "hp charge", "stats charge+",
                 "stats power", "war god's power", "clear head s", "divine petal", "thunder current s",
                 "primordial power", "dragonscale protection", "indestructible shield", "speed of light",
                 "rarest", "reverse hour hand", "thunder burn", "glorious soul" -> MaterialQualitySystem.TraitType.POSITIVE;

            case "sticky goo s", "sticky goo m", "power throw", "power throw+", "explosive", "rapid",
                 "rapid+", "slowdown s", "slowdown m", "curse strength", "curse protection",
                 "assassin poison s", "grievous wound s", "twilight invitation s", "hazy outline s",
                 "expensive",  "expensive++" -> MaterialQualitySystem.TraitType.NEGATIVE;

            case "sponge", "quality", "icy echo", "perpetual ice s", "secret rainbow", "soft texture",
                 "mystic life", "resonant", "fantasy spore", "infinite energy", "glittering darkness" -> MaterialQualitySystem.TraitType.NEUTRAL;

            default -> MaterialQualitySystem.TraitType.NEUTRAL;
        };
    }

    private static void teleportPlayerRandomly(Player player, double maxDistance) {
        if (player.level() instanceof ServerLevel) {
            for (int i = 0; i < 16; ++i) {
                BlockPos targetPos = player.blockPosition().offset(RANDOM.nextInt(16) - 8, RANDOM.nextInt(8) - 4, RANDOM.nextInt(16) - 8);
                if (player.level().getBlockState(targetPos).isAir() && player.level().getBlockState(targetPos.above()).isAir()) {
                    player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
                    return;
                }
            }
        }
    }
}