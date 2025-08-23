package net.JordanRiver.KisekiLegend.items;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderers.KisekiFishingRodRenderer;
import net.JordanRiver.KisekiLegend.fishing.FishData;
import net.JordanRiver.KisekiLegend.fishing.FishRegistry;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.JordanRiver.KisekiLegend.fishing.RodType;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;

import java.util.List;
import java.util.Set;

public class KisekiFishingRodItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final RodType rodType;
    private boolean isCurrentlyFishing = false;

    public KisekiFishingRodItem(Properties properties, RodType rodType) {
        super(properties);
        this.rodType = rodType;
    }
    public static String getBaitFromRod(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("BaitType")) {
                String baitType = tag.getString("BaitType");
                return baitType.isEmpty() ? null : baitType;
            }
        }
        return null; // No bait attached
    }




    public static boolean removeBaitFromRod(ItemStack rodStack) {
        if (rodStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = rodStack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("BaitType")) {
                // Remove bait data
                tag.remove("BaitType");
                tag.remove("BaitCount");

                if (tag.isEmpty()) {
                    rodStack.remove(DataComponents.CUSTOM_DATA);
                } else {
                    rodStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
                return true;
            }
        }
        return false;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack rodStack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            return handleBaitRemoval(rodStack, player);
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack baitStack = player.getItemInHand(otherHand);

        if (canUseBait(baitStack)) {
            if (tryAttachBait(rodStack, baitStack, player)) {
                return InteractionResultHolder.success(rodStack);
            } else {
                return InteractionResultHolder.fail(rodStack);
            }
        }

        // CRITICAL: Trigger cast animation BEFORE starting fishing
        if (level.isClientSide) {
            triggerCastAnimation(player);
        }

        return castFishingRod(level, player, rodStack);
    }
    public void triggerCastAnimation(Player player) {
        if (player.level().isClientSide) {
            AnimationController<?> controller = getAnimatableInstanceCache()
                    .getManagerForId(player.getId())
                    .getAnimationControllers()
                    .get("controller");
            if (controller != null) {
                controller.forceAnimationReset();
                controller.setAnimation(RawAnimation.begin().then("cast", Animation.LoopType.HOLD_ON_LAST_FRAME));
                isCurrentlyFishing = true;
            }
        }
    }

    private InteractionResultHolder<ItemStack> handleBaitRemoval(ItemStack rodStack, Player player) {
        String currentBait = getBaitFromRod(rodStack);
        if (currentBait == null) {
            player.displayClientMessage(Component.literal("No bait attached! Right-click with bait to attach."), true);
            return InteractionResultHolder.fail(rodStack);
        }

        // Get stored count
        int storedCount = 1;
        if (rodStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = rodStack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("BaitCount")) {
                storedCount = tag.getInt("BaitCount");
            }
        }

        // Remove bait data completely
        CompoundTag tag = rodStack.has(DataComponents.CUSTOM_DATA) ?
                rodStack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();

        if (tag.contains("BaitType")) {
            String baitType = tag.getString("BaitType");

            // Clear bait data
            tag.remove("BaitType");
            tag.remove("BaitCount");

            // Update rod
            if (tag.isEmpty()) {
                rodStack.remove(DataComponents.CUSTOM_DATA);
            } else {
                rodStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }

            // Give back bait
            ItemStack baitItem = new ItemStack(getBaitItem(baitType), storedCount);
            if (!player.getInventory().add(baitItem)) {
                player.drop(baitItem, false);
            }

            player.displayClientMessage(Component.literal("Removed " + storedCount + "x " + baitType), true);
            return InteractionResultHolder.success(rodStack);
        }

        return InteractionResultHolder.pass(rodStack);
    }

    private boolean canUseBait(ItemStack stack) {
        if (stack.getItem() instanceof BaitItem) {
            return true;
        }
        if (stack.getItem() instanceof FishItem fishItem) {
            FishData fishData = FishRegistry.getFishData(fishItem.getFishType());
            return fishData != null && fishData.canBeBait();
        }
        return false;
    }
    private InteractionResultHolder<ItemStack> castFishingRod(Level level, Player player, ItemStack rodStack) {
        if (level.isClientSide) {
            String currentBait = getBaitFromRod(rodStack);

            if (currentBait == null) {
                player.displayClientMessage(Component.literal("No bait attached! Right-click with bait to attach."), true);
                return InteractionResultHolder.fail(rodStack);
            }

            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getViewVector(1.0F);
            Vec3 endPos = eyePos.add(lookVec.scale(16.0));

            ClipContext clipContext = new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY, player);
            BlockHitResult hitResult = level.clip(clipContext);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                FluidState fluidState = level.getFluidState(hitResult.getBlockPos());
                if (fluidState.getType() == Fluids.WATER) {
                    if (consumeBaitFromRod(rodStack)) {
                        // Use exact hit location for bobber position
                        Vec3 waterPos = hitResult.getLocation();
                        int waterAreaSize = calculateWaterArea(level, BlockPos.containing(waterPos));

                        if (waterAreaSize < 2) {
                            player.displayClientMessage(Component.literal("Water area too small for fishing! Need at least 2x2 water."), true);
                            return InteractionResultHolder.fail(rodStack);
                        }

                        FishingGameManager.startFishingGame(player, rodType, waterPos, currentBait, waterAreaSize);
                        player.awardStat(Stats.ITEM_USED.get(this));
                        return InteractionResultHolder.success(rodStack);
                    }
                }
            }
        }
        return InteractionResultHolder.pass(rodStack);
    }


    private int calculateWaterArea(Level level, BlockPos centerPos) {
        int minSize = 2;
        int maxSize = 8;

        // Count actual water blocks in expanding areas
        for (int radius = 1; radius <= 4; radius++) {
            int totalWaterBlocks = 0;
            int totalArea = 0;

            // Check entire area, not just perimeter
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = centerPos.offset(x, 0, z);
                    FluidState fluidState = level.getFluidState(checkPos);
                    totalArea++;

                    if (fluidState.getType() == Fluids.WATER) {
                        totalWaterBlocks++;
                    }
                }
            }

            double waterRatio = (double) totalWaterBlocks / totalArea;

            KisekiLegend.LOGGER.info("Radius " + radius + ": " + totalWaterBlocks + "/" + totalArea + " = " + String.format("%.2f", waterRatio * 100) + "% water");

            // If water ratio drops below 70%, use current water block count as boundary
            if (waterRatio < 0.7) {
                // Use actual water blocks, not theoretical area
                int boundarySize = Math.max(minSize, Math.min(maxSize, (int)Math.sqrt(totalWaterBlocks) * 2));
                KisekiLegend.LOGGER.info("=== DYNAMIC BOUNDARY: " + boundarySize + " (based on " + totalWaterBlocks + " water blocks) ===");
                return boundarySize;
            }
        }

        return maxSize;
    }

    private static boolean consumeBaitFromRod(ItemStack rodStack) {
        if (rodStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = rodStack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("BaitCount") && tag.contains("BaitType")) {
                int currentCount = tag.getInt("BaitCount");
                if (currentCount > 1) {
                    // Reduce count
                    tag.putInt("BaitCount", currentCount - 1);
                    rodStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return true;
                } else if (currentCount == 1) {
                    // Remove bait completely
                    tag.remove("BaitType");
                    tag.remove("BaitCount");
                    if (tag.isEmpty()) {
                        rodStack.remove(DataComponents.CUSTOM_DATA);
                    } else {
                        rodStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    }
                    return true;
                }
            }
        }
        return false; // No bait to consume
    }

    public static boolean tryAttachBait(ItemStack rodStack, ItemStack baitStack, Player player) {
        if (!(rodStack.getItem() instanceof KisekiFishingRodItem)) {
            return false;
        }

        String baitType = null;
        int baitCount = baitStack.getCount();

        // Get bait type from item
        if (baitStack.getItem() instanceof BaitItem baitItem) {
            baitType = baitItem.getBaitType();
        } else if (baitStack.getItem() instanceof FishItem fishItem) {
            FishData fishData = FishRegistry.getFishData(fishItem.getFishType());
            if (fishData != null && fishData.canBeBait()) {
                baitType = fishItem.getFishType();
            } else {
                player.displayClientMessage(Component.literal("This fish cannot be used as bait!"), true);
                return false;
            }
        }

        if (baitType == null || baitType.isEmpty() || baitCount <= 0) {
            return false;
        }

        // Check if rod already has bait
        String currentBait = getBaitFromRod(rodStack);
        if (currentBait != null) {
            player.displayClientMessage(Component.literal("Rod already has bait attached! Shift+Right-click to remove."), true);
            return false;
        }

        // Try to add bait
        if (addBaitToRod(rodStack, baitType, baitCount)) {
            baitStack.shrink(baitCount);
            player.displayClientMessage(
                    Component.literal("Added " + baitCount + "x " + baitType + " to fishing rod"), true);
            return true;
        }

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Remove this method completely to prevent conflicting interactions
        return InteractionResult.PASS;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private PlayState predicate(AnimationState<KisekiFishingRodItem> animationState) {
        AnimationController<KisekiFishingRodItem> controller = animationState.getController();

        // If we just finished the cast animation, hold at last frame
        if (controller.getCurrentAnimation() != null &&
                controller.getCurrentAnimation().animation().name().equals("cast") &&
                controller.getAnimationState() == AnimationController.State.STOPPED) {
            // Hold at last frame of cast
            return PlayState.STOP;
        }

        // Default to idle animation
        controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }
    private PlayState animationPredicate(AnimationState<KisekiFishingRodItem> animationState) {
        AnimationController<KisekiFishingRodItem> controller = animationState.getController();

        // Check if we're currently fishing
        if (FishingGameManager.isActive() && isCurrentlyFishing) {
            // Hold on cast animation during fishing
            AnimationProcessor.QueuedAnimation currentAnim = controller.getCurrentAnimation();
            if (currentAnim == null || !currentAnim.animation().name().equals("cast")) {
                controller.setAnimation(RawAnimation.begin().then("cast", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            return PlayState.CONTINUE;
        } else {
            // Default to idle when not fishing
            if (isCurrentlyFishing) {
                // Reset flag and return to idle
                isCurrentlyFishing = false;
                controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
            } else {
                AnimationProcessor.QueuedAnimation currentAnim = controller.getCurrentAnimation();
                if (currentAnim == null || !currentAnim.animation().name().equals("idle")) {
                    controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
                }
            }
            return PlayState.CONTINUE;
        }
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void triggerAnimation(Player player, String animationName) {
        if (player.level().isClientSide) {
            // Trigger animation on client side
            triggerAnim(player, animationName);
        }
    }

    private void triggerAnim(Player player, String animationName) {
        AnimationController<?> controller = getAnimatableInstanceCache()
                .getManagerForId(player.getId())
                .getAnimationControllers()
                .get("controller");
        if (controller != null) {
            controller.forceAnimationReset();
            if (animationName.equals("cast")) {
                // Play cast once and hold at last frame
                controller.setAnimation(RawAnimation.begin().then("cast", Animation.LoopType.PLAY_ONCE));
            } else {
                // For other animations, play once then return to idle
                controller.setAnimation(RawAnimation.begin()
                        .then(animationName, Animation.LoopType.PLAY_ONCE)
                        .thenLoop("idle"));
            }
        }
    }
    public void resetToIdle(Player player) {
        if (player.level().isClientSide) {
            AnimationController<?> controller = getAnimatableInstanceCache()
                    .getManagerForId(player.getId())
                    .getAnimationControllers()
                    .get("controller");
            if (controller != null) {
                controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
                isCurrentlyFishing = false;
            }
        }
    }

    // Bait system methods (same as before)
    private InteractionResultHolder<ItemStack> removeBait(ItemStack rodStack, Player player) {
        CustomData customData = rodStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Bait")) {
                String baitName = tag.getString("Bait");
                tag.remove("Bait");
                rodStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                ItemStack baitItem = new ItemStack(getBaitItem(baitName));
                if (baitItem.getItem() != net.minecraft.world.item.Items.AIR) {
                    if (!player.getInventory().add(baitItem)) {
                        player.drop(baitItem, false);
                    }
                }

                player.displayClientMessage(
                        Component.literal("Removed bait: " + baitName), true);

                return InteractionResultHolder.success(rodStack);
            }
        }
        return InteractionResultHolder.pass(rodStack);
    }

    private net.minecraft.world.item.Item getBaitItem(String baitName) {
        // Regular bait items
        return switch (baitName) {
            case "earthworm" -> ModItems.EARTHWORM.get();
            case "polychaete" -> ModItems.POLYCHAETE.get();
            case "shrimplet" -> ModItems.SHRIMPLET.get();
            case "dumplings" -> ModItems.DUMPLINGS.get();
            case "red_flies" -> ModItems.RED_FLIES.get();
            case "river_bug" -> ModItems.RIVER_BUG.get();
            case "roe" -> ModItems.ROE.get();
            case "river_snail" -> ModItems.RIVER_SNAIL.get();
            case "frog" -> ModItems.FROG.get();

            // Fish that can be used as bait
            case "carp" -> ModItems.CARP.get();
            case "crab" -> ModItems.CRAB.get();
            case "dace" -> ModItems.DACE.get();
            case "eel" -> ModItems.EEL.get();
            case "kasago" -> ModItems.KASAGO.get();
            case "salmon" -> ModItems.SALMON.get();
            case "sea_bass" -> ModItems.SEA_BASS.get();
            case "trout" -> ModItems.TROUT.get();
            case "yamany" -> ModItems.YAMANY.get();

            default -> net.minecraft.world.item.Items.AIR;
        };
    }
    @Override
    public boolean isFoil(ItemStack stack) {
        String currentBait = getBaitFromRod(stack);
        return currentBait != null; // Glow when bait is attached
    }
    public static boolean addBaitToRod(ItemStack stack, String baitType, int count) {
        if (baitType == null || baitType.isEmpty() || count <= 0) {
            return false;
        }

        String currentBait = getBaitFromRod(stack);
        if (currentBait == null) { // Only add if no current bait
            CompoundTag tag = new CompoundTag();
            tag.putString("BaitType", baitType);
            tag.putInt("BaitCount", count);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return true;
        }
        return false;
    }

    public static boolean attachItemAsBait(ItemStack rodStack, ItemStack baitStack, Player player) {
        String baitType = null;

        // Check if it's a regular bait item
        if (baitStack.getItem() instanceof BaitItem baitItem) {
            baitType = baitItem.getBaitType();
        }
        // Check if it's a fish that can be used as bait
        else if (baitStack.getItem() instanceof FishItem fishItem) {
            FishData fishData = FishRegistry.getFishData(fishItem.getFishType());
            if (fishData != null && fishData.canBeBait()) {
                baitType = fishItem.getFishType();
            }
        }

        if (baitType != null && getBaitFromRod(rodStack).equals("earthworm")) {
            if (addBaitToRod(rodStack, baitType, baitStack.getCount())) {
                baitStack.shrink(baitStack.getCount()); // Consume all
                player.displayClientMessage(
                        Component.literal("Added " + baitStack.getCount() + "x " + baitType + " to fishing rod"), true);
                return true;
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.literal("Kiseki Fishing Rod"));
        tooltipComponents.add(Component.translatable("tooltip.kisekilegend.fishing_rod.type", rodType.getDisplayName()));
        tooltipComponents.add(Component.translatable("tooltip.kisekilegend.fishing_rod.time_bonus",
                String.format("%.2f", rodType.getTimeBonus())));

        String currentBait = getBaitFromRod(stack);
        if (currentBait != null) {
            int baitCount = 1;
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                if (tag.contains("BaitCount")) {
                    baitCount = tag.getInt("BaitCount");
                }
            }
            tooltipComponents.add(Component.literal("Current Bait: " + baitCount + "x " + currentBait));
            tooltipComponents.add(Component.literal("Shift+Right-click to remove bait"));
        } else {
            tooltipComponents.add(Component.literal("No bait attached - Right-click with bait to attach"));
        }

        Set<String> affinityBaits = rodType.getAffinityBaits();
        if (!affinityBaits.isEmpty()) {
            tooltipComponents.add(Component.literal("Affinity Baits:"));
            for (String bait : affinityBaits) {
                tooltipComponents.add(Component.literal("  • " + bait));
            }
        }
    }

    public RodType getRodType() {
        return rodType;
    }
    // Add this method to KisekiFishingRodItem.java
    // Replace the initializeClient method with this corrected version:
    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private KisekiFishingRodRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new KisekiFishingRodRenderer();
                }
                return this.renderer;
            }
        });
    }
}