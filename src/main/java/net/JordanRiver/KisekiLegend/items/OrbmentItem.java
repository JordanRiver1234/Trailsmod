package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderer.item.OrbmentItemRenderer;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class OrbmentItem extends Item implements GeoItem {
    private static final RawAnimation CAST_ANIM = RawAnimation.begin().then("cast", Animation.LoopType.LOOP);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().then("idle", Animation.LoopType.LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OrbmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        HitResult target = player.pick(5.0D, 0.0F, false);
        ItemStack stack = player.getItemInHand(hand);

        if (target.getType() == HitResult.Type.MISS) {  // Air click
            if (player.isShiftKeyDown()) {  // Shift-right-click: Trigger spell casting animation
                if (level instanceof ServerLevel serverLevel) {
                    // Trigger animation server-side (syncs to client)
                    triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "cast_controller", "cast");
                }
                // Add your spell-casting logic here (e.g., schedule cast)
                return InteractionResultHolder.success(stack);
            } else {  // Non-shift air click: Open menu
                if (!level.isClientSide()) {
                    player.openMenu(new SimpleMenuProvider(
                            (windowId, inv, plyr) -> new OrbmentMenu(windowId, inv),
                            Component.literal("Orbment")
                    ));
                }
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);  // If clicking block, do nothing or handle separately
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cast_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("cast", CAST_ANIM)
                .triggerableAnim("idle", IDLE_ANIM));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private OrbmentItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new OrbmentItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        // Don't automatically set custom model data here - let the handler manage it
        super.onCraftedBy(stack, level, player);
    }

    // Create stack with 3D model for hand rendering
    public static ItemStack get3DStack() {
        ItemStack stack = new ItemStack(ModItems.ORBMENT_ITEM.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return stack;
    }

    // Create stack with 2D model for GUI/hotbar
    public static ItemStack get2DStack() {
        ItemStack stack = new ItemStack(ModItems.ORBMENT_ITEM.get());
        // Don't set custom model data - uses default 2D model
        return stack;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Write only the inventory & slot count (no EP) back to the stack.
     * (Kept for backward compatibility if you ever need it.)
     */
    public static void saveInventory(ItemStack stack, SizedItemStackHandler handler, int unlockedSlots, Level level) {
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = existing.copyTag();
        tag.put("orbment_inventory", handler.serializeNBT(level.registryAccess()));
        tag.putInt("orbment_unlocked", unlockedSlots);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Write the entire OrbmentComponent (slots, inventory, AND currentEP) back into the stack.
     */
    public static void saveComponent(ItemStack stack, OrbmentComponent component, Level level) {
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = existing.copyTag();

        // 1) unlocked slots & inventory:
        tag.put("orbment_inventory", component.getInventory().serializeNBT(level.registryAccess()));
        tag.putInt("orbment_unlocked", component.getUnlockedSlots());

        // 2) current EP:
        tag.putInt("CurrentEP", component.getCurrentEP());

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static OrbmentComponent loadComponent(ItemStack stack, Level level) {
        OrbmentComponent component = new OrbmentComponent();

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();

            // unlocked slots
            if (tag.contains("orbment_unlocked", Tag.TAG_INT)) {
                component.setUnlockedSlots(tag.getInt("orbment_unlocked"));
            }

            // inventory
            if (tag.contains("orbment_inventory", Tag.TAG_COMPOUND)) {
                SizedItemStackHandler handler = new SizedItemStackHandler(OrbmentMenu.ORBMENT_SLOT_COUNT);
                handler.deserializeNBT(level.registryAccess(), tag.getCompound("orbment_inventory"));
                component.setInventory(handler);
            }

            // restore EP if present
            if (tag.contains("CurrentEP", Tag.TAG_INT)) {
                component.setCurrentEP(tag.getInt("CurrentEP"));
            }
        }

        return component;
    }
}