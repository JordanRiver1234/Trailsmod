package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.JordanRiver.KisekiLegend.capability.PlayerRecipeProgressProvider;
import net.JordanRiver.KisekiLegend.crafting.recipe.QuartzCraftingRecipe;
import net.JordanRiver.KisekiLegend.init.ModSoundEvents;
import net.JordanRiver.KisekiLegend.menu.QuartzMachineMenu;
import net.JordanRiver.KisekiLegend.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.JordanRiver.KisekiLegend.util.ModTags;
import net.minecraft.world.item.Items;

import java.util.*;

public class QuartzMachineScreen extends AbstractContainerScreen<QuartzMachineMenu> {

    private final Map<String, Pos> nodePositions = new HashMap<>();
    private List<QuartzCraftingRecipe> recipeList = new ArrayList<>();
    private Button synthesisButton;
    private String selectedMaterialType = null;
    private enum ScreenState {SELECTING_RECIPE, INSERTING_MATERIAL}
    private final Map<String, Boolean> categoryExpanded = new HashMap<>();
    private final Map<String, List<QuartzCraftingRecipe>> categoryRecipes = new HashMap<>();
    private ScreenState currentState = ScreenState.SELECTING_RECIPE;
    private String selectedNodeId = null;
    private int scrollOffset = 0;
    private long lastClickTime = 0;
    private int hoveredFilteredSlot = -1;
    private int maxScrollOffset = 0;
    private int viewOffsetX = 0;
    private int viewOffsetY = 0;
    private boolean isDragging = false;
    private long lastInventoryClickTime = 0;
    private static final long CLICK_COOLDOWN = 150; // 150ms between clicks
       private int lastMouseX = 0;
    private long lastCategoryClickTime = 0;
    private String lastClickedCategory = "";
    private int lastMouseY = 0;
    private boolean isInitializing = true;
    private boolean startupSoundPlayed = false;
    private boolean processingSoundPlayed = false;
    private boolean completeSoundPlayed = false;

    private long initStartTime = 0;
    private static final int LOADING_DURATION = 60; // 3 seconds at 20 TPS
    private int selectedInventoryTab = 0;
    private int inventoryTabScrollOffset = 0;
    private static final int MAX_VISIBLE_TABS = 8;
    private static final int VISIBLE_LINES = 25;
    private final List<String> elementalCategories = List.of("water", "fire", "earth", "wind", "time", "space", "mirage");
    private String selectedCategory = "water";
    private int recipeScrollOffset = 0;
    private final Map<Integer, Integer> filteredSlotMapping = new HashMap<>(); // displaySlot -> actualSlot
    private int clickedDisplaySlot = -1;
    private boolean isRecipeUnlocked(QuartzCraftingRecipe recipe) {
        if (minecraft.player == null) return false;

        String recipeId = recipe.getId().getPath();

        // Always unlock tier 1 recipes
        if (recipeId.endsWith("_1")) {
            return true;
        }

        // Special case for freeze - unlocked by either hp_2 OR mind_2
        if (recipeId.equals("freeze")) {
            return hasCompletedRecipe("hp_2") || hasCompletedRecipe("mind_2");
        }

        // Check if prerequisite is completed
        String prerequisite = getPrerequisiteRecipe(recipeId);
        if (prerequisite != null) {
            return hasCompletedRecipe(prerequisite); // CHANGED: Use helper method consistently
        }

        return true; // Fallback for recipes without prerequisites
    }

    private String getPrerequisiteRecipe(String recipeId) {
        // Map each recipe to its prerequisite
        if (recipeId.endsWith("_2")) {
            return recipeId.replace("_2", "_1");
        } else if (recipeId.endsWith("_3")) {
            return recipeId.replace("_3", "_2");
        }

        // Handle morph recipes - they unlock when their base recipe is completed
        Map<String, String> morphPrerequisites = Map.of(
                "freeze", "hp_2", // Also available from mind_2, but we'll check hp_2
                "poison", "defense_2",
                "mute", "defense_2",
                "petrify", "defense_3",
                "heal", "hp_3",
                "seal", "attack_2",
                "confuse", "attack_2",
                "strike", "attack_3",
                "sleep", "shield_2",
                "scent", "evade_2"
        );

        // Additional morph prerequisites (Map.of has a 10 entry limit)
        Map<String, String> additionalMorphs = Map.of(
                "blind", "action_2",
                "range_1", "move_2",
                "eagle_eye", "ep_cut_2",
                "information", "ep_2",
                "haze", "ep_2",
                "cloak", "hit_2"
        );

        String prerequisite = morphPrerequisites.get(recipeId);
        if (prerequisite == null) {
            prerequisite = additionalMorphs.get(recipeId);
        }

        return prerequisite;
    }

