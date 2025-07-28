package net.JordanRiver.KisekiLegend.block.entity;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlockEntities;
import net.JordanRiver.KisekiLegend.client.screen.QuartzMachineScreen;
import net.JordanRiver.KisekiLegend.crafting.recipe.QuartzCraftingRecipe;
import net.JordanRiver.KisekiLegend.item.ModItems;
import net.JordanRiver.KisekiLegend.item.enhancement.ItemEnhancementSystem;
import net.JordanRiver.KisekiLegend.item.enhancement.MaterialQualitySystem;
import net.JordanRiver.KisekiLegend.menu.QuartzMachineMenu;
import net.JordanRiver.KisekiLegend.network.NetworkHandler;
import net.JordanRiver.KisekiLegend.network.QuartzMachineSyncPacket;
import net.JordanRiver.KisekiLegend.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import java.util.*;

public class QuartzMachineBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack resultSlot = ItemStack.EMPTY;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    @Nullable
    private ResourceLocation activeRecipeId;
    private final Map<String, Long> lastPacketTime = new HashMap<>();
    private final Set<String> unlockedNodes = new HashSet<>();
    private final Set<String> completedNodes = new HashSet<>();
    private CompoundTag storedItems = new CompoundTag(); // Stores actual ItemStacks
    private static final Map<String, Set<Item>> MATERIAL_CACHE = new HashMap<>();
    private static boolean tagsInitialized = false;
    private List<ItemStack> itemsForRendering = new ArrayList<>();
    private int synthesisTick = 0;

    public QuartzMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.QUARTZ_MACHINE_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    private ItemStack applyCompletionEffects(ItemStack baseResult, Set<String> completedNodes, Set<String> allNodes, QuartzCraftingRecipe recipe) {
        System.out.println("=== APPLY COMPLETION EFFECTS START ==="); // ADD THIS
        ItemStack result = baseResult.copy();
        // ... rest of method
        // Collect all materials used in synthesis AND their existing enhancements
        List<Item> allMaterialsUsed = new ArrayList<>();
        List<ItemStack> allMaterialStacks = new ArrayList<>(); // NEW: Store full ItemStacks
        int totalMaterialQuality = 0;
        int materialCount = 0;

// Calculate base quality from materials used
        for (String nodeId : completedNodes) {
            CompoundTag nodeStoredItems = this.storedItems.getCompound(nodeId);
            for (String materialType : nodeStoredItems.getAllKeys()) {
                ListTag items = nodeStoredItems.getList(materialType, 10);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    try {
                        Item itemType = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("id")));
                        allMaterialsUsed.add(itemType);

                        // NEW: Parse full ItemStack to access existing enhancements
                        ItemStack materialStack;
                        if (level != null && level.registryAccess() != null) {
                            materialStack = ItemStack.parseOptional(level.registryAccess(), itemTag);
                        } else {
                            materialStack = new ItemStack(itemType, itemTag.getInt("count"));
                        }
                        allMaterialStacks.add(materialStack);

                        // Use enhanced quality (base + existing quality)
                        int baseQuality = MaterialQualitySystem.getBaseQuality(itemType);
                        int existingQuality = ItemEnhancementSystem.Quality.getQuality(materialStack);
                        int combinedQuality = Math.max(baseQuality, existingQuality); // Use higher of the two

                        System.out.println("Material: " + itemType + " base: " + baseQuality + " existing: " + existingQuality + " using: " + combinedQuality);
                        totalMaterialQuality += combinedQuality;
                        materialCount++;

                    } catch (Exception e) {
                        System.out.println("Error parsing material: " + e.getMessage());
                    }
                }
            }
        }

        int baseQuality = materialCount > 0 ? Math.max(ItemEnhancementSystem.Quality.DEFAULT_QUALITY, totalMaterialQuality / materialCount) : ItemEnhancementSystem.Quality.DEFAULT_QUALITY;
        System.out.println("Calculated base quality: " + baseQuality + " (total: " + totalMaterialQuality + ", count: " + materialCount + ")"); // ADD THIS
        ItemEnhancementSystem.Quality.setQuality(result, baseQuality);
        System.out.println("Quality after setting: " + ItemEnhancementSystem.Quality.getQuality(result)); // ADD THIS

        // Replace the trait application section in applyCompletionEffects with this:

// Apply material traits and effects with stacking and limits
        Map<String, Integer> combinedTraits = MaterialQualitySystem.getCombinedTraits(allMaterialsUsed);
        System.out.println("Combined traits from materials: " + combinedTraits);

        // Stack traits with existing ones, but with limits
        final int MAX_TRAIT_LEVEL = 10; // Configurable limit
        final int MAX_TRAITS_PER_ITEM = 5; // Maximum number of different traits

        Map<String, Integer> existingTraits = ItemEnhancementSystem.Traits.getAllTraits(result);
        Map<String, Integer> finalTraits = new HashMap<>(existingTraits);

        for (Map.Entry<String, Integer> newTrait : combinedTraits.entrySet()) {
            String traitName = newTrait.getKey();
            int newLevel = newTrait.getValue();

            if (finalTraits.containsKey(traitName)) {
                // Stack with existing trait, but cap at max level
                int currentLevel = finalTraits.get(traitName);
                int stackedLevel = Math.min(MAX_TRAIT_LEVEL, currentLevel + newLevel);
                finalTraits.put(traitName, stackedLevel);
                System.out.println("Stacked trait " + traitName + ": " + currentLevel + " + " + newLevel + " = " + stackedLevel);
            } else if (finalTraits.size() < MAX_TRAITS_PER_ITEM) {
                // Add new trait if under limit
                finalTraits.put(traitName, Math.min(MAX_TRAIT_LEVEL, newLevel));
                System.out.println("Added new trait " + traitName + " level " + newLevel);
            } else {
                // Replace lowest level trait if new trait is stronger
                String weakestTrait = finalTraits.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                if (weakestTrait != null && finalTraits.get(weakestTrait) < newLevel) {
                    finalTraits.remove(weakestTrait);
                    finalTraits.put(traitName, Math.min(MAX_TRAIT_LEVEL, newLevel));
                    System.out.println("Replaced weak trait " + weakestTrait + " with " + traitName + " level " + newLevel);
                } else {
                    System.out.println("Trait limit reached, new trait not strong enough: " + traitName);
                }
            }
        }

// NEW: Inherit traits and effects from input materials BEFORE applying material-based ones
        System.out.println("=== INHERITING FROM INPUT MATERIALS ===");
        Map<String, Integer> inheritedTraits = new HashMap<>();
        Map<String, ItemEnhancementSystem.ItemEffect> inheritedEffects = new HashMap<>();

        for (ItemStack materialStack : allMaterialStacks) {
            // Inherit traits (with 50% power retention)
            Map<String, Integer> materialTraits = ItemEnhancementSystem.Traits.getAllTraits(materialStack);
            for (Map.Entry<String, Integer> trait : materialTraits.entrySet()) {
                String traitName = trait.getKey();
                int inheritedLevel = Math.max(1, trait.getValue() / 2); // 50% retention, min 1

                if (inheritedTraits.containsKey(traitName)) {
                    inheritedTraits.put(traitName, Math.min(10,
                            inheritedTraits.get(traitName) + inheritedLevel)); // 10 = MAX_TRAIT_LEVEL
                } else {
                    // Replace lowest level trait if inherited trait is stronger
                    String weakestTrait = inheritedTraits.entrySet().stream()
                            .min(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (weakestTrait != null && inheritedTraits.get(weakestTrait) < inheritedLevel) {
                        inheritedTraits.remove(weakestTrait);
                        inheritedTraits.put(traitName, inheritedLevel);
                        System.out.println("Replaced weak inherited trait " + weakestTrait + " with " + traitName);
                    }
                }
                System.out.println("Inherited trait: " + traitName + " level " + inheritedLevel);
            }

            // Inherit effects (with 60% power retention)
            List<ItemEnhancementSystem.ItemEffect> materialEffects = ItemEnhancementSystem.Effects.getAllEffects(materialStack);
            for (ItemEnhancementSystem.ItemEffect effect : materialEffects) {
                String effectName = effect.name();
                float inheritedValue = effect.value() * 0.6f; // 60% retention
                int inheritedDuration = (int) (effect.duration() * 0.6f);

                if (inheritedEffects.containsKey(effectName)) {
                    ItemEnhancementSystem.ItemEffect existing = inheritedEffects.get(effectName);
                    float combinedValue = Math.min(10.0f, existing.value() + inheritedValue); // 10.0f = MAX_EFFECT_VALUE
                    int combinedDuration = Math.min(1200, existing.duration() + inheritedDuration); // 1200 = MAX_EFFECT_DURATION
                    inheritedEffects.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, combinedValue, combinedDuration));
                } else if (inheritedEffects.size() < 3) { // 3 = MAX_EFFECTS_PER_ITEM
                    inheritedEffects.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, inheritedValue, inheritedDuration));
                }
                System.out.println("Inherited effect: " + effectName + " value " + inheritedValue);
            }
        }

