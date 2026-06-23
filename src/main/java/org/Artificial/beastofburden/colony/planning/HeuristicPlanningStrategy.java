package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Conservative score-based strategy for open-ended colony growth.
 */
public final class HeuristicPlanningStrategy implements PlanningStrategy
{
    @Override
    @Nullable
    public BuildTask selectNextTask(@NotNull final PlanningContext context)
    {
        final var colony = context.colony();
        final ColonySnapshot snapshot = context.snapshot();
        final List<BuildTask> candidates = new ArrayList<>();

        if (!snapshot.hasBuilderHut())
        {
            if (snapshot.getPendingCount(PlannedBuildingType.BUILDER) == 0 && !ColdStartManager.hasPendingBuilderHut(colony))
            {
                return new BuildTask(PlannedBuildingType.BUILDER, BuildTaskAction.BUILD_NEW, 1, 1000, null, "foundation");
            }
            return null;
        }

        addFieldTask(colony, snapshot, candidates);
        addBuildTask(snapshot, candidates, PlannedBuildingType.RESIDENCE, housingScore(snapshot), "housing");
        addBuildTask(snapshot, candidates, PlannedBuildingType.TAVERN, snapshot.hasTavern() ? 0 : 620, "first_tavern");
        addBuildTask(snapshot, candidates, PlannedBuildingType.WAREHOUSE, snapshot.hasWarehouse() ? 0 : 580, "logistics");
        addBuildTask(snapshot, candidates, PlannedBuildingType.COURIER, snapshot.hasActiveCourier() ? 0 : 540, "logistics");
        addBuildTask(snapshot, candidates, PlannedBuildingType.FARMER, foodScore(snapshot, PlannedBuildingType.FARMER), "food");
        addBuildTask(snapshot, candidates, PlannedBuildingType.COOK, snapshot.hasCook() || !snapshot.hasFoodSource() ? 0 : 430, "food_chain");
        addBuildTask(snapshot, candidates, PlannedBuildingType.FORESTER, resourceScore(snapshot, PlannedBuildingType.FORESTER), "resource");
        addBuildTask(snapshot, candidates, PlannedBuildingType.MINER, resourceScore(snapshot, PlannedBuildingType.MINER), "resource");
        addBuildTask(snapshot, candidates, PlannedBuildingType.GUARD_TOWER, guardScore(snapshot), "security");
        addBuildTask(snapshot, candidates, PlannedBuildingType.BUILDER, builderCoverageScore(snapshot), "builder_coverage");
        addBuildTask(snapshot, candidates, PlannedBuildingType.UNIVERSITY, universityScore(snapshot), "research");

        addUpgradeTask(colony, snapshot, candidates, PlannedBuildingType.BUILDER, 3, 260, "builder_upgrade");
        addUpgradeTask(colony, snapshot, candidates, PlannedBuildingType.TOWN_HALL, 5, 180, "townhall_upgrade");

        return candidates.stream().max(Comparator.comparingDouble(BuildTask::getPriority)).orElse(null);
    }

    @Override
    @NotNull
    public String describeState(@NotNull final PlanningContext context)
    {
        final ColonySnapshot snapshot = context.snapshot();
        return "heuristic beds=" + snapshot.getEmptyBeds()
          + " jobSlots=" + snapshot.getUnfilledWorkerSlots()
          + " jobless=" + snapshot.getJoblessAdults();
    }

    private static void addFieldTask(
      @NotNull final com.minecolonies.api.colony.IColony colony,
      @NotNull final ColonySnapshot snapshot,
      @NotNull final List<BuildTask> candidates)
    {
        final BlockPos farmerPos = FieldPlanner.findFarmerNeedingField(colony);
        if (farmerPos != null)
        {
            candidates.add(new BuildTask(
              PlannedBuildingType.FARMER,
              BuildTaskAction.PLACE_FIELD,
              1,
              snapshot.hasFoodSource() ? 240 : 520,
              farmerPos,
              "farmer_field"
            ));
        }
    }

    private static void addBuildTask(
      @NotNull final ColonySnapshot snapshot,
      @NotNull final List<BuildTask> candidates,
      @NotNull final PlannedBuildingType type,
      final float score,
      @NotNull final String reason)
    {
        if (score <= 0 || snapshot.getPendingCount(type) > 0)
        {
            return;
        }

        candidates.add(new BuildTask(type, BuildTaskAction.BUILD_NEW, 1, score, null, reason));
    }

    private static void addUpgradeTask(
      @NotNull final com.minecolonies.api.colony.IColony colony,
      @NotNull final ColonySnapshot snapshot,
      @NotNull final List<BuildTask> candidates,
      @NotNull final PlannedBuildingType type,
      final int maxTarget,
      final float baseScore,
      @NotNull final String reason)
    {
        final IBuilding target = findUpgradeTarget(colony, type, maxTarget);
        if (target == null)
        {
            return;
        }

        candidates.add(new BuildTask(
          type,
          BuildTaskAction.UPGRADE,
          target.getBuildingLevel() + 1,
          baseScore + snapshot.getPopulation() * 4,
          target.getID(),
          reason
        ));
    }

    private static float housingScore(@NotNull final ColonySnapshot snapshot)
    {
        if (!snapshot.needsMoreHousing())
        {
            return 0;
        }
        return 520 + snapshot.getUnfilledWorkerSlots() * 20 + snapshot.getHomelessCount() * 40;
    }

    private static float foodScore(@NotNull final ColonySnapshot snapshot, @NotNull final PlannedBuildingType type)
    {
        if (snapshot.getBuiltCount(type) > 0)
        {
            return snapshot.getAvgSaturation() < 6.0 ? 300 : 0;
        }
        return snapshot.getAvgSaturation() < 8.0 || !snapshot.hasFoodSource() ? 560 : 260;
    }

    private static float resourceScore(@NotNull final ColonySnapshot snapshot, @NotNull final PlannedBuildingType type)
    {
        return snapshot.getBuiltCount(type) == 0 && snapshot.getPopulation() >= 3 ? 360 : 0;
    }

    private static float guardScore(@NotNull final ColonySnapshot snapshot)
    {
        return snapshot.getPopulation() >= 6 && snapshot.getGuardRatio() < 0.35 ? 420 : 0;
    }

    private static float builderCoverageScore(@NotNull final ColonySnapshot snapshot)
    {
        if (snapshot.getPendingCount(PlannedBuildingType.BUILDER) > 0)
        {
            return 0;
        }
        return snapshot.getPopulation() >= 8 && snapshot.getOperationalWorkHuts() > snapshot.getBuiltCount(PlannedBuildingType.BUILDER) * 4
          ? 320
          : 0;
    }

    private static float universityScore(@NotNull final ColonySnapshot snapshot)
    {
        return !snapshot.hasUniversity() && snapshot.hasCraftingChain() ? 300 : 0;
    }

    @Nullable
    private static IBuilding findUpgradeTarget(
      @NotNull final com.minecolonies.api.colony.IColony colony,
      @NotNull final PlannedBuildingType type,
      final int maxTarget)
    {
        IBuilding lowest = null;
        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (!type.matches(building.getBuildingType())
                  || !building.isBuilt()
                  || building.getBuildingLevel() <= 0
                  || building.getBuildingLevel() >= maxTarget
                  || building.getBuildingLevel() >= building.getMaxBuildingLevel())
            {
                continue;
            }
            if (lowest == null || building.getBuildingLevel() < lowest.getBuildingLevel())
            {
                lowest = building;
            }
        }
        return lowest;
    }
}
