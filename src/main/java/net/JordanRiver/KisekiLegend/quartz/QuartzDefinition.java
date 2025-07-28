package net.JordanRiver.KisekiLegend.quartz;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class QuartzDefinition {
    private final BiConsumer<LivingEntity, Player> onHit;

    public QuartzDefinition(BiConsumer<LivingEntity, Player> onHit) {
        this.onHit = onHit;
    }

    // Constructor for quartz with no special effects
    public QuartzDefinition() {
        this(null);
    }

    /** Apply on-hit proc only */
    public void applyOnHit(LivingEntity target, Player attacker) {
        if (onHit != null) {
            onHit.accept(target, attacker);
        }
    }

    // Sepith values are handled by QuartzItem in ModItems
    // No need to duplicate them here
}