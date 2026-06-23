package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks world space already reserved by existing colony structures.
 */
public final class OccupancyMap
{
    /** Minimum empty blocks between blueprint bounds of two huts (see {@link PlanningConfig#minBlueprintSeparation()}). */
    public static int minBlueprintSeparation()
    {
        return PlanningConfig.minBlueprintSeparation();
    }

    private OccupancyMap()
    {
    }

    @NotNull
    public static List<BuildingFootprint> collectFootprints(@NotNull final IColony colony)
    {
        return collectFootprints(colony, ColonyBuildings.getAllBuildings(colony));
    }

    @NotNull
    public static List<BuildingFootprint> collectFootprints(
      @NotNull final IColony colony,
      @NotNull final Collection<IBuilding> buildings)
    {
        final Level world = colony.getWorld();
        final List<BuildingFootprint> footprints = new ArrayList<>();
        final Set<BlockPos> seenAnchors = new HashSet<>();

        for (final IBuilding building : buildings)
        {
            addBuildingFootprint(footprints, seenAnchors, building, world);
        }

        for (final IBuilding building : ColonyBuildings.findManagerBuildingsMissingFromAggregate(colony, buildings))
        {
            addBuildingFootprint(footprints, seenAnchors, building, world);
        }

        try
        {
            for (final IWorkOrder order : colony.getWorkManager().getWorkOrders().values())
            {
                if (order == null || !isConstructionWorkOrder(order.getWorkOrderType()))
                {
                    continue;
                }

                final BlockPos location = order.getLocation();
                if (location == null)
                {
                    continue;
                }

                final boolean anchorAlreadySeen = seenAnchors.contains(location);
                if (world != null)
                {
                    final IBuilding atLocation = com.minecolonies.api.colony.IColonyManager.getInstance().getBuilding(world, location);
                    if (atLocation != null)
                    {
                        addBuildingFootprint(footprints, seenAnchors, atLocation, world);
                        if (order.getWorkOrderType() == WorkOrderType.BUILD)
                        {
                            continue;
                        }
                    }
                }

                if (anchorAlreadySeen
                      && order.getWorkOrderType() != WorkOrderType.UPGRADE
                      && order.getWorkOrderType() != WorkOrderType.REPAIR)
                {
                    continue;
                }

                final BuildingFootprint footprint = footprintFromWorkOrder(order);
                if (footprint != null)
                {
                    footprints.add(footprint);
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to inspect work orders for colony {} footprints: {}", colony.getID(), ex.toString());
        }

        return footprints;
    }

    private static void addBuildingFootprint(
      @NotNull final List<BuildingFootprint> footprints,
      @NotNull final Set<BlockPos> seenAnchors,
      @NotNull final IBuilding building,
      @Nullable final Level world)
    {
        final BlockPos anchor = building.getPosition();
        if (anchor == null || !seenAnchors.add(anchor))
        {
            return;
        }

        footprints.add(BuildingFootprint.fromBuilding(building, world));
    }

    private static boolean isConstructionWorkOrder(@NotNull final WorkOrderType type)
    {
        return type == WorkOrderType.BUILD || type == WorkOrderType.UPGRADE || type == WorkOrderType.REPAIR;
    }

    @NotNull
    public static Set<BlockPos> collectReservedFootprint(@NotNull final IColony colony)
    {
        return collectReservedFootprint(collectFootprints(colony));
    }

    @NotNull
    public static Set<BlockPos> collectReservedFootprint(@NotNull final List<BuildingFootprint> footprints)
    {
        final Set<BlockPos> reserved = new HashSet<>();

        for (final BuildingFootprint footprint : footprints)
        {
            addFootprintCells(reserved, footprint);
        }

        return reserved;
    }

    public static boolean overlapsFootprints(
      @NotNull final Level world,
      @NotNull final List<BuildingFootprint> existing,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final BuildingFootprint candidate = BuildingFootprint.fromSchematic(anchor, world, blueprint, facing, false);
        final int minGap = Math.max(1, minBlueprintSeparation());

        for (final BuildingFootprint footprint : existing)
        {
            if (candidate.conflictsWith(footprint, minGap))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean overlapsAnchor(@NotNull final Set<BlockPos> reserved, @NotNull final BlockPos anchor)
    {
        return reserved.contains(columnKey(anchor));
    }

    /**
     * Clears natural terrain at the hut anchor before construction tape placement.
     *
     * @return false when a non-clearable block remains at the anchor or headroom cell.
     */
    public static boolean prepareAnchorSite(@NotNull final Level world, @NotNull final BlockPos anchor)
    {
        if (!clearAnchorCell(world, anchor))
        {
            return false;
        }

        return clearAnchorCell(world, anchor.above());
    }

    private static boolean clearAnchorCell(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        final BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced())
        {
            if (!state.isAir())
            {
                world.destroyBlock(pos, false);
            }
            return true;
        }

        if (isClearable(state, true))
        {
            world.destroyBlock(pos, false);
            return world.getBlockState(pos).isAir() || world.getBlockState(pos).canBeReplaced();
        }

        return false;
    }

    /**
     * Loose anchor check for candidate search – builders clear vegetation and level terrain.
     */
    public static boolean isLooseAnchorCandidate(
      @NotNull final Level world,
      @NotNull final BlockPos anchor,
      @NotNull final Set<BlockPos> reserved)
    {
        if (overlapsAnchor(reserved, anchor))
        {
            return false;
        }

        final BlockState below = world.getBlockState(anchor.below());
        return !below.isAir()
          && !below.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER)
          && !below.getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA);
    }

    private static void addFootprintCells(
      @NotNull final Set<BlockPos> reserved,
      @NotNull final BuildingFootprint footprint)
    {
        for (int x = footprint.minX(); x <= footprint.maxX(); x++)
        {
            for (int z = footprint.minZ(); z <= footprint.maxZ(); z++)
            {
                reserved.add(columnKey(x, z));
            }
        }
    }

    @NotNull
    private static BlockPos columnKey(@NotNull final BlockPos pos)
    {
        return columnKey(pos.getX(), pos.getZ());
    }

    @NotNull
    private static BlockPos columnKey(final int x, final int z)
    {
        return new BlockPos(x, 0, z);
    }

    @Nullable
    private static BuildingFootprint footprintFromWorkOrder(@NotNull final IWorkOrder order)
    {
        final BlockPos location = order.getLocation();
        if (location == null)
        {
            return null;
        }

        try
        {
            final AABB box = order.getBoundingBox();
            if (box != null && box.getXsize() > 0 && box.getYsize() > 0 && box.getZsize() > 0)
            {
                return BuildingFootprint.fromAabb(box);
            }
        }
        catch (final Exception ignored)
        {
        }

        final String pack = BlueprintPaths.defaultPack(order.getStructurePack());
        final String path = order.getStructurePath();
        if (path == null || path.isEmpty() || !StructurePacks.hasPack(pack))
        {
            return BuildingFootprint.fromAnchor(location);
        }

        try
        {
            final Blueprint blueprint = StructurePacks.getBlueprint(pack, path);
            if (blueprint == null)
            {
                return BuildingFootprint.fromAnchor(location);
            }

            final var world = order.getColony() == null ? null : order.getColony().getWorld();
            final Direction facing = BuildingFootprint.rotationToFacing(order.getRotation());
            if (world != null)
            {
                return BuildingFootprint.fromSchematic(location, world, blueprint, facing, order.isMirrored());
            }

            return BuildingFootprint.fromBlueprint(blueprint, location, facing);
        }
        catch (final Exception ex)
        {
            return BuildingFootprint.fromAnchor(location);
        }
    }

    static boolean isClearable(@NotNull final BlockState state, final boolean allowNaturalClear)
    {
        if (state.isAir() || state.canBeReplaced())
        {
            return true;
        }

        if (!allowNaturalClear)
        {
            return false;
        }

        return state.is(BlockTags.LEAVES)
          || state.is(BlockTags.LOGS)
          || state.is(BlockTags.REPLACEABLE)
          || state.is(Blocks.GRASS_BLOCK)
          || state.is(Blocks.DIRT)
          || state.is(Blocks.COARSE_DIRT)
          || state.is(Blocks.ROOTED_DIRT)
          || state.is(Blocks.PODZOL)
          || state.is(Blocks.MYCELIUM)
          || state.is(Blocks.SNOW)
          || state.is(Blocks.SAND)
          || state.is(Blocks.GRAVEL)
          || state.is(Blocks.STONE)
          || state.is(Blocks.DEEPSLATE)
          || state.is(Blocks.TUFF)
          || state.is(Blocks.ANDESITE)
          || state.is(Blocks.DIORITE)
          || state.is(Blocks.GRANITE);
    }
}
