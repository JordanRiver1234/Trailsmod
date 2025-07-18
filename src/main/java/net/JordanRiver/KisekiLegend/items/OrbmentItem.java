package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.client.renderer.item.OrbmentItemRenderer;
import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class OrbmentItem extends Item implements GeoItem {
    private static final RawAnimation CAST_ANIM = RawAnimation.begin().then("cast", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().then("idle", Animation.LoopType.LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OrbmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // This method will now ONLY handle the non-shift right-click to open the menu.
        // The shift-right-click for casting is handled entirely by ArtInputHandler.
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                        (windowId, inv, plyr) -> new OrbmentMenu(windowId, inv),
                        Component.literal("Orbment")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        // Pass for shift-clicks, allowing ArtInputHandler to take over without conflict.
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cast_controller", 0, state -> {
            var controller = state.getController();

            // Debug logging
            System.out.println("Animation Controller State: " + (controller.getCurrentAnimation() != null ? controller.getCurrentAnimation().animation().name() : "null"));

            // If we have a running animation that isn't finished, continue it
            if (controller.getCurrentAnimation() != null && !controller.hasAnimationFinished()) {
                System.out.println("Animation running: " + controller.getCurrentAnimation().animation().name());
                return PlayState.CONTINUE;
            }

            // Only set idle if no animation is running or the current animation has finished
            // This prevents idle from overriding triggered animations
            System.out.println("Setting idle animation");
            return state.setAndContinue(IDLE_ANIM);
        }).triggerableAnim("cast", CAST_ANIM)
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Saves the entire OrbmentComponent's data to the ItemStack's NBT.
     * @param stack The ItemStack to save to.
     * @param component The OrbmentComponent data to save.
     * @param level The level, used to access registries.
     */
    public static void saveComponent(ItemStack stack, OrbmentComponent component, Level level) {
        HolderLookup.Provider provider = level.registryAccess();
        CompoundTag componentTag = component.serializeNBT(provider);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(componentTag));
    }

    /**
     * Loads an OrbmentComponent from an ItemStack.
     * If the item is new (no line data), it initializes random sepith lines.
     * @param stack The ItemStack to load from.
     * @param level The level, used to access registries.
     * @return A fully loaded and initialized OrbmentComponent.
     */
    public static OrbmentComponent loadComponent(ItemStack stack, Level level) {
        OrbmentComponent component = new OrbmentComponent();
        HolderLookup.Provider provider = level.registryAccess();

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();
            component.deserializeNBT(provider, tag);
        }

        // If lines have never been initialized, do it now and save back to the item.
        if (!component.areLinesInitialized()) {
            component.initializeLines();
            saveComponent(stack, component, level);
        }

        return component;
    }
}