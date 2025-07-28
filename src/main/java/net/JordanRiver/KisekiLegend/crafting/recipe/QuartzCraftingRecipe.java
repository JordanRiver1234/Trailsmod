package net.JordanRiver.KisekiLegend.crafting.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/**
 * Represents a full quartz crafting recipe, loaded from a JSON file.
 * This is the top-level object for a recipe like "hp_1".
 */
public class QuartzCraftingRecipe {
    private final ResourceLocation id;
    private final String result; // The item ID of the final quartz, e.g., "kisekilegend:hp_1"
    private final String startingNode; // The ID of the first node in the tree
    private final Map<String, Node> nodes; // A map of all nodes in this recipe, keyed by their ID
    private final String requiredItem; // Add this field

    public QuartzCraftingRecipe(ResourceLocation id, String result, String startingNode, Map<String, Node> nodes, String requiredItem) {
        this.id = id;
        this.result = result;
        this.startingNode = startingNode;
        this.nodes = nodes;
        this.requiredItem = requiredItem; // Add this line
    }

    // Add getter
    public String getRequiredItem() { return requiredItem; }


    public ResourceLocation getId() { return id; }
    public String getResult() { return result; }
    public String getStartingNode() { return startingNode; }
    public Map<String, Node> getNodes() { return nodes; }
    public Node getNode(String id) { return nodes.get(id); }

    /**
     * Represents a single node in the crafting tree (the circles).
     */
    public static class Node {
        private String type; // "EFFECT", "TRAIT", "QUALITY", "RECIPE_MORPH"
        private String value; // The effect/trait name, e.g., "Max HP +5%" or "defense_2" for morph
        private Map<String, Integer> materialRequirements; // e.g., {"WATER": 3, "PLANT": 2}
        private java.util.List<String> unlocks; // List of node IDs this node unlocks upon completion

        // Getters
        public String getType() { return type; }
        public String getValue() { return value; }
        public Map<String, Integer> getMaterialRequirements() { return materialRequirements; }
        public java.util.List<String> getUnlocks() { return unlocks; }
    }
}
