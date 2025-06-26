package net.JordanRiver.KisekiLegend.quartz;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;

import java.util.function.BiConsumer;

public class QuartzDefinition {
    private final Holder<MobEffect> selfBuff;
    private final int selfAmp, selfDur;
    private final BiConsumer<LivingEntity, Player> onHit;

    public QuartzDefinition(Holder<MobEffect> selfBuff,
                            int selfAmp,
                            int selfDur,
                            BiConsumer<LivingEntity, Player> onHit) {
        this.selfBuff = selfBuff;
        this.selfAmp = selfAmp;
        this.selfDur = selfDur;
        this.onHit = onHit;
    }

    /** Apply the self-buff each tick */
    public void applySelfBuff(Player wearer) {
        if (selfBuff != null) {
            wearer.addEffect(new MobEffectInstance(
                    selfBuff,
                    selfDur,
                    selfAmp,
                    /* ambient */ false,
                    /* visible */ false,
                    /* showIcon */ true
            ));
        }
    }

    /** Apply on-hit proc */
    public void applyOnHit(LivingEntity target, Player attacker) {
        if (onHit != null) {
            onHit.accept(target, attacker);
        }
    }
    /** Returns the raw MobEffect (or null) that this quartz grants to its wearer. */
    public MobEffect getSelfEffectType() {
        return selfBuff == null ? null : selfBuff.value();
    }
    // at bottom of QuartzDefinition:
    /** Returns the underlying effect holder (or null). */
    public Holder<MobEffect> getSelfBuffHolder() {
        return selfBuff;
    }


}