package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.Artificial.beastofburden.util.ColonyLogistics;
import org.Artificial.beastofburden.util.ColonyWorkforce;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Collects colony metrics for the planning engine.
 */
public final class ColonySnapshotCollector
{
    private ColonySnapshotCollector()
    {
    }

    @NotNull
    public static ColonySnapshot collect(@NotNull final IColony colony)
    {
        int population = 0;
        int homeless = 0;
        double saturationSum = 0;
        int guardCount = 0;

        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (citizen.isChild())
            {
                continue;
            }

            population++;
            saturationSum += citizen.getSaturation();

            if (citizen.getHomeBuilding() == null)
            {
                homeless++;
            }

            final IBuilding work = citizen.getWorkBuilding();
            if (work != null)
            {
                final String key = work.getBuildingType().getTranslationKey();
                if (key.contains("guard") || key.contains("barracks"))
                {
                    guardCount++;
                }
            }
        }

        final double avgSaturation = population > 0 ? saturationSum / population : 0;

        final Map<PlannedBuildingType, Integer> built = ColonySnapshot.emptyCounts();
        final Map<PlannedBuildingType, Integer> pending = ColonySnapshot.emptyCounts();

        boolean hasBuilder = false;
        int maxBuilderLevel = 0;
        boolean hasWarehouse = false;
        boolean hasSawmill = false;
        boolean hasStonemason = false;
        boolean hasBlacksmith = false;
        boolean hasUniversity = false;
        boolean hasGuardTower = false;
        boolean hasCook = false;
        boolean hasTavern = false;

        final String structurePack = StructurePackResolver.resolveColonyPack(colony);

        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            final PlannedBuildingType type = mapBuilding(building.getBuildingType());
            if (type != null)
            {
                if (building.isBuilt() && building.getBuildingLevel() > 0)
                {
                    built.merge(type, 1, Integer::sum);
                }

                if (type == PlannedBuildingType.BUILDER && ColdStartManager.isOperationalBuilderHut(building))
                {
                    hasBuilder = true;
                    maxBuilderLevel = Math.max(maxBuilderLevel, Math.max(1, building.getBuildingLevel()));
                }
                if (type == PlannedBuildingType.BUILDER && !ColdStartManager.isOperationalBuilderHut(building))
                {
                    pending.merge(type, 1, Integer::sum);
                }
                if (type == PlannedBuildingType.WAREHOUSE && building.getBuildingLevel() > 0)
                {
                    hasWarehouse = true;
                }
                if (type == PlannedBuildingType.SAWMILL && building.getBuildingLevel() > 0)
                {
                    hasSawmill = true;
                }
                if (type == PlannedBuildingType.STONEMASON && building.getBuildingLevel() > 0)
                {
                    hasStonemason = true;
                }
                if (type == PlannedBuildingType.BLACKSMITH && building.getBuildingLevel() > 0)
                {
                    hasBlacksmith = true;
                }
                if (type == PlannedBuildingType.UNIVERSITY && building.getBuildingLevel() > 0)
                {
                    hasUniversity = true;
                }
                if (type == PlannedBuildingType.GUARD_TOWER && building.getBuildingLevel() > 0)
                {
                    hasGuardTower = true;
                }
                if (type == PlannedBuildingType.COOK && building.getBuildingLevel() > 0)
                {
                    hasCook = true;
                }
                if (type == PlannedBuildingType.TAVERN && building.getBuildingLevel() > 0)
                {
                    hasTavern = true;
                }
            }

        }

        int pendingOrders = 0;
        try
        {
            for (final IWorkOrder order : colony.getWorkManager().getWorkOrders().values())
            {
                if (order == null || order.isClaimed())
                {
                    continue;
                }

                pendingOrders++;

                final WorkOrderType orderType = order.getWorkOrderType();
                if (orderType == WorkOrderType.BUILD || orderType == WorkOrderType.UPGRADE || orderType == WorkOrderType.REPAIR)
                {
                    final PlannedBuildingType type = mapTranslationKey(order.getTranslationKey());
                    if (type != null)
                    {
                        pending.merge(type, 1, Integer::sum);
                    }
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to inspect work orders for colony {}: {}", colony.getID(), ex.toString());
        }

        final ColonyWorkforce.Metrics workforce = ColonyWorkforce.collect(colony);

        return new ColonySnapshot(
          population,
          homeless,
          avgSaturation,
          guardCount,
          hasBuilder,
          maxBuilderLevel,
          hasWarehouse,
          ColonyLogistics.hasActiveDeliveryman(colony),
          hasSawmill,
          hasStonemason,
          hasBlacksmith,
          hasUniversity,
          hasGuardTower,
          hasCook,
          hasTavern,
          pendingOrders,
          workforce.emptyBeds(),
          workforce.unfilledWorkerSlots(),
          workforce.joblessAdults(),
          workforce.operationalWorkHuts(),
          built,
          pending,
          colony.getCenter(),
          structurePack
        );
    }

    @Nullable
    private static PlannedBuildingType mapBuilding(@NotNull final BuildingEntry entry)
    {
        return mapTranslationKey(entry.getTranslationKey());
    }

    @Nullable
    private static PlannedBuildingType mapTranslationKey(@NotNull final String translationKey)
    {
        for (final PlannedBuildingType type : PlannedBuildingType.values())
        {
            if (translationKey.equals(type.getEntry().getTranslationKey()))
            {
                return type;
            }
        }
        return null;
    }
}
