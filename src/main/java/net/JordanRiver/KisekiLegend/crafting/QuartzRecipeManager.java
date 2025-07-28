package net.JordanRiver.KisekiLegend.crafting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.crafting.recipe.QuartzCraftingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class QuartzRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER_NAME = "quartz_recipes";

    private Map<ResourceLocation, QuartzCraftingRecipe> recipes = new HashMap<>();

    public QuartzRecipeManager() {
        super(GSON, FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, QuartzCraftingRecipe> loadedRecipes = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                // Manually parse to pass the ID to the constructor
                JsonObject json = entry.getValue().getAsJsonObject();
                String result = json.get("result").getAsString();
                String startingNode = json.get("startingNode").getAsString();

                Map<String, QuartzCraftingRecipe.Node> nodes = new HashMap<>();
                JsonObject nodesJson = json.getAsJsonObject("nodes");
                for(String key : nodesJson.keySet()) {
                    QuartzCraftingRecipe.Node node = GSON.fromJson(nodesJson.get(key), QuartzCraftingRecipe.Node.class);
                    nodes.put(key, node);
                }

                String requiredItem = json.has("requiredItem") ? json.get("requiredItem").getAsString() : null;
                QuartzCraftingRecipe recipe = new QuartzCraftingRecipe(id, result, startingNode, nodes, requiredItem);
                loadedRecipes.put(id, recipe);

            } catch (Exception e) {
                KisekiLegend.LOGGER.error("Couldn't parse quartz recipe {}", id, e);
            }
        }
        this.recipes = loadedRecipes;
        KisekiLegend.LOGGER.info("Loaded {} quartz recipes", this.recipes.size());
    }

    public Map<ResourceLocation, QuartzCraftingRecipe> getRecipes() {
        return recipes;
    }

    public QuartzCraftingRecipe getRecipe(ResourceLocation id) {
        return recipes.get(id);
    }
}
