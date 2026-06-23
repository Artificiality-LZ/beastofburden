package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.ColonyFieldSupport;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Counts live colony state for fixed-plan requirements.
 */
public final class ScriptedPlanProgress
{
    private ScriptedPlanProgress()
    {
    }

    public static boolean isRequirementMet(
      @NotNull final PlanningContext context,
      @NotNull final FixedPlanRequirement requirement)
    {
        return switch (requirement.getKind())
        {
            case BUILDING -> countCommittedBuildingSlots(context, requirement.getBuildingType(), requirement.getMinLevel())
              >= requirement.getCount();
            case FIELD -> countFields(context.colony()) >= requirement.getCount();
        };
    }

    public static boolean isStepOperationalComplete(
      @NotNull final PlanningContext context,
      @NotNull final FixedPlanStep step)
    {
        for (final FixedPlanRequirement requirement : step.getRequirements())
        {
            final boolean complete = switch (requirement.getKind())
            {
                case BUILDING -> countBuiltBuildingSlots(context, requirement.getBuildingType(), requirement.getMinLevel())
                  >= requirement.getCount();
                case FIELD -> countFields(context.colony()) >= requirement.getCount();
            };
            if (!complete)
            {
                return false;
            }
        }
        return true;
    }

    public static int countCommittedBuildingSlots(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      final int minLevel)
    {
        final Set<BlockPos> seen = new HashSet<>();
        int count = 0;

        for (final IBuilding building : context.buildings())
        {
            if (matchesCommittedBuilding(context.colony(), building, type, minLevel, seen))
            {
                count++;
            }
        }

        count += countPendingOrders(context, type, seen);
        return count;
    }

    public static int countBuiltBuildingSlots(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      final int minLevel)
    {
        final Set<BlockPos> seen = new HashSet<>();
        int count = 0;
        for (final IBuilding building : context.buildings())
        {
            if (building.getPosition() != null
                  && seen.add(building.getPosition())
                  && type.matches(building.getBuildingType())
                  && building.isBuilt()
                  && building.getBuildingLevel() >= minLevel)
            {
                count++;
            }
        }
        return count;
    }

    public static int countFields(@NotNull final IColony colony)
    {
        return ColonyFieldSupport.countFields(colony);
    }

    private static boolean matchesCommittedBuilding(
      @NotNull final IColony colony,
      @NotNull final IBuilding building,
      @NotNull final PlannedBuildingType type,
      final int minLevel,
      @NotNull final Set<BlockPos> seen)
    {
        final BlockPos pos = building.getPosition();
        if (pos == null || !seen.add(pos) || !type.matches(building.getBuildingType()))
        {
            return false;
        }

        if (building.getBuildingLevel() >= minLevel || building.isBuilt())
        {
            return true;
        }

        if (minLevel <= 1)
        {
            return true;
        }

        return hasWorkOrderAt(colony, pos);
    }

    private static int countPendingOrders(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      @NotNull final Set<BlockPos> seen)
    {
        int count = 0;
        try
        {
            for (final var order : context.colony().getWorkManager().getWorkOrders().values())
            {
                if (order == null || order.getLocation() == null || !isConstruction(order.getWorkOrderType()))
                {
                    continue;
                }
                if (!seen.add(order.getLocation()))
                {
                    continue;
                }

                final IBuilding building = com.minecolonies.api.colony.IColonyManager.getInstance()
                  .getBuilding(context.colony().getWorld(), order.getLocation());
                if ((building != null && type.matches(building.getBuildingType())) || type == mapOrderType(order.getTranslationKey()))
                {
                    count++;
                }
            }
        }
        catch (final Exception ignored)
        {
        }
        return count;
    }

    private static boolean hasWorkOrderAt(@NotNull final IColony colony, @NotNull final BlockPos pos)
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

    private static PlannedBuildingType mapOrderType(@NotNull final String translationKey)
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
