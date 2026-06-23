package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.jobs.ModJobs;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Small bootstrap helper for the first builder hut.
 */
public final class ColdStartManager
{
    private ColdStartManager()
    {
    }

    public static boolean isOperationalBuilderHut(@NotNull final IBuilding building)
    {
        return isBuilderHut(building) && (building.getBuildingLevel() > 0 || building.isBuilt());
    }

    public static int countOperationalBuilderHuts(@NotNull final IColony colony)
    {
        int count = 0;
        for (final IBuilding building : org.Artificial.beastofburden.util.ColonyBuildings.getAllBuildings(colony))
        {
            if (isOperationalBuilderHut(building))
            {
                count++;
            }
        }
        return count;
    }

    public static boolean isColdStart(@NotNull final IColony colony)
    {
        return countOperationalBuilderHuts(colony) == 0;
    }

    public static boolean hasPendingBuilderHut(@NotNull final IColony colony)
    {
        return findPendingBuilderHut(colony) != null;
    }

    public static boolean isBootstrapping(@NotNull final IColony colony)
    {
        return isColdStart(colony) && hasPendingBuilderHut(colony);
    }

    @NotNull
    public static String tick(@NotNull final IColony colony)
    {
        final IBuilding hut = findPendingBuilderHut(colony);
        if (hut == null)
        {
            return "";
        }

        final HireResult result = tryHireBuilder(colony, hut);
        return switch (result)
        {
            case HIRED -> "cold_start:hired@" + hut.getPosition().toShortString();
            case NO_WORKER -> "cold_start:waiting_for_citizen";
            case FAILED -> "cold_start:hire_failed";
            case NOT_APPLICABLE -> "cold_start:builder_hut_pending";
        };
    }

    @NotNull
    public static HireResult tryHireBuilder(@NotNull final IColony colony, @NotNull final IBuilding hut)
    {
        if (!isBuilderHut(hut) || !hut.hasModule(IAssignsCitizen.class))
        {
            return HireResult.NOT_APPLICABLE;
        }
        if (!hut.getAllAssignedCitizen().isEmpty())
        {
            return HireResult.NOT_APPLICABLE;
        }

        final IAssignsCitizen module = hut.getFirstModuleOccurance(IAssignsCitizen.class);
        if (module.isFull())
        {
            return HireResult.NOT_APPLICABLE;
        }
        if (module.getHiringMode() != HiringMode.AUTO)
        {
            module.setHiringMode(HiringMode.AUTO);
        }

        final ICitizenData citizen = colony.getCitizenManager().getJoblessCitizen();
        if (citizen == null)
        {
            return HireResult.NO_WORKER;
        }

        if (!module.assignCitizen(citizen))
        {
            return HireResult.FAILED;
        }

        BeastofBurdenLog.info("Cold start: hired {} as builder for hut at {}", citizen.getName(), hut.getPosition());
        return HireResult.HIRED;
    }

    @Nullable
    public static IBuilding findPendingBuilderHut(@NotNull final IColony colony)
    {
        for (final IBuilding building : org.Artificial.beastofburden.util.ColonyBuildings.getAllBuildings(colony))
        {
            if (isBuilderHut(building) && !isOperationalBuilderHut(building))
            {
                return building;
            }
        }
        return null;
    }

    public static int countUnbuiltBuilderHuts(@NotNull final IColony colony)
    {
        int count = 0;
        for (final IBuilding building : org.Artificial.beastofburden.util.ColonyBuildings.getAllBuildings(colony))
        {
            if (isBuilderHut(building) && !isOperationalBuilderHut(building))
            {
                count++;
            }
        }
        return count;
    }

    @Nullable
    public static IBuilding getBuildingAt(@NotNull final IColony colony, @NotNull final BlockPos pos)
    {
        return colony.getWorld() == null ? null : IColonyManager.getInstance().getBuilding(colony.getWorld(), pos);
    }

    private static boolean isBuilderHut(@NotNull final IBuilding building)
    {
        return ModBuildings.builder.get().getRegistryName().equals(building.getBuildingType().getRegistryName());
    }

    public enum HireResult
    {
        HIRED,
        NO_WORKER,
        FAILED,
        NOT_APPLICABLE
    }
}
