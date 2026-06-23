package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.Artificial.beastofburden.event.ColonyRequestEventHandler;
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
     * @return {@code true} when every assigned beast has no item-generation/delivery work.
     */
    public static boolean hasIdleBeast(@NotNull final TownHallBeastofburdenModule module)
    {
        if (module.getAssignedCitizen().isEmpty())
        {
            return false;
        }

        for (final var citizen : module.getAssignedCitizen())
        {
            if (citizen.getJob() instanceof JobBeastofburden job)
            {
                final EntityAIBeastofburden ai = job.getWorkerAI();
                if (ai != null && ai.isExecutingLogisticsWork())
                {
                    return false;
                }
            }
        }

        return true;
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

            final var queue = ColonyRequestEventHandler.getQueue(job.getColony());
            if (queue.hasInFlight())
            {
                appendBlocker(blockers, citizen.getId() + ":inFlight=" + queue.hasInFlight());
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

    /**
     * @return {@code true} when there is at least one builder and fewer active jobs than builders.
     */
    public static boolean hasBuilderCapacity(@NotNull final IColony colony)
    {
        final int builders = countBuilderHuts(colony);
        return builders > 0 && countActiveConstructionOrders(colony) < builders;
    }

    static boolean isOrderStillActive(@NotNull final IWorkOrder order, @NotNull final IColony colony)
    {
        try
        {
            final Object result = order.getClass().getMethod("isValid", IColony.class).invoke(order, colony);
            return result instanceof Boolean bool && bool;
        }
        catch (final ReflectiveOperationException ex)
        {
            return true;
        }
    }

    private static boolean isConstructionOrder(@NotNull final IWorkOrder order)
    {
        final WorkOrderType type = order.getWorkOrderType();
        return type == WorkOrderType.BUILD || type == WorkOrderType.UPGRADE || type == WorkOrderType.REPAIR;
    }
}
