package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bootstrap helper for the first builder hut.
 * <p>
 * MineColonies rejects a construction work order when no builder is assigned yet
 * ({@code BUILDER_NECESSARY} / {@code BUILDER_TOO_FAR_AWAY}). Cold start therefore:
 * place the hut block → hire a jobless citizen as builder → then request construction.
 */
public final class ColdStartManager
{
    private ColdStartManager()
    {
        throw new IllegalStateException("Utility class");
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

    /**
     * First builder hut: keep the block in the world and hire before creating a work order.
     */
    public static boolean shouldDeferWorkOrder(@NotNull final IColony colony, @NotNull final BuildTask task)
    {
        return task.getType() == PlannedBuildingType.BUILDER
          && task.getAction() == BuildTaskAction.BUILD_NEW
          && isColdStart(colony);
    }

    @NotNull
    public static String tick(@NotNull final IColony colony)
    {
        final IBuilding hut = findPendingBuilderHut(colony);
        if (hut == null)
        {
            return "";
        }
        return bootstrap(colony, hut);
    }

    /**
     * Hire a builder into the unbuilt hut if needed, then request its construction work order.
     */
    @NotNull
    public static String bootstrap(@NotNull final IColony colony, @NotNull final IBuilding hut)
    {
        if (isOperationalBuilderHut(hut))
        {
            final HireResult hire = tryHireBuilder(colony, hut);
            return hire == HireResult.HIRED
              ? "cold_start:hired@" + hut.getPosition().toShortString()
              : "";
        }

        final HireResult hire = tryHireBuilder(colony, hut);
        if (hire == HireResult.NO_WORKER)
        {
            return "cold_start:waiting_for_citizen";
        }
        if (hire == HireResult.FAILED)
        {
            return "cold_start:hire_failed";
        }
        if (hut.getAllAssignedCitizen().isEmpty())
        {
            return "cold_start:waiting_for_citizen";
        }

        if (PlanningWorkOrders.hasConstructionOrderAt(colony, hut.getPosition()))
        {
            return hire == HireResult.HIRED
              ? "cold_start:hired@" + hut.getPosition().toShortString()
              : "cold_start:builder_hut_pending";
        }

        if (!requestConstruction(colony, hut))
        {
            return "cold_start:work_order_failed";
        }

        return hire == HireResult.HIRED
          ? "cold_start:hired@" + hut.getPosition().toShortString()
          : "cold_start:work_order_requested";
    }

    /**
     * Town Hall waiting token while the first builder hut is not yet operational.
     */
    @NotNull
    public static String waitingDecision(@NotNull final String bootstrapNote)
    {
        if (bootstrapNote.contains("waiting_for_citizen") || bootstrapNote.contains("hire_failed"))
        {
            return "waiting_for_builder";
        }
        if (bootstrapNote.contains("work_order_failed"))
        {
            return "work_order_failed";
        }
        return "builder_construction_pending";
    }

    private static boolean requestConstruction(@NotNull final IColony colony, @NotNull final IBuilding hut)
    {
        final BlockPos hutPos = hut.getPosition();
        if (PlanningWorkOrders.hasConstructionOrderAt(colony, hutPos))
        {
            return true;
        }

        try
        {
            // Claim the order on this hut so the newly hired builder can build their own workplace.
            // Passing ZERO makes MineColonies treat the site as "too far from any builder".
            hut.requestUpgrade(null, hutPos);
        }
        catch (final RuntimeException ex)
        {
            BeastofBurdenLog.warn("Cold start: failed to request builder hut construction at {} in colony {}: {}", hutPos, colony.getID(), ex.toString());
            return false;
        }

        if (!PlanningWorkOrders.hasConstructionOrderAt(colony, hutPos))
        {
            BeastofBurdenLog.warn("Cold start: no work order after requesting builder hut construction at {}", hutPos);
            return false;
        }

        BeastofBurdenLog.info("Cold start: requested construction of builder hut at {}", hutPos);
        return true;
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
