package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IDefinesCoreBuildingStatsModule;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.Artificial.beastofburden.colony.planning.PlannedBuildingType;
import org.jetbrains.annotations.NotNull;

/**
 * Colony housing and worker-slot metrics for population/work alignment.
 */
public final class ColonyWorkforce
{
    private ColonyWorkforce()
    {
    }

    @NotNull
    public static Metrics collect(@NotNull final IColony colony)
    {
        int bedCapacity = 0;
        int bedsUsed = 0;
        int unfilledWorkerSlots = 0;
        int operationalWorkHuts = 0;

        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            final PlannedBuildingType plannedType = PlannedBuildingType.fromEntry(building.getBuildingType());
            if (plannedType == null || !isOperational(building, plannedType))
            {
                continue;
            }

            if (isHousingType(plannedType))
            {
                final int capacity = housingBedCapacity(building);
                if (capacity <= 0)
                {
                    continue;
                }

                bedCapacity += capacity;
                bedsUsed += housingBedsUsed(building, colony);
            }
            else
            {
                int moduleMax = 0;
                int moduleAssigned = 0;
                for (final IAssignsCitizen module : building.getModulesByType(IAssignsCitizen.class))
                {
                    moduleMax += Math.max(0, module.getModuleMax());
                    moduleAssigned += module.getAssignedCitizen().size();
                }

                if (moduleMax <= 0)
                {
                    continue;
                }

                if (isWorkerHutType(plannedType))
                {
                    operationalWorkHuts++;
                    unfilledWorkerSlots += Math.max(0, moduleMax - moduleAssigned);
                }
            }
        }

        int joblessAdults = 0;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (citizen.isChild())
            {
                continue;
            }

            if (citizen.getWorkBuilding() == null)
            {
                joblessAdults++;
            }
        }

        final int emptyBeds = Math.max(0, bedCapacity - bedsUsed);
        return new Metrics(bedCapacity, emptyBeds, unfilledWorkerSlots, joblessAdults, operationalWorkHuts);
    }

    /**
     * @return open worker slots on operational huts of this type.
     */
    public static int countUnfilledSlotsForType(@NotNull final IColony colony, @NotNull final PlannedBuildingType type)
    {
        int open = 0;
        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            final PlannedBuildingType buildingType = PlannedBuildingType.fromEntry(building.getBuildingType());
            if (buildingType == null || !isOperational(building, buildingType) || !matchesPlannedType(building.getBuildingType(), type))
            {
                continue;
            }

            for (final IAssignsCitizen module : building.getModulesByType(IAssignsCitizen.class))
            {
                open += Math.max(0, module.getModuleMax() - module.getAssignedCitizen().size());
            }
        }
        return open;
    }

    private static boolean isOperational(@NotNull final IBuilding building, @NotNull final PlannedBuildingType type)
    {
        if (isHousingType(type))
        {
            return building.getBuildingLevel() > 0 || building.isBuilt();
        }

        return building.getBuildingLevel() > 0 && (building.isBuilt() || !building.hasWorkOrder());
    }

    private static int housingBedCapacity(@NotNull final IBuilding building)
    {
        int capacity = 0;
        for (final IAssignsCitizen module : building.getModulesByType(IAssignsCitizen.class))
        {
            capacity += Math.max(0, module.getModuleMax());
        }

        if (capacity > 0)
        {
            return capacity;
        }

        for (final IDefinesCoreBuildingStatsModule module : building.getModulesByType(IDefinesCoreBuildingStatsModule.class))
        {
            final var stat = module.getMaxInhabitants();
            if (stat != null)
            {
                capacity = Math.max(capacity, stat.apply(building.getBuildingLevel()));
            }
        }

        if (capacity > 0)
        {
            return capacity;
        }

        return Math.max(0, building.getBuildingLevel());
    }

    private static int housingBedsUsed(@NotNull final IBuilding building, @NotNull final IColony colony)
    {
        final int assigned = building.getAllAssignedCitizen().size();
        if (assigned > 0)
        {
            return assigned;
        }

        int residents = 0;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (!citizen.isChild() && building.equals(citizen.getHomeBuilding()))
            {
                residents++;
            }
        }
        return residents;
    }

    private static boolean isHousingType(@NotNull final PlannedBuildingType type)
    {
        return type == PlannedBuildingType.RESIDENCE || type == PlannedBuildingType.TAVERN;
    }

    private static boolean isWorkerHutType(@NotNull final PlannedBuildingType type)
    {
        return type != PlannedBuildingType.TOWN_HALL
          && type != PlannedBuildingType.WAREHOUSE
          && type != PlannedBuildingType.BUILDER
          && !isHousingType(type);
    }

    private static boolean matchesPlannedType(@NotNull final BuildingEntry entry, @NotNull final PlannedBuildingType type)
    {
        return entry.getTranslationKey().equals(type.getEntry().getTranslationKey());
    }

    public record Metrics(int bedCapacity, int emptyBeds, int unfilledWorkerSlots, int joblessAdults, int operationalWorkHuts)
    {
        /**
         * More housing when beds are full but work huts still need citizens (natural growth via children).
         */
        public boolean needsMoreHousing(final int homelessCount)
        {
            if (homelessCount > 0)
            {
                return true;
            }

            return bedCapacity > 0 && emptyBeds <= 0 && unfilledWorkerSlots > 0;
        }
    }
}