    private boolean hasCompletedRecipe(String recipeId) {
        if (minecraft.player == null) return false;

        return minecraft.player.getCapability(PlayerRecipeProgressProvider.PLAYER_RECIPE_PROGRESS)
                .map(progress -> progress.hasCompletedRecipe(recipeId))
                .orElse(false);
    }
    private List<String> getAllMaterialTypes() {
        return List.of(
                "all", // Special "all" category to show everything
                "water_material", "fire_material", "earth_material", "wind_material",
                "time_material", "space_material", "mirage_material", "jewel", "mystery",
                "accessory", "bomb", "cooking", "dessert", "elixir", "food",
                "gunpowder", "ingot", "liquid", "magic_tool", "medicinal", "medicine",
                "oil", "ore", "poison", "spice", "sundry", "supplement", "threads",
                "wool", "plant", "cloth"
        );
    }
    private ResourceLocation getNodeTexture(QuartzCraftingRecipe.Node nodeData) {
        return switch (nodeData.getType().toLowerCase()) {
            case "recipe_morph" -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/recipe_node_circle.png");
            case "trait" -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/trait_node_circle.png");
            case "quality" -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/quality_node_circle.png");
            case "effect" -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/effect_node_circle.png");
            case "quartz" -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/quartz_node_circle.png");
            default -> ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/node_circle.png");
        };
    }
    private boolean itemMatchesSelectedTab(ItemStack item) {
        if (item.isEmpty()) return false;

        List<String> materialTypes = getAllMaterialTypes();
        if (selectedInventoryTab < 0 || selectedInventoryTab >= materialTypes.size()) {
            return false;
        }

        String tabMaterialType = materialTypes.get(selectedInventoryTab);

        // Special case for "all" tab - show everything
        if (tabMaterialType.equals("all")) {
            return true;
        }

        // Check if item matches the current tab's material type
        return checkItemMatchesMaterialClientSide(item, tabMaterialType);
    }
    private boolean checkItemMatchesMaterialClientSide(ItemStack item, String materialType) {
        if (item.isEmpty()) return false;

        // Duplicate the server-side logic for client-side validation
        TagKey<Item> tag = switch (materialType.toLowerCase()) {
            case "all" -> null;
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

        if (materialType.equals("all")) return true;
        if (tag == null) return false;

        boolean matches = item.is(tag);
        System.out.println("Client-side tag check: " + item.getDisplayName().getString() + " is " + tag.location() + " = " + matches);
        return matches;
    }


    public QuartzMachineScreen(QuartzMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

        // Clear and initialize existing maps to prevent null pointer exceptions
        this.categoryExpanded.clear();
        this.categoryRecipes.clear();
        this.nodePositions.clear();

        // Initialize all categories as collapsed
        for (String category : elementalCategories) {
            this.categoryExpanded.put(category, false);
            this.categoryRecipes.put(category, new ArrayList<>());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    protected void init() {
        super.init();

        // Start loading sequence IMMEDIATELY
        this.isInitializing = true;
        this.initStartTime = System.currentTimeMillis();

        // Reset loading sounds
        resetLoadingSounds();

        System.out.println("Starting loading sequence at: " + initStartTime);

        this.titleLabelX = -200;
        this.inventoryLabelX = -200;
        this.titleLabelY = -200;
        this.inventoryLabelY = -200;

        nodePositions.clear();

        this.synthesisButton = addRenderableWidget(Button.builder(Component.literal("Synthesize"), this::onSynthesisButtonPressed)
                .bounds(this.width / 2 - 100, 10, 120, 20)
                .build());

        // Initialize categories immediately (don't delay this)
        initializeCategoryRecipes();
        // Don't call updateNodePositions here - let onRecipeChanged handle it
    }

    private boolean isLoadingComplete() {
        if (!isInitializing) return true;

        long elapsed = System.currentTimeMillis() - initStartTime;

        // Force minimum 3 second loading duration
        return elapsed >= 3000; // 3000ms = 3 seconds
    }
    private void initializeCategoryRecipes() {
        Map<String, List<String>> categoryToPatterns = Map.of(
                "water", List.of("hp_1", "hp_2", "hp_3", "mind_1", "mind_2", "mind_3", "freeze", "heal"),
                "fire", List.of("attack_1", "attack_2", "attack_3", "confuse", "strike", "seal"),
                "earth", List.of("defense_1", "defense_2", "defense_3", "mute", "petrify", "poison"),
                "wind", List.of("shield_1", "shield_2", "shield_3", "evade_1", "evade_2", "evade_3", "impede_1", "impede_2", "impede_3", "sleep", "scent"),
                "time", List.of("action_1", "action_2", "action_3", "blind", "cast_1", "cast_2", "deathblow_1", "deathblow_2"),
                "space", List.of("move_1", "move_2", "move_3", "ep_cut_1", "ep_cut_2", "ep_cut_3", "range_1", "eagle_eye"),
                "mirage", List.of("ep_1", "ep_2", "ep_3", "hit_1", "hit_2", "hit_3", "information", "haze", "cloak")
        );

        categoryExpanded.clear();
        categoryRecipes.clear();

        for (String category : elementalCategories) {
            categoryExpanded.put(category, false);

            List<String> patterns = categoryToPatterns.getOrDefault(category, List.of());
            Set<String> addedRecipes = new HashSet<>(); // Prevent duplicates
            List<QuartzCraftingRecipe> recipes = new ArrayList<>();

            for (QuartzCraftingRecipe recipe : KisekiLegend.getQuartzRecipeManager().getRecipes().values()) {
                String recipeId = recipe.getId().getPath();

                // Only add if not already added and matches pattern
                if (!addedRecipes.contains(recipeId) && patterns.contains(recipeId)) {
                    recipes.add(recipe);
                    addedRecipes.add(recipeId);
                    System.out.println("Added recipe: " + recipeId + " to category: " + category);
                }
            }

            categoryRecipes.put(category, recipes);
        }
    }
    private void updateNodePositions() {
        nodePositions.clear();

        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getActiveRecipeId() == null) {
            return;
        }

        String recipeId = blockEntity.getActiveRecipeId().getPath();
        System.out.println("updateNodePositions() - Setting positions for recipe: " + recipeId);

        if (recipeId.equals("hp_1")) {
            // HP_1 positions (as per your original file, unmodified)
            nodePositions.put("start_hp", new Pos(this.width / 2 - 80, this.height / 2));
            nodePositions.put("quality_boost", new Pos(this.width / 2, this.height / 2));
            nodePositions.put("recipe_unlock_hp2", new Pos(this.width / 2 + 80, this.height / 2));
        } else if (recipeId.equals("hp_2")) {
            // HP_2 positions - a wide, branching layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_hp2", new Pos(centerX - 140, centerY));
            nodePositions.put("effect_root_hp2", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 20, centerY - 55));
            nodePositions.put("effect_branch", new Pos(centerX + 20, centerY + 55));
            nodePositions.put("quality_upper", new Pos(centerX + 100, centerY - 70));
            nodePositions.put("quality_lower", new Pos(centerX + 100, centerY + 70));
            nodePositions.put("morph_freeze", new Pos(centerX + 100, centerY - 20));
            nodePositions.put("morph_mind1", new Pos(centerX + 100, centerY + 20));
            nodePositions.put("recipe_unlock_hp3", new Pos(centerX + 180, centerY));
        } else if (recipeId.equals("hp_3")) {
            // HP_3 positions - a tall, symmetrical "fountain" layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_hp3", new Pos(centerX, centerY + 90));
            nodePositions.put("effect_root_hp3", new Pos(centerX, centerY + 30));
            nodePositions.put("trait_upper_branch", new Pos(centerX - 60, centerY - 20));
            nodePositions.put("trait_lower_branch", new Pos(centerX + 60, centerY - 20));
            nodePositions.put("quality_upper", new Pos(centerX - 70, centerY - 80));
            nodePositions.put("morph_heal", new Pos(centerX - 10, centerY - 80));
            nodePositions.put("quality_lower", new Pos(centerX + 70, centerY - 80));
            nodePositions.put("effect_final_boost", new Pos(centerX + 10, centerY - 80));
        } else if (recipeId.equals("defense_1")) {
            // Defense_1 positions - simple horizontal line
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_def", new Pos(centerX - 75, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_def2", new Pos(centerX + 75, centerY));
        } else if (recipeId.equals("defense_2")) {
            // Defense_2 positions - an "arrowhead" layout pointing right
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_def2", new Pos(centerX - 120, centerY));
            nodePositions.put("effect_root_def2", new Pos(centerX - 50, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 20, centerY - 45));
            nodePositions.put("effect_branch", new Pos(centerX + 20, centerY + 45));
            nodePositions.put("quality_upper", new Pos(centerX + 90, centerY - 45));
            nodePositions.put("morph_poison", new Pos(centerX + 90, centerY - 15));
            nodePositions.put("quality_lower", new Pos(centerX + 90, centerY + 45));
            nodePositions.put("morph_mute", new Pos(centerX + 90, centerY + 15));
            nodePositions.put("recipe_unlock_def3", new Pos(centerX + 160, centerY));
        } else if (recipeId.equals("defense_3")) {
            // Defense_3 positions - a wide, diamond-like layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_def3", new Pos(centerX - 150, centerY));
            nodePositions.put("effect_root_def3", new Pos(centerX - 70, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX, centerY - 60));
            nodePositions.put("trait_lower_branch", new Pos(centerX, centerY + 60));
            nodePositions.put("quality_upper", new Pos(centerX + 80, centerY - 70));
            nodePositions.put("morph_petrify", new Pos(centerX + 80, centerY - 20));
            nodePositions.put("quality_lower", new Pos(centerX + 80, centerY + 70));
            nodePositions.put("effect_final_boost", new Pos(centerX + 80, centerY + 20));
        } else if (recipeId.equals("poison")) {
            // Poison positions - a small triangle
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_poison", new Pos(centerX, centerY + 40));
            nodePositions.put("effect_potency", new Pos(centerX - 50, centerY - 20));
            nodePositions.put("quality_node", new Pos(centerX + 50, centerY - 20));
        } else if (recipeId.equals("mute")) {
            // Mute positions - a 'Y' shape
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_mute", new Pos(centerX, centerY + 40));
            nodePositions.put("effect_duration", new Pos(centerX - 50, centerY - 30));
            nodePositions.put("quality_node", new Pos(centerX + 50, centerY - 30));
        } else if (recipeId.equals("petrify")) {
            // Petrify positions - a fork with an extra branch
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_petrify", new Pos(centerX - 80, centerY));
            nodePositions.put("effect_chance", new Pos(centerX, centerY - 40));
            nodePositions.put("trait_node", new Pos(centerX, centerY + 40));
            nodePositions.put("quality_node", new Pos(centerX + 80, centerY - 40));
        } else if (recipeId.equals("mind_1")) {
            // Mind_1 positions - simple horizontal
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_mind", new Pos(centerX - 70, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_mind2", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("mind_2")) {
            // Mind_2 positions - top-down flow
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_mind2", new Pos(centerX, centerY - 80));
            nodePositions.put("effect_root_mind2", new Pos(centerX, centerY - 30));
            nodePositions.put("trait_branch", new Pos(centerX - 50, centerY + 20));
            nodePositions.put("morph_freeze", new Pos(centerX + 50, centerY + 20));
            nodePositions.put("quality_node", new Pos(centerX - 50, centerY + 70));
            nodePositions.put("recipe_unlock_mind3", new Pos(centerX + 50, centerY + 70));
        } else if (recipeId.equals("mind_3")) {
            // Mind_3 positions - an open, square-like structure
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_mind3", new Pos(centerX - 90, centerY - 40));
            nodePositions.put("effect_root_mind3", new Pos(centerX, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 90, centerY - 40));
            nodePositions.put("trait_lower_branch", new Pos(centerX - 90, centerY + 40));
            nodePositions.put("quality_upper", new Pos(centerX, centerY - 80));
            nodePositions.put("quality_lower", new Pos(centerX, centerY + 80));
        } else if (recipeId.equals("freeze")) {
            // Freeze positions - angled line
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_freeze", new Pos(centerX - 60, centerY + 30));
            nodePositions.put("effect_potency", new Pos(centerX, centerY));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY - 30));
        } else if (recipeId.equals("heal")) {
            // Heal positions - gentle curve
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_heal", new Pos(centerX - 70, centerY));
            nodePositions.put("effect_time", new Pos(centerX, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("attack_1")) {
            // Attack_1 positions - simple horizontal
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_atk", new Pos(centerX - 80, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_atk2", new Pos(centerX + 80, centerY));
        } else if (recipeId.equals("attack_2")) {
            // Attack_2 positions - asymmetrical branching
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_atk2", new Pos(centerX - 130, centerY));
            nodePositions.put("effect_root_atk2", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_branch", new Pos(centerX, centerY - 50));
            nodePositions.put("effect_branch", new Pos(centerX + 30, centerY + 50));
            nodePositions.put("quality_upper", new Pos(centerX + 70, centerY - 50));
            nodePositions.put("quality_lower", new Pos(centerX + 100, centerY + 50));
            nodePositions.put("morph_seal", new Pos(centerX, centerY));
            nodePositions.put("morph_confuse", new Pos(centerX + 80, centerY));
            nodePositions.put("recipe_unlock_atk3", new Pos(centerX + 170, centerY));
        } else if (recipeId.equals("attack_3")) {
            // Attack_3 positions - aggressive, sharp-angled layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_atk3", new Pos(centerX - 100, centerY));
            nodePositions.put("effect_root_atk3", new Pos(centerX - 30, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 40, centerY - 60));
            nodePositions.put("trait_lower_branch", new Pos(centerX + 40, centerY + 60));
            nodePositions.put("quality_upper", new Pos(centerX + 120, centerY - 60));
            nodePositions.put("quality_lower", new Pos(centerX + 120, centerY + 60));
            nodePositions.put("effect_final_boost", new Pos(centerX + 120, centerY + 20));
            nodePositions.put("morph_strike", new Pos(centerX + 120, centerY - 20));
        } else if (recipeId.equals("seal")) {
            // Seal positions - simple fork
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_seal", new Pos(centerX - 50, centerY));
            nodePositions.put("effect_duration", new Pos(centerX + 40, centerY - 30));
            nodePositions.put("quality_node", new Pos(centerX + 40, centerY + 30));
        } else if (recipeId.equals("confuse")) {
            // Confuse positions - zig-zag
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_confuse", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_potency", new Pos(centerX, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY));
        } else if (recipeId.equals("strike")) {
            // Strike positions - balanced fork
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_strike", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_node", new Pos(centerX + 20, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 20, centerY + 40));
        } else if (recipeId.equals("shield_1")) {
            // Shield_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_shield", new Pos(centerX - 75, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_shield2", new Pos(centerX + 75, centerY));
        } else if (recipeId.equals("shield_2")) {
            // Shield_2 positions - wide and simple
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_shield2", new Pos(centerX - 100, centerY));
            nodePositions.put("effect_root_shield2", new Pos(centerX - 30, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 40, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 110, centerY - 40));
            nodePositions.put("morph_sleep", new Pos(centerX + 40, centerY + 40));
            nodePositions.put("recipe_unlock_shield3", new Pos(centerX + 110, centerY + 40));
        } else if (recipeId.equals("shield_3")) {
            // Shield_3 positions - a full, protective circle-like layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_shield3", new Pos(centerX, centerY));
            nodePositions.put("effect_root_shield3", new Pos(centerX, centerY - 60));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 70, centerY - 60));
            nodePositions.put("quality_upper", new Pos(centerX + 70, centerY));
            nodePositions.put("trait_lower_branch", new Pos(centerX - 70, centerY - 60));
            nodePositions.put("quality_lower", new Pos(centerX - 70, centerY));
        } else if (recipeId.equals("evade_1")) {
            // Evade_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_evade", new Pos(centerX - 70, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_evade2", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("evade_2")) {
            // Evade_2 positions - a 'Z' flow
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_evade2", new Pos(centerX - 90, centerY - 40));
            nodePositions.put("effect_root_evade2", new Pos(centerX, centerY - 40));
            nodePositions.put("trait_branch", new Pos(centerX, centerY + 40));
            nodePositions.put("morph_scent", new Pos(centerX + 90, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX - 90, centerY + 40));
            nodePositions.put("recipe_unlock_evade3", new Pos(centerX + 90, centerY + 40));
        } else if (recipeId.equals("evade_3")) {
            // Evade_3 positions - a flowing "S" curve
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_evade3", new Pos(centerX - 100, centerY - 60));
            nodePositions.put("effect_root_evade3", new Pos(centerX - 30, centerY - 30));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 40, centerY));
            nodePositions.put("trait_lower_branch", new Pos(centerX - 30, centerY + 30));
            nodePositions.put("quality_upper", new Pos(centerX + 110, centerY + 30));
            nodePositions.put("quality_lower", new Pos(centerX + 40, centerY + 60));
        } else if (recipeId.equals("impede_1")) {
            // Impede_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_impede", new Pos(centerX - 75, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_impede2", new Pos(centerX + 75, centerY));
        } else if (recipeId.equals("impede_2")) {
            // Impede_2 positions - simple balanced fork
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_impede2", new Pos(centerX - 80, centerY));
            nodePositions.put("effect_root_impede2", new Pos(centerX, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 80, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 80, centerY + 40));
            nodePositions.put("recipe_unlock_impede3", new Pos(centerX + 150, centerY));
        } else if (recipeId.equals("impede_3")) {
            // Impede_3 positions - tall and linear
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_impede3", new Pos(centerX, centerY - 80));
            nodePositions.put("effect_root_impede3", new Pos(centerX, centerY - 30));
            nodePositions.put("trait_branch", new Pos(centerX, centerY + 20));
            nodePositions.put("quality_node", new Pos(centerX, centerY + 70));
        } else if (recipeId.equals("sleep")) {
            // Sleep positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_sleep", new Pos(centerX - 50, centerY + 30));
            nodePositions.put("effect_potency", new Pos(centerX, centerY - 20));
            nodePositions.put("quality_node", new Pos(centerX + 50, centerY + 30));
        } else if (recipeId.equals("scent")) {
            // Scent positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_scent", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_range", new Pos(centerX + 20, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 20, centerY + 40));
        } else if (recipeId.equals("action_1")) {
            // Action_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_action", new Pos(centerX - 80, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_action2", new Pos(centerX + 80, centerY));
        } else if (recipeId.equals("action_2")) {
            // Action_2 positions - multi-fork layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_action2", new Pos(centerX - 120, centerY));
            nodePositions.put("effect_root_action2", new Pos(centerX - 50, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 20, centerY - 50));
            nodePositions.put("morph_branch", new Pos(centerX + 20, centerY + 50));
            nodePositions.put("quality_upper", new Pos(centerX + 90, centerY - 70));
            nodePositions.put("recipe_unlock_action3", new Pos(centerX + 160, centerY - 50));
            nodePositions.put("morph_blind", new Pos(centerX + 90, centerY + 30));
            nodePositions.put("morph_deathblow", new Pos(centerX + 90, centerY + 70));
        } else if (recipeId.equals("action_3")) {
            // Action_3 positions - ladder layout
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_action3", new Pos(centerX - 90, centerY));
            nodePositions.put("effect_root_action3", new Pos(centerX - 30, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 30, centerY - 40));
            nodePositions.put("trait_lower_branch", new Pos(centerX + 30, centerY + 40));
            nodePositions.put("quality_upper", new Pos(centerX + 90, centerY - 40));
            nodePositions.put("quality_lower", new Pos(centerX + 90, centerY + 40));
        } else if (recipeId.equals("blind")) {
            // Blind positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_blind", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_potency", new Pos(centerX, centerY + 40));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY));
        } else if (recipeId.equals("cast_1")) {
            // Cast_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_cast", new Pos(centerX - 70, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_cast2", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("cast_2")) {
            // Cast_2 positions - compact fork
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_cast2", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_root_cast2", new Pos(centerX, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 60, centerY - 35));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY + 35));
        } else if (recipeId.equals("deathblow_1")) {
            // Deathblow_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_db1", new Pos(centerX - 60, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_db2", new Pos(centerX + 60, centerY));
        } else if (recipeId.equals("deathblow_2")) {
            // Deathblow_2 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_db2", new Pos(centerX - 70, centerY));
            nodePositions.put("effect_root_db2", new Pos(centerX, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 70, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 70, centerY + 40));
        } else if (recipeId.equals("move_1")) {
            // Move_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_move", new Pos(centerX - 70, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_move2", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("move_2")) {
            // Move_2 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_move2", new Pos(centerX - 90, centerY));
            nodePositions.put("effect_root_move2", new Pos(centerX - 30, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 30, centerY - 40));
            nodePositions.put("morph_range", new Pos(centerX + 30, centerY + 40));
            nodePositions.put("quality_node", new Pos(centerX + 90, centerY - 40));
            nodePositions.put("recipe_unlock_move3", new Pos(centerX + 90, centerY + 40));
        } else if (recipeId.equals("move_3")) {
            // Move_3 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_move3", new Pos(centerX, centerY + 70));
            nodePositions.put("effect_root_move3", new Pos(centerX, centerY + 20));
            nodePositions.put("trait_branch", new Pos(centerX, centerY - 30));
            nodePositions.put("quality_node", new Pos(centerX, centerY - 80));
        } else if (recipeId.equals("ep_cut_1")) {
            // EP_Cut_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_epcut", new Pos(centerX - 80, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_epcut2", new Pos(centerX + 80, centerY));
        } else if (recipeId.equals("ep_cut_2")) {
            // EP_Cut_2 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_epcut2", new Pos(centerX - 100, centerY));
            nodePositions.put("effect_root_epcut2", new Pos(centerX - 30, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 40, centerY - 40));
            nodePositions.put("morph_eagle_eye", new Pos(centerX + 40, centerY + 40));
            nodePositions.put("quality_node", new Pos(centerX + 110, centerY - 40));
            nodePositions.put("recipe_unlock_epcut3", new Pos(centerX + 110, centerY + 40));
        } else if (recipeId.equals("ep_cut_3")) {
            // EP_Cut_3 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_epcut3", new Pos(centerX - 140, centerY));
            nodePositions.put("effect_root_epcut3", new Pos(centerX - 70, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX, centerY - 50));
            nodePositions.put("trait_lower_branch", new Pos(centerX, centerY + 50));
            nodePositions.put("quality_upper", new Pos(centerX + 70, centerY - 50));
            nodePositions.put("quality_lower", new Pos(centerX + 70, centerY + 50));
        } else if (recipeId.equals("range_1")) {
            // Range_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_range", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_node", new Pos(centerX + 10, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 10, centerY + 40));
        } else if (recipeId.equals("eagle_eye")) {
            // Eagle_Eye positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_eagle", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_accuracy", new Pos(centerX, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY));
        } else if (recipeId.equals("ep_1")) {
            // EP_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_ep", new Pos(centerX - 80, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_ep2", new Pos(centerX + 80, centerY));
        } else if (recipeId.equals("ep_2")) {
            // EP_2 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_ep2", new Pos(centerX - 110, centerY));
            nodePositions.put("effect_root_ep2", new Pos(centerX - 40, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 30, centerY - 50));
            nodePositions.put("morph_branch", new Pos(centerX + 30, centerY + 50));
            nodePositions.put("quality_upper", new Pos(centerX + 100, centerY - 50));
            nodePositions.put("morph_info", new Pos(centerX + 100, centerY + 20));
            nodePositions.put("morph_haze", new Pos(centerX + 100, centerY + 80));
            nodePositions.put("recipe_unlock_ep3", new Pos(centerX + 170, centerY - 50));
        } else if (recipeId.equals("ep_3")) {
            // EP_3 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_ep3", new Pos(centerX - 130, centerY));
            nodePositions.put("effect_root_ep3", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX, centerY - 60));
            nodePositions.put("trait_lower_branch", new Pos(centerX, centerY + 60));
            nodePositions.put("quality_upper", new Pos(centerX + 70, centerY - 60));
            nodePositions.put("quality_lower", new Pos(centerX + 70, centerY + 60));
        } else if (recipeId.equals("hit_1")) {
            // Hit_1 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_hit", new Pos(centerX - 70, centerY));
            nodePositions.put("quality_boost", new Pos(centerX, centerY));
            nodePositions.put("recipe_unlock_hit2", new Pos(centerX + 70, centerY));
        } else if (recipeId.equals("hit_2")) {
            // Hit_2 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_hit2", new Pos(centerX - 90, centerY));
            nodePositions.put("effect_root_hit2", new Pos(centerX, centerY));
            nodePositions.put("trait_branch", new Pos(centerX + 70, centerY - 45));
            nodePositions.put("morph_cloak", new Pos(centerX + 70, centerY + 45));
            nodePositions.put("quality_node", new Pos(centerX + 140, centerY - 45));
            nodePositions.put("recipe_unlock_hit3", new Pos(centerX + 140, centerY));
        } else if (recipeId.equals("hit_3")) {
            // Hit_3 positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_hit3", new Pos(centerX - 110, centerY));
            nodePositions.put("effect_root_hit3", new Pos(centerX - 40, centerY));
            nodePositions.put("trait_upper_branch", new Pos(centerX + 30, centerY - 40));
            nodePositions.put("trait_lower_branch", new Pos(centerX + 30, centerY + 40));
            nodePositions.put("quality_upper", new Pos(centerX + 100, centerY - 40));
            nodePositions.put("quality_lower", new Pos(centerX + 100, centerY + 40));
        } else if (recipeId.equals("information")) {
            // Information positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_info", new Pos(centerX - 60, centerY));
            nodePositions.put("trait_node", new Pos(centerX, centerY - 40));
            nodePositions.put("quality_node", new Pos(centerX + 60, centerY));
        } else if (recipeId.equals("haze")) {
            // Haze positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_haze", new Pos(centerX - 60, centerY));
            nodePositions.put("effect_debuff", new Pos(centerX + 20, centerY + 40));
            nodePositions.put("quality_node", new Pos(centerX + 20, centerY - 40));
        } else if (recipeId.equals("cloak")) {
            // Cloak positions
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            nodePositions.put("start_cloak", new Pos(centerX, centerY + 50));
            nodePositions.put("effect_stealth", new Pos(centerX - 50, centerY - 20));
            nodePositions.put("quality_node", new Pos(centerX + 50, centerY - 20));
        } else {
            System.out.println("DEBUG init() - No matching recipe found for: " + recipeId + " - node positions will be empty");
        }

        }





    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int panelWidth = 80;
        pGuiGraphics.fill(5, 5, 5 + panelWidth, this.height - 5, 0xBF3B270A);
        pGuiGraphics.renderOutline(5, 5, panelWidth, this.height - 10, 0xFF1A1104);

