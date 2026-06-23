package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * All data needed by one autonomous planning pass.
 */
public final class PlanningContext
{
    private final IColony colony;
    private final ColonySnapshot snapshot;
    private Collection<IBuilding> buildings;
    private List<BuildingFootprint> footprints;
    private Set<BlockPos> occupiedColumns;
    private Set<BlockPos> roadNodes;

    private PlanningContext(@NotNull final IColony colony, @NotNull final ColonySnapshot snapshot)
    {
        this.colony = colony;
        this.snapshot = snapshot;
    }

    @NotNull
    public static PlanningContext collect(@NotNull final IColony colony)
    {
        return new PlanningContext(colony, ColonySnapshotCollector.collect(colony));
    }

    @NotNull
    public IColony colony()
    {
        return colony;
    }

    @NotNull
    public ColonySnapshot snapshot()
    {
        return snapshot;
    }

    @NotNull
    public Collection<IBuilding> buildings()
    {
        if (buildings == null)
        {
            buildings = new ArrayList<>(ColonyBuildings.getAllBuildings(colony));
        }
        return buildings;
    }

    @NotNull
    public List<BuildingFootprint> footprints()
    {
        if (footprints == null)
        {
            footprints = OccupancyMap.collectFootprints(colony, buildings());
        }
        return footprints;
    }

    @NotNull
    public Set<BlockPos> occupiedColumns()
    {
        if (occupiedColumns == null)
        {
            occupiedColumns = OccupancyMap.collectReservedFootprint(footprints());
        }
        return occupiedColumns;
    }

    @NotNull
    public Set<BlockPos> roadNodes()
    {
        if (roadNodes == null)
        {
            roadNodes = RoadPlanner.collectNetworkNodes(colony, buildings());
        }
        return roadNodes;
    }
}
