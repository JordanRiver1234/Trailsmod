package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.client.renderer.item.OrbmentItemRenderer;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.OrbmentSyncPacket;
import net.JordanRiver.KisekiLegend.orbal.Element;
import net.JordanRiver.KisekiLegend.orbal.OrbmentComponent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static net.JordanRiver.KisekiLegend.orbal.OrbmentComponent.MAX_SLOTS;

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

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                // FORCE initialization here before opening menu
                OrbmentComponent component = loadComponent(stack, level, (ServerPlayer) player);

                // Send sync packet to client immediately after initialization
                if (component.areLinesInitialized()) {
                    HolderLookup.Provider provider = level.registryAccess();
                    CompoundTag componentTag = component.serializeNBT(provider);
                    int slot = findOrbmentSlot((ServerPlayer) player, stack);
                    NetworkHandler.sendToPlayer(new OrbmentSyncPacket(componentTag, slot), (ServerPlayer) player);
                }

                player.openMenu(new SimpleMenuProvider(
                        (windowId, inv, plyr) -> new OrbmentMenu(windowId, inv),
                        Component.literal("Orbment")
                ));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSoundEvents.ORBMENT_MENU_OPEN.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cast_controller", 0, state -> {
            var controller = state.getController();
            if (controller.getCurrentAnimation() != null && !controller.hasAnimationFinished()) {
                return PlayState.CONTINUE;
            }
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

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);

        // Initialize immediately when crafted
        if (!level.isClientSide()) {
            OrbmentComponent component = new OrbmentComponent();
            component.initializeLines();
            saveComponent(stack, component, level, (ServerPlayer) player);
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        // Don't initialize here - let each item initialize when first used
        return stack;
    }

    public static void saveComponent(ItemStack stack, OrbmentComponent component, Level level, ServerPlayer player) {
        synchronized (stack) {
            HolderLookup.Provider provider = level.registryAccess();
            CompoundTag componentTag = component.serializeNBT(provider);

            // Add a unique identifier to prevent data sharing between identical items
            componentTag.putLong("UniqueID", System.nanoTime() + ThreadLocalRandom.current().nextLong());

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(componentTag));
        }
    }

    public static void updateSelectedArt(ItemStack orbmentStack, String artName, Level level, ServerPlayer player) {
        if (orbmentStack.isEmpty() || !(orbmentStack.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent component = loadComponent(orbmentStack, level, player);
        component.setLastSelectedArtName(artName);
        component.markDirty();
        saveComponent(orbmentStack, component, level, player);

        if (!level.isClientSide()) {
            // Add packet sync here if needed
        }
    }

    public static void updateFavoriteArt(ItemStack orbmentStack, int slot, String artName, Level level, ServerPlayer player) {
        if (orbmentStack.isEmpty() || !(orbmentStack.getItem() instanceof OrbmentItem)) return;

        OrbmentComponent component = loadComponent(orbmentStack, level, player);
        component.setFavorite(slot, artName);
        component.markDirty();
        saveComponent(orbmentStack, component, level, player);

        if (!level.isClientSide()) {
            synchronized (("orbment_" + System.identityHashCode(orbmentStack)).intern()) {
                HolderLookup.Provider provider = level.registryAccess();
                CompoundTag componentTag = component.serializeNBT(provider);
                orbmentStack.set(DataComponents.CUSTOM_DATA, CustomData.of(componentTag));
            }
        }
    }
    public static OrbmentComponent loadComponent(ItemStack stack, Level level, ServerPlayer player) {
        OrbmentComponent component = new OrbmentComponent();
        HolderLookup.Provider provider = level.registryAccess();

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();
            try {
                component.deserializeNBT(provider, tag);
                if (component.areLinesInitialized()) {
                    return component;
                }
            } catch (Exception e) {
                System.out.println("Failed to deserialize, will initialize fresh");
            }
        }

        // Only initialize on server side and immediately save
        if (!level.isClientSide()) {
            component.initializeLines();
            saveComponent(stack, component, level, player);
        }

        return component;
    }
    public static OrbmentComponent loadComponentClientSide(ItemStack stack, Level level) {
        OrbmentComponent component = new OrbmentComponent();
        HolderLookup.Provider provider = level.registryAccess();

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();
            try {
                component.deserializeNBT(provider, tag);
            } catch (Exception e) {
                // On client, just return empty component if deserialization fails
            }
        }
        return component;
    }
    private static int findOrbmentSlot(ServerPlayer player, ItemStack targetStack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == targetStack) return i;
        }
        return -1;
    }
}