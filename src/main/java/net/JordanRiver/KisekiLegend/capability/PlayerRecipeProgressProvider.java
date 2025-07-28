package net.JordanRiver.KisekiLegend.capability;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRecipeProgressProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerRecipeProgressCapability> PLAYER_RECIPE_PROGRESS =
            CapabilityManager.get(new CapabilityToken<PlayerRecipeProgressCapability>() {
            });

    private PlayerRecipeProgressCapability progress = null;
    private final LazyOptional<PlayerRecipeProgressCapability> optional = LazyOptional.of(this::getOrCreateProgress);

    private PlayerRecipeProgressCapability getOrCreateProgress() {
        if (progress == null) {
            progress = new PlayerRecipeProgressCapability();
        }
        return progress;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_RECIPE_PROGRESS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        return getOrCreateProgress().serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        getOrCreateProgress().deserializeNBT(provider, nbt);
    }
}