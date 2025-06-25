package net.JordanRiver.KisekiLegend.orbal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class StatBonus {
    public int hp = 0;
    public int ep = 0;
    public int str = 0;
    public int def = 0;
    public int ats = 0;
    public int adf = 0;
    public int spd = 0;
    public int mov = 0;
    public int dex = 0;
    public int agl = 0;

    public boolean invisibility = false;
    public boolean regen = false;

    public int epCut = 0; // % EP reduction for Arts
    public int castSpeed = 0; // % cast speed boost

    // === Serialization ===

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("hp", hp);
        tag.putInt("ep", ep);
        tag.putInt("str", str);
        tag.putInt("def", def);
        tag.putInt("ats", ats);
        tag.putInt("adf", adf);
        tag.putInt("spd", spd);
        tag.putInt("mov", mov);
        tag.putInt("dex", dex);
        tag.putInt("agl", agl);
        tag.putBoolean("invisibility", invisibility);
        tag.putBoolean("regen", regen);
        tag.putInt("epCut", epCut);
        tag.putInt("castSpeed", castSpeed);
        return tag;
    }

    public static StatBonus fromTag(CompoundTag tag) {
        StatBonus bonus = new StatBonus();
        bonus.hp = tag.getInt("hp");
        bonus.ep = tag.getInt("ep");
        bonus.str = tag.getInt("str");
        bonus.def = tag.getInt("def");
        bonus.ats = tag.getInt("ats");
        bonus.adf = tag.getInt("adf");
        bonus.spd = tag.getInt("spd");
        bonus.mov = tag.getInt("mov");
        bonus.dex = tag.getInt("dex");
        bonus.agl = tag.getInt("agl");
        bonus.invisibility = tag.getBoolean("invisibility");
        bonus.regen = tag.getBoolean("regen");
        bonus.epCut = tag.getInt("epCut");
        bonus.castSpeed = tag.getInt("castSpeed");
        return bonus;
    }

    public void combine(StatBonus other) {
        this.hp += other.hp;
        this.ep += other.ep;
        this.str += other.str;
        this.def += other.def;
        this.ats += other.ats;
        this.adf += other.adf;
        this.spd += other.spd;
        this.mov += other.mov;
        this.dex += other.dex;
        this.agl += other.agl;
        this.invisibility |= other.invisibility;
        this.regen |= other.regen;
        this.epCut += other.epCut;
        this.castSpeed += other.castSpeed;
    }

    public boolean isEmpty() {
        return hp == 0 && ep == 0 && str == 0 && def == 0 && ats == 0 && adf == 0 &&
                spd == 0 && mov == 0 && dex == 0 && agl == 0 && !invisibility && !regen;
    }
}


