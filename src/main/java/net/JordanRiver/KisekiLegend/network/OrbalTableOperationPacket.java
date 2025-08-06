package net.JordanRiver.KisekiLegend.network;

import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import javax.annotation.Nullable;

public class OrbalTableOperationPacket {
    private final BlockPos blockPos;
    private final OperationType operationType;
    private final String elementType;
    private final int slotIndex;
    private final float[] slotPosition;

    public enum OperationType {
        ADD_SLOT, REMOVE_SLOT, CHANGE_ELEMENT, CLOSE_SLOT
    }

    public OrbalTableOperationPacket(BlockPos pos, OperationType type, String elementType, int slotIndex, @Nullable float[] position) {
        this.blockPos = pos;
        this.operationType = type;
        this.elementType = elementType;
        this.slotIndex = slotIndex;
        this.slotPosition = position;
    }

    public static void encode(OrbalTableOperationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeEnum(packet.operationType);
        buffer.writeUtf(packet.elementType);
        buffer.writeInt(packet.slotIndex);

        if (packet.slotPosition != null) {
            buffer.writeBoolean(true);
            for (float f : packet.slotPosition) {
                buffer.writeFloat(f);
            }
        } else {
            buffer.writeBoolean(false);
        }
    }

    public static OrbalTableOperationPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        OperationType type = buffer.readEnum(OperationType.class);
        String elementType = buffer.readUtf();
        int slotIndex = buffer.readInt();

        float[] position = null;
        if (buffer.readBoolean()) {
            position = new float[3];
            for (int i = 0; i < 3; i++) {
                position[i] = buffer.readFloat();
            }
        }

        return new OrbalTableOperationPacket(pos, type, elementType, slotIndex, position);
    }

    public static void handle(OrbalTableOperationPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                if (level.getBlockEntity(packet.blockPos) instanceof OrbalTableBlockEntity blockEntity) {


                    // Check if there's a weapon first
                    if (blockEntity.getWeaponItem().isEmpty()) {
                        System.out.println("No weapon in table!");
                        return;
                    }

                    OrbalTableBlockEntity.WeaponSlotOperation.Type operationType = switch (packet.operationType) {
                        case ADD_SLOT -> OrbalTableBlockEntity.WeaponSlotOperation.Type.ADD_SLOT;
                        case REMOVE_SLOT -> OrbalTableBlockEntity.WeaponSlotOperation.Type.REMOVE_SLOT;
                        case CHANGE_ELEMENT -> OrbalTableBlockEntity.WeaponSlotOperation.Type.CHANGE_ELEMENT;
                        case CLOSE_SLOT -> OrbalTableBlockEntity.WeaponSlotOperation.Type.CLOSE_SLOT;
                    };

                    OrbalTableBlockEntity.WeaponSlotOperation operation = new OrbalTableBlockEntity.WeaponSlotOperation(
                            operationType, packet.elementType, packet.slotIndex, packet.slotPosition
                    );

                    System.out.println("Starting slot operation...");
                    blockEntity.startSlotOperation(operation);
                    System.out.println("Operation started successfully");
                }
            }
        });
        context.setPacketHandled(true);
    }
}