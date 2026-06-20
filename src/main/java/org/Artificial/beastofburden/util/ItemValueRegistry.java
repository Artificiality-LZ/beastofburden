package org.Artificial.beastofburden.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.Artificial.beastofburden.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Resolves per-item "value" for generation timing.
 * <p>
 * Priority: config overrides, then recipe-derived values (cheapest craft path),
 * then baked-in defaults, then {@link Config#defaultItemValue}.
 */
public final class ItemValueRegistry
{
    private static final Map<Item, Integer> BAKED_DEFAULTS = Map.ofEntries(
      Map.entry(item("minecraft:dirt"), 1),
      Map.entry(item("minecraft:grass_block"), 1),
      Map.entry(item("minecraft:sand"), 1),
      Map.entry(item("minecraft:gravel"), 2),
      Map.entry(item("minecraft:cobblestone"), 2),
      Map.entry(item("minecraft:stone"), 2),
      Map.entry(item("minecraft:deepslate"), 3),
      Map.entry(item("minecraft:oak_log"), 8),
      Map.entry(item("minecraft:oak_planks"), 2),
      Map.entry(item("minecraft:stick"), 1),
      Map.entry(item("minecraft:coal"), 5),
      Map.entry(item("minecraft:charcoal"), 5),
      Map.entry(item("minecraft:iron_ingot"), 20),
      Map.entry(item("minecraft:gold_ingot"), 50),
      Map.entry(item("minecraft:copper_ingot"), 8),
      Map.entry(item("minecraft:redstone"), 3),
      Map.entry(item("minecraft:lapis_lazuli"), 6),
      Map.entry(item("minecraft:quartz"), 8),
      Map.entry(item("minecraft:diamond"), 100),
      Map.entry(item("minecraft:emerald"), 120),
      Map.entry(item("minecraft:obsidian"), 15),
      Map.entry(item("minecraft:flint"), 3),
      Map.entry(item("minecraft:clay_ball"), 2),
      Map.entry(item("minecraft:brick"), 4),
      Map.entry(item("minecraft:wheat"), 2),
      Map.entry(item("minecraft:bread"), 6),
      Map.entry(item("minecraft:leather"), 8),
      Map.entry(item("minecraft:string"), 2),
      Map.entry(item("minecraft:feather"), 2),
      Map.entry(item("minecraft:bone"), 3),
      Map.entry(item("minecraft:gunpowder"), 10),
      Map.entry(item("minecraft:blaze_rod"), 25),
      Map.entry(item("minecraft:ender_pearl"), 40),
      Map.entry(item("minecraft:nether_star"), 500),
      Map.entry(item("minecraft:ancient_debris"), 150),
      Map.entry(item("minecraft:netherite_scrap"), 200),
      Map.entry(item("minecraft:netherite_ingot"), 800)
    );

    private static final List<RecipeType<?>> DERIVATION_TYPES = List.of(
      RecipeType.CRAFTING,
      RecipeType.SMELTING,
      RecipeType.BLASTING,
      RecipeType.SMOKING,
      RecipeType.STONECUTTING,
      RecipeType.SMITHING
    );

    @SuppressWarnings("unchecked")
    private static void collectRecipes(@NotNull final RecipeManager manager, @NotNull final List<Recipe<?>> target)
    {
        for (final RecipeType<?> recipeType : DERIVATION_TYPES)
        {
            target.addAll((Collection<? extends Recipe<?>>) (Collection<?>) manager.getAllRecipesFor((RecipeType) recipeType));
        }
    }

    private static final Map<Item, Integer> RESOLVED = new HashMap<>();

    @Nullable
    private static RecipeManager recipeManager;

    @Nullable
    private static RegistryAccess registryAccess;

    private ItemValueRegistry()
    {
    }

    public static void reload(@NotNull final Level level)
    {
        recipeManager = level.getRecipeManager();
        registryAccess = level.registryAccess();
        rebuildResolvedValues();
    }

    public static void onConfigReloaded()
    {
        if (recipeManager != null)
        {
            rebuildResolvedValues();
        }
        else
        {
            RESOLVED.clear();
            seedBaseValues(RESOLVED);
        }
    }

    public static int getStackValue(@NotNull final ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0;
        }

        return getPerItemValue(stack.getItem()) * stack.getCount();
    }

    public static int getPerItemValue(@NotNull final Item item)
    {
        final Integer resolved = RESOLVED.get(item);
        if (resolved != null)
        {
            return resolved;
        }

        final Integer explicit = Config.explicitItemValues.get(item);
        if (explicit != null)
        {
            return explicit;
        }

        final Integer baked = BAKED_DEFAULTS.get(item);
        if (baked != null)
        {
            return baked;
        }

        return Config.defaultItemValue;
    }

    private static void rebuildResolvedValues()
    {
        RESOLVED.clear();
        seedBaseValues(RESOLVED);

        if (!Config.deriveFromRecipes || recipeManager == null || registryAccess == null)
        {
            return;
        }

        deriveFromRecipes(recipeManager);
    }

    private static void seedBaseValues(@NotNull final Map<Item, Integer> target)
    {
        BAKED_DEFAULTS.forEach(target::putIfAbsent);
        Config.explicitItemValues.forEach(target::put);
    }

    private static void deriveFromRecipes(@NotNull final RecipeManager manager)
    {
        boolean changed = true;
        int iterations = 0;

        while (changed && iterations < 256)
        {
            changed = false;
            iterations++;

            final List<Recipe<?>> recipes = new ArrayList<>();
            collectRecipes(manager, recipes);
            for (final Recipe<?> recipe : recipes)
            {
                if (tryApplyRecipe(recipe))
                {
                    changed = true;
                }
            }
        }
    }

    private static boolean tryApplyRecipe(@NotNull final Recipe<?> recipe)
    {
        final ItemStack result = recipe.getResultItem(registryAccess);
        if (result.isEmpty())
        {
            return false;
        }

        final Item output = result.getItem();
        if (Config.explicitItemValues.containsKey(output))
        {
            return false;
        }

        final int ingredientCost = sumIngredientCost(recipe.getIngredients());
        if (ingredientCost < 0)
        {
            return false;
        }

        final int outputCount = Math.max(1, result.getCount());
        final int derived = (ingredientCost + outputCount - 1) / outputCount;
        final int previous = RESOLVED.getOrDefault(output, Integer.MAX_VALUE);
        if (derived < previous)
        {
            RESOLVED.put(output, derived);
            return true;
        }

        return false;
    }

    private static int sumIngredientCost(@NotNull final List<Ingredient> ingredients)
    {
        int total = 0;

        for (final Ingredient ingredient : ingredients)
        {
            final int cost = cheapestIngredientCost(ingredient);
            if (cost < 0)
            {
                return -1;
            }

            total += cost;
        }

        return total;
    }

    private static int cheapestIngredientCost(@NotNull final Ingredient ingredient)
    {
        if (ingredient.isEmpty())
        {
            return 0;
        }

        int cheapest = Integer.MAX_VALUE;
        for (final ItemStack option : ingredient.getItems())
        {
            if (option.isEmpty())
            {
                continue;
            }

            final int cost = getResolvedOrSeededValue(option.getItem()) * option.getCount();
            if (cost < 0)
            {
                cheapest = -1;
                break;
            }

            cheapest = Math.min(cheapest, cost);
        }

        return cheapest == Integer.MAX_VALUE ? -1 : cheapest;
    }

    /**
     * Value known during recipe derivation (no default fallback).
     */
    private static int getResolvedOrSeededValue(@NotNull final Item item)
    {
        final Integer explicit = Config.explicitItemValues.get(item);
        if (explicit != null)
        {
            return explicit;
        }

        final Integer resolved = RESOLVED.get(item);
        if (resolved != null)
        {
            return resolved;
        }

        final Integer baked = BAKED_DEFAULTS.get(item);
        if (baked != null)
        {
            return baked;
        }

        return -1;
    }

    private static Item item(@NotNull final String id)
    {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
