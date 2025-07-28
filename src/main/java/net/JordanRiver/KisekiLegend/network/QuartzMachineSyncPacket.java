package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuartzMachineSyncPacket {
    private final BlockPos blockPos;
    private final ResourceLocation activeRecipeId;
    private final Set<String> unlockedNodes;
    private final CompoundTag storedItems;
    private final Set<String> completedNodes;
    private final boolean isSynthesizing;
    private final long synthesisStartTime;
    private final List<ItemStack> floatingItems;

    public QuartzMachineSyncPacket(BlockPos pos, @Nullable ResourceLocation recipeId, Set<String> unlockedNodes, CompoundTag storedItems, Set<String> completedNodes, boolean isSynthesizing, long synthesisStartTime, List<ItemStack> floatingItems) {
        this.blockPos = pos;
        this.activeRecipeId = recipeId;
        this.unlockedNodes = unlockedNodes;
        this.storedItems = storedItems;
        this.completedNodes = completedNodes;
        this.isSynthesizing = isSynthesizing;
        this.synthesisStartTime = synthesisStartTime;
        this.floatingItems = floatingItems;
    }

    public static void encode(QuartzMachineSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeBoolean(packet.activeRecipeId != null);
        if (packet.activeRecipeId != null) {
            buffer.writeResourceLocation(packet.activeRecipeId);
        }
        buffer.writeCollection(packet.unlockedNodes, FriendlyByteBuf::writeUtf);
        buffer.writeNbt(packet.storedItems);
        buffer.writeCollection(packet.completedNodes, FriendlyByteBuf::writeUtf);
        buffer.writeBoolean(packet.isSynthesizing);
        buffer.writeLong(packet.synthesisStartTime);

        buffer.writeInt(packet.floatingItems.size());
        for (ItemStack item : packet.floatingItems) {
            if (item.isEmpty()) {
                buffer.writeBoolean(false);
            } else {
                buffer.writeBoolean(true);
                buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item.getItem()));
                buffer.writeInt(item.getCount());
            }
        }
    }

    public static QuartzMachineSyncPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        Set<String> unlockedNodes = buffer.readCollection(HashSet::new, FriendlyByteBuf::readUtf);
        CompoundTag storedItems = buffer.readNbt();
        Set<String> completedNodes = buffer.readCollection(HashSet::new, FriendlyByteBuf::readUtf);
        boolean isSynthesizing = buffer.readBoolean();
        long synthesisStartTime = buffer.readLong();

        int itemCount = buffer.readInt();
        List<ItemStack> floatingItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            if (buffer.readBoolean()) {
                ResourceLocation itemId = buffer.readResourceLocation();
                int count = buffer.readInt();
                Item itemType = BuiltInRegistries.ITEM.get(itemId);
                floatingItems.add(new ItemStack(itemType, count));
            } else {
                floatingItems.add(ItemStack.EMPTY);
            }
        }

        return new QuartzMachineSyncPacket(pos, recipeId, unlockedNodes, storedItems, completedNodes, isSynthesizing, synthesisStartTime, floatingItems);
    }

    public static void handle(QuartzMachineSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(packet.blockPos) instanceof QuartzMachineBlockEntity blockEntity) {
                blockEntity.updateStateFromServer(packet.activeRecipeId, packet.unlockedNodes, packet.storedItems, packet.completedNodes);
                blockEntity.setFloatingItemsFromServer(packet.floatingItems, packet.isSynthesizing, packet.synthesisStartTime);
            }
        });
        context.setPacketHandled(true);
    }
}