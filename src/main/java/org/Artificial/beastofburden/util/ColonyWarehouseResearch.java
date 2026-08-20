package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.costs.IResearchCost;
import com.minecolonies.api.tileentities.AbstractTileEntityWareHouse;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Pays university research costs from colony warehouse storage.
 */
public final class ColonyWarehouseResearch
{
    private ColonyWarehouseResearch()
    {
    }

    public static boolean hasCosts(@NotNull final IColony colony, @NotNull final List<IResearchCost> costs)
    {
        if (costs.isEmpty())
        {
            return true;
        }

        for (final IResearchCost cost : costs)
        {
            if (!hasCost(colony, cost))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean payCosts(@NotNull final IColony colony, @NotNull final List<IResearchCost> costs)
    {
        if (!hasCosts(colony, costs))
        {
            return false;
        }

        for (final IResearchCost cost : costs)
        {
            if (!payCost(colony, cost))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean hasCosts(@NotNull final IColony colony, @NotNull final IGlobalResearch research)
    {
        return hasCosts(colony, research.getCostList());
    }

    public static boolean payCosts(@NotNull final IColony colony, @NotNull final IGlobalResearch research)
    {
        return payCosts(colony, research.getCostList());
    }

    private static boolean hasCost(@NotNull final IColony colony, @NotNull final IResearchCost cost)
    {
        for (final ItemStack template : toStacks(cost))
        {
            if (template.isEmpty())
            {
                continue;
            }

            boolean found = false;
            for (final IWareHouse warehouse : ColonyBuildings.getWarehouses(colony))
            {
                final AbstractTileEntityWareHouse tile = warehouse.getTileEntity();
                if (tile != null && tile.hasMatchingItemStackInWarehouse(template, template.getCount(), true, true, 0))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean payCost(@NotNull final IColony colony, @NotNull final IResearchCost cost)
    {
        for (final ItemStack template : toStacks(cost))
        {
            if (template.isEmpty())
            {
                continue;
            }

            int remaining = template.getCount();
            for (final IWareHouse warehouse : ColonyBuildings.getWarehouses(colony))
            {
                final AbstractTileEntityWareHouse tile = warehouse.getTileEntity();
                if (tile == null)
                {
                    continue;
                }

                final List<Tuple<ItemStack, BlockPos>> matches = tile.getMatchingItemStacksInWarehouse(
                  stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, template, true, true)
                );

                for (final Tuple<ItemStack, BlockPos> match : matches)
                {
                    if (remaining <= 0)
                    {
                        return true;
                    }

                    final Level world = colony.getWorld();
                    if (world == null)
                    {
                        return false;
                    }

                    final var blockEntity = world.getBlockEntity(match.getB());
                    if (blockEntity == null)
                    {
                        continue;
                    }

                    final IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                    if (handler == null)
                    {
                        continue;
                    }

                    for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++)
                    {
                        final ItemStack inSlot = handler.getStackInSlot(slot);
                        if (ItemStackUtils.isEmpty(inSlot)
                              || !ItemStackUtils.compareItemStacksIgnoreStackSize(inSlot, template, true, true))
                        {
                            continue;
                        }

                        remaining -= handler.extractItem(slot, remaining, false).getCount();
                    }
                }
            }

            if (remaining > 0)
            {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static List<ItemStack> toStacks(@NotNull final IResearchCost cost)
    {
        final List<ItemStack> stacks = new java.util.ArrayList<>();
        for (final Item item : cost.getItems())
        {
            stacks.add(new ItemStack(item, cost.getCount()));
        }
        return stacks;
    }
}