// Only draw categories if loading is complete
        if (isLoadingComplete()) {
            drawCategoryDropdowns(pGuiGraphics);
        } else {
            // Draw "Loading..." text in the left panel during loading
            pGuiGraphics.drawString(this.font, Component.literal("Loading..."), 10, 50, 0xFFFFFFFF, false);
        }



// Render player inventory first (if in material insertion mode)
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            // Draw inventory background panel first - MOVED LEFT
            int invPanelX = this.width / 2 - 140; // Changed from -100
            int invPanelY = this.height / 2 + 30;
            int invPanelWidth = 200;
            int invPanelHeight = 120;

            pGuiGraphics.fill(invPanelX, invPanelY, invPanelX + invPanelWidth, invPanelY + invPanelHeight, 0xE01A3A4A);
            pGuiGraphics.renderOutline(invPanelX, invPanelY, invPanelWidth, invPanelHeight, 0xFFFFFFFF);

            // Draw inventory tabs

            // Temporarily adjust positions for proper inventory rendering
            int originalLeftPos = this.leftPos;
            int originalTopPos = this.topPos;

            // Position inventory inside the panel
            this.leftPos = invPanelX + 12;
            this.topPos = invPanelY + 10;

            // Render only matching inventory slots
            renderFilteredInventory(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

            // Restore original positions
            this.leftPos = originalLeftPos;
            this.topPos = originalTopPos;
        }


        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();

// New code - show button when at least one node is completed:
        boolean anyNodeCompleted = false;
        if (blockEntity != null && blockEntity.getActiveRecipeId() != null) {
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(blockEntity.getActiveRecipeId());
            if (recipe != null) {
                anyNodeCompleted = recipe.getNodes().keySet().stream()
                        .anyMatch(nodeId -> blockEntity.isNodeCompleted(nodeId));
            }
        }
        this.synthesisButton.visible = (currentState == ScreenState.SELECTING_RECIPE && anyNodeCompleted);

// Continue with the existing code...
        // Show loading screen during initialization, regardless of recipe state
        if (currentState == ScreenState.SELECTING_RECIPE) {
            if (isLoadingComplete()) {
                // Only draw recipe graph if we have a recipe AND loading is complete
                if (blockEntity != null && blockEntity.getActiveRecipeId() != null) {
                    drawRecipeGraph(pGuiGraphics);
                }
            } else {
                drawLoadingScreen(pGuiGraphics);
            }
        }


// Then render inventory panel on top (highest z-index)
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            drawInventoryPanel(pGuiGraphics);
            drawNodeSlots(pGuiGraphics);
        }

