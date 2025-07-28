package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.JordanRiver.KisekiLegend.crafting.recipe.QuartzCraftingRecipe;
import net.JordanRiver.KisekiLegend.menu.QuartzMachineMenu;
import net.JordanRiver.KisekiLegend.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        // Implement your recipe unlocking logic here
        // For example, hp_1 is always unlocked, hp_2 requires hp_1 to be completed
        String recipeId = recipe.getId().getPath();

        if (recipeId.equals("hp_1")) {
            return true; // Always unlocked
        } else if (recipeId.equals("hp_2")) {
            // Check if player has completed hp_1 (you'll need to track this)
            // For now, return true for testing
            return true;
        }

        return true; // Default to unlocked for other recipes
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
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -200;
        this.inventoryLabelX = -200;
        this.titleLabelY = -200;
        this.inventoryLabelY = -200;

        // Initialize category recipes instead of updateRecipeList()
        initializeCategoryRecipes();

        // Clear existing positions
        nodePositions.clear();

        // Get the current active recipe to determine which positions to use
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getActiveRecipeId() != null) {
            String recipeId = blockEntity.getActiveRecipeId().getPath();

            if (recipeId.equals("hp_1")) {
                // HP_1 positions (your existing ones)
                nodePositions.put("start_hp", new Pos(this.width / 2 - 80, this.height / 2));
                nodePositions.put("quality_boost", new Pos(this.width / 2, this.height / 2));
                nodePositions.put("recipe_unlock_hp2", new Pos(this.width / 2 + 80, this.height / 2));
            } else if (recipeId.equals("hp_2")) {
                // HP_2 positions - balanced branching layout
                int centerX = this.width / 2;
                int centerY = this.height / 2;

                // Starting node at center-left
                nodePositions.put("start_hp2", new Pos(centerX - 120, centerY));

                // First branch level (from start) - moderate spread
                nodePositions.put("trait_branch1", new Pos(centerX - 40, centerY - 50));
                nodePositions.put("effect_branch1", new Pos(centerX - 40, centerY + 50));

                // Second branch level - positioned between first and quality nodes
                nodePositions.put("trait_branch2", new Pos(centerX + 30, centerY + 25));

                // Quality nodes (convergence level) - closer together
                nodePositions.put("quality_upper", new Pos(centerX + 90, centerY - 40));
                nodePositions.put("quality_middle", new Pos(centerX + 90, centerY));
                nodePositions.put("quality_lower", new Pos(centerX + 90, centerY + 40));

                // Final recipe node (end)
                nodePositions.put("final_recipe", new Pos(centerX + 150, centerY));
            }
        }
        this.synthesisButton = addRenderableWidget(Button.builder(Component.literal("Synthesize"), this::onSynthesisButtonPressed)
                .bounds(this.width / 2 - 100, 10, 120, 20)
                .build());
    }

    private void initializeCategoryRecipes() {
        Map<String, List<String>> categoryToPatterns = Map.of(
                "water", List.of("hp_", "heal_"),
                "fire", List.of("attack_"),
                "earth", List.of("defense_"),
                "wind", List.of("speed_"),
                "time", List.of("time_"),
                "space", List.of("space_"),
                "mirage", List.of("mirage_")
        );

        // Initialize all categories as collapsed
        for (String category : elementalCategories) {
            categoryExpanded.put(category, false);

            List<String> patterns = categoryToPatterns.getOrDefault(category, List.of(category));
            List<QuartzCraftingRecipe> recipes = KisekiLegend.getQuartzRecipeManager().getRecipes().values().stream()
                    .filter(r -> {
                        String recipeId = r.getId().getPath();
                        return patterns.stream().anyMatch(pattern -> recipeId.contains(pattern));
                    })
                    .toList();

            categoryRecipes.put(category, recipes);
        }
    }

    private void onSynthesisButtonPressed(Button button) {
        QuartzMachineBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            NetworkHandler.sendToServer(new StartSynthesisPacket(blockEntity.getBlockPos()));
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        // Left Panel
        int panelWidth = 80;
        pGuiGraphics.fill(5, 5, 5 + panelWidth, this.height - 5, 0xBF3B270A);
        pGuiGraphics.renderOutline(5, 5, panelWidth, this.height - 10, 0xFF1A1104);

        drawCategoryDropdowns(pGuiGraphics);


// Render player inventory first (if in material insertion mode)
        if (currentState == ScreenState.INSERTING_MATERIAL) {
            // Draw inventory background panel first - MOVED LEFT
            int invPanelX = this.width / 2 - 140; // Changed from -100
            int invPanelY = this.height / 2 + 30;
            int invPanelWidth = 200;
            int invPanelHeight = 120;

            pGuiGraphics.fill(invPanelX, invPanelY, invPanelX + invPanelWidth, invPanelY + invPanelHeight, 0xE0000000);
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
        if (blockEntity != null && blockEntity.getActiveRecipeId() != null && currentState == ScreenState.SELECTING_RECIPE) {
            drawRecipeGraph(pGuiGraphics);
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
        // Calculate total lines needed
        int totalLines = 0;
        for (String category : elementalCategories) {
            totalLines++; // Category header
            if (categoryExpanded.get(category)) {
                totalLines += categoryRecipes.get(category).size();
            }
        }

        // Update max scroll offset
        maxScrollOffset = Math.max(0, totalLines - VISIBLE_LINES);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        // Set up clipping for the scrollable area
        int panelX = 5;
        int panelY = 15;
        int panelWidth = 80;
        int panelHeight = VISIBLE_LINES * 12; // Use consistent line height for calculation

        // Enable scissor test for clipping
        graphics.enableScissor(panelX, panelY, panelX + panelWidth, panelY + panelHeight);

        int currentLine = 0;
        int drawY = panelY - (scrollOffset * 12); // This calculation is now correct

        for (String category : elementalCategories) {
            boolean isExpanded = categoryExpanded.get(category);

            // Draw category header if it's visible
            if (currentLine >= scrollOffset && currentLine < scrollOffset + VISIBLE_LINES) {
                String indicator = isExpanded ? "▼ " : "► ";
                Component categoryText = Component.literal(indicator + category);
                graphics.drawString(this.font, categoryText, 15, drawY, 0xFFFFFFFF, true);
            }

            drawY += 12;
            currentLine++;

            // Draw recipes if expanded
            if (isExpanded) {
                List<QuartzCraftingRecipe> recipes = categoryRecipes.get(category);
                for (QuartzCraftingRecipe recipe : recipes) {
                    if (currentLine >= scrollOffset && currentLine < scrollOffset + VISIBLE_LINES) {
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
                            color = 0xFFFFAA00;
                        } else {
                            color = 0xFFCCCCCC;
                        }
                        graphics.drawString(this.font, name, 20, drawY, color, false);
                    }
                    drawY += 12; // UNIFIED LINE HEIGHT: Changed from 10 to 12
                    currentLine++;
                }
            }
        }

        // Disable scissor test
        graphics.disableScissor();

        // Draw scroll indicator if needed
        if (maxScrollOffset > 0) {
            drawScrollIndicator(graphics, panelX + panelWidth - 10, panelY, panelHeight);
        }
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
        graphics.fill(this.leftPos, this.topPos, this.leftPos + panelWidth, this.topPos + panelHeight, 0xE0000000);
        graphics.renderOutline(this.leftPos, this.topPos, panelWidth, panelHeight, 0xFFFFFFFF);

        this.imageWidth = panelWidth;
        this.imageHeight = panelHeight;

        // Draw title at proper position
        graphics.drawCenteredString(this.font, Component.literal("Select Material"),
                this.leftPos + panelWidth / 2, this.topPos + 8, 0xFFFFFFFF);
    }
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        // Check if mouse is over the left panel
        if (pMouseX >= 5 && pMouseX <= 125 && pMouseY >= 15) {
            int scrollDirection = pScrollY > 0 ? -1 : 1; // Reverse for natural scrolling
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + scrollDirection));
            return true;
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
        if (currentTime - lastInventoryClickTime < 200) { // Increased from CLICK_COOLDOWN to 200ms
            return true; // Ignore rapid clicks
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
                    // CRITICAL: Double-check item still exists and matches before sending packet
                    ItemStack itemInSlot = minecraft.player.getInventory().getItem(actualSlot);
                    if (!itemInSlot.isEmpty() && itemMatchesSelectedTab(itemInSlot)) {
                        lastInventoryClickTime = currentTime;
                        System.out.println("Sending insert packet - slot: " + actualSlot + ", item: " + itemInSlot);

                        // Send packet
                        NetworkHandler.sendToServer(new InsertMaterialPacket(
                                blockEntity.getBlockPos(), selectedNodeId, actualSlot, selectedMaterialType));

                        // IMPORTANT: Don't modify client inventory here - let server handle it
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
        int panelY = 15;
        int panelWidth = 75;
        int panelHeight = VISIBLE_LINES * 12;

        // Ignore clicks outside the recipe panel
        if (pMouseX < panelX || pMouseX > panelX + panelWidth || pMouseY < panelY || pMouseY > panelY + panelHeight) {
            return false;
        }

        // Calculate which line was clicked, accounting for the scroll offset
        int clickedLine = (int)((pMouseY - panelY) / 12) + scrollOffset;
        int currentLine = 0;

        for (String category : elementalCategories) {
            // Check for click on the category header itself
            if (currentLine == clickedLine) {
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
                            // Send the selection packet to the server.
                            // The server will handle the logic and send a sync packet back.
                            NetworkHandler.sendToServer(new SelectQuartzRecipePacket(recipe.getId(), blockEntity.getBlockPos()));
                            // DO NOT call init() here. Let the server's response update the GUI.
                        }
                        return true;
                    }
                    currentLine++;
                }
            }
        }
        return false;
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
        graphics.fill(slotPanelX, slotPanelY, slotPanelX + slotPanelWidth, slotPanelY + slotPanelHeight, 0xE04A3426);
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
        Set<String> unlocked = blockEntity.getUnlockedNodes();
        System.out.println("Unlocked nodes: " + unlocked + ", node positions: " + nodePositions);
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
            currentState = ScreenState.SELECTING_RECIPE;
            selectedNodeId = null;
            selectedMaterialType = null;
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);

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