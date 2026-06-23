package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Assigns build tasks to the nearest capable builder hut.
 */
public final class BuilderAssigner
{
    private BuilderAssigner()
    {
    }

    private static int builderRadiusSq()
    {
        final int radius = PlanningConfig.builderRadius();
        return radius * radius;
    }

    private static int maxQueueSize()
    {
        return PlanningConfig.maxBuilderQueueSize();
    }

    @Nullable
    public static BlockPos assign(@NotNull final IColony colony, @NotNull final BlockPos target, final int targetLevel)
    {
        return assignOrDefault(colony, target, targetLevel, false);
    }

    @Nullable
    public static BlockPos assignOrDefault(
      @NotNull final IColony colony,
      @NotNull final BlockPos target,
      final int targetLevel,
      final boolean coldStart)
    {
        final List<BuilderCandidate> candidates = new ArrayList<>();

        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (!isBuilderHut(building) || building.getBuildingLevel() < targetLevel)
            {
                continue;
            }

            final int queueSize = countQueuedWork(colony, building.getID());
            if (queueSize >= maxQueueSize())
            {
                continue;
            }

            candidates.add(new BuilderCandidate(building.getID(), (long) building.getPosition().distSqr(target), queueSize));
        }

        final BlockPos chosen = candidates.stream()
          .min(Comparator.comparingLong(BuilderCandidate::distanceSq).thenComparingInt(BuilderCandidate::queueSize))
          .map(BuilderCandidate::position)
          .orElse(null);

        if (chosen != null)
        {
            return chosen;
        }

        return coldStart ? BlockPos.ZERO : null;
    }

    public static boolean isWithinBuilderRange(@NotNull final IColony colony, @NotNull final BlockPos target, final int targetLevel)
    {
        return isWithinBuilderRange(colony, target, targetLevel, false);
    }

    public static boolean isWithinBuilderRange(
      @NotNull final IColony colony,
      @NotNull final BlockPos target,
      final int targetLevel,
      final boolean coldStart)
    {
        if (coldStart)
        {
            return true;
        }

        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (isBuilderHut(building)
                  && building.getBuildingLevel() >= targetLevel
                  && building.getPosition().distSqr(target) <= builderRadiusSq())
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isBuilderHut(@NotNull final IBuilding building)
    {
        return ModBuildings.builder.get().getRegistryName().equals(building.getBuildingType().getRegistryName());
    }

    private static int countQueuedWork(@NotNull final IColony colony, @NotNull final BlockPos builderPos)
    {
        int count = 0;
        try
        {
            for (final var order : colony.getWorkManager().getWorkOrders().values())
            {
                if (order != null && builderPos.equals(order.getClaimedBy()))
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

    private record BuilderCandidate(@NotNull BlockPos position, long distanceSq, int queueSize)
    {
    }
}
