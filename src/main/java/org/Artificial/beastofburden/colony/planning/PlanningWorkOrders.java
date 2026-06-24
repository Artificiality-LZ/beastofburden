package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Helpers for inspecting MineColonies construction work orders.
 */
public final class PlanningWorkOrders
{
    private PlanningWorkOrders()
    {
    }

    public static boolean hasConstructionOrderAt(@NotNull final IColony colony, @NotNull final BlockPos pos)
    {
        try
        {
            for (final var order : colony.getWorkManager().getWorkOrders().values())
            {
                if (order != null && pos.equals(order.getLocation()) && isConstruction(order.getWorkOrderType()))
                {
                    return true;
                }
            }
        }
        catch (final Exception ignored)
        {
        }
        return false;
    }

    private static boolean isConstruction(@NotNull final WorkOrderType type)
    {
        return type == WorkOrderType.BUILD || type == WorkOrderType.UPGRADE || type == WorkOrderType.REPAIR;
    }
}
