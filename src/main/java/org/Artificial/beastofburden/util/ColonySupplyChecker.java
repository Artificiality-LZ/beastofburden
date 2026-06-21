package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.tileentities.AbstractTileEntityWareHouse;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Checks whether the colony can still supply a request through normal logistics.
 */
public final class ColonySupplyChecker
{
    private ColonySupplyChecker()
    {
    }

    /**
     * @return {@code true} when the requester building or a warehouse already has enough of the requested item/tool.
     */
    public static boolean canColonySupply(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        if (!(request.getRequest() instanceof IDeliverable deliverable))
        {
            return false;
        }

        if (canFulfillFromRequesterBuilding(colony, request, deliverable))
        {
            return true;
        }

        return canFulfillFromWarehouse(colony, request, deliverable);
    }

    private static boolean canFulfillFromWarehouse(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request,
      @NotNull final IDeliverable deliverable)
    {
        if (!ColonyLogistics.hasWarehouse(colony))
        {
            return false;
        }

        if (deliverable instanceof Tool tool)
        {
            return warehouseHasMatchingTool(colony, tool);
        }

        final ItemStack probe = probeStack(request, deliverable);
        if (ItemStackUtils.isEmpty(probe))
        {
            return false;
        }

        try
        {
            final int needed = Math.max(1, deliverable.getCount());
            final int minimum = Math.max(1, deliverable.getMinimumCount());

            for (final IWareHouse warehouse : ColonyBuildings.getWarehouses(colony))
            {
                final AbstractTileEntityWareHouse tile = warehouse.getTileEntity();
                if (tile == null)
                {
                    continue;
                }

                if (tile.hasMatchingItemStackInWarehouse(probe, needed, true))
                {
                    return true;
                }

                if (minimum < needed && tile.hasMatchingItemStackInWarehouse(probe, minimum, true))
                {
                    return true;
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to inspect warehouse stock for colony {}: {}", colony.getID(), ex.toString());
        }

        return false;
    }

    private static boolean warehouseHasMatchingTool(@NotNull final IColony colony, @NotNull final Tool tool)
    {
        try
        {
            for (final IWareHouse warehouse : ColonyBuildings.getWarehouses(colony))
            {
                final AbstractTileEntityWareHouse tile = warehouse.getTileEntity();
                if (tile == null)
                {
                    continue;
                }

                if (tile.hasMatchingItemStackInWarehouse(tool.getResult(), 1, true))
                {
                    return true;
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to inspect warehouse tools for colony {}: {}", colony.getID(), ex.toString());
        }

        return false;
    }

    private static boolean canFulfillFromRequesterBuilding(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request,
      @NotNull final IDeliverable deliverable)
    {
        final var target = ColonyLogistics.findRequestTarget(colony, request.getId());
        if (target.isEmpty() || colony.getWorld() == null)
        {
            return false;
        }

        final BlockEntity blockEntity = colony.getWorld().getBlockEntity(target.get().building().getID());
        if (blockEntity == null)
        {
            return false;
        }

        if (deliverable instanceof Tool tool)
        {
            return buildingHasMatchingTool(blockEntity, tool);
        }

        final ItemStack probe = probeStack(request, deliverable);
        if (ItemStackUtils.isEmpty(probe))
        {
            return false;
        }

        final int needed = Math.max(1, deliverable.getMinimumCount() > 0 ? deliverable.getMinimumCount() : deliverable.getCount());
        return InventoryUtils.hasEnoughInProvider(blockEntity, probe, needed);
    }

    private static boolean buildingHasMatchingTool(@NotNull final BlockEntity blockEntity, @NotNull final Tool tool)
    {
        if (!ItemStackUtils.isEmpty(tool.getResult()) && InventoryUtils.hasEnoughInProvider(blockEntity, tool.getResult(), 1))
        {
            return true;
        }

        for (final ItemStack candidate : InventoryUtils.getInventoryAsListFromProviderForSide(blockEntity, null))
        {
            if (tool.matches(candidate))
            {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private static ItemStack probeStack(@NotNull final IRequest<?> request, @NotNull final IDeliverable deliverable)
    {
        ItemStack probe = RequestItemUtils.extractItemStack(request);
        if (ItemStackUtils.isEmpty(probe))
        {
            probe = deliverable.getResult();
        }

        if (ItemStackUtils.isEmpty(probe))
        {
            probe = deliverable.copyWithCount(1).getResult();
        }

        if (!ItemStackUtils.isEmpty(probe))
        {
            probe = probe.copy();
            probe.setCount(1);
        }

        return probe;
    }
}
