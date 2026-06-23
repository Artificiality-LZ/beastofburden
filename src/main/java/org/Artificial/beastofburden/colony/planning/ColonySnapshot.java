package org.Artificial.beastofburden.colony.planning;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Point-in-time colony metrics used by the planning engine.
 */
public final class ColonySnapshot
{
    private final int population;
    private final int homelessCount;
    private final double avgSaturation;
    private final int guardCount;
    private final boolean hasBuilderHut;
    private final int maxBuilderLevel;
    private final boolean hasWarehouse;
    private final boolean hasActiveCourier;
    private final boolean hasSawmill;
    private final boolean hasStonemason;
    private final boolean hasBlacksmith;
    private final boolean hasUniversity;
    private final boolean hasGuardTower;
    private final boolean hasCook;
    private final boolean hasTavern;
    private final int pendingWorkOrders;
    private final int emptyBeds;
    private final int unfilledWorkerSlots;
    private final int joblessAdults;
    private final int operationalWorkHuts;
    private final Map<PlannedBuildingType, Integer> builtCounts;
    private final Map<PlannedBuildingType, Integer> pendingCounts;
    @NotNull
    private final BlockPos colonyCenter;
    @Nullable
    private final String structurePack;

    public ColonySnapshot(
      final int population,
      final int homelessCount,
      final double avgSaturation,
      final int guardCount,
      final boolean hasBuilderHut,
      final int maxBuilderLevel,
      final boolean hasWarehouse,
      final boolean hasActiveCourier,
      final boolean hasSawmill,
      final boolean hasStonemason,
      final boolean hasBlacksmith,
      final boolean hasUniversity,
      final boolean hasGuardTower,
      final boolean hasCook,
      final boolean hasTavern,
      final int pendingWorkOrders,
      final int emptyBeds,
      final int unfilledWorkerSlots,
      final int joblessAdults,
      final int operationalWorkHuts,
      @NotNull final Map<PlannedBuildingType, Integer> builtCounts,
      @NotNull final Map<PlannedBuildingType, Integer> pendingCounts,
      @NotNull final BlockPos colonyCenter,
      @Nullable final String structurePack)
    {
        this.population = population;
        this.homelessCount = homelessCount;
        this.avgSaturation = avgSaturation;
        this.guardCount = guardCount;
        this.hasBuilderHut = hasBuilderHut;
        this.maxBuilderLevel = maxBuilderLevel;
        this.hasWarehouse = hasWarehouse;
        this.hasActiveCourier = hasActiveCourier;
        this.hasSawmill = hasSawmill;
        this.hasStonemason = hasStonemason;
        this.hasBlacksmith = hasBlacksmith;
        this.hasUniversity = hasUniversity;
        this.hasGuardTower = hasGuardTower;
        this.hasCook = hasCook;
        this.hasTavern = hasTavern;
        this.pendingWorkOrders = pendingWorkOrders;
        this.emptyBeds = emptyBeds;
        this.unfilledWorkerSlots = unfilledWorkerSlots;
        this.joblessAdults = joblessAdults;
        this.operationalWorkHuts = operationalWorkHuts;
        this.builtCounts = Map.copyOf(builtCounts);
        this.pendingCounts = Map.copyOf(pendingCounts);
        this.colonyCenter = colonyCenter;
        this.structurePack = structurePack;
    }

    public int getPopulation()
    {
        return population;
    }

    public int getHomelessCount()
    {
        return homelessCount;
    }

    public double getAvgSaturation()
    {
        return avgSaturation;
    }

    public int getGuardCount()
    {
        return guardCount;
    }

    public boolean hasBuilderHut()
    {
        return hasBuilderHut;
    }

    public int getMaxBuilderLevel()
    {
        return maxBuilderLevel;
    }

    public boolean hasWarehouse()
    {
        return hasWarehouse;
    }

    public boolean hasActiveCourier()
    {
        return hasActiveCourier;
    }

    public boolean hasSawmill()
    {
        return hasSawmill;
    }

    public boolean hasStonemason()
    {
        return hasStonemason;
    }

    public boolean hasBlacksmith()
    {
        return hasBlacksmith;
    }

    public boolean hasUniversity()
    {
        return hasUniversity;
    }

    public boolean hasGuardTower()
    {
        return hasGuardTower;
    }

    public boolean hasCook()
    {
        return hasCook;
    }

    public boolean hasTavern()
    {
        return hasTavern;
    }

    public int getPendingWorkOrders()
    {
        return pendingWorkOrders;
    }

    public int getEmptyBeds()
    {
        return emptyBeds;
    }

    public int getUnfilledWorkerSlots()
    {
        return unfilledWorkerSlots;
    }

    public int getJoblessAdults()
    {
        return joblessAdults;
    }

    public int getOperationalWorkHuts()
    {
        return operationalWorkHuts;
    }

    /**
     * @return {@code true} when housing is needed to support natural population growth for open jobs.
     */
    public boolean needsMoreHousing()
    {
        if (homelessCount > 0)
        {
            return true;
        }

        return emptyBeds <= 0 && unfilledWorkerSlots > 0;
    }

    public int getBuiltCount(@NotNull final PlannedBuildingType type)
    {
        return builtCounts.getOrDefault(type, 0);
    }

    public int getPendingCount(@NotNull final PlannedBuildingType type)
    {
        return pendingCounts.getOrDefault(type, 0);
    }

    @NotNull
    public BlockPos getColonyCenter()
    {
        return colonyCenter;
    }

    @Nullable
    public String getStructurePack()
    {
        return structurePack;
    }

    public double getGuardRatio()
    {
        return population <= 0 ? 0 : (double) guardCount / population;
    }

    public boolean hasCraftingChain()
    {
        return hasSawmill && hasStonemason && hasBlacksmith;
    }

    public boolean hasFoodSource()
    {
        return getBuiltCount(PlannedBuildingType.FARMER) > 0
          || getBuiltCount(PlannedBuildingType.FORESTER) > 0
          || getBuiltCount(PlannedBuildingType.MINER) > 0;
    }

    public int countBasicResourceBuildings()
    {
        return getBuiltCount(PlannedBuildingType.FARMER)
          + getBuiltCount(PlannedBuildingType.FORESTER)
          + getBuiltCount(PlannedBuildingType.MINER);
    }

    @NotNull
    public static Map<PlannedBuildingType, Integer> emptyCounts()
    {
        final Map<PlannedBuildingType, Integer> counts = new EnumMap<>(PlannedBuildingType.class);
        for (final PlannedBuildingType type : PlannedBuildingType.values())
        {
            counts.put(type, 0);
        }
        return counts;
    }
}
