package org.Artificial.beastofburden;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.Artificial.beastofburden.util.ItemValueRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Beastofburden.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Log beast-of-burden request scanning and AI decisions (for troubleshooting).")
            .define("debugLogging", false);

    private static final ForgeConfigSpec.IntValue BASE_GENERATION_TICKS = BUILDER
            .comment("Base generation time in ticks before item value and skill modifiers.")
            .defineInRange("baseGenerationTicks", 60, 1, 20_000);

    private static final ForgeConfigSpec.IntValue MIN_GENERATION_TICKS = BUILDER
            .comment("Minimum generation time in ticks regardless of item value or skill.")
            .defineInRange("minGenerationTicks", 40, 1, 20_000);

    private static final ForgeConfigSpec.DoubleValue TICKS_PER_ITEM_VALUE = BUILDER
            .comment("Extra ticks added per point of total item value (value x stack count).")
            .defineInRange("ticksPerItemValue", 3.0, 0.0, 10_000.0);

    private static final ForgeConfigSpec.DoubleValue STRENGTH_SPEED_BONUS = BUILDER
            .comment("Generation speed bonus per Strength level (0.05 = 5% faster per level).")
            .defineInRange("strengthSpeedBonus", 0.05, 0.0, 10.0);

    private static final ForgeConfigSpec.IntValue DEFAULT_ITEM_VALUE = BUILDER
            .comment("Fallback value for items with no explicit or recipe-derived value.")
            .defineInRange("defaultItemValue", 5, 1, 1_000_000);

    private static final ForgeConfigSpec.BooleanValue DERIVE_FROM_RECIPES = BUILDER
            .comment("Derive unknown item values from crafting/smelting recipes (e.g. diamond block = 9 x diamond).")
            .define("deriveItemValuesFromRecipes", true);

    private static final ForgeConfigSpec.IntValue WORK_LOG_MAX_ENTRIES = BUILDER
            .comment("Maximum work-history entries stored per colony Town Hall.")
            .defineInRange("workLogMaxEntries", 500, 10, 10_000);

    private static final ForgeConfigSpec.IntValue WORK_LOG_HISTORY_DAYS = BUILDER
            .comment("Days of work history shown in the UI (0 = show all stored entries).")
            .defineInRange("workLogHistoryDays", 100, 0, 10_000);

    private static final ForgeConfigSpec.IntValue PLANNING_RETRY_COOLDOWN = BUILDER
            .comment(
              "Planning passes to wait after an idle or failed planning pass.",
              "This is counted in Town Hall module ticks, not normal Minecraft game ticks."
            )
            .defineInRange("planningRetryCooldown", 1, 0, 20);

    private static final ForgeConfigSpec.IntValue PLANNING_COLD_START_COOLDOWN = BUILDER
            .comment(
              "Planning passes to wait after placing the first builder hut.",
              "The first builder hut construction already blocks further planning, so this should stay small."
            )
            .defineInRange("planningColdStartCooldown", 1, 0, 20);

    private static final ForgeConfigSpec.IntValue PLANNING_SEARCH_RADIUS = BUILDER
            .comment("Horizontal search radius for new hut placement.")
            .defineInRange("planningSearchRadius", 96, 16, 256);

    private static final ForgeConfigSpec.IntValue PLANNING_MAX_CANDIDATES = BUILDER
            .comment("Maximum placement candidates evaluated per building type.")
            .defineInRange("planningMaxCandidates", 500, 50, 5000);

    private static final ForgeConfigSpec.IntValue PLANNING_BUILDER_RADIUS = BUILDER
            .comment("Maximum distance from a builder hut to assign construction work.")
            .defineInRange("planningBuilderRadius", 100, 32, 256);

    private static final ForgeConfigSpec.IntValue PLANNING_MAX_BUILDER_QUEUE = BUILDER
            .comment("Maximum queued work orders per builder hut.")
            .defineInRange("planningMaxBuilderQueue", 3, 1, 10);

    private static final ForgeConfigSpec.IntValue PLANNING_MIN_BLUEPRINT_SEPARATION = BUILDER
            .comment("Minimum empty blocks between hut blueprint bounds (0 = touching allowed).")
            .defineInRange("planningMinBlueprintSeparation", 4, 0, 16);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_VALUES = BUILDER
            .comment(
              "Explicit item values as 'namespace:path=value'.",
              "Overrides defaults and recipe-derived values.",
              "Unlisted items use baked-in defaults, then recipe rules, then defaultItemValue."
            )
            .defineListAllowEmpty("itemValues", Config::defaultItemValueEntries, Config::validateItemValueEntry);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean debugLogging;
    public static int baseGenerationTicks;
    public static int minGenerationTicks;
    public static double ticksPerItemValue;
    public static double strengthSpeedBonus;
    public static int defaultItemValue;
    public static boolean deriveFromRecipes;
    public static int workLogMaxEntries;
    public static int workLogHistoryDays;
    public static int planningRetryCooldown;
    public static int planningColdStartCooldown;
    public static int planningSearchRadius;
    public static int planningMaxCandidates;
    public static int planningBuilderRadius;
    public static int planningMaxBuilderQueue;
    public static int planningMinBlueprintSeparation;
    public static Map<Item, Integer> explicitItemValues = Map.of();

    private static List<String> defaultItemValueEntries()
    {
        return List.of(
          "minecraft:dirt=1",
          "minecraft:grass_block=1",
          "minecraft:sand=1",
          "minecraft:gravel=2",
          "minecraft:cobblestone=2",
          "minecraft:stone=2",
          "minecraft:deepslate=3",
          "minecraft:oak_log=8",
          "minecraft:oak_planks=2",
          "minecraft:stick=1",
          "minecraft:coal=5",
          "minecraft:charcoal=5",
          "minecraft:iron_ingot=20",
          "minecraft:gold_ingot=50",
          "minecraft:copper_ingot=8",
          "minecraft:redstone=3",
          "minecraft:lapis_lazuli=6",
          "minecraft:quartz=8",
          "minecraft:diamond=100",
          "minecraft:emerald=120",
          "minecraft:obsidian=15",
          "minecraft:flint=3",
          "minecraft:clay_ball=2",
          "minecraft:brick=4",
          "minecraft:wheat=2",
          "minecraft:bread=6",
          "minecraft:leather=8",
          "minecraft:string=2",
          "minecraft:feather=2",
          "minecraft:bone=3",
          "minecraft:gunpowder=10",
          "minecraft:blaze_rod=25",
          "minecraft:ender_pearl=40",
          "minecraft:nether_star=500",
          "minecraft:ancient_debris=150",
          "minecraft:netherite_scrap=200",
          "minecraft:netherite_ingot=800"
        );
    }

    private static boolean validateItemValueEntry(final Object obj)
    {
        return obj instanceof String entry && parseItemValueEntry(entry).isPresent();
    }

    static Optional<Integer> parseItemValueEntry(final String entry)
    {
        final int separator = entry.lastIndexOf('=');
        if (separator <= 0 || separator >= entry.length() - 1)
        {
            return Optional.empty();
        }

        final String itemId = entry.substring(0, separator).trim();
        final String valueText = entry.substring(separator + 1).trim();
        if (!ResourceLocation.isValidResourceLocation(itemId))
        {
            return Optional.empty();
        }

        try
        {
            final int value = Integer.parseInt(valueText);
            if (value < 1)
            {
                return Optional.empty();
            }

            if (!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(itemId)))
            {
                return Optional.empty();
            }

            return Optional.of(value);
        }
        catch (final NumberFormatException ignored)
        {
            return Optional.empty();
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC)
        {
            return;
        }

        debugLogging = DEBUG_LOGGING.get();
        baseGenerationTicks = BASE_GENERATION_TICKS.get();
        minGenerationTicks = MIN_GENERATION_TICKS.get();
        ticksPerItemValue = TICKS_PER_ITEM_VALUE.get();
        strengthSpeedBonus = STRENGTH_SPEED_BONUS.get();
        defaultItemValue = DEFAULT_ITEM_VALUE.get();
        deriveFromRecipes = DERIVE_FROM_RECIPES.get();
        workLogMaxEntries = WORK_LOG_MAX_ENTRIES.get();
        workLogHistoryDays = WORK_LOG_HISTORY_DAYS.get();
        planningRetryCooldown = PLANNING_RETRY_COOLDOWN.get();
        planningColdStartCooldown = PLANNING_COLD_START_COOLDOWN.get();
        planningSearchRadius = PLANNING_SEARCH_RADIUS.get();
        planningMaxCandidates = PLANNING_MAX_CANDIDATES.get();
        planningBuilderRadius = PLANNING_BUILDER_RADIUS.get();
        planningMaxBuilderQueue = PLANNING_MAX_BUILDER_QUEUE.get();
        planningMinBlueprintSeparation = PLANNING_MIN_BLUEPRINT_SEPARATION.get();

        final Map<Item, Integer> parsed = new HashMap<>();
        for (final String entry : ITEM_VALUES.get())
        {
            parseItemValueEntry(entry).ifPresent(value -> {
                final String itemId = entry.substring(0, entry.lastIndexOf('=')).trim();
                final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
                if (item != null)
                {
                    parsed.put(item, value);
                }
            });
        }
        explicitItemValues = Map.copyOf(parsed);

        reloadCachedValues();
    }

    static void reloadCachedValues()
    {
        ItemValueRegistry.onConfigReloaded();
    }

    public static void saveCommonConfig()
    {
        SPEC.save();
    }

    public static void applyRuntimeValues(
      final int baseTicks,
      final int minTicks,
      final double ticksPerValue,
      final double strengthBonus,
      final int defaultValue,
      final boolean deriveRecipes,
      final int logMaxEntries,
      final int logHistoryDays,
      @NotNull final Map<Item, Integer> itemValues)
    {
        BASE_GENERATION_TICKS.set(baseTicks);
        MIN_GENERATION_TICKS.set(minTicks);
        TICKS_PER_ITEM_VALUE.set(ticksPerValue);
        STRENGTH_SPEED_BONUS.set(strengthBonus);
        DEFAULT_ITEM_VALUE.set(defaultValue);
        DERIVE_FROM_RECIPES.set(deriveRecipes);
        WORK_LOG_MAX_ENTRIES.set(logMaxEntries);
        WORK_LOG_HISTORY_DAYS.set(logHistoryDays);

        final List<String> entries = new ArrayList<>();
        for (final Map.Entry<Item, Integer> entry : itemValues.entrySet())
        {
            entries.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey()) + "=" + entry.getValue());
        }
        ITEM_VALUES.set(entries);

        baseGenerationTicks = baseTicks;
        minGenerationTicks = minTicks;
        ticksPerItemValue = ticksPerValue;
        strengthSpeedBonus = strengthBonus;
        defaultItemValue = defaultValue;
        deriveFromRecipes = deriveRecipes;
        workLogMaxEntries = logMaxEntries;
        workLogHistoryDays = logHistoryDays;
        explicitItemValues = Map.copyOf(itemValues);

        reloadCachedValues();
    }

    @NotNull
    public static Map<Item, Integer> copyExplicitItemValues()
    {
        return new HashMap<>(explicitItemValues);
    }
}
