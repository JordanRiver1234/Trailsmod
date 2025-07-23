package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SetSelectedArtPacket {
    private final String artName;

    public SetSelectedArtPacket(String artName) {
        this.artName = artName;
    }

    public static void encode(SetSelectedArtPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.artName);
    }

    public static SetSelectedArtPacket decode(FriendlyByteBuf buffer) {
        return new SetSelectedArtPacket(buffer.readUtf());
    }

    public static void handle(SetSelectedArtPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack orbmentStack = findOrbment(player);
                if (!orbmentStack.isEmpty() && orbmentStack.getItem() instanceof OrbmentItem) {
                    try {
                        OrbmentComponent component = OrbmentItem.loadComponent(orbmentStack, player.level(), (ServerPlayer) player);
                        component.setLastSelectedArtName(packet.artName);
                        OrbmentItem.saveComponent(orbmentStack, component, player.level(), (ServerPlayer) player);
                        player.getInventory().setChanged();

                        KisekiLegend.LOGGER.info("SERVER: Set selected art to {}", packet.artName);

                    } catch (Exception e) {
                        KisekiLegend.LOGGER.error("Failed to set selected art on server: ", e);
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