// Draw inventory tabs LAST for proper z-order
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            int invPanelX = this.width / 2 - 140;
            int invPanelY = this.height / 2 + 30;
            drawInventoryTabs(pGuiGraphics, invPanelX, invPanelY - 25);

            this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        }
        // Render synthesis button last so it appears on top
        if (this.synthesisButton.visible) {
            this.synthesisButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

    }
    // Replace the drawLoadingScreen method with this enhanced version
    private void drawLoadingScreen(GuiGraphics graphics) {
        long elapsed = System.currentTimeMillis() - initStartTime;
        float progress = Math.min(1.0f, elapsed / 3000.0f);

        // Cover the ENTIRE screen with a gradient background
        drawTechGradientBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw custom logo if available (you'll need to add your logo texture)
        drawCustomLogo(graphics, centerX, centerY - 100);

        // Enhanced loading text with glow effect
        String dots = getDynamicLoadingDots(elapsed);
        Component loadingText = Component.literal("Initializing Quartz Matrix" + dots);

        // Draw text with shadow and glow
        drawGlowText(graphics, loadingText, centerX, centerY - 40, 0xFF4A8FB8, 0xFF1A3A4A);

        // Enhanced armored hand progress bar
        drawArmoredHandProgressBar(graphics, centerX, centerY - 10, progress);

        // System status with typewriter effect
        drawSystemStatus(graphics, centerX, centerY + 30, elapsed, progress);

        // Enhanced tech circuit animation
        drawTechCircuitPattern(graphics, centerX, centerY, elapsed);

        // Add particle effects
        drawTechParticles(graphics, centerX, centerY, elapsed);

        // Play loading sounds (you'll need to implement sound triggering)
        playLoadingSounds(elapsed);
    }

    // Brownish-blue gradient background
    private void drawTechGradientBackground(GuiGraphics graphics) {
        int topColor = 0xFF2D1F15;    // Dark brown
        int middleColor = 0xFF1A3A4A; // Dark brownish-blue
        int bottomColor = 0xFF0F2A3A; // Darker blue

        int height = this.height;
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            int color;

            if (ratio < 0.5f) {
                // Top to middle
                float localRatio = ratio * 2;
                color = interpolateColor(topColor, middleColor, localRatio);
            } else {
                // Middle to bottom
                float localRatio = (ratio - 0.5f) * 2;
                color = interpolateColor(middleColor, bottomColor, localRatio);
            }

            graphics.fill(0, y, this.width, y + 1, color);
        }
    }

    // Custom logo rendering (add your logo texture path)
    private void drawCustomLogo(GuiGraphics graphics, int centerX, int centerY) {
        try {
            // Replace with your actual logo texture path
            ResourceLocation logoTexture = ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/quartz_logo.png");
            RenderSystem.setShaderTexture(0, logoTexture);

            // Pulsing logo effect
            long time = System.currentTimeMillis();
            float pulse = (float)(Math.sin(time * 0.003) * 0.1 + 0.9);
            RenderSystem.setShaderColor(pulse, pulse, pulse, 1.0f);

            // Draw logo (adjust size as needed)
            graphics.blit(logoTexture, centerX - 32, centerY - 32, 0, 0, 64, 64, 64, 64);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback: draw text logo
            Component logoText = Component.literal("KISEKI LEGEND");
            drawGlowText(graphics, logoText, centerX, centerY, 0xFF4A8FB8, 0xFF1A3A4A);
        }
    }

    // Dynamic loading dots animation
    private String getDynamicLoadingDots(long elapsed) {
        int dotCount = (int)((elapsed / 300) % 5); // Cycle every 1.5 seconds
        return switch (dotCount) {
            case 0 -> "";
            case 1 -> ".";
            case 2 -> "..";
            case 3 -> "...";
            case 4 -> "....";
            default -> "";
        };
    }

    // Glowing text effect
    private void drawGlowText(GuiGraphics graphics, Component text, int x, int y, int mainColor, int glowColor) {
        // Draw glow (larger, darker)
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX != 0 || offsetY != 0) {
                    graphics.drawCenteredString(this.font, text, x + offsetX, y + offsetY, glowColor);
                }
            }
        }

        // Draw main text
        graphics.drawCenteredString(this.font, text, x, y, mainColor);
    }

    // Armored hand progress bar
    private void drawArmoredHandProgressBar(GuiGraphics graphics, int centerX, int centerY, float progress) {
        int barWidth = 240;
        int barHeight = 24;
        int barX = centerX - barWidth / 2;
        int barY = centerY;

        // Draw hand outline (empty state)
        drawArmoredHandOutline(graphics, barX, barY, barWidth, barHeight);

        // Draw progress fill with armor segments
        if (progress > 0) {
            drawArmoredHandFill(graphics, barX, barY, barWidth, barHeight, progress);
        }

        // Draw progress percentage with custom positioning
        String progressText = String.format("%.0f%%", progress * 100);
        drawGlowText(graphics, Component.literal(progressText), centerX, centerY + barHeight + 8, 0xFF4A8FB8, 0xFF1A3A4A);
    }

    // Armored hand outline shape
    private void drawArmoredHandOutline(GuiGraphics graphics, int x, int y, int width, int height) {
        // Main body
        graphics.fill(x, y, x + width, y + height, 0xFF2D1F15);

        // Fingers (simplified geometric representation)
        int fingerWidth = width / 6;
        for (int i = 0; i < 4; i++) {
            int fingerX = x + (i + 1) * fingerWidth;
            int fingerY = y - 8;
            graphics.fill(fingerX, fingerY, fingerX + fingerWidth - 2, fingerY + 8, 0xFF2D1F15);
            graphics.renderOutline(fingerX, fingerY, fingerWidth - 2, 8, 0xFF4A3426);
        }

        // Thumb
        int thumbX = x - 12;
        int thumbY = y + height / 3;
        graphics.fill(thumbX, thumbY, thumbX + 12, thumbY + height / 2, 0xFF2D1F15);
        graphics.renderOutline(thumbX, thumbY, 12, height / 2, 0xFF4A3426);

        // Main outline
        graphics.renderOutline(x, y, width, height, 0xFF4A3426);
    }

    // Armored hand fill animation
    private void drawArmoredHandFill(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        int fillWidth = (int)(width * progress);

        // Segment the fill to look like armor plates
        int segmentCount = 8;
        int segmentWidth = fillWidth / segmentCount;

        for (int i = 0; i < segmentCount && i * segmentWidth < fillWidth; i++) {
            int segX = x + 2 + (i * (width / segmentCount));
            int segWidth = Math.min(segmentWidth - 2, fillWidth - (i * segmentWidth));

            if (segWidth > 0) {
                // Gradient from brownish to blue
                int segmentColor = interpolateColor(0xFF6B4423, 0xFF4A8FB8, (float)i / segmentCount);
                graphics.fill(segX, y + 2, segX + segWidth, y + height - 2, segmentColor);

                // Add metallic highlight
                graphics.fill(segX, y + 2, segX + segWidth, y + 4, 0xFF8BB8D8);
            }
        }

        // Fill fingers based on progress
        int fingerWidth = width / 6;
        int filledFingers = (int)(progress * 4);
        for (int i = 0; i < filledFingers; i++) {
            int fingerX = x + (i + 1) * fingerWidth;
            int fingerY = y - 8;
            graphics.fill(fingerX + 1, fingerY + 1, fingerX + fingerWidth - 3, fingerY + 7, 0xFF4A8FB8);
        }

        // Fill thumb if progress > 80%
        if (progress > 0.8f) {
            int thumbX = x - 12;
            int thumbY = y + height / 3;
            graphics.fill(thumbX + 1, thumbY + 1, thumbX + 11, thumbY + height / 2 - 1, 0xFF4A8FB8);
        }
    }

    // System status with typewriter effect
    private void drawSystemStatus(GuiGraphics graphics, int centerX, int centerY, long elapsed, float progress) {
        String[] statusMessages = {
                "Calibrating resonance frequencies...",
                "Synchronizing elemental matrices...",
                "Loading synthesis protocols...",
                "Establishing quantum links...",
                "Armoring systems online...",
                "Aeon System complete!"
        };

        int statusIndex = Math.min(statusMessages.length - 1, (int)(progress * statusMessages.length));
        String fullMessage = statusMessages[statusIndex];

        // Typewriter effect
        int visibleChars = (int)((elapsed % 2000) / 50); // 50ms per character
        String displayMessage = fullMessage.substring(0, Math.min(visibleChars, fullMessage.length()));

        // Position status text lower and with different styling
        drawGlowText(graphics, Component.literal(displayMessage), centerX, centerY, 0xFF88B8AA, 0xFF2D4A3A);
    }

    // Enhanced tech circuit pattern
    private void drawTechCircuitPattern(GuiGraphics graphics, int centerX, int centerY, long elapsed) {
        float pulse = (float)(Math.sin(elapsed * 0.008) * 0.3 + 0.7);

        // Draw multiple circuit rings
        drawCircuitRing(graphics, centerX, centerY, 100, elapsed * 0.001, 0xFF4A8FB8, pulse);
        drawCircuitRing(graphics, centerX, centerY, 140, elapsed * -0.0008, 0xFF6B7B88, pulse * 0.8f);
        drawCircuitRing(graphics, centerX, centerY, 180, elapsed * 0.0006, 0xFF4A6B58, pulse * 0.6f);

        // Central hub
        drawTechHub(graphics, centerX, centerY, pulse);
    }
    // Background circuit animation for recipe view
    private void drawBackgroundCircuitPattern(GuiGraphics graphics, int centerX, int centerY, long elapsed) {
        float pulse = (float)(Math.sin(elapsed * 0.003) * 0.2 + 0.3); // Slower and more subtle pulse

        // Draw multiple circuit rings with much lower opacity
        drawBackgroundCircuitRing(graphics, centerX, centerY, 150, elapsed * 0.0003, 0x20406080, pulse); // Much slower rotation
        drawBackgroundCircuitRing(graphics, centerX, centerY, 200, elapsed * -0.0002, 0x20304050, pulse * 0.8f);
        drawBackgroundCircuitRing(graphics, centerX, centerY, 250, elapsed * 0.0001, 0x20203040, pulse * 0.6f);

        // Central hub with very low opacity
        drawBackgroundTechHub(graphics, centerX, centerY, pulse);
    }

    // Individual circuit ring
    private void drawCircuitRing(GuiGraphics graphics, int centerX, int centerY, int radius, double rotation, int baseColor, float intensity) {
        int nodeCount = 12;
        for (int i = 0; i < nodeCount; i++) {
            double angle = rotation + (i * 2 * Math.PI / nodeCount);
            int x = centerX + (int)(Math.cos(angle) * radius);
            int y = centerY + (int)(Math.sin(angle) * radius);

            // Draw circuit nodes (hexagonal instead of circular)
            drawHexagonalNode(graphics, x, y, 3, baseColor, intensity);

            // Draw connecting lines
            if (i < nodeCount - 1) {
                double nextAngle = rotation + ((i + 1) * 2 * Math.PI / nodeCount);
                int nextX = centerX + (int)(Math.cos(nextAngle) * radius);
                int nextY = centerY + (int)(Math.sin(nextAngle) * radius);

                int lineColor = (int)(baseColor & 0x00FFFFFF) | ((int)(intensity * 128) << 24);
                drawTechLine(graphics, x, y, nextX, nextY, lineColor);
            }
        }
    }

    // Hexagonal circuit node
    private void drawHexagonalNode(GuiGraphics graphics, int centerX, int centerY, int size, int color, float intensity) {
        int glowColor = (int)((color & 0x00FFFFFF) | ((int)(intensity * 255) << 24));

        // Draw hexagon approximation
        for (int i = 0; i < 6; i++) {
            double angle1 = i * Math.PI / 3;
            double angle2 = (i + 1) * Math.PI / 3;

            int x1 = centerX + (int)(Math.cos(angle1) * size);
            int y1 = centerY + (int)(Math.sin(angle1) * size);
            int x2 = centerX + (int)(Math.cos(angle2) * size);
            int y2 = centerY + (int)(Math.sin(angle2) * size);

            drawTechLine(graphics, x1, y1, x2, y2, glowColor);
        }

        // Central glow
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, glowColor);
    }

    // Technical line drawing with thickness
    private void drawTechLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // Enhanced line drawing with anti-aliasing effect
        drawLine(graphics, x1, y1, x2, y2, color);
        drawLine(graphics, x1 + 1, y1, x2 + 1, y2, color & 0x7FFFFFFF); // Semi-transparent parallel line
    }

    // Central technology hub
    private void drawTechHub(GuiGraphics graphics, int centerX, int centerY, float pulse) {
        int hubSize = (int)(20 + pulse * 5);

        // Outer glow ring
        drawCircle(graphics, centerX, centerY, hubSize + 3, (int)(0x4A4A8FB8 | ((int)(pulse * 100) << 24)));

        // Main hub body
        graphics.fill(centerX - hubSize/2, centerY - hubSize/2,
                centerX + hubSize/2, centerY + hubSize/2, 0xFF2D4A5A);

        // Inner core
        int coreSize = hubSize / 3;
        graphics.fill(centerX - coreSize, centerY - coreSize,
                centerX + coreSize, centerY + coreSize,
                interpolateColor(0xFF4A8FB8, 0xFF8BB8D8, pulse));
    }

    // Floating tech particles
    private void drawTechParticles(GuiGraphics graphics, int centerX, int centerY, long elapsed) {
        Random random = new Random(elapsed / 1000); // Stable seed for consistent particles

        for (int i = 0; i < 20; i++) {
            float particleTime = (elapsed + i * 200) * 0.001f;
            float x = centerX + (float)(Math.sin(particleTime) * (100 + i * 10));
            float y = centerY + (float)(Math.cos(particleTime * 1.3) * (80 + i * 8));

            float alpha = (float)(Math.sin(particleTime * 2) * 0.3 + 0.4);
            int particleColor = 0xFF4A8FB8 | ((int)(alpha * 255) << 24);

            graphics.fill((int)x, (int)y, (int)x + 2, (int)y + 2, particleColor);
        }
    }

    private void playLoadingSounds(long elapsed) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Startup sound at 0.5 seconds
            if (elapsed >= 500 && !startupSoundPlayed) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        ModSoundEvents.QUARTZ_STARTUP.get(),
                        1.0f, // pitch
                        0.7f  // volume
                ));
                startupSoundPlayed = true;
            }

            // Processing sound at 1.5 seconds
            else if (elapsed >= 1500 && !processingSoundPlayed) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        ModSoundEvents.QUARTZ_PROCESSING.get(),
                        1.0f, // pitch
                        0.8f  // volume
                ));
                processingSoundPlayed = true;
            }

            // Completion sound at 2.8 seconds
            else if (elapsed >= 2800 && !completeSoundPlayed) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        ModSoundEvents.QUARTZ_COMPLETE.get(),
                        1.0f, // pitch
                        0.9f  // volume
                ));
                completeSoundPlayed = true;
            }

        } catch (Exception e) {
            System.out.println("Error playing loading sound: " + e.getMessage());
        }
    }
    private void resetLoadingSounds() {
        startupSoundPlayed = false;
        processingSoundPlayed = false;
        completeSoundPlayed = false;
    }
    // Utility: Color interpolation
    private int interpolateColor(int color1, int color2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    // Add this method for animated circuit graphics:
    private void drawCircuitPattern(GuiGraphics graphics, int centerX, int centerY, long elapsed) {
        float pulse = (float)(Math.sin(elapsed * 0.01) * 0.5 + 0.5);
        int glowColor = (int)(pulse * 255) << 16 | 0x4400AA; // Pulsing blue

        // Draw animated circuits around the loading area
        for (int i = 0; i < 8; i++) {
            double angle = (elapsed * 0.002) + (i * Math.PI / 4);
            int radius = 80;
            int x = centerX + (int)(Math.cos(angle) * radius);
            int y = centerY + (int)(Math.sin(angle) * radius);

            // Draw small circuit nodes
            graphics.fill(x - 2, y - 2, x + 2, y + 2, glowColor);

            // Draw connecting lines to center
            drawLine(graphics, centerX, centerY, x, y, glowColor | 0x80000000);
        }
    }
    // Background version of circuit ring with lower opacity
    private void drawBackgroundCircuitRing(GuiGraphics graphics, int centerX, int centerY, int radius, double rotation, int baseColor, float intensity) {
        int nodeCount = 16; // More nodes for denser pattern
        for (int i = 0; i < nodeCount; i++) {
            double angle = rotation + (i * 2 * Math.PI / nodeCount);
            int x = centerX + (int)(Math.cos(angle) * radius) + viewOffsetX; // Apply view offset
            int y = centerY + (int)(Math.sin(angle) * radius) + viewOffsetY; // Apply view offset

            // Draw background nodes with very low opacity
            drawBackgroundHexagonalNode(graphics, x, y, 2, baseColor, intensity * 0.3f);

            // Draw connecting lines with even lower opacity
            if (i < nodeCount - 1) {
                double nextAngle = rotation + ((i + 1) * 2 * Math.PI / nodeCount);
                int nextX = centerX + (int)(Math.cos(nextAngle) * radius) + viewOffsetX;
                int nextY = centerY + (int)(Math.sin(nextAngle) * radius) + viewOffsetY;

                int lineColor = (int)(baseColor & 0x00FFFFFF) | ((int)(intensity * 30) << 24); // Very low alpha
                drawTechLine(graphics, x, y, nextX, nextY, lineColor);
            }
        }
    }

    private void drawBackgroundHexagonalNode(GuiGraphics graphics, int centerX, int centerY, int size, int color, float intensity) {
        int glowColor = (int)((color & 0x00FFFFFF) | ((int)(intensity * 60) << 24)); // Very low alpha

        // Draw smaller hexagon for background
        for (int i = 0; i < 6; i++) {
            double angle1 = i * Math.PI / 3;
            double angle2 = (i + 1) * Math.PI / 3;

            int x1 = centerX + (int)(Math.cos(angle1) * size);
            int y1 = centerY + (int)(Math.sin(angle1) * size);
            int x2 = centerX + (int)(Math.cos(angle2) * size);
            int y2 = centerY + (int)(Math.sin(angle2) * size);

            drawTechLine(graphics, x1, y1, x2, y2, glowColor);
        }

        // Very subtle central glow
        graphics.fill(centerX, centerY, centerX + 1, centerY + 1, glowColor);
    }

    private void drawBackgroundTechHub(GuiGraphics graphics, int centerX, int centerY, float pulse) {
        int hubSize = (int)(12 + pulse * 3); // Smaller hub

        // Apply view offset to hub position
        int adjustedX = centerX + viewOffsetX;
        int adjustedY = centerY + viewOffsetY;

        // Very subtle outer glow ring
        drawCircle(graphics, adjustedX, adjustedY, hubSize + 2, (int)(0x10304050 | ((int)(pulse * 40) << 24)));

        // Subtle main hub body
        graphics.fill(adjustedX - hubSize/2, adjustedY - hubSize/2,
                adjustedX + hubSize/2, adjustedY + hubSize/2, 0x20304050);

        // Very subtle inner core
        int coreSize = hubSize / 3;
        graphics.fill(adjustedX - coreSize, adjustedY - coreSize,
                adjustedX + coreSize, adjustedY + coreSize, 0x30405060);
    }
    // Update your onRecipeChanged method to handle loading state:
    public void onRecipeChanged() {
        // Always update node positions when recipe changes, regardless of loading state
        updateNodePositions();
    }
    // 5. Add this new method to draw inventory tabs:
    private void drawInventoryTabs(GuiGraphics graphics, int panelX, int tabY) {
        List<String> materialTypes = getAllMaterialTypes();
        int totalTabs = materialTypes.size();
        int maxVisibleTabs = Math.min(MAX_VISIBLE_TABS, totalTabs);

        // Ensure scroll offset is valid
        int maxScrollOffset = Math.max(0, totalTabs - MAX_VISIBLE_TABS);
        inventoryTabScrollOffset = Math.max(0, Math.min(inventoryTabScrollOffset, maxScrollOffset));

        // Draw scroll left arrow if needed
        if (inventoryTabScrollOffset > 0) {
            graphics.fill(panelX - 15, tabY, panelX - 5, tabY + 20, 0xFF666666);
            graphics.drawCenteredString(this.font, Component.literal("<"), panelX - 10, tabY + 6, 0xFFFFFFFF);
        }

        // Draw scroll right arrow if needed
        if (inventoryTabScrollOffset < maxScrollOffset) {
            int rightArrowX = panelX + (maxVisibleTabs * 25) + 5;
            graphics.fill(rightArrowX, tabY, rightArrowX + 10, tabY + 20, 0xFF666666);
            graphics.drawCenteredString(this.font, Component.literal(">"), rightArrowX + 5, tabY + 6, 0xFFFFFFFF);
        }

        // Draw visible tabs
        for (int i = 0; i < maxVisibleTabs; i++) {
            int tabIndex = inventoryTabScrollOffset + i;
            if (tabIndex >= totalTabs) break;

            int tabX = panelX + (i * 25);
            boolean isSelected = tabIndex == selectedInventoryTab;

            // Draw tab background
            int tabColor = isSelected ? 0xFF4A3426 : 0xFF2D1F15;
            graphics.fill(tabX, tabY, tabX + 23, tabY + 20, tabColor);
            graphics.renderOutline(tabX, tabY, 23, 20, 0xFF666666);

            // Draw tab text (abbreviated) - SMALLER FONT
            String materialType = materialTypes.get(tabIndex);
            String tabText = getTabDisplayName(materialType);
            int textColor = isSelected ? 0xFFFFAA00 : 0xFFFFFFFF;

// Use a smaller scale for text
            graphics.pose().pushPose();
            graphics.pose().scale(0.7f, 0.7f, 1.0f);
            graphics.drawCenteredString(this.font, Component.literal(tabText),
                    (int)((tabX + 11) / 0.7f), (int)((tabY + 7) / 0.7f), textColor);
            graphics.pose().popPose();
        }

    }

    // In QuartzMachineScreen.java

    private void drawCategoryDropdowns(GuiGraphics graphics) {
        // Don't draw anything during loading
        // Calculate total lines needed
        int totalLines = 0;
        for (String category : elementalCategories) {
            totalLines++; // Category header
            Boolean expanded = categoryExpanded.get(category);
            if (expanded != null && expanded) {
                List<QuartzCraftingRecipe> recipes = categoryRecipes.get(category);
                if (recipes != null) {
                    totalLines += recipes.size();
                }
            }
        }


        // Update max scroll offset with more generous scrolling
        int availableLines = Math.min(VISIBLE_LINES, (this.height - 40) / 12); // Dynamic based on screen height
        maxScrollOffset = Math.max(0, totalLines - availableLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        // Set up clipping for the scrollable area
        int panelX = 5;
        int panelY = 35; // Moved down to make room for progress bar
        int panelWidth = 80;
        int panelHeight = availableLines * 12; // Dynamic height

        // Draw progress bar at the top
        drawOverallProgressBar(graphics, panelX, 5, panelWidth);

        // Draw scroll background
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xBF3B270A);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF1A1104);

        // Enable scissor test for clipping
        graphics.enableScissor(panelX, panelY, panelX + panelWidth, panelY + panelHeight);

        int currentLine = 0;
        int drawY = panelY - (scrollOffset * 12);

        for (String category : elementalCategories) {
            Boolean expanded = categoryExpanded.get(category);
            boolean isExpanded = expanded != null && expanded;

            // Draw category header if it's visible
            if (currentLine >= scrollOffset && currentLine < scrollOffset + availableLines) {
                String indicator = isExpanded ? "▼ " : "► ";
                Component categoryText = Component.literal(indicator + category);
                graphics.drawString(this.font, categoryText, 4, drawY + 2, 0xFFFFFFFF, true);
            }

            drawY += 12;
            currentLine++;

            // Draw recipes if expanded
            if (isExpanded) {
                List<QuartzCraftingRecipe> recipes = categoryRecipes.get(category);
                if (recipes != null) {
                    for (QuartzCraftingRecipe recipe : recipes) {
                        if (currentLine >= scrollOffset && currentLine < scrollOffset + availableLines) {
                            String resultId = recipe.getResult();
                            String displayName = resultId.substring(resultId.lastIndexOf(':') + 1);
                            Component name = Component.literal("    " + displayName);

                            QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
                            boolean isUnlocked = isRecipeUnlocked(recipe);
                            int color;

                            if (!isUnlocked) {
                                color = 0xFF666666;
                                name = Component.literal("  🔒 " + displayName);
                            } else if (blockEntity != null && recipe.getId().equals(blockEntity.getActiveRecipeId())) {
                                color = 0xFFFFAA00; // Orange for selected
                                name = Component.literal("  ★ " + displayName); // Star for active
                            } else {
                                color = 0xFF88FF88; // Light green for unlocked
                                name = Component.literal("  ✓ " + displayName); // Checkmark for unlocked
                            }
                            graphics.drawString(this.font, name, 8, drawY + 2, color, false); // Moved left and down
                        }
                        drawY += 12;
                        currentLine++;
                    }
                }
            }
        }
        // Disable scissor test
        graphics.disableScissor();

        // Enhanced scroll indicator
        if (maxScrollOffset > 0) {
            drawEnhancedScrollIndicator(graphics, panelX + panelWidth - 8, panelY, panelHeight);
        }
    }
    private void drawOverallProgressBar(GuiGraphics graphics, int x, int y, int width) {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getActiveRecipeId() == null) {
            return;
        }

        QuartzCraftingRecipe activeRecipe = KisekiLegend.getQuartzRecipeManager().getRecipe(blockEntity.getActiveRecipeId());
        if (activeRecipe == null) {
            return;
        }

        // Calculate progress for active recipe
        int totalNodes = activeRecipe.getNodes().size();
        int completedNodes = 0;

        for (String nodeId : activeRecipe.getNodes().keySet()) {
            if (blockEntity.isNodeCompleted(nodeId)) {
                completedNodes++;
            }
        }

        float progress = totalNodes > 0 ? (float) completedNodes / totalNodes : 0f;

        // Draw progress bar background
        int barHeight = 20;
        graphics.fill(x, y, x + width, y + barHeight, 0xFF2D1F15);
        graphics.renderOutline(x, y, width, barHeight, 0xFF666666);

        // Draw progress bar fill
        if (progress > 0) {
            int fillWidth = (int) ((width - 2) * progress);
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + barHeight - 1, 0xFF00AA00);
        }

        // Draw progress text
        String progressText = completedNodes + "/" + totalNodes;
        int textX = x + (width / 2) - (this.font.width(progressText) / 2);
        int textY = y + 6;
        graphics.drawString(this.font, Component.literal(progressText), textX, textY, 0xFFFFFFFF, true);
    }

    private void drawEnhancedScrollIndicator(GuiGraphics graphics, int x, int y, int height) {
        // Draw scroll track
        graphics.fill(x, y, x + 6, y + height, 0xFF444444);
        graphics.renderOutline(x, y, 6, height, 0xFF666666);

        // Calculate scroll thumb position and size
        int availableLines = Math.min(VISIBLE_LINES, (this.height - 40) / 12);
        float scrollPercentage = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        float thumbHeight = Math.max(15, height * 0.3f); // Minimum thumb size
        float thumbY = y + (height - thumbHeight) * scrollPercentage;

        // Draw scroll thumb with gradient effect
        graphics.fill(x + 1, (int) thumbY, x + 5, (int) (thumbY + thumbHeight), 0xFFAAAAAA);
        graphics.renderOutline(x + 1, (int) thumbY, 4, (int) thumbHeight, 0xFFCCCCCC);
    }
    private void drawScrollIndicator(GuiGraphics graphics, int x, int y, int height) {
        // Draw scroll track
        graphics.fill(x, y, x + 6, y + height, 0xFF333333);

        // Calculate scroll thumb position and size
        float scrollPercentage = (float) scrollOffset / maxScrollOffset;
        float thumbHeight = Math.max(10, height * ((float) VISIBLE_LINES / (VISIBLE_LINES + maxScrollOffset)));
        float thumbY = y + (height - thumbHeight) * scrollPercentage;

        // Draw scroll thumb
        graphics.fill(x + 1, (int) thumbY, x + 5, (int) (thumbY + thumbHeight), 0xFF888888);
    }

    private void drawInventoryPanel(GuiGraphics graphics) {
        int panelWidth = 140;
        int panelHeight = 120;

        // Center the panel properly - MOVED LEFT
        this.leftPos = this.width / 2 - panelWidth / 2 - 40; // Added -40
        this.topPos = this.height / 2 - panelHeight / 2 - 20;

        // Draw dark brown background with higher alpha for visibility
        graphics.fill(this.leftPos, this.topPos, this.leftPos + panelWidth, this.topPos + panelHeight, 0xE01A3A4A);
        graphics.renderOutline(this.leftPos, this.topPos, panelWidth, panelHeight, 0xFFFFFFFF);

        this.imageWidth = panelWidth;
        this.imageHeight = panelHeight;

        // Draw title at proper position
        graphics.drawCenteredString(this.font, Component.literal("Select Material"),
                this.leftPos + panelWidth / 2, this.topPos + 8, 0xFFFFFFFF);
    }
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        // Check if mouse is over the left panel with expanded bounds
        if (pMouseX >= 5 && pMouseX <= 85 && pMouseY >= 15 && pMouseY <= this.height - 25) {
            int scrollDirection = pScrollY > 0 ? -3 : 3; // Faster scrolling, 3 lines at a time
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + scrollDirection));
            return true;
        }

        // Also handle inventory panel scrolling if in material insertion mode
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            int invPanelX = this.width / 2 - 140;
            int invPanelY = this.height / 2 + 30;
            if (pMouseX >= invPanelX && pMouseX <= invPanelX + 200 &&
                    pMouseY >= invPanelY && pMouseY <= invPanelY + 120) {
                // Handle inventory tab scrolling
                if (pScrollY > 0 && selectedInventoryTab > 0) {
                    selectedInventoryTab--;
                    return true;
                } else if (pScrollY < 0 && selectedInventoryTab < getAllMaterialTypes().size() - 1) {
                    selectedInventoryTab++;
                    return true;
                }
            }
        }

        return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
    }
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (isDragging && currentState == ScreenState.SELECTING_RECIPE) {
            viewOffsetX += (int) (pMouseX - lastMouseX);
            viewOffsetY += (int) (pMouseY - lastMouseY);
            lastMouseX = (int) pMouseX;
            lastMouseY = (int) pMouseY;
            return true;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            isDragging = false;
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        System.out.println("Mouse clicked at: " + pMouseX + ", " + pMouseY + ", button: " + pButton + ", state: " + currentState);

        // Synthesis button - highest priority
        if (this.synthesisButton.visible && this.synthesisButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }

        // Dragging for recipe view
        if (currentState == ScreenState.SELECTING_RECIPE && pButton == 0 && pMouseX > 85) {
            isDragging = true;
            lastMouseX = (int) pMouseX;
            lastMouseY = (int) pMouseY;
            return true;
        }

        // Category dropdown clicks
        if (pMouseX >= 10 && pMouseX <= 85 && pMouseY >= 15) {
            return handleCategoryClick(pMouseX, pMouseY, pButton);
        }

        // Material insertion UI clicks
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            // Tab clicks
            if (handleTabClicks(pMouseX, pMouseY, pButton)) {
                return true;
            }

            // Right-click to remove items
            if (pButton == 1) {
                return handleItemRemoval(pMouseX, pMouseY);
            }

            // Left-click to insert items
            if (pButton == 0) {
                return handleItemInsertion(pMouseX, pMouseY);
            }
        }

        // Node clicks in recipe view
        if (currentState == ScreenState.SELECTING_RECIPE) {
            return handleNodeClicks(pMouseX, pMouseY, pButton);
        }

        return false;
    }
    private boolean handleItemRemoval(double pMouseX, double pMouseY) {
        int slotStartX = this.leftPos + 5;
        int slotY = this.topPos + 47;

        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && selectedNodeId != null && selectedMaterialType != null) {
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(blockEntity.getActiveRecipeId());
            if (recipe != null) {
                QuartzCraftingRecipe.Node nodeData = recipe.getNode(selectedNodeId);
                if (nodeData != null) {
                    int requiredCount = nodeData.getMaterialRequirements().getOrDefault(selectedMaterialType, 0);

                    for (int i = 0; i < requiredCount; i++) {
                        int itemSlotX = slotStartX + (i * 18);
                        int itemSlotY = slotY;

                        if (pMouseX >= itemSlotX - 2 && pMouseX <= itemSlotX + 18 &&
                                pMouseY >= itemSlotY - 2 && pMouseY <= itemSlotY + 18) {
                            NetworkHandler.sendToServer(new RemoveMaterialPacket(blockEntity.getBlockPos(), selectedNodeId, selectedMaterialType));
                            return true;
                        }
                    }
                }
            }

            // Right-clicked outside slots - close panel
            currentState = ScreenState.SELECTING_RECIPE;
            selectedNodeId = null;
            selectedMaterialType = null;
            return true;
        }
        return false;
    }
    private boolean handleItemInsertion(double pMouseX, double pMouseY) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInventoryClickTime < 200) {
            return true;
        }

        int invPanelX = this.width / 2 - 140;
        int invPanelY = this.height / 2 + 30;
        int tempLeftPos = invPanelX + 12;
        int tempTopPos = invPanelY + 10;

        if (pMouseX >= tempLeftPos && pMouseX <= tempLeftPos + (9 * 18) &&
                pMouseY >= tempTopPos && pMouseY <= tempTopPos + (4 * 18)) {

            int relativeX = (int) pMouseX - tempLeftPos;
            int relativeY = (int) pMouseY - tempTopPos;
            int col = relativeX / 18;
            int row = relativeY / 18;
            int displaySlot = row * 9 + col;

            Integer actualSlot = filteredSlotMapping.get(displaySlot);
            if (actualSlot != null && selectedNodeId != null && selectedMaterialType != null) {
                QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
                if (blockEntity != null) {
                    ItemStack itemInSlot = minecraft.player.getInventory().getItem(actualSlot);
                    if (!itemInSlot.isEmpty() && itemMatchesSelectedTab(itemInSlot)) {
                        // Play item insertion sound
                        playUISound(ModSoundEvents.UI_SPIRAL_TICK.get(), 1.3f, 0.6f);

                        lastInventoryClickTime = currentTime;
                        System.out.println("Sending insert packet - slot: " + actualSlot + ", item: " + itemInSlot);

                        NetworkHandler.sendToServer(new InsertMaterialPacket(
                                blockEntity.getBlockPos(), selectedNodeId, actualSlot, selectedMaterialType));
                    } else {
                        System.out.println("Item validation failed - empty: " + itemInSlot.isEmpty() +
                                ", matches: " + itemMatchesSelectedTab(itemInSlot));
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleNodeClicks(double pMouseX, double pMouseY, int pButton) {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getActiveRecipeId() != null) {
            ResourceLocation recipeId = blockEntity.getActiveRecipeId();
            QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(recipeId);
            if (recipe != null) {
                Set<String> unlocked = blockEntity.getUnlockedNodes();

                for (String nodeId : unlocked) {
                    Pos nodePos = nodePositions.get(nodeId);
                    if (nodePos == null) continue;

                    int adjustedNodeX = nodePos.x + viewOffsetX;
                    int adjustedNodeY = nodePos.y + viewOffsetY;

                    double nodeDistance = Math.sqrt(Math.pow(pMouseX - adjustedNodeX, 2) + Math.pow(pMouseY - adjustedNodeY, 2));
                    if (nodeDistance <= 20) {
                        QuartzCraftingRecipe.Node nodeData = recipe.getNode(nodeId);
                        if (nodeData != null && !nodeData.getMaterialRequirements().isEmpty()) {
                            // Play node selection sound
                            playUISound(ModSoundEvents.UI_RINGS_ENGAGE.get(), 1.1f, 0.7f);

                            String firstMaterialType = nodeData.getMaterialRequirements().keySet().iterator().next();
                            selectedNodeId = nodeId;
                            selectedMaterialType = firstMaterialType;
                            currentState = ScreenState.INSERTING_MATERIAL;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    private boolean handleCategoryClick(double pMouseX, double pMouseY, int pButton) {
        // Define the clickable area for the recipe list
        int panelX = 10;
        int panelY = 35;
        int panelWidth = 75;
        int panelHeight = Math.min(VISIBLE_LINES, (this.height - 40) / 12) * 12;

        // Use dynamic panel height
        int availableLines = Math.min(VISIBLE_LINES, (this.height - 40) / 12);
        int dynamicPanelHeight = availableLines * 12;

        // Ignore clicks outside the recipe panel
        if (pMouseX < panelX || pMouseX > panelX + panelWidth || pMouseY < panelY || pMouseY > panelY + dynamicPanelHeight) {
            return false;
        }

        // Calculate which line was clicked, accounting for the scroll offset
        int clickedLine = (int)((pMouseY - panelY) / 12) + scrollOffset;
        int currentLine = 0;

        for (String category : elementalCategories) {
            // Check for click on the category header itself
            if (currentLine == clickedLine) {
                // Play category expand/collapse sound
                playUISound(ModSoundEvents.UI_ELEMENT_HOVER_TICK.get(), 1.2f, 0.5f);

                categoryExpanded.put(category, !categoryExpanded.get(category));
                return true;
            }
            currentLine++;

            // If the category is expanded, check its recipes
            if (categoryExpanded.get(category)) {
                List<QuartzCraftingRecipe> recipes = categoryRecipes.get(category);
                for (QuartzCraftingRecipe recipe : recipes) {
                    if (currentLine == clickedLine) {
                        // This is the clicked recipe
                        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
                        if (blockEntity != null && isRecipeUnlocked(recipe)) {
                            // Play recipe selection sound
                            playUISound(ModSoundEvents.UI_ART_SELECT_CHIME.get(), 1.0f, 0.8f);

                            // Send the selection packet to the server.
                            NetworkHandler.sendToServer(new SelectQuartzRecipePacket(recipe.getId(), blockEntity.getBlockPos()));
                        } else {
                            // Play locked sound for locked recipes
                            playUISound(ModSoundEvents.QUARTZ_ERROR.get(), 0.8f, 0.6f);
                        }
                        return true;
                    }
                    currentLine++;
                }
            }
        }
        return false;
    }
    private void onSynthesisButtonPressed(Button button) {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            // Play synthesis start sound
            playUISound(ModSoundEvents.QUARTZ_PROCESSING.get(), 1.0f, 1.0f);

            NetworkHandler.sendToServer(new StartSynthesisPacket(blockEntity.getBlockPos()));
        }
    }
    private boolean handleTabClicks(double pMouseX, double pMouseY, int pButton) {
        int invPanelX = this.width / 2 - 140;
        int invPanelY = this.height / 2 + 30;
        int tabY = invPanelY - 25;

        List<String> materialTypes = getAllMaterialTypes();
        int maxVisibleTabs = Math.min(MAX_VISIBLE_TABS, materialTypes.size());

        if (pButton == 0) {
            // Left arrow
            if (pMouseX >= invPanelX - 15 && pMouseX <= invPanelX - 5 &&
                    pMouseY >= tabY && pMouseY <= tabY + 20 && inventoryTabScrollOffset > 0) {
                inventoryTabScrollOffset--;
                return true;
            }

            // Right arrow
            int rightArrowX = invPanelX + (maxVisibleTabs * 25) + 5;
            int maxScrollOffset = Math.max(0, materialTypes.size() - MAX_VISIBLE_TABS);
            if (pMouseX >= rightArrowX && pMouseX <= rightArrowX + 10 &&
                    pMouseY >= tabY && pMouseY <= tabY + 20 && inventoryTabScrollOffset < maxScrollOffset) {
                inventoryTabScrollOffset++;
                return true;
            }

            // Tab clicks
            for (int i = 0; i < maxVisibleTabs; i++) {
                int tabIndex = inventoryTabScrollOffset + i;
                if (tabIndex >= materialTypes.size()) break;

                int tabX = invPanelX + (i * 25);
                if (pMouseX >= tabX && pMouseX <= tabX + 23 && pMouseY >= tabY && pMouseY <= tabY + 20) {
                    selectedInventoryTab = tabIndex;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void slotClicked(net.minecraft.world.inventory.Slot pSlot, int pSlotId, int pMouseButton, net.minecraft.world.inventory.ClickType pType) {
        System.out.println("slotClicked - state: " + currentState + ", selectedNodeId: " + selectedNodeId);
        System.out.println("Clicked slot ID: " + pSlotId + ", item: " + (pSlot != null ? pSlot.getItem() : "null"));

        if (currentState == ScreenState.INSERTING_MATERIAL) {
            // Don't let the regular slot system handle clicks in material insertion mode
            // All inventory clicks should go through our custom click handler in mouseClicked
            return;
        }

        // Only call super for non-material insertion states
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
    }
    // Add this helper method to convert menu slot IDs to inventory slots
    private int convertMenuSlotToInventorySlot(int menuSlotId) {
        // This assumes your menu follows standard slot ordering:
        // Slots 0-35 are player inventory (9-35 main inventory, 0-8 hotbar)
        if (menuSlotId >= 0 && menuSlotId <= 35) {
            if (menuSlotId >= 9 && menuSlotId <= 35) {
                // Main inventory slots (convert from menu ordering to inventory ordering)
                return menuSlotId;
            } else if (menuSlotId >= 0 && menuSlotId <= 8) {
                // Hotbar slots
                return menuSlotId;
            }
        }
        return -1; // Invalid slot
    }
    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        // Intentionally empty
    }
    private void drawNodeSlots(GuiGraphics graphics) {
        if (selectedNodeId == null || selectedMaterialType == null) return;

        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getActiveRecipeId() == null) return;

        // Create a small panel for the slots
        int slotPanelWidth = 120;
        int slotPanelHeight = 40;
        int slotPanelX = this.leftPos + 10;
        int slotPanelY = this.topPos + 25;

        // Draw slot panel background
        graphics.fill(slotPanelX, slotPanelY, slotPanelX + slotPanelWidth, slotPanelY + slotPanelHeight, 0xE01A3A4A);
        graphics.renderOutline(slotPanelX, slotPanelY, slotPanelWidth, slotPanelHeight, 0xFF2D1F15);

        // Draw material type label
        String displayName = formatMaterialTypeName(selectedMaterialType);
        graphics.drawString(this.font, Component.literal("Required:"),
                slotPanelX + 2, slotPanelY + 2, 0xFFFFD700, false);
        graphics.drawString(this.font, Component.literal(displayName),
                slotPanelX + 2, slotPanelY + 12, 0xFFFFFFFF, false);

        // Position slots inside the panel
        int slotStartX = slotPanelX + 5;
        int slotY = slotPanelY + 22;

        // Get stored items for this node and material type - CLIENT SAFE VERSION
        CompoundTag nodeStoredItems = blockEntity.getStoredItems().getCompound(selectedNodeId);
        ListTag storedList = nodeStoredItems.getList(selectedMaterialType, 10);

        // Draw node slots horizontally
        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(blockEntity.getActiveRecipeId());
        if (recipe != null) {
            QuartzCraftingRecipe.Node nodeData = recipe.getNode(selectedNodeId);
            if (nodeData != null) {
                int requiredCount = nodeData.getMaterialRequirements().getOrDefault(selectedMaterialType, 0);

                for (int i = 0; i < requiredCount; i++) {
                    int itemSlotX = slotStartX + (i * 18);
                    int itemSlotY = slotY;

                    // Draw standard Minecraft slot appearance
                    graphics.fill(itemSlotX - 1, itemSlotY - 1, itemSlotX + 17, itemSlotY + 17, 0xFF373737);
                    graphics.fill(itemSlotX, itemSlotY, itemSlotX + 16, itemSlotY + 16, 0xFF8B8B8B);

                    // CLIENT-SAFE ITEM RENDERING
                    if (i < storedList.size()) {
                        try {
                            CompoundTag itemTag = storedList.getCompound(i);

                            if (!itemTag.isEmpty() && itemTag.contains("id")) {
                                ItemStack storedItem = parseItemStackSafely(itemTag);

                                if (!storedItem.isEmpty()) {
                                    graphics.renderItem(storedItem, itemSlotX, itemSlotY);
                                    if (storedItem.getCount() > 1) {
                                        graphics.renderItemDecorations(this.font, storedItem, itemSlotX, itemSlotY);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Error rendering stored item in slot " + i + ": " + e.getMessage());
                        }
                    }
                }

                // Show progress next to slots
                graphics.drawString(this.font, Component.literal(storedList.size() + "/" + requiredCount),
                        slotStartX + (requiredCount * 18) + 5, slotY + 2, 0xFFFFFFFF, false);
            }
        }
    }
    // Add this new helper method to QuartzMachineScreen:
    private ItemStack parseItemStackSafely(CompoundTag itemTag) {
        try {
            // Try registry-based parsing first
            if (minecraft.level != null && minecraft.level.registryAccess() != null) {
                ItemStack parsed = ItemStack.parseOptional(minecraft.level.registryAccess(), itemTag);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }

            // Fallback to manual parsing
            if (itemTag.contains("id")) {
                String itemId = itemTag.getString("id");
                int count = itemTag.getInt("count");

                ResourceLocation itemLocation = ResourceLocation.parse(itemId);
                Item itemType = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemLocation);

                if (itemType != null && itemType != Items.AIR) {
                    return new ItemStack(itemType, Math.max(1, count));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse item: " + e.getMessage());
        }

        return ItemStack.EMPTY;
    }
    private String getTabDisplayName(String materialType) {
        return switch (materialType.toLowerCase()) {
            case "all" -> "All";
            case "water_material" -> "Water";
            case "fire_material" -> "Fire";
            case "earth_material" -> "Earth";
            case "wind_material" -> "Wind";
            case "time_material" -> "Time";
            case "space_material" -> "Space";
            case "mirage_material" -> "Mirage";
            case "magic_tool" -> "Tool";
            default -> materialType.length() > 4 ?
                    materialType.substring(0, 4) :
                    materialType;
        };
    }
    private String formatMaterialTypeName(String materialType) {
        return switch (materialType.toLowerCase()) {
            case "water_material" -> "Water Material";
            case "fire_material" -> "Fire Material";
            case "earth_material" -> "Earth Material";
            case "wind_material" -> "Wind Material";
            case "time_material" -> "Time Material";
            case "space_material" -> "Space Material";
            case "mirage_material" -> "Mirage Material";
            case "plant" -> "Plant";
            case "jewel" -> "Jewel";
            case "mystery" -> "Mystery";
            case "accessory" -> "Accessory";
            case "bomb" -> "Bomb";
            case "cooking" -> "Cooking";
            case "dessert" -> "Dessert";
            case "elixir" -> "Elixir";
            case "food" -> "Food";
            case "gunpowder" -> "Gunpowder";
            case "ingot" -> "Ingot";
            case "liquid" -> "Liquid";
            case "magic_tool" -> "Magic Tool";
            case "medicinal" -> "Medicinal";
            case "medicine" -> "Medicine";
            case "oil" -> "Oil";
            case "ore" -> "Ore";
            case "poison" -> "Poison";
            case "spice" -> "Spice";
            case "sundry" -> "Sundry";
            case "supplement" -> "Supplement";
            case "threads" -> "Threads";
            case "wool" -> "Wool";
            case "cloth" -> "Cloth";
            default -> materialType;
        };
    }
    // Replace the renderFilteredInventory method with this STABLE version:
    private void renderFilteredInventory(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.player == null) return;

        int invPanelX = this.width / 2 - 140;
        int invPanelY = this.height / 2 + 30;
        int tempLeftPos = invPanelX + 12;
        int tempTopPos = invPanelY + 10;

        this.hoveredFilteredSlot = -1;
        filteredSlotMapping.clear();

        Inventory playerInv = minecraft.player.getInventory();

        int slotSize = 18;
        int slotsPerRow = 9;
        int rows = 4;

        int displaySlot = 0;

        // CRITICAL: Create stable snapshot of inventory at render time
        for (int actualSlot = 0; actualSlot < playerInv.getContainerSize(); actualSlot++) {
            ItemStack item = playerInv.getItem(actualSlot);

            if (!item.isEmpty() && itemMatchesSelectedTab(item)) {
                int row = displaySlot / slotsPerRow;
                int col = displaySlot % slotsPerRow;

                if (row >= rows) break;

                int slotX = tempLeftPos + (col * slotSize);
                int slotY = tempTopPos + (row * slotSize);

                filteredSlotMapping.put(displaySlot, actualSlot);

                // Draw slot background
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF373737);
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);

                // Render item - use the snapshot we took
                graphics.renderItem(item, slotX, slotY);
                if (item.getCount() > 1) {
                    graphics.renderItemDecorations(this.font, item, slotX, slotY);
                }

                // Highlight on hover
                if (mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16) {
                    graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
                    this.hoveredFilteredSlot = actualSlot;
                }

                displaySlot++;
            }
        }
    }
    private void drawRecipeGraph(GuiGraphics graphics) {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        ResourceLocation recipeId = blockEntity.getActiveRecipeId();
        if (recipeId == null) {
            return;
        }
        QuartzCraftingRecipe recipe = KisekiLegend.getQuartzRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            return;
        }

        drawBackgroundCircuitPattern(graphics, this.width / 2, this.height / 2, System.currentTimeMillis());

        Set<String> unlocked = blockEntity.getUnlockedNodes();
        CompoundTag allProgress = blockEntity.getStoredItems();

        // Draw connections first
        for (Map.Entry<String, QuartzCraftingRecipe.Node> entry : recipe.getNodes().entrySet()) {
            if (!unlocked.contains(entry.getKey())) continue;
            Pos startPos = nodePositions.get(entry.getKey());
            if (startPos == null) continue;

            // Apply view offset to start position
            int adjustedStartX = startPos.x + viewOffsetX;
            int adjustedStartY = startPos.y + viewOffsetY;

            int lineColor = blockEntity.isNodeCompleted(entry.getKey()) ? 0xFFFFAA00 : 0xFF808080;

            for (String unlockTarget : entry.getValue().getUnlocks()) {
                Pos endPos = nodePositions.get(unlockTarget);
                if (endPos != null) {
                    // Apply view offset to end position
                    int adjustedEndX = endPos.x + viewOffsetX;
                    int adjustedEndY = endPos.y + viewOffsetY;

                    // Draw line from start to end (handles all directions)
                    drawLine(graphics, adjustedStartX, adjustedStartY, adjustedEndX, adjustedEndY, lineColor);
                }
            }
        }

// Draw nodes
        for (Map.Entry<String, QuartzCraftingRecipe.Node> entry : recipe.getNodes().entrySet()) {
            String nodeId = entry.getKey();
            QuartzCraftingRecipe.Node nodeData = entry.getValue();
            Pos pos = nodePositions.get(nodeId);
            if (pos == null) continue;

            // Apply view offset to node position
            int adjustedNodeX = pos.x + viewOffsetX;
            int adjustedNodeY = pos.y + viewOffsetY;

            // Draw custom node image with glow effect
            drawCustomNodeCircle(graphics, adjustedNodeX, adjustedNodeY, unlocked.contains(nodeId), blockEntity.isNodeCompleted(nodeId), nodeData);

            String categoryLabel = getCategoryLabel(nodeData);
            drawMultiLineText(graphics, categoryLabel, adjustedNodeX, adjustedNodeY + 2, 0xFFFFFF);

            // Draw node type panel (trait, effect, quality, etc.) at top-left
            drawNodeTypePanel(graphics, adjustedNodeX, adjustedNodeY, nodeData);

            // Draw material requirement slots (small outer circles)
            CompoundTag nodeStoredItems = allProgress.getCompound(nodeId);
            List<Map.Entry<String, Integer>> requirements = new ArrayList<>(nodeData.getMaterialRequirements().entrySet());
            int totalSlots = requirements.stream().mapToInt(Map.Entry::getValue).sum();
            int currentSlot = 0;
            if (totalSlots == 0) continue;

            for (Map.Entry<String, Integer> req : requirements) {
                ListTag storedList = nodeStoredItems.getList(req.getKey(), 10);
                int filledCount = storedList.size();

                for (int i = 0; i < req.getValue(); i++) {
                    double angle = Math.PI + (currentSlot * (Math.PI * 1.5 / (totalSlots <= 1 ? 1 : totalSlots - 1)));
                    // Apply view offset to material slot positions
                    int matX = adjustedNodeX + (int) (Math.cos(angle) * 25);
                    int matY = adjustedNodeY + (int) (Math.sin(angle) * 25);
                    drawCircle(graphics, matX, matY, 4, 0xFF000000);
                    int fillColor = (i < filledCount) ? 0xFF55FF55 : 0xFFFFFFFF;
                    drawCircle(graphics, matX, matY, 3, fillColor);
                    currentSlot++;
                }
            }
        }
    }
    @Override
    public void onClose() {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            // Only return items and clear recipe if NOT synthesizing
            if (!blockEntity.isSynthesizing()) {
                NetworkHandler.sendToServer(new ReturnStoredItemsPacket(blockEntity.getBlockPos()));
                NetworkHandler.sendToServer(new SelectQuartzRecipePacket(null, blockEntity.getBlockPos()));
            }
            // If synthesizing, let it complete naturally - DON'T CLEAR RECIPE
        }
        super.onClose();
    }
    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        super.mouseMoved(pMouseX, pMouseY);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Close material insertion panel with Escape key (but stay in recipe view)
        if (pKeyCode == 256 && currentState == ScreenState.INSERTING_MATERIAL) { // 256 is Escape key
            // Play close sound
            playUISound(ModSoundEvents.UI_CLOCK_CLOSE.get(), 1.0f, 0.5f);

            currentState = ScreenState.SELECTING_RECIPE;
            selectedNodeId = null;
            selectedMaterialType = null;
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    private void playUISound(SoundEvent soundEvent, float pitch, float volume) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        soundEvent,
                        pitch,
                        volume
                ));
            }
        } catch (Exception e) {
            System.out.println("Error playing UI sound: " + e.getMessage());
        }
    }
    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // Calculate the differences
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        // Determine direction
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;

        // Bresenham's line algorithm for smooth diagonal lines
        int err = dx - dy;
        int x = x1;
        int y = y1;

        while (true) {
            // Draw pixel with some thickness for visibility
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);

            if (x == x2 && y == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
    private void drawMultiLineText(GuiGraphics graphics, String text, int centerX, int centerY, int color) {
        String[] words = text.split(" ");
        if (words.length <= 1) {
            // Single word, draw normally
            graphics.drawCenteredString(this.font, text, centerX, centerY, color);
        } else {
            // Multiple words, split into lines
            List<String> lines = new ArrayList<>();
            for (String word : words) {
                lines.add(word);
            }

            // Draw each line
            int lineHeight = this.font.lineHeight;
            int startY = centerY - (lines.size() * lineHeight / 2);

            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(this.font, lines.get(i), centerX, startY + (i * lineHeight), color);
            }
        }
    }
    private void drawCustomNodeCircle(GuiGraphics graphics, int centerX, int centerY, boolean unlocked, boolean completed, QuartzCraftingRecipe.Node nodeData) {
        // Set up texture rendering
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Load texture based on node type
        ResourceLocation nodeTexture = getNodeTexture(nodeData);
        RenderSystem.setShaderTexture(0, nodeTexture);

        // Draw glow effect first (larger, translucent)
        if (completed) {
            RenderSystem.setShaderColor(1.0F, 0.67F, 0.0F, 0.3F); // Orange glow
        } else if (unlocked) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.3F); // White glow
        } else {
            RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 0.3F); // Gray glow
        }

// Draw main node - try different approach
        graphics.blit(nodeTexture, centerX - 20, centerY - 20, 0, 0, 40, 40, 40, 40);

        // Reset shader color
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


    private String getCategoryLabel(QuartzCraftingRecipe.Node nodeData) {
        // Get the first material requirement as the category
        if (!nodeData.getMaterialRequirements().isEmpty()) {
            String materialType = nodeData.getMaterialRequirements().keySet().iterator().next();
            return formatMaterialTypeName(materialType);
        }
        return "Node";
    }

    private void drawNodeTypePanel(GuiGraphics graphics, int nodeX, int nodeY, QuartzCraftingRecipe.Node nodeData) {
        String nodeType = nodeData.getType().equals("RECIPE_MORPH") ? "RECIPE" : nodeData.getType().toUpperCase();

        // Calculate smaller panel position - MOVED CLOSER AND MADE SMALLER
        int panelWidth = Math.max(20, this.font.width(nodeType) + 4); // Smaller padding
        int panelHeight = 10; // Smaller height
        int panelX = nodeX - 15 - panelWidth / 2; // Closer to node, centered horizontally
        int panelY = nodeY - 15; // Closer to node top

        // Draw dark brown translucent background
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xBF4A3426);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF2D1F15);

        // Draw smaller text using scale
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f); // Make text smaller (50% of original size)
        graphics.drawString(this.font, Component.literal(nodeType),
                (int)((panelX + 2) / 0.5f), (int)((panelY + 2) / 0.5f), 0xFFFFD700, false);
        graphics.pose().popPose();
    }

    private void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    private record Pos(int x, int y) {}
}