package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks builder capacity and whether beasts are free to plan.
 */
public final class PlanningWorkload
{
    private PlanningWorkload()
    {
    }

    /**
     * @return {@code true} when at least one assigned beast is not generating or delivering.
     */
    public static boolean hasIdleBeast(@NotNull final TownHallBeastofburdenModule module)
    {
        if (module.getAssignedCitizen().isEmpty())
        {
            return false;
        }

        for (final var citizen : module.getAssignedCitizen())
        {
            if (!(citizen.getJob() instanceof JobBeastofburden job))
            {
                continue;
            }

            final EntityAIBeastofburden ai = job.getWorkerAI();
            if (ai == null || !ai.isExecutingLogisticsWork())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Human-readable reason why beasts block planning (empty when idle).
     */
    @NotNull
    public static String describeBeastBlockers(@NotNull final TownHallBeastofburdenModule module)
    {
        if (module.getAssignedCitizen().isEmpty())
        {
            return "no_assigned_beast";
        }

        final StringBuilder blockers = new StringBuilder();
        for (final var citizen : module.getAssignedCitizen())
        {
            if (!(citizen.getJob() instanceof JobBeastofburden job))
            {
                appendBlocker(blockers, citizen.getId() + ":wrong_job");
                continue;
            }

            final EntityAIBeastofburden ai = job.getWorkerAI();
            if (ai == null)
            {
                appendBlocker(blockers, citizen.getId() + ":no_ai");
                continue;
            }

            if (ai.getGenerationTask().isWorking())
            {
                appendBlocker(blockers, citizen.getId() + ":generating");
            }

            if (ai.getGenerationTask().hasPendingDelivery())
            {
                appendBlocker(blockers, citizen.getId() + ":delivering");
            }
        }

        return blockers.isEmpty() ? "" : blockers.toString();
    }

    private static void appendBlocker(@NotNull final StringBuilder blockers, @NotNull final String reason)
    {
        if (!blockers.isEmpty())
        {
            blockers.append(',');
        }
        blockers.append(reason);
    }

    /**
     * @return operational builder huts (level &gt; 0).
     */
    public static int countBuilderHuts(@NotNull final IColony colony)
    {
        return ColdStartManager.countOperationalBuilderHuts(colony);
    }

    /**
     * @return valid construction work orders (build / upgrade / repair) still in progress.
     */
    public static int countActiveConstructionOrders(@NotNull final IColony colony)
    {
        int count = 0;
        try
        {
            for (final IWorkOrder order : colony.getWorkManager().getWorkOrders().values())
            {
                if (order == null || !isConstructionOrder(order))
                {
                    continue;
                }

                if (!isOrderStillActive(order, colony))
                {
                    continue;
                }

                count++;
            }
        }
        catch (final Exception ignored)
        {
        }
        return count;
    }

    /**
     * @return {@code true} when planning may run (idle beasts + construction capacity or cold-start builder placement).
     */
    public static boolean canAttemptPlanning(@NotNull final IColony colony)
    {
        if (ColdStartManager.isBootstrapping(colony))
        {
            return false;
        }

        final int builders = countBuilderHuts(colony);
        final int active = countActiveConstructionOrders(colony);

        if (builders == 0)
        {
            return ColdStartManager.countUnbuiltBuilderHuts(colony) == 0;
        }

        return active < builders;
    }

    static boolean isOrderStillActive(@NotNull final IWorkOrder order, @NotNull final IColony colony)
    {
        if (order instanceof IServerWorkOrder serverOrder)
        {
            return serverOrder.isValid(colony);
        }
        return true;
    }

    private static boolean isConstructionOrder(@NotNull final IWorkOrder order)
    {
        final WorkOrderType type = order.getWorkOrderType();
        return type == WorkOrderType.BUILD || type == WorkOrderType.UPGRADE || type == WorkOrderType.REPAIR;
    }
}