// Add inherited traits to finalTraits map
        for (Map.Entry<String, Integer> inheritedTrait : inheritedTraits.entrySet()) {
            String traitName = inheritedTrait.getKey();
            int inheritedLevel = inheritedTrait.getValue();

            if (finalTraits.containsKey(traitName)) {
                int currentLevel = finalTraits.get(traitName);
                finalTraits.put(traitName, Math.min(10, currentLevel + inheritedLevel)); // 10 = MAX_TRAIT_LEVEL
                System.out.println("Stacked inherited trait " + traitName + ": " + currentLevel + " + " + inheritedLevel);
            } else if (finalTraits.size() < 5) { // 5 = MAX_TRAITS_PER_ITEM
                finalTraits.put(traitName, inheritedLevel);
                System.out.println("Added inherited trait " + traitName + ": " + inheritedLevel);
            }
        }
// Clear existing traits and apply final ones
        for (String existingTrait : existingTraits.keySet()) {
            ItemEnhancementSystem.Traits.removeTrait(result, existingTrait);
        }
        for (Map.Entry<String, Integer> finalTrait : finalTraits.entrySet()) {
            ItemEnhancementSystem.Traits.addTrait(result, finalTrait.getKey(), finalTrait.getValue());
        }

// Apply material effects with stacking and limits
        List<MaterialQualitySystem.EffectData> combinedEffects = MaterialQualitySystem.getCombinedEffects(allMaterialsUsed);
        System.out.println("Combined effects from materials: " + combinedEffects.size());

        final int MAX_EFFECTS_PER_ITEM = 3; // Maximum number of different effects
        final float MAX_EFFECT_VALUE = 10.0f; // Maximum effect strength
        final int MAX_EFFECT_DURATION = 1200; // Maximum duration (60 seconds)

// Get existing effects and organize by name for stacking
        List<ItemEnhancementSystem.ItemEffect> existingEffects = ItemEnhancementSystem.Effects.getAllEffects(result);
        Map<String, ItemEnhancementSystem.ItemEffect> effectMap = new HashMap<>();
        for (ItemEnhancementSystem.ItemEffect effect : existingEffects) {
            effectMap.put(effect.name(), effect);
        }

// Process new effects
        for (MaterialQualitySystem.EffectData newEffect : combinedEffects) {
            String effectName = newEffect.getName();

            if (effectMap.containsKey(effectName)) {
                // Stack with existing effect
                ItemEnhancementSystem.ItemEffect existing = effectMap.get(effectName);
                float stackedValue = Math.min(MAX_EFFECT_VALUE, existing.value() + newEffect.getValue());
                int stackedDuration = Math.min(MAX_EFFECT_DURATION, existing.duration() + newEffect.getDuration());
                effectMap.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, stackedValue, stackedDuration));
                System.out.println("Stacked effect " + effectName + ": " + existing.value() + " + " + newEffect.getValue() + " = " + stackedValue);
            } else if (effectMap.size() < MAX_EFFECTS_PER_ITEM) {
                // Add new effect if under limit
                float cappedValue = Math.min(MAX_EFFECT_VALUE, newEffect.getValue());
                int cappedDuration = Math.min(MAX_EFFECT_DURATION, newEffect.getDuration());
                effectMap.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, cappedValue, cappedDuration));
                System.out.println("Added new effect " + effectName + ": " + cappedValue);
            } else {
                // Replace weakest effect if new effect is stronger
                String weakestEffect = effectMap.entrySet().stream()
                        .min(Map.Entry.comparingByValue((e1, e2) -> Float.compare(e1.value(), e2.value())))
                        .map(Map.Entry::getKey)
                        .orElse(null);

                float cappedValue = Math.min(MAX_EFFECT_VALUE, newEffect.getValue());
                int cappedDuration = Math.min(MAX_EFFECT_DURATION, newEffect.getDuration());

                if (weakestEffect != null && effectMap.get(weakestEffect).value() < cappedValue) {
                    effectMap.remove(weakestEffect);
                    effectMap.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, cappedValue, cappedDuration));
                    System.out.println("Replaced weak effect " + weakestEffect + " with " + effectName + ": " + cappedValue);
                } else {
                    System.out.println("Effect limit reached, new effect not strong enough: " + effectName);
                }
            }
        }

// Add inherited effects to effectMap
        for (ItemEnhancementSystem.ItemEffect inheritedEffect : inheritedEffects.values()) {
            String effectName = inheritedEffect.name();

            if (effectMap.containsKey(effectName)) {
                ItemEnhancementSystem.ItemEffect existing = effectMap.get(effectName);
                float stackedValue = Math.min(10.0f, existing.value() + inheritedEffect.value()); // 10.0f = MAX_EFFECT_VALUE
                int stackedDuration = Math.min(1200, existing.duration() + inheritedEffect.duration()); // 1200 = MAX_EFFECT_DURATION
                effectMap.put(effectName, new ItemEnhancementSystem.ItemEffect(effectName, stackedValue, stackedDuration));
                System.out.println("Stacked inherited effect " + effectName);
            } else if (effectMap.size() < 3) { // 3 = MAX_EFFECTS_PER_ITEM
                effectMap.put(effectName, inheritedEffect);
                System.out.println("Added inherited effect " + effectName);
            }
        }

// Clear existing effects and apply final ones
        for (ItemEnhancementSystem.ItemEffect existingEffect : existingEffects) {
            ItemEnhancementSystem.Effects.removeEffect(result, existingEffect.name());
        }
        for (ItemEnhancementSystem.ItemEffect finalEffect : effectMap.values()) {
            ItemEnhancementSystem.Effects.addEffect(result, finalEffect.name(), finalEffect.value(), finalEffect.duration());
        }

        System.out.println("Final traits after stacking: " + ItemEnhancementSystem.Traits.getAllTraits(result));
        System.out.println("Final effects after stacking: " + ItemEnhancementSystem.Effects.getAllEffects(result).size());

