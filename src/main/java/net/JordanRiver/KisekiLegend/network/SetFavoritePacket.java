package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SetFavoritePacket {
    private final int slot;
    private final String artName;

    public SetFavoritePacket(int slot, String artName) {
        this.slot = slot;
        this.artName = artName;
    }

    public static void encode(SetFavoritePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.slot);
        buffer.writeUtf(packet.artName);
    }

    public static SetFavoritePacket decode(FriendlyByteBuf buffer) {
        return new SetFavoritePacket(buffer.readInt(), buffer.readUtf());
    }

    public static void handle(SetFavoritePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Find orbment on SERVER side
                ItemStack orbmentStack = findOrbment(player);
                if (!orbmentStack.isEmpty() && orbmentStack.getItem() instanceof OrbmentItem) {
                    try {
                        // Load component on SERVER
                        OrbmentComponent component = OrbmentItem.loadComponent(orbmentStack, player.level(), player);

                        // Set favorite on SERVER
                        component.setFavorite(packet.slot, packet.artName);

                        // Save on SERVER
                        OrbmentItem.saveComponent(orbmentStack, component, player.level(), player);

                        // Mark inventory as changed on SERVER
                        player.getInventory().setChanged();

                        KisekiLegend.LOGGER.info("SERVER: Set favorite {} to {}", packet.slot, packet.artName);

                    } catch (Exception e) {
                        KisekiLegend.LOGGER.error("Failed to set favorite on server: ", e);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static ItemStack findOrbment(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof OrbmentItem) return mainHand;

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof OrbmentItem) return offHand;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof OrbmentItem) return stack;
        }
        return ItemStack.EMPTY;
    }
}