package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Road network reachability checks and simple auto-paving between buildings and the colony core.
 */
public final class RoadPlanner
{
    private static final int MAX_ACCESS_STEPS = 128;
    private static final int MAX_PAVE_BLOCKS = 48;
    private static final int SEARCH_LIMIT = 900;

    private RoadPlanner()
    {
    }

    public static void paveEntrance(@NotNull final IColony colony, @NotNull final BlockPos anchor, @NotNull final Direction facing)
    {
        final Level world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return;
        }

        final BlockPos entrance = surfacePos(world, anchor.relative(facing));
        final Set<BlockPos> network = collectNetworkNodes(colony);
        final BlockPos targetNode = nearestNode(entrance, network);
        if (targetNode == null)
        {
            return;
        }

        if (entrance.distSqr(targetNode) <= nearNetworkDistanceSq())
        {
            paveFootprint(world, colony, entrance);
            return;
        }

        final List<BlockPos> path = findWalkPath(world, colony, targetNode, entrance);
        if (path.isEmpty())
        {
            return;
        }

        int paved = 0;
        for (final BlockPos step : path)
        {
            if (paved >= MAX_PAVE_BLOCKS)
            {
                break;
            }

            if (paveFootprint(world, colony, step))
            {
                paved++;
            }
        }

        if (paved > 0)
        {
            BeastofBurdenLog.info("Colony {} paved {} road blocks toward {}", colony.getID(), paved, entrance.toShortString());
        }
    }

    private static int nearNetworkDistanceSq()
    {
        final int radius = Math.max(40, PlanningConfig.searchRadius() / 2);
        return radius * radius;
    }

    public static double roadScoreBonus(@NotNull final IColony colony, @NotNull final BlockPos anchor)
    {
        return roadScoreBonus(colony, anchor, collectNetworkNodes(colony));
    }

    public static double roadScoreBonus(
      @NotNull final IColony colony,
      @NotNull final BlockPos anchor,
      @NotNull final Set<BlockPos> network)
    {
        final Level world = colony.getWorld();
        if (world == null)
        {
            return 0;
        }

        final BlockPos ground = surfacePos(world, anchor);
        final int distance = findWalkDistance(world, colony, ground, network);
        if (distance <= MAX_ACCESS_STEPS)
        {
            return 12.0 / (1.0 + distance / 8.0);
        }

        return -4.0;
    }

    @NotNull
    static Set<BlockPos> collectNetworkNodes(@NotNull final IColony colony)
    {
        return collectNetworkNodes(colony, ColonyBuildings.getAllBuildings(colony));
    }

    @NotNull
    static Set<BlockPos> collectNetworkNodes(
      @NotNull final IColony colony,
      @NotNull final Collection<IBuilding> buildings)
    {
        final Set<BlockPos> nodes = new HashSet<>();
        final Level world = colony.getWorld();
        if (world == null)
        {
            return nodes;
        }

        nodes.add(surfacePos(world, colony.getCenter()));

        for (final IBuilding building : buildings)
        {
            if (!building.isBuilt() || building.getBuildingLevel() <= 0)
            {
                continue;
            }

            nodes.add(surfacePos(world, building.getPosition()));
        }

        return nodes;
    }

    private static int findWalkDistance(
      @NotNull final Level world,
      @NotNull final IColony colony,
      @NotNull final BlockPos start,
      @NotNull final Set<BlockPos> network)
    {
        final List<BlockPos> path = findWalkPath(world, colony, start, nearestNode(start, network));
        return path.isEmpty() ? Integer.MAX_VALUE : path.size();
    }

    @NotNull
    private static List<BlockPos> findWalkPath(
      @NotNull final Level world,
      @NotNull final IColony colony,
      @NotNull final BlockPos start,
      @Nullable final BlockPos goal)
    {
        if (goal == null)
        {
            return List.of();
        }

        final BlockPos startGround = surfacePos(world, start);
        final BlockPos goalGround = surfacePos(world, goal);
        if (startGround.equals(goalGround))
        {
            return List.of(startGround);
        }

        final Set<Long> visited = new HashSet<>();
        final Queue<BlockPos> queue = new ArrayDeque<>();
        final java.util.Map<Long, BlockPos> parent = new java.util.HashMap<>();

        queue.add(startGround);
        visited.add(startGround.asLong());

        int explored = 0;
        while (!queue.isEmpty() && explored++ < SEARCH_LIMIT)
        {
            final BlockPos current = queue.poll();
            if (current.distSqr(goalGround) <= 4)
            {
                return reconstruct(parent, current);
            }

            for (final BlockPos next : walkNeighbors(world, colony, current))
            {
                if (!visited.add(next.asLong()))
                {
                    continue;
                }

                parent.put(next.asLong(), current);
                queue.add(next);
            }
        }

        return List.of();
    }

    @NotNull
    private static List<BlockPos> reconstruct(@NotNull final java.util.Map<Long, BlockPos> parent, @NotNull final BlockPos end)
    {
        final List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = end;
        while (cursor != null)
        {
            path.add(0, cursor);
            cursor = parent.get(cursor.asLong());
        }
        return path;
    }

    @NotNull
    private static List<BlockPos> walkNeighbors(@NotNull final Level world, @NotNull final IColony colony, @NotNull final BlockPos pos)
    {
        final List<BlockPos> neighbors = new ArrayList<>(8);
        for (final Direction dir : Direction.Plane.HORIZONTAL)
        {
            final BlockPos flat = pos.offset(dir.getStepX(), 0, dir.getStepZ());
            if (isWalkable(world, colony, flat))
            {
                neighbors.add(flat);
            }

            final BlockPos up = flat.above();
            if (isWalkable(world, colony, up))
            {
                neighbors.add(up);
            }

            final BlockPos down = flat.below();
            if (isWalkable(world, colony, down))
            {
                neighbors.add(down);
            }
        }
        return neighbors;
    }

    private static boolean isWalkable(@NotNull final Level world, @NotNull final IColony colony, @NotNull final BlockPos pos)
    {
        if (!colony.isCoordInColony(world, pos))
        {
            return false;
        }

        final BlockPos feet = surfacePos(world, pos);
        final BlockState atFeet = world.getBlockState(feet);
        final BlockState below = world.getBlockState(feet.below());
        final BlockState above = world.getBlockState(feet.above());

        if (!hasSolidGround(below))
        {
            return false;
        }

        if (isPassable(atFeet) && isPassable(above))
        {
            return true;
        }

        return isStandableSurface(atFeet) && isPassable(above);
    }

    private static boolean hasSolidGround(@NotNull final BlockState state)
    {
        return !state.isAir()
          && !state.getFluidState().is(Fluids.WATER)
          && !state.getFluidState().is(Fluids.LAVA);
    }

    private static boolean isStandableSurface(@NotNull final BlockState state)
    {
        return hasSolidGround(state) && state.isSolid();
    }

    private static boolean isPassable(@NotNull final BlockState state)
    {
        return state.isAir() || state.canBeReplaced() || isRoadBlock(state) || OccupancyMap.isClearable(state, true);
    }

    private static boolean paveFootprint(@NotNull final Level world, @NotNull final IColony colony, @NotNull final BlockPos pos)
    {
        final BlockPos ground = surfacePos(world, pos);
        if (!colony.isCoordInColony(world, ground))
        {
            return false;
        }

        final BlockState existing = world.getBlockState(ground);
        if (!existing.isAir() && !OccupancyMap.isClearable(existing, true) && !isRoadBlock(existing))
        {
            return false;
        }

        world.setBlockAndUpdate(ground, pickRoadBlock().defaultBlockState());
        return true;
    }

    @NotNull
    private static Block pickRoadBlock()
    {
        return Blocks.COBBLESTONE;
    }

    private static boolean isRoadBlock(@NotNull final BlockState state)
    {
        return state.is(BlockTags.STONE_BRICKS)
          || state.is(Blocks.COBBLESTONE)
          || state.is(Blocks.MOSSY_COBBLESTONE)
          || state.is(Blocks.STONE_BRICKS)
          || state.is(Blocks.BRICKS)
          || state.is(Blocks.GRAVEL)
          || state.is(Blocks.DIRT_PATH)
          || state.is(Blocks.SMOOTH_STONE_SLAB)
          || state.is(Blocks.COBBLESTONE_SLAB);
    }

    @Nullable
    private static BlockPos nearestNode(@NotNull final BlockPos from, @NotNull final Set<BlockPos> network)
    {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (final BlockPos node : network)
        {
            final double dist = node.distSqr(from);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = node;
            }
        }
        return best;
    }

    @NotNull
    private static BlockPos surfacePos(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        final int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }
}