// NEW: Apply thematic bonuses based on recipe purpose
        String recipeId = recipe.getId().toString();
        result = applyThematicBonuses(result, recipeId, allMaterialsUsed);
        // Apply node completion bonuses
        for (String completedNodeId : completedNodes) {
            QuartzCraftingRecipe.Node nodeData = recipe.getNode(completedNodeId);
            if (nodeData != null) {
                String nodeType = nodeData.getType().toLowerCase();

                switch (nodeType) {
                    case "quality" -> {
                        // Quality nodes boost the base quality
                        int currentQuality = ItemEnhancementSystem.Quality.getQuality(result);
                        ItemEnhancementSystem.Quality.setQuality(result, currentQuality + 50);
                    }
                    case "trait" -> {
                        // Trait nodes enhance existing traits or add new ones
                        Map<String, Integer> nodeExistingTraits = ItemEnhancementSystem.Traits.getAllTraits(result);
                        if (!nodeExistingTraits.isEmpty()) {
                            // Enhance a random existing trait
                            String traitName = nodeExistingTraits.keySet().iterator().next();
                            int currentLevel = nodeExistingTraits.get(traitName);
                            ItemEnhancementSystem.Traits.addTrait(result, traitName, currentLevel + 1);
                        } else {
                            // Add a generic crafting trait
                            ItemEnhancementSystem.Traits.addTrait(result, "Well Crafted", 1);
                        }
                    }
                    case "effect" -> {
                        // Effect nodes enhance existing effects
                        List<ItemEnhancementSystem.ItemEffect> nodeExistingEffects = ItemEnhancementSystem.Effects.getAllEffects(result);
                        if (!nodeExistingEffects.isEmpty()) {
                            // Enhance the first effect
                            ItemEnhancementSystem.ItemEffect firstEffect = nodeExistingEffects.get(0);
                            ItemEnhancementSystem.Effects.removeEffect(result, firstEffect.name());
                            ItemEnhancementSystem.Effects.addEffect(result, firstEffect.name(),
                                    firstEffect.value() * 1.2f, firstEffect.duration() + 100);
                        } else {
                            // Add a generic synthesis effect
                            ItemEnhancementSystem.Effects.addEffect(result, "Synthesis Boost", 1.0f, 300);
                        }

                    }
                }

                System.out.println("Applied " + nodeType + " enhancement from node: " + completedNodeId);
            }
        }

        // Apply penalties for missed nodes
        Set<String> missedNodes = new HashSet<>(allNodes);
        missedNodes.removeAll(completedNodes);

        if (!missedNodes.isEmpty()) {
            for (String missedNodeId : missedNodes) {
                QuartzCraftingRecipe.Node missedNode = recipe.getNode(missedNodeId);
                if (missedNode != null) {
                    String nodeType = missedNode.getType().toLowerCase();

                    switch (nodeType) {
                        case "quality" -> {
                            // Reduce quality for missed quality nodes
                            int currentQuality = ItemEnhancementSystem.Quality.getQuality(result);
                            ItemEnhancementSystem.Quality.setQuality(result, Math.max(50, currentQuality - 100));
                        }
                        case "trait" -> {
                            // Add negative traits for missed trait nodes
                            ItemEnhancementSystem.Traits.addTrait(result, "Imperfect", 1);
                        }
                        case "effect" -> {
                            // Add negative effects for missed effect nodes
                            ItemEnhancementSystem.Effects.addEffect(result, "Incomplete", -0.5f, 300);
                        }
                    }
                }
            }
        }

        // Store synthesis completion data WITHOUT overwriting existing custom data
        CompoundTag existingData = ItemEnhancementSystem.getCustomData(result); // You'll need to make this method public
        existingData.putFloat("SynthesisCompletion", (float) completedNodes.size() / allNodes.size());
        existingData.putInt("CompletedNodes", completedNodes.size());
        existingData.putInt("TotalNodes", allNodes.size());
        existingData.putInt("MaterialQuality", baseQuality);

        result.set(DataComponents.CUSTOM_DATA, CustomData.of(existingData));

        System.out.println("Final result quality: " + ItemEnhancementSystem.Quality.getQuality(result));
        System.out.println("Traits: " + ItemEnhancementSystem.Traits.getAllTraits(result));
        System.out.println("Effects: " + ItemEnhancementSystem.Effects.getAllEffects(result).size());

        return result;
    }

    public void insertMaterial(Player player, String nodeId, String requiredMaterialType) {
        System.out.println("insertMaterial called - Server side: " + (level != null && !level.isClientSide()));

        // Ensure we're on the server side for tag checking
        if (level == null || level.isClientSide()) {
            System.out.println("Skipping - not on server side");
            return;
        }


        if (this.activeRecipeId == null || !this.unlockedNodes.contains(nodeId) || isSynthesizing()) {
            System.out.println("Early return - activeRecipeId null: " + (this.activeRecipeId == null) +
                    ", node unlocked: " + this.unlockedNodes.contains(nodeId) +
                    ", node completed: " + this.completedNodes.contains(nodeId) +
                    ", synthesizing: " + isSynthesizing());
            return;
        }

        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
        if (recipe == null) {
            System.out.println("Recipe is null for id: " + this.activeRecipeId);
            return;
        }

        QuartzCraftingRecipe.Node nodeData = recipe.getNode(nodeId);
        if (nodeData == null) {
            System.out.println("Node data is null for nodeId: " + nodeId);
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        debugTagCheck(heldItem); // Add this line
        System.out.println("Player held item: " + heldItem);
        if (heldItem.isEmpty()) {
            System.out.println("Held item is empty");
            return;
        }

        System.out.println("Node material requirements: " + nodeData.getMaterialRequirements());
// With this:
        CompoundTag nodeStoredItems = storedItems.getCompound(nodeId);
        int currentCount = 0;
        if (nodeStoredItems.contains(requiredMaterialType)) {
            ListTag storedList = nodeStoredItems.getList(requiredMaterialType, 10);
            currentCount = storedList.size();
        }

        if (nodeData.getMaterialRequirements().containsKey(requiredMaterialType)) {
            int requiredCount = nodeData.getMaterialRequirements().get(requiredMaterialType);

            System.out.println("Checking specific material: " + requiredMaterialType + " (need " + requiredCount + ", have " + currentCount + ")");

            if (currentCount < requiredCount) {
                boolean itemMatches;

                // Special handling for quartz material type - check for specific required item
                if (requiredMaterialType.equals("quartz") && recipe.getRequiredItem() != null) {
                    // Check if the held item matches the specific required quartz
                    ResourceLocation requiredItemId = ResourceLocation.parse(recipe.getRequiredItem());
                    Item requiredItem = BuiltInRegistries.ITEM.get(requiredItemId);
                    itemMatches = heldItem.getItem() == requiredItem;
                    System.out.println("Required specific quartz: " + recipe.getRequiredItem() + ", held item matches: " + itemMatches);
                } else {
                    // Use normal tag-based matching for other materials
                    itemMatches = checkItemMatchesMaterial(heldItem, requiredMaterialType);
                }

                if (itemMatches) {
                    System.out.println("Item matches! Storing item in node.");

                    // Create a single-count copy for storage
                    ItemStack itemToStore = heldItem.copy();
                    itemToStore.setCount(1);

// Store the item in the node
                    storeItemInNode(nodeId, requiredMaterialType, itemToStore);

// Remove ONE item from player inventory - use the proper inventory method
                    // Remove ONE item from player inventory - use the proper inventory method
                    if (!player.getAbilities().instabuild) { // Don't consume in creative mode
                        heldItem.shrink(1);
                        player.getInventory().setChanged();

                        // Sync the main hand slot specifically
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                                    -2, 0, player.getInventory().selected, heldItem));
                        }

                    }
                    checkNodeCompletion(nodeId, nodeData);
                    setChanged();
                    syncToClient();
                    return;
                }
            } else {
                System.out.println("Material requirement already fulfilled for: " + requiredMaterialType);
            }
        } else {
            System.out.println("Material type not required for this node: " + requiredMaterialType);
        }
    }


    private void storeItemInNode(String nodeId, String materialType, ItemStack item) {
        CompoundTag nodeData = storedItems.getCompound(nodeId);
        ListTag materialList = nodeData.getList(materialType, 10); // 10 = CompoundTag type

        // Fix: Check if level and registryAccess are available
        CompoundTag itemTag;
        if (level != null && level.registryAccess() != null) {
            itemTag = (CompoundTag) item.save(level.registryAccess());
        } else {
            // Fallback: create a minimal tag with essential item data
            itemTag = new CompoundTag();
            itemTag.putString("id", BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
            itemTag.putInt("count", item.getCount());

            // For 1.21.1: Check for components instead of NBT
            if (!item.getComponents().isEmpty()) {
                // Save components data - this replaces the old NBT tag system
                CompoundTag componentsTag = new CompoundTag();
                // You may need to implement component serialization here
                // For now, we'll skip complex component data in fallback mode
                itemTag.put("components", componentsTag);
            }
        }

        // Debug output
        System.out.println("Saving item: " + item + " to tag: " + itemTag);

        materialList.add(itemTag);
        nodeData.put(materialType, materialList);
        storedItems.put(nodeId, nodeData);

        // Additional debug
        System.out.println("After saving - materialList size: " + materialList.size());
        if (materialList.size() > 0) {
            System.out.println("Last stored item tag: " + materialList.getCompound(materialList.size() - 1));
        }
        // At the very end of storeItemInNode method:
        System.out.println("Stored item NBT: " + itemTag);
        CompoundTag testRead = materialList.getCompound(materialList.size() - 1);
        System.out.println("Read back item NBT: " + testRead);
    }


    public void removeLastItemFromNode(String nodeId, String materialType, Player player) {
        // Check synthesis state FIRST
        if (this.isSynthesizing) {
            System.out.println("Cannot remove items during synthesis");
            return;
        }

        // CRITICAL: Only allow removal on server side
        if (level == null || level.isClientSide()) {
            System.out.println("Cannot remove items on client side");
            return;
        }

        if (this.activeRecipeId == null || !this.unlockedNodes.contains(nodeId)) return;

        CompoundTag nodeStoredItems = storedItems.getCompound(nodeId);
        ListTag storedList = nodeStoredItems.getList(materialType, 10);

        if (storedList.size() > 0) {
            // Remove the last item FIRST
            CompoundTag lastItemTag = storedList.getCompound(storedList.size() - 1);
            storedList.remove(storedList.size() - 1);
            nodeStoredItems.put(materialType, storedList);
            storedItems.put(nodeId, nodeStoredItems);

            System.out.println("Item removed! New list size: " + storedList.size());

            // Parse the item to return
            ItemStack itemToReturn;
            if (level != null && level.registryAccess() != null) {
                itemToReturn = ItemStack.parseOptional(level.registryAccess(), lastItemTag);
            } else {
                if (lastItemTag.contains("id")) {
                    try {
                        Item itemType = BuiltInRegistries.ITEM.get(ResourceLocation.parse(lastItemTag.getString("id")));
                        int count = lastItemTag.getInt("count");
                        itemToReturn = new ItemStack(itemType, count);
                    } catch (Exception e) {
                        itemToReturn = ItemStack.EMPTY;
                    }
                } else {
                    itemToReturn = ItemStack.EMPTY;
                }
            }

            if (!itemToReturn.isEmpty()) {
                // Force exactly 1 item
                itemToReturn.setCount(1);

                // Give item back to player using a safer method
                if (!player.getInventory().add(itemToReturn)) {
                    // Drop if inventory full
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            level, player.getX(), player.getY(), player.getZ(), itemToReturn);
                    level.addFreshEntity(itemEntity);
                }

// CRITICAL: Sync inventory immediately (no delay)
                if (player instanceof ServerPlayer serverPlayer) {
                    player.getInventory().setChanged();

                    // Send inventory sync packet
                    serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket(
                            serverPlayer.containerMenu.containerId,
                            serverPlayer.containerMenu.incrementStateId(),
                            serverPlayer.containerMenu.getItems(),
                            serverPlayer.containerMenu.getCarried()
                    ));
                }
            }
            // Re-check completion status
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
            if (recipe != null) {
                QuartzCraftingRecipe.Node nodeData = recipe.getNode(nodeId);
                if (nodeData != null) {
                    checkNodeCompletion(nodeId, nodeData);
                }
            }

            this.completedNodes.remove(nodeId);
            setChanged();

// CRITICAL: Force immediate sync with debug
            System.out.println("Server: About to sync after removal - stored items size: " + storedItems.getAllKeys().size());
            syncToClient();
        }
    }


    public void clearStoredItems() {
        storedItems = new CompoundTag();
        // Reset completion status
        this.completedNodes.clear();
        this.unlockedNodes.clear();
        if (activeRecipeId != null) {
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(activeRecipeId);
            if (recipe != null) this.unlockedNodes.add(recipe.getStartingNode());
        }
    }

    public void returnStoredItemsToPlayer(Player player) {
        // Only return items on server side to prevent duplication
        if (level == null || level.isClientSide()) {
            System.out.println("returnStoredItemsToPlayer called on client side - ignoring");
            return;
        }

        System.out.println("Returning stored items to player - items to return: " + storedItems.getAllKeys());

        // Return all stored items to player inventory
        for (String nodeId : storedItems.getAllKeys()) {
            CompoundTag nodeData = storedItems.getCompound(nodeId);
            for (String materialType : nodeData.getAllKeys()) {
                ListTag items = nodeData.getList(materialType, 10);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    ItemStack item;
                    if (level != null && level.registryAccess() != null) {
                        item = ItemStack.parseOptional(level.registryAccess(), itemTag);
                    } else {
                        // Fallback parsing for minimal format
                        if (itemTag.contains("id")) {
                            try {
                                Item itemType = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("id")));
                                int count = itemTag.getInt("count");
                                item = new ItemStack(itemType, count);

                            } catch (Exception e) {
                                item = ItemStack.EMPTY;
                            }
                        } else {
                            item = ItemStack.EMPTY;
                        }
                    }
                    if (!item.isEmpty()) {
                        player.getInventory().add(item);
                    }
                }
            }
        }
    }

    // Add this new method to QuartzMachineBlockEntity class:
    private ItemStack applyThematicBonuses(ItemStack result, String recipeId, List<Item> materialsUsed) {
        System.out.println("Applying thematic bonuses for recipe: " + recipeId);

        Map<String, ThematicBonus> thematicBonuses = Map.ofEntries(
                Map.entry("defense_1", new ThematicBonus(
                        List.of("Defense Charge", "Steel Protection", "Quality"),
                        List.of("Slowdown S"),
                        List.of(new EffectBonus("DEF Up S", 1.0f, 600))
                )),
                Map.entry("defense_2", new ThematicBonus(
                        List.of("Defense Charge", "Steel Protection", "Indestructible Shield"),
                        List.of("Slowdown M"),
                        List.of(new EffectBonus("DEF Up S", 2.0f, 800), new EffectBonus("Guardian Mirror S", 1.5f, 800))
                )),
                Map.entry("defense_3", new ThematicBonus(
                        List.of("Dragonscale Protection", "Indestructible Shield", "Glorious Soul", "Divine Petal"),
                        List.of("Stats Power"),
                        List.of(new EffectBonus("Defense Veil", 3.0f, 1200), new EffectBonus("Guardian Mirror S", 2.0f, 600))
                )),
                Map.entry("poison", new ThematicBonus(
                        List.of("Assassin Poison S", "Grievous Wound S"),
                        List.of("Twilight Invitation S"),
                        List.of(new EffectBonus("Inflict Poison M", 2.0f, 400), new EffectBonus("Poison Damage XS", 1.0f, 300))
                )),
                Map.entry("mute", new ThematicBonus(
                        List.of("Slowdown S", "Slowdown M", "Sticky Goo S"),
                        List.of("Curse Strength"),
                        List.of(new EffectBonus("SPD Down S", 2.0f, 400), new EffectBonus("Inflict Slow S", 1.0f, 400))
                )),
                Map.entry("petrify", new ThematicBonus(
                        List.of("Steel Protection", "Slowdown M", "Defense Charge"),
                        List.of("Indestructible Shield"),
                        List.of(new EffectBonus("SPD Down S", 3.0f, 300), new EffectBonus("Inflict Curse S", 1.0f, 200))
                )),

                //<editor-fold desc="Water Quartz Thematics">
                Map.entry("hp_1", new ThematicBonus(
                        List.of("Healing", "HP Charge", "Healing Taste S"),
                        List.of("Quality"),
                        List.of(new EffectBonus("HP Gain S", 2.0f, 400))
                )),
                Map.entry("hp_2", new ThematicBonus(
                        List.of("Healing+", "Terrific Healing", "Natural Medicine"),
                        List.of("Rich Flavor"),
                        List.of(new EffectBonus("HP Regen M", 1.0f, 600), new EffectBonus("Feeling Full S", 1.0f, 600))
                )),
                Map.entry("hp_3", new ThematicBonus(
                        List.of("Healing++", "Terrific Healing", "Glorious Soul", "Divine Petal"),
                        List.of("Stats Power"),
                        List.of(new EffectBonus("HP Regen L", 2.0f, 800), new EffectBonus("HP Gain L", 2.0f, 1))
                )),
                Map.entry("mind_1", new ThematicBonus(
                        List.of("Mystic Life", "Skill Charge", "Flowing Wisdom"),
                        List.of("Secret Rainbow"),
                        List.of(new EffectBonus("Enhance Skills +3%", 1.0f, 1200))
                )),
                Map.entry("mind_2", new ThematicBonus(
                        List.of("Skill Charge+", "Resonant", "Free Soul"),
                        List.of("Glittering Darkness"),
                        List.of(new EffectBonus("Enhance Skills +7%", 2.0f, 1200), new EffectBonus("Magic Veil", 1.0f, 800))
                )),
                Map.entry("mind_3", new ThematicBonus(
                        List.of("Glorious Soul", "Stats Power", "Divine Petal"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("All Stats Up L", 2.0f, 1200), new EffectBonus("Remove Debuffs", 1.0f, 1200))
                )),
                Map.entry("freeze", new ThematicBonus(
                        List.of("Icy Echo", "Perpetual Ice S", "Sticky Goo S"),
                        List.of("Sticky Goo M"),
                        List.of(new EffectBonus("Inflict Frostbite M", 2.0f, 400), new EffectBonus("SPD Down S", 2.0f, 400))
                )),
                Map.entry("heal", new ThematicBonus(
                        List.of("Healing+", "Terrific Healing", "Natural Medicine"),
                        List.of("Sponge"),
                        List.of(new EffectBonus("Remove Ailments", 1.0f, 1), new EffectBonus("HP Regen L", 3.0f, 300))
                )),
                //</editor-fold>

                //<editor-fold desc="Fire Quartz Thematics">
                Map.entry("attack_1", new ThematicBonus(
                        List.of("Sharp Edge S", "Smoldering Lunacy", "Attack Charge"),
                        List.of("Destructive"),
                        List.of(new EffectBonus("ATK Up S", 1.0f, 600))
                )),
                Map.entry("attack_2", new ThematicBonus(
                        List.of("War God's Power", "Smoldering Lunacy", "Destructive+"),
                        List.of("Grievous Wound S"),
                        List.of(new EffectBonus("ATK Up M", 2.0f, 800), new EffectBonus("Fire Damage L", 1.5f, 800))
                )),
                Map.entry("attack_3", new ThematicBonus(
                        List.of("War God's Power", "Stats Power", "Destructive++", "Glorious Soul"),
                        List.of("Divine Petal"),
                        List.of(new EffectBonus("Fire Damage XL", 3.0f, 1200), new EffectBonus("Inflict Burn L", 2.0f, 400))
                )),
                Map.entry("seal", new ThematicBonus(
                        List.of("Twilight Invitation S", "Hazy Outline S", "Curse Strength"),
                        List.of("Assassin Poison S"),
                        List.of(new EffectBonus("Inflict Curse M", 2.0f, 400), new EffectBonus("All Stats Down S", 1.0f, 400))
                )),
                Map.entry("confuse", new ThematicBonus(
                        List.of("Fantasy Spore", "Glittering Darkness", "Rapid+"),
                        List.of("Sticky Goo S"),
                        List.of(new EffectBonus("Random Effect", 1.0f, 600), new EffectBonus("Surprise! S", 1.0f, 400))
                )),
                Map.entry("strike", new ThematicBonus(
                        List.of("Critical++", "Sharp Edge S", "Destructive+"),
                        List.of("Best Quality"),
                        List.of(new EffectBonus("Enhance Critical +20%", 3.0f, 600))
                )),
                //</editor-fold>

                //<editor-fold desc="Wind Quartz Thematics">
                Map.entry("shield_1", new ThematicBonus(
                        List.of("Defense Charge", "Soft Texture", "Quality"),
                        List.of("Natural Medicine"),
                        List.of(new EffectBonus("Guardian Mirror S", 1.0f, 600))
                )),
                Map.entry("shield_2", new ThematicBonus(
                        List.of("Steel Protection", "Free Soul", "Defense Charge"),
                        List.of("Natural Medicine"),
                        List.of(new EffectBonus("Defense Veil", 2.0f, 800), new EffectBonus("Slow Falling", 1.0f, 800))
                )),
                Map.entry("shield_3", new ThematicBonus(
                        List.of("Dragonscale Protection", "Free Soul", "Divine Petal"),
                        List.of("Glorious Soul"),
                        List.of(new EffectBonus("Defense Veil", 4.0f, 1200), new EffectBonus("Fire Resistance", 1.0f, 0))
                )),
                Map.entry("evade_1", new ThematicBonus(
                        List.of("Speed Charge", "Light Glow", "Speed of Light"),
                        List.of("Power Throw"),
                        List.of(new EffectBonus("SPD Up S", 1.0f, 600))
                )),
                Map.entry("evade_2", new ThematicBonus(
                        List.of("Free Soul", "Speed of Light", "Speed Charge"),
                        List.of("Power Throw+"),
                        List.of(new EffectBonus("SPD Up M", 2.0f, 800), new EffectBonus("Levitation", 1.0f, 800))
                )),
                Map.entry("evade_3", new ThematicBonus(
                        List.of("Speed of Light", "Free Soul", "Rarest"),
                        List.of("Divine Petal"),
                        List.of(new EffectBonus("Wind Rider", 3.0f, 1200), new EffectBonus("Levitation", 1.0f, 300))
                )),
                Map.entry("impede_1", new ThematicBonus(
                        List.of("Slowdown S", "Sticky Goo S", "Grievous Wound S"),
                        List.of("Curse Protection"),
                        List.of(new EffectBonus("Inflict Slow S", 1.0f, 400))
                )),
                Map.entry("impede_2", new ThematicBonus(
                        List.of("Slowdown M", "Curse Strength", "Sticky Goo M"),
                        List.of("Curse Protection"),
                        List.of(new EffectBonus("Inflict Slow S", 2.0f, 600), new EffectBonus("ATK Down S", 1.0f, 600))
                )),
                Map.entry("impede_3", new ThematicBonus(
                        List.of("Slowdown M", "Hazy Outline S", "Twilight Invitation S"),
                        List.of("Curse Strength"),
                        List.of(new EffectBonus("Inflict Slow S", 3.0f, 800), new EffectBonus("Inflict Curse L", 1.0f, 300))
                )),
                Map.entry("sleep", new ThematicBonus(
                        List.of("Twilight Invitation S", "Curse Strength", "Soft Texture"),
                        List.of("Mystic Life"),
                        List.of(new EffectBonus("SPD Down S", 2.0f, 600), new EffectBonus("ATK Down S", 1.0f, 600))
                )),
                Map.entry("scent", new ThematicBonus(
                        List.of("Rich Flavor", "Mild Sweetness", "Natural Medicine"),
                        List.of("Area Bonus"),
                        List.of(new EffectBonus("Money Magnet", 1.0f, 1200))
                )),
                //</editor-fold>

                //<editor-fold desc="Time Quartz Thematics">
                Map.entry("action_1", new ThematicBonus(
                        List.of("Speed Charge", "Reverse Hour Hand", "Thunder Burn"),
                        List.of("Quality"),
                        List.of(new EffectBonus("SPD Up S", 1.0f, 400))
                )),
                Map.entry("action_2", new ThematicBonus(
                        List.of("Speed Charge", "Thunder Burn", "Free Soul", "High Quality"),
                        List.of("Critical"),
                        List.of(new EffectBonus("SPD Up M", 2.0f, 600), new EffectBonus("Lightning Damage S", 1.0f, 600))
                )),
                Map.entry("action_3", new ThematicBonus(
                        List.of("Speed of Light", "Glorious Soul", "Stats Power", "Reverse Hour Hand"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("All Stats Up L", 3.0f, 800), new EffectBonus("Lightning Damage S", 2.0f, 800))
                )),
                Map.entry("blind", new ThematicBonus(
                        List.of("Hazy Outline S", "Twilight Invitation S", "Curse Strength"),
                        List.of("Rapid+"),
                        List.of(new EffectBonus("ATK Down S", 2.0f, 400), new EffectBonus("SPD Down S", 1.0f, 400))
                )),
                Map.entry("cast_1", new ThematicBonus(
                        List.of("Skill Charge", "Resonant", "Thunder Burn"),
                        List.of("Secret Rainbow"),
                        List.of(new EffectBonus("Enhance Skills +3%", 1.0f, 400))
                )),
                Map.entry("cast_2", new ThematicBonus(
                        List.of("Skill Charge+", "Resonant", "Free Soul", "Clear Head S"),
                        List.of("High Quality"),
                        List.of(new EffectBonus("Enhance Skills +7%", 1.5f, 600), new EffectBonus("Magic Veil", 2.0f, 600))
                )),
                Map.entry("deathblow_1", new ThematicBonus(
                        List.of("Critical+", "Sharp Edge S", "Destructive", "Explosive"),
                        List.of("Power Throw"),
                        List.of(new EffectBonus("Enhance Critical +20%", 2.0f, 400))
                )),
                Map.entry("deathblow_2", new ThematicBonus(
                        List.of("War God's Power", "Critical++", "Stats Power", "Hazy Outline S"),
                        List.of("Rapid+"),
                        List.of(new EffectBonus("Dragon Slayer", 4.0f, 600), new EffectBonus("Self Harm", 0.5f, 600))
                )),
                //</editor-fold>

                //<editor-fold desc="Space Quartz Thematics">
                Map.entry("move_1", new ThematicBonus(
                        List.of("Speed Charge", "Light Glow", "Speed of Light"),
                        List.of("Flowing Wisdom"),
                        List.of(new EffectBonus("SPD Up S", 1.0f, 800))
                )),
                Map.entry("move_2", new ThematicBonus(
                        List.of("Free Soul", "Speed of Light", "Overflowing Courage", "Rapid+"),
                        List.of("Fantasy Spore"),
                        List.of(new EffectBonus("SPD Up M", 1.5f, 1000), new EffectBonus("Levitation", 1.0f, 1000))
                )),
                Map.entry("move_3", new ThematicBonus(
                        List.of("Speed of Light", "Free Soul", "Overflowing Courage", "Primordial Power"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("Wind Rider", 2.0f, 1200), new EffectBonus("Slow Falling", 1.0f, 1200))
                )),
                Map.entry("ep_cut_1", new ThematicBonus(
                        List.of("Skill Charge", "High Quality"),
                        List.of("Quality"),
                        List.of(new EffectBonus("Enhance Skills +3%", 1.2f, 1200))
                )),
                Map.entry("ep_cut_2", new ThematicBonus(
                        List.of("Skill Charge+", "High Quality", "Free Soul", "Clear Head S"),
                        List.of("Best Quality"),
                        List.of(new EffectBonus("Enhance Skills +7%", 1.5f, 1200), new EffectBonus("Remove Debuffs", 1.0f, 1200))
                )),
                Map.entry("ep_cut_3", new ThematicBonus(
                        List.of("Skill Charge+", "Glorious Soul", "Stats Power", "Divine Petal"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("Enhance Skills +10%", 2.0f, 1200), new EffectBonus("Magic Veil", 1.0f, 1200))
                )),
                Map.entry("range_1", new ThematicBonus(
                        List.of("Clear Head S", "Critical", "Sharp Edge S"),
                        List.of("Thunder Current S"),
                        List.of(new EffectBonus("Eye for Materials", 1.0f, 1200))
                )),
                Map.entry("eagle_eye", new ThematicBonus(
                        List.of("Clear Head S", "Light Glow", "Best Quality", "Thunder Current S"),
                        List.of("Critical+"),
                        List.of(new EffectBonus("Night Vision", 1.0f, 1200), new EffectBonus("Treasure Hunter", 2.0f, 1200))
                )),
                //</editor-fold>

                //<editor-fold desc="Mirage Quartz Thematics">
                Map.entry("ep_1", new ThematicBonus(
                        List.of("Skill Charge", "Mystic Life"),
                        List.of("Secret Rainbow"),
                        List.of(new EffectBonus("Enhance Skills +3%", 1.3f, 1000))
                )),
                Map.entry("ep_2", new ThematicBonus(
                        List.of("Skill Charge+", "Free Soul", "Mystic Life"),
                        List.of("Resonant"),
                        List.of(new EffectBonus("Enhance Skills +7%", 1.6f, 1000), new EffectBonus("Remove Debuffs", 1.0f, 1000))
                )),
                Map.entry("ep_3", new ThematicBonus(
                        List.of("Skill Charge+", "Glorious Soul", "Divine Petal"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("Enhance Skills +10%", 2.5f, 1000), new EffectBonus("All Stats Up L", 1.5f, 1000))
                )),
                Map.entry("hit_1", new ThematicBonus(
                        List.of("Critical", "Sharp Edge S"),
                        List.of("Quality"),
                        List.of(new EffectBonus("Critical Rate Up S", 1.0f, 600))
                )),
                Map.entry("hit_2", new ThematicBonus(
                        List.of("Critical+", "Sharp Edge S", "Best Quality"),
                        List.of("Clear Head S"),
                        List.of(new EffectBonus("Critical Rate Up S", 2.0f, 800), new EffectBonus("Eye for Materials", 1.0f, 800))
                )),
                Map.entry("hit_3", new ThematicBonus(
                        List.of("Critical++", "Sharp Edge S", "Best Quality", "Stats Power"),
                        List.of("Rarest"),
                        List.of(new EffectBonus("Enhance Critical +20%", 3.0f, 1200), new EffectBonus("Treasure Hunter", 2.0f, 1200))
                )),
                Map.entry("information", new ThematicBonus(
                        List.of("Clear Head S", "Thunder Current S", "Secret Rainbow"),
                        List.of("Light Glow"),
                        List.of(new EffectBonus("Eye for Materials", 1.0f, 3600), new EffectBonus("XP Gain", 1.1f, 3600))
                )),
                Map.entry("haze", new ThematicBonus(
                        List.of("Free Soul", "Slowdown S", "Glittering Darkness", "Fantasy Spore"),
                        List.of("Soft Texture"),
                        List.of(new EffectBonus("Inflict Slow S", 1.0f, 400), new EffectBonus("Random Effect", 1.0f, 200))
                )),
                Map.entry("cloak", new ThematicBonus(
                        List.of("Free Soul", "Hazy Outline S", "Twilight Invitation S"),
                        List.of("Rapid+"),
                        List.of(new EffectBonus("Random Effect", 1.0f, 600), new EffectBonus("ATK Down S", 1.0f, 300))
                ))
        );

        // Extract base recipe name (remove modid prefix)
        String baseRecipeId = recipeId.contains(":") ? recipeId.split(":")[1] : recipeId;
        ThematicBonus bonus = thematicBonuses.get(baseRecipeId);

        if (bonus == null) {
            System.out.println("No thematic bonus defined for: " + baseRecipeId);
            return result;
        }

        // Check for thematic matches
        Map<String, Integer> currentTraits = ItemEnhancementSystem.Traits.getAllTraits(result);
        List<ItemEnhancementSystem.ItemEffect> currentEffects = ItemEnhancementSystem.Effects.getAllEffects(result);

        int thematicMatches = 0;

        // Count trait matches
        for (String matchingTrait : bonus.matchingTraitsEffects) {
            if (currentTraits.containsKey(matchingTrait)) {
                thematicMatches++;
                System.out.println("Found thematic trait match: " + matchingTrait);
            }
        }

        // Count effect matches
        for (String matchingEffect : bonus.matchingTraitsEffects) {
            if (currentEffects.stream().anyMatch(effect -> effect.name().equals(matchingEffect))) {
                thematicMatches++;
                System.out.println("Found thematic effect match: " + matchingEffect);
            }
        }

        if (thematicMatches > 0) {
            System.out.println("Found " + thematicMatches + " thematic matches! Applying bonuses...");

            // Apply quality bonus (10% per match, max 50%)
            int currentQuality = ItemEnhancementSystem.Quality.getQuality(result);
            int qualityBonus = Math.min(50, thematicMatches * 10);
            ItemEnhancementSystem.Quality.setQuality(result, currentQuality + qualityBonus);
            System.out.println("Applied quality bonus: +" + qualityBonus + " (total: " + (currentQuality + qualityBonus) + ")");

            // Add bonus traits
            for (String bonusTrait : bonus.bonusTraits) {
                int traitLevel = Math.min(5, thematicMatches); // Scale with matches, max level 5
                ItemEnhancementSystem.Traits.addTrait(result, bonusTrait, traitLevel);
                System.out.println("Added bonus trait: " + bonusTrait + " level " + traitLevel);
            }

            // Add bonus effects
            for (EffectBonus bonusEffect : bonus.bonusEffects) {
                float scaledValue = bonusEffect.value * (1.0f + (thematicMatches - 1) * 0.5f); // 50% bonus per extra match
                int scaledDuration = bonusEffect.duration + (thematicMatches - 1) * 100; // +100 ticks per extra match
                ItemEnhancementSystem.Effects.addEffect(result, bonusEffect.name, scaledValue, scaledDuration);
                System.out.println("Added bonus effect: " + bonusEffect.name + " (value: " + scaledValue + ", duration: " + scaledDuration + ")");
            }

            // Special thematic synergy bonus for perfect matches
            if (thematicMatches >= 3) {
                ItemEnhancementSystem.Traits.addTrait(result, "Perfect Synthesis", 1);
                ItemEnhancementSystem.Effects.addEffect(result, "Synergy Boost", 3.0f, 1200);
                System.out.println("Applied PERFECT SYNTHESIS bonus!");
            }
        } else {
            System.out.println("No thematic matches found for " + baseRecipeId);
        }

        return result;
    }

    // Add these helper classes
    private static class ThematicBonus {
        final List<String> matchingTraitsEffects;
        final List<String> bonusTraits;
        final List<EffectBonus> bonusEffects;

        ThematicBonus(List<String> matching, List<String> traits, List<EffectBonus> effects) {
            this.matchingTraitsEffects = matching;
            this.bonusTraits = traits;
            this.bonusEffects = effects;
        }
    }

    private static class EffectBonus {
        final String name;
        final float value;
        final int duration;

        EffectBonus(String name, float value, int duration) {
            this.name = name;
            this.value = value;
            this.duration = duration;
        }
    }


    private void checkNodeCompletion(String nodeId, QuartzCraftingRecipe.Node nodeData) {
        CompoundTag nodeStoredItems = storedItems.getCompound(nodeId);

        // Check if ALL requirements are met
        boolean isComplete = true;
        for (Map.Entry<String, Integer> req : nodeData.getMaterialRequirements().entrySet()) {
            ListTag storedList = nodeStoredItems.getList(req.getKey(), 10);
            if (storedList.size() < req.getValue()) {
                isComplete = false;
                break;
            }
        }

        if (isComplete) {
            this.completedNodes.add(nodeId);
            nodeData.getUnlocks().forEach(this.unlockedNodes::add);
        } else {
            // IMPORTANT: Remove from completed if no longer complete
            this.completedNodes.remove(nodeId);

            // NEW: Remove unlocked nodes that were unlocked by this node
            // but only if no other completed node unlocks them
            for (String unlockedByThis : nodeData.getUnlocks()) {
                boolean stillUnlockedByOther = false;

                // Check if any other completed node also unlocks this
                for (String otherCompletedNode : this.completedNodes) {
                    if (!otherCompletedNode.equals(nodeId)) {
                        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
                        if (recipe != null) {
                            QuartzCraftingRecipe.Node otherNode = recipe.getNode(otherCompletedNode);
                            if (otherNode != null && otherNode.getUnlocks().contains(unlockedByThis)) {
                                stillUnlockedByOther = true;
                                break;
                            }
                        }
                    }
                }

                // Also check if this is the starting node (always unlocked)
                QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
                if (recipe != null && unlockedByThis.equals(recipe.getStartingNode())) {
                    stillUnlockedByOther = true;
                }

                // Remove from unlocked if no other node unlocks it
                if (!stillUnlockedByOther) {
                    this.unlockedNodes.remove(unlockedByThis);
                }
            }
        }
    }

    private boolean checkItemMatchesMaterial(ItemStack item, String materialType) {
        if (item.isEmpty()) {
            return false;
        }

        TagKey<Item> tag = switch (materialType.toLowerCase()) {
            case "all" -> null; // Special case - matches everything
            case "jewel" -> ModTags.Items.JEWEL;
            case "quartz" -> ModTags.Items.QUARTZ;
            case "water_material" -> ModTags.Items.WATER_MATERIAL;
            case "fire_material" -> ModTags.Items.FIRE_MATERIAL;
            case "earth_material" -> ModTags.Items.EARTH_MATERIAL;
            case "wind_material" -> ModTags.Items.WIND_MATERIAL;
            case "time_material" -> ModTags.Items.TIME_MATERIAL;
            case "space_material" -> ModTags.Items.SPACE_MATERIAL;
            case "mirage_material" -> ModTags.Items.MIRAGE_MATERIAL;
            case "mystery" -> ModTags.Items.MYSTERY;
            case "accessory" -> ModTags.Items.ACCESSORY;
            case "bomb" -> ModTags.Items.BOMB;
            case "cooking" -> ModTags.Items.COOKING;
            case "dessert" -> ModTags.Items.DESSERT;
            case "elixir" -> ModTags.Items.ELIXIR;
            case "food" -> ModTags.Items.FOOD;
            case "gunpowder" -> ModTags.Items.GUNPOWDER;
            case "ingot" -> ModTags.Items.INGOT;
            case "liquid" -> ModTags.Items.LIQUID;
            case "magic_tool" -> ModTags.Items.MAGIC_TOOL;
            case "medicinal" -> ModTags.Items.MEDICINAL;
            case "medicine" -> ModTags.Items.MEDICINE;
            case "oil" -> ModTags.Items.OIL;
            case "ore" -> ModTags.Items.ORE;
            case "poison" -> ModTags.Items.POISON;
            case "spice" -> ModTags.Items.SPICE;
            case "sundry" -> ModTags.Items.SUNDRY;
            case "supplement" -> ModTags.Items.SUPPLEMENT;
            case "threads" -> ModTags.Items.THREADS;
            case "wool" -> ModTags.Items.WOOL;
            case "plant" -> ModTags.Items.PLANT;
            case "cloth" -> ModTags.Items.CLOTH;
            default -> null;
        };

        // "all" category matches everything
        if (materialType.equals("all")) {
            return true;
        }

        if (tag == null) {
            return false;
        }

        return item.is(tag);
    }
    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);

        // Always save empty stored items - they should never persist between sessions
        pTag.put("StoredItems", new CompoundTag());

        pTag.putString("ActiveRecipe", this.activeRecipeId != null ? this.activeRecipeId.toString() : "");
        pTag.putBoolean("IsSynthesizing", false); // Never save synthesis state
        pTag.putLong("SynthesisStartTime", 0);

        // Don't save temporary node states either
        pTag.put("UnlockedNodes", new ListTag());
        pTag.put("CompletedNodes", new ListTag());
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.storedItems = pTag.getCompound("StoredItems");
        String recipeStr = pTag.getString("ActiveRecipe");
        this.activeRecipeId = recipeStr.isEmpty() ? null : ResourceLocation.parse(recipeStr);
        this.isSynthesizing = pTag.getBoolean("IsSynthesizing");
        this.synthesisStartTime = pTag.getLong("SynthesisStartTime");

        // Load unlocked and completed nodes
        this.unlockedNodes.clear();
        ListTag unlockedList = pTag.getList("UnlockedNodes", 8); // 8 = StringTag
        for (int i = 0; i < unlockedList.size(); i++) {
            this.unlockedNodes.add(unlockedList.getString(i));
        }

        this.completedNodes.clear();
        ListTag completedList = pTag.getList("CompletedNodes", 8);
        for (int i = 0; i < completedList.size(); i++) {
            this.completedNodes.add(completedList.getString(i));
        }
    }

    // In QuartzMachineBlockEntity.java

    public void insertMaterialFromSlot(Player player, String nodeId, String requiredMaterialType, int slotIndex) {
        // Server-side only check
        if (level == null || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // --- All of your existing validation logic remains the same ---
        if (this.activeRecipeId == null || !this.unlockedNodes.contains(nodeId) || isSynthesizing()) {
            System.out.println("insertMaterialFromSlot validation failed - activeRecipeId: " + this.activeRecipeId +
                    ", nodeId unlocked: " + this.unlockedNodes.contains(nodeId) +
                    ", unlocked nodes: " + this.unlockedNodes +
                    ", synthesizing: " + isSynthesizing());
            return;
        }
        if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack slotItem = player.getInventory().getItem(slotIndex);
        if (slotItem.isEmpty()) {
            return;
        }
        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
        if (recipe == null) {
            return;
        }
        QuartzCraftingRecipe.Node nodeData = recipe.getNode(nodeId);
        if (nodeData == null) {
            return;
        }
        CompoundTag nodeStoredItems = storedItems.getCompound(nodeId);
        ListTag storedList = nodeStoredItems.getList(requiredMaterialType, 10);
        int currentCount = storedList.size();
        int requiredCount = nodeData.getMaterialRequirements().getOrDefault(requiredMaterialType, 0);
        if (currentCount >= requiredCount) {
            return;
        }
        boolean itemMatches;
        if (requiredMaterialType.equals("quartz") && recipe.getRequiredItem() != null) {
            ResourceLocation requiredItemId = ResourceLocation.parse(recipe.getRequiredItem());
            Item requiredItem = BuiltInRegistries.ITEM.get(requiredItemId);
            itemMatches = slotItem.getItem() == requiredItem;
        } else {
            itemMatches = checkItemMatchesMaterial(slotItem, requiredMaterialType);
        }
        if (!itemMatches) {
            return;
        }
        // --- End of validation ---


        // ALL CHECKS PASSED - PERFORM INSERTION
        ItemStack itemToStore = slotItem.copy();
        itemToStore.setCount(1);
        storeItemInNode(nodeId, requiredMaterialType, itemToStore);

// Consume the item from the player's inventory (works in both creative and survival)
        System.out.println("ATTEMPTING TO CONSUME ITEM - Creative mode: " + player.getAbilities().instabuild);
// Get fresh reference to the slot item to ensure we have the current state
        ItemStack currentSlotItem = player.getInventory().getItem(slotIndex);
        System.out.println("Before shrink - slot " + slotIndex + ": " + currentSlotItem + " (count: " + currentSlotItem.getCount() + ")");

        if (!currentSlotItem.isEmpty()) {
            // Always consume the item, regardless of game mode
            // This makes the crafting system consistent between creative and survival
            currentSlotItem.shrink(1);
            // CRITICAL: Update the slot with the modified stack
            player.getInventory().setItem(slotIndex, currentSlotItem);
            System.out.println("After shrink - slot " + slotIndex + ": " + currentSlotItem + " (count: " + currentSlotItem.getCount() + ")");
        } else {
            System.out.println("ERROR: Slot item became empty before consumption!");
        }

        // Update the state of the machine
        checkNodeCompletion(nodeId, nodeData);
        setChanged();

        // --- RELIABLE SYNCING ---

        // 1. Force a full sync of the player's inventory via their open container.
        // This is the most reliable way to prevent visual desync bugs.
        serverPlayer.containerMenu.broadcastChanges();

        // 2. Sync the block entity's state to the client immediately.
        syncToClient();
    }
    public void debugInventoryState(Player player, String context) {
        System.out.println("=== INVENTORY DEBUG: " + context + " ===");
        System.out.println("Server side: " + (level != null && !level.isClientSide()));

        for (int i = 0; i < Math.min(9, player.getInventory().getContainerSize()); i++) {
            ItemStack item = player.getInventory().getItem(i);
            System.out.println("Slot " + i + ": " + item + " (count: " + item.getCount() + ")");
        }
        System.out.println("==================");
    }



    public void doDelayedInventorySync(Player player) {
        if (player instanceof ServerPlayer serverPlayer && level != null && !level.isClientSide()) {
            // Send updates for all inventory slots
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                        -2, 0, i, player.getInventory().getItem(i)));
            }
        }
    }
    public void onMenuClosed(Player player) {
        // Already confirmed this is server side from menu.removed()
        System.out.println("Menu closed on server - stored items before return: " + this.storedItems.getAllKeys());

        // Only return items and clear if NOT synthesizing
        if (!isSynthesizing()) {
            returnStoredItemsToPlayer(player);
            clearStoredItems();
            setChanged();
            // Don't sync immediately after clearing - it causes the empty sync
            // syncToClient();
        } else {
            System.out.println("Menu closed during synthesis - NOT clearing storage");
        }
    }
    private boolean isSynthesizing = false;
    private long synthesisStartTime = 0;
    private List<ItemStack> floatingItems = new ArrayList<>();

    public void startSynthesis() {
        System.out.println("startSynthesis() called - activeRecipeId: " + this.activeRecipeId);

        if (this.activeRecipeId == null || this.isSynthesizing || this.completedNodes.isEmpty()) {
            System.out.println("Early return from startSynthesis");
            return;
        }

        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
        if (recipe == null) {
            System.out.println("Recipe not found in startSynthesis for ID: " + this.activeRecipeId);
            return;
        }

        // Collect all stored items for animation BEFORE clearing them
        this.floatingItems.clear();
        for (String nodeId : storedItems.getAllKeys()) {
            CompoundTag nodeData = storedItems.getCompound(nodeId);
            for (String materialType : nodeData.getAllKeys()) {
                ListTag items = nodeData.getList(materialType, 10);
                System.out.println("Processing " + items.size() + " items from " + nodeId + "/" + materialType); // Add this debug
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    ItemStack item;
                    if (level != null && level.registryAccess() != null) {
                        item = ItemStack.parseOptional(level.registryAccess(), itemTag);
                    } else {
                        // Fallback parsing
                        if (itemTag.contains("id")) {
                            try {
                                Item itemType = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("id")));
                                int count = itemTag.getInt("count");
                                item = new ItemStack(itemType, count);
                            } catch (Exception e) {
                                item = ItemStack.EMPTY;
                            }
                        } else {
                            item = ItemStack.EMPTY;
                        }
                    }
                    if (!item.isEmpty()) {
                        this.floatingItems.add(item);
                    }
                }
            }
        }

        System.out.println("Collected " + this.floatingItems.size() + " items for animation");

        // Set synthesis state but DON'T clear stored items yet - let completeSynthesis do it
        this.isSynthesizing = true;
        this.synthesisStartTime = level.getGameTime();

        setChanged();
        syncToClient();
    }
    public boolean isSynthesizing() {
        return this.isSynthesizing;
    }

    public List<ItemStack> getFloatingItems() {
        return this.floatingItems;
    }

    public long getSynthesisStartTime() {
        return this.synthesisStartTime;
    }
    public static void tick(Level level, BlockPos blockPos, BlockState blockState, QuartzMachineBlockEntity entity) {
        // Handle synthesis particles and completion
        if (entity.isSynthesizing) {
            long currentTime = level.getGameTime();
            long elapsed = currentTime - entity.synthesisStartTime;

            // Spawn particles every 10 ticks ONLY while synthesizing
            if (elapsed % 10 == 0 && level.isClientSide) {
                entity.spawnSynthesisParticles();
            }

            // Complete synthesis after 120 ticks
            if (elapsed >= 120) {
                entity.completeSynthesis();
            }
        }
    }
    private void spawnSynthesisParticles() {
        if (level != null && level.isClientSide && this.isSynthesizing) { // Add isSynthesizing check
            // Spawn particles above the machine
            for (int i = 0; i < 5; i++) {
                double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                double y = worldPosition.getY() + 2.0;
                double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, x, y, z, 0, -0.1, 0);
            }
        }
    }

    private void completeSynthesis() {
        System.out.println("completeSynthesis() called - Server side: " + (level != null && !level.isClientSide()));
        System.out.println("Active recipe ID before lookup: " + this.activeRecipeId);

        // If activeRecipeId is null, try to get it from the last selected recipe
        // This is a fallback mechanism
        if (this.activeRecipeId == null) {
            System.out.println("activeRecipeId is null! Cannot complete synthesis.");
            // Clear synthesis state and return
            this.isSynthesizing = false;
            this.synthesisStartTime = 0;
            this.floatingItems.clear();
            this.completedNodes.clear();
            this.unlockedNodes.clear();
            setChanged();
            syncToClient();
            return;
        }

        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(this.activeRecipeId);
        if (recipe == null) {
            System.out.println("Recipe is null for ID: " + this.activeRecipeId);
            System.out.println("Available recipes: " + KisekiLegend.getQuartzRecipeManager().getRecipes().keySet());
            return;
        }

        System.out.println("Recipe found: " + recipe.getId() + ", result: " + recipe.getResult());


        if (level != null && !level.isClientSide()) {
            try {
                // NEW: Calculate completion percentage and modify result
                Set<String> completedNodes = this.completedNodes;
                Set<String> allNodes = recipe.getNodes().keySet();
                float completionPercentage = (float) completedNodes.size() / allNodes.size();

                System.out.println("Synthesis completion: " + completedNodes.size() + "/" + allNodes.size() + " (" + (completionPercentage * 100) + "%)");

                Item resultItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(recipe.getResult()));
                ItemStack resultStack = new ItemStack(resultItem);

                // NEW: Apply completion-based modifications
                resultStack = applyCompletionEffects(resultStack, completedNodes, allNodes, recipe);
                // Clear stored items and completion state now that synthesis is finishing
                this.storedItems = new CompoundTag();

                System.out.println("Created result stack: " + resultStack + ", isEmpty: " + resultStack.isEmpty());
                System.out.println("Attempting to drop item at: " + worldPosition);

                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.5, // 1.5 blocks high
                        worldPosition.getZ() + 0.5,
                        resultStack
                );

                itemEntity.setPickUpDelay(20);
                itemEntity.setDeltaMovement(0, 0.1, 0);

                boolean success = level.addFreshEntity(itemEntity);
                System.out.println("Item entity added successfully: " + success);
            } catch (Exception e) {
                System.out.println("Error creating result item: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Clear synthesis state
        this.isSynthesizing = false;
        this.synthesisStartTime = 0;
        this.floatingItems.clear();
        this.completedNodes.clear();
        this.unlockedNodes.clear();

        // Clear the active recipe LAST
        this.activeRecipeId = null;
        this.resultSlot = ItemStack.EMPTY;

        setChanged();
        syncToClient();
    }
    public void setActiveRecipe(@Nullable ResourceLocation recipeId) {
        System.out.println("setActiveRecipe called: " + recipeId + " (was: " + this.activeRecipeId + ")");

        this.activeRecipeId = recipeId;
        this.unlockedNodes.clear();
        this.completedNodes.clear();
        this.resultSlot = ItemStack.EMPTY;

        if (recipeId != null) {
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(recipeId);
            if (recipe != null) this.unlockedNodes.add(recipe.getStartingNode());
        }

        setChanged();
        syncToClient();
    }

    // Replace the updateStateFromServer method with this THREAD-SAFE version:
    public void updateStateFromServer(@Nullable ResourceLocation recipeId, Set<String> unlocked, CompoundTag receivedStoredItems, Set<String> completedFromServer) {
        // CRITICAL: Only update on client side and on main thread
        if (level == null || !level.isClientSide()) {
            return;
        }

        // Ensure we're on the main client thread using Minecraft.getInstance()
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(() -> updateStateFromServer(recipeId, unlocked, receivedStoredItems, completedFromServer));
            return;
        }

        System.out.println("CLIENT: Updating state from server");
        System.out.println("Received recipe: " + recipeId);
        System.out.println("Received unlocked nodes: " + unlocked);
        System.out.println("Received stored items keys: " + receivedStoredItems.getAllKeys());

        // Update all state atomically
        this.activeRecipeId = recipeId;

        this.unlockedNodes.clear();
        this.unlockedNodes.addAll(unlocked);

        this.completedNodes.clear();
        this.completedNodes.addAll(completedFromServer);

        // CRITICAL: Deep copy to prevent reference issues
        this.storedItems = receivedStoredItems.copy();

        System.out.println("CLIENT: State updated - activeRecipeId: " + this.activeRecipeId + ", unlockedNodes: " + this.unlockedNodes);

        // Force block entity to mark as changed for rendering updates
        setChanged();


    }
    private void debugTagCheck(ItemStack item) {
        System.out.println("=== TAG DEBUG ===");
        System.out.println("Item: " + item);
        System.out.println("Item registry name: " + item.getItem().getDescriptionId());

        // Test some known tags
        System.out.println("Is sapling (vanilla tag): " + item.is(net.minecraft.tags.ItemTags.SAPLINGS));
        System.out.println("Is plant (mod tag): " + item.is(ModTags.Items.PLANT));
        System.out.println("Is water material (mod tag): " + item.is(ModTags.Items.WATER_MATERIAL));

        System.out.println("Plant tag location: " + ModTags.Items.PLANT.location());
        System.out.println("==================");
    }
    public void setFloatingItemsFromServer(List<ItemStack> items, boolean synthesizing, long startTime) {
        this.floatingItems.clear();
        this.floatingItems.addAll(items);
        this.isSynthesizing = synthesizing;
        this.synthesisStartTime = startTime;
        System.out.println("Client received " + items.size() + " floating items for animation");
        System.out.println("Floating items: " + items);
    }
    private long lastSyncTime = 0;
    private static final long SYNC_COOLDOWN = 100; // Increased to 100ms

    public void syncToClient() {
        if (level == null || level.isClientSide()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_COOLDOWN) {
            return; // Prevent rapid sync calls
        }
        lastSyncTime = currentTime;

        try {
            // Create IMMUTABLE copies to prevent concurrent modification
            ResourceLocation safeRecipeId = this.activeRecipeId;
            Set<String> safeUnlockedNodes = Set.copyOf(this.unlockedNodes);
            Set<String> safeCompletedNodes = Set.copyOf(this.completedNodes);
            CompoundTag safeStoredItems = this.storedItems.copy();
            List<ItemStack> safeFloatingItems = List.copyOf(this.floatingItems);

            System.out.println("Syncing to client - recipe: " + safeRecipeId + ", unlocked nodes: " + safeUnlockedNodes + ", stored items keys: " + safeStoredItems.getAllKeys());

// Single network packet
            NetworkHandler.sendToAllClients(new QuartzMachineSyncPacket(
                    this.worldPosition,
                    safeRecipeId,
                    safeUnlockedNodes,
                    safeStoredItems,
                    safeCompletedNodes,
                    this.isSynthesizing,
                    this.synthesisStartTime,
                    safeFloatingItems
            ));

        } catch (Exception e) {
            System.out.println("Error during sync: " + e.getMessage());
        }
    }

    // REMOVED: This method is no longer needed as syncing is now event-driven.
    // public void scheduledSync() { ... }

    public CompoundTag getStoredItems() {
        return this.storedItems;
    }

    public boolean isNodeCompleted(String nodeId) {
        return this.completedNodes.contains(nodeId);
    }

    public List<ItemStack> getItemsForRendering() {
        return itemsForRendering;
    }

    public float getItemAnimationProgress() {
        if (!isSynthesizing || synthesisStartTime == 0) return 0f;
        long elapsed = level.getGameTime() - synthesisStartTime;
        return Math.min(1.0f, elapsed / 120.0f); // Changed from 100 to 120 ticks
    }

    public List<ItemRenderData> getFloatingItemPositions() {
        if (!isSynthesizing) return List.of();



        List<ItemRenderData> positions = new ArrayList<>();
        float progress = getItemAnimationProgress();

        for (int i = 0; i < floatingItems.size(); i++) {
            ItemStack item = floatingItems.get(i);
            double angle = (2 * Math.PI * i) / floatingItems.size();

// For 2-block tall machine, animate in the middle of the upper block
            double startX = worldPosition.getX() + 0.5 + Math.cos(angle) * 1.2;
            double startZ = worldPosition.getZ() + 0.5 + Math.sin(angle) * 1.2;
            double startY = worldPosition.getY() + 1.5; // Middle of upper block (1.5 from bottom)

            double endX = worldPosition.getX() + 0.5;
            double endZ = worldPosition.getZ() + 0.5;
            double endY = worldPosition.getY() + 1.5; // Same level, or 1.6 for sl ghtly higher convergence

            double currentX = startX + (endX - startX) * progress;
            double currentZ = startZ + (endZ - startZ) * progress;
            double currentY = startY + (endY - startY) * progress;

            positions.add(new ItemRenderData(item, currentX, currentY, currentZ));
        }

        return positions;
    }

    public static record ItemRenderData(ItemStack item, double x, double y, double z) {}
    // ADDED: This getter is now present for the screen to call.
    public Set<String> getCompletedNodes() {
        return this.completedNodes;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Remove the item handler capability entirely
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Remove lazyItemHandler initialization
    }
    public void reconstructFloatingItemsForAnimation() {
        if (!this.isSynthesizing) return;

        // Collect all stored items for animation BEFORE clearing them
        this.floatingItems.clear();
        for (String nodeId : storedItems.getAllKeys()) {
            CompoundTag nodeData = storedItems.getCompound(nodeId);
            for (String materialType : nodeData.getAllKeys()) {
                ListTag items = nodeData.getList(materialType, 10);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    ItemStack item;
                    if (level != null && level.registryAccess() != null) {
                        item = ItemStack.parseOptional(level.registryAccess(), itemTag);
                    } else {
                        // Fallback parsing for minimal format
                        if (itemTag.contains("id")) {
                            try {
                                Item itemType = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("id")));
                                int count = itemTag.getInt("count");
                                item = new ItemStack(itemType, count);
                            } catch (Exception e) {
                                item = ItemStack.EMPTY;
                            }
                        } else {
                            item = ItemStack.EMPTY;
                        }
                    }
                    if (!item.isEmpty()) {
                        this.floatingItems.add(item);
                    }
                }
            }
        }

        System.out.println("Collected " + this.floatingItems.size() + " items for animation");

// IMPORTANT: Clear stored items AFTER collecting them for animation
        this.storedItems = new CompoundTag();
    }
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        // Remove lazyItemHandler invalidation
    }

    // Add getter for result slot
    public ItemStack getResultSlot() {
        return resultSlot;
    }

    public void writeScreenOpeningData(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }


    public void setResultSlot(ItemStack stack) {
        this.resultSlot = stack;
        setChanged();
        syncToClient();
    }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) { return saveWithoutMetadata(pRegistries); }
    @Override public Component getDisplayName() { return Component.literal("Quartz Synthesizer"); }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new QuartzMachineMenu(pContainerId, pPlayerInventory, this);
    }
    @Nullable public ResourceLocation getActiveRecipeId() { return activeRecipeId; }
    public Set<String> getUnlockedNodes() { return unlockedNodes; }
}