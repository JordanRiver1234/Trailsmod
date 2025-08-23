package net.JordanRiver.KisekiLegend.entities.fish;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class GarvelzeEntity extends BaseFishEntity {
    public GarvelzeEntity (EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public String getFishType() {
        return "garvelze";
    }
}