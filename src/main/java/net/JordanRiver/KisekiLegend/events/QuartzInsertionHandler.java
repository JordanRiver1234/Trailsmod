package net.JordanRiver.KisekiLegend.events;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber
public class QuartzInsertionHandler {


    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (offHand.getItem() instanceof QuartzItem && isWeaponOrTool(mainHand)) {
            System.out.println("=== RIGHT-CLICK QUARTZ INSERTION ===");
            System.out.println("Weapon: " + mainHand.getDisplayName().getString());
            System.out.println("Quartz: " + offHand.getDisplayName().getString());

            WeaponSlotData slotData = WeaponSlotData.getOrCreate(mainHand);
            System.out.println("Weapon has " + slotData.getActiveSlotCount() + " active slots");

            if (slotData.canInsertQuartz(offHand)) {
                if (!player.level().isClientSide()) {
                    System.out.println("Attempting to insert quartz...");

                    // Find the first available slot
                    int targetSlotIndex = -1;
                    for (int i = 0; i < slotData.getSlots().size(); i++) {
                        WeaponSlotData.WeaponSlot slot = slotData.getSlot(i);
                        if (slot != null && !slot.isClosed && slot.canInsertQuartz(offHand)) {
                            targetSlotIndex = i;
                            break;
                        }
                    }

                    if (targetSlotIndex >= 0) {
                        System.out.println("Inserting into slot " + targetSlotIndex);

                        if (slotData.getSlot(targetSlotIndex).insertQuartz(offHand.copy())) {
                            // Apply quartz effects
                            if (offHand.getItem() instanceof QuartzItem quartzItem) {
                                System.out.println("Applying quartz effects to weapon...");
                                quartzItem.onInsertedIntoWeapon(mainHand, offHand);

                                // DEBUG: Check what's actually on the weapon now
                                QuartzItem.debugWeaponData(mainHand);
                            }

                            // Save weapon data with updated slot information
                            WeaponSlotData.save(mainHand, slotData);

                            // Consume the quartz
                            offHand.shrink(1);

                            player.sendSystemMessage(Component.literal("Quartz inserted successfully into slot " + (targetSlotIndex + 1) + "!"));

                            // Play sound effect
                            player.level().playSound(null, player.blockPosition(),
                                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);

                            System.out.println("Quartz insertion completed successfully!");
                            event.setCanceled(true);
                        } else {
                            System.out.println("Failed to insert quartz into slot");
                        }
                    } else {
                        System.out.println("No available slot found for quartz");
                        player.sendSystemMessage(Component.literal("No compatible slots available for this quartz!"));
                    }
                }
            } else {
                if (!player.level().isClientSide()) {
                    System.out.println("Cannot insert quartz - no compatible slots");
                    player.sendSystemMessage(Component.literal("This weapon has no compatible slots for this quartz type!"));
                }
            }

            event.setCanceled(true); // Always cancel to prevent other interactions
        }
    }
    private static void applyBuffsToWeapon(ItemStack weapon, Map<String, Float> buffs) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // Keep existing modifiers
        ItemAttributeModifiers existing = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (existing != null) {
            for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // Add new buffs
        for (Map.Entry<String, Float> buff : buffs.entrySet()) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(
                    ResourceLocation.fromNamespaceAndPath("minecraft", buff.getKey()));

            if (attribute != null) {
                AttributeModifier modifier = new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "quartz_" + buff.getKey()),
                        buff.getValue(),
                        AttributeModifier.Operation.ADD_VALUE
                );
                builder.add(Holder.direct(attribute), modifier, EquipmentSlotGroup.MAINHAND);
            }
        }

        weapon.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
    private static boolean isWeaponOrTool(ItemStack stack) {
        return stack.isDamageableItem() ||
                stack.getItem() instanceof net.minecraft.world.item.TieredItem ||
                stack.getItem() instanceof net.minecraft.world.item.SwordItem ||
                stack.getItem() instanceof net.minecraft.world.item.AxeItem ||
                stack.getItem() instanceof net.minecraft.world.item.PickaxeItem ||
                stack.getItem() instanceof net.minecraft.world.item.ShovelItem ||
                stack.getItem() instanceof net.minecraft.world.item.HoeItem ||
                stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                stack.getItem() instanceof net.minecraft.world.item.TridentItem;
    }
}