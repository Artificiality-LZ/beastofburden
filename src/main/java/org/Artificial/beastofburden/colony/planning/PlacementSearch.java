package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Anchor scan and placement scoring for new hut blueprints.
 */
public final class PlacementSearch
{
    private static final int ANCHOR_STEP = 3;

    private PlacementSearch()
    {
    }

    @Nullable
    public static Placement findBestPlacement(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      final int level)
    {
        final Level world = context.colony().getWorld();
        if (world == null)
        {
            return null;
        }

        final Blueprint blueprint = BlueprintPaths.loadBlueprint(
          BlueprintPaths.defaultPack(context.snapshot().getStructurePack()),
          type,
          level
        );
        if (blueprint == null)
        {
            return null;
        }

        final List<Candidate> candidates = collectCandidates(context, type, blueprint);
        candidates.sort(Comparator.comparingDouble(Candidate::baseScore).reversed());

        Placement best = null;
        for (final Candidate candidate : candidates)
        {
            if (!isValidForEveryFacing(context, type, blueprint, candidate.anchor()))
            {
                continue;
            }

            final Direction facing = bestFacing(context, blueprint, candidate.anchor());
            final double score = candidate.baseScore() + facingScore(context, blueprint, candidate.anchor(), facing);
            if (best == null || score > best.score())
            {
                best = new Placement(candidate.anchor(), facing, score);
            }
        }

        return best;
    }

    @NotNull
    private static List<Candidate> collectCandidates(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      @NotNull final Blueprint blueprint)
    {
        final Level world = context.colony().getWorld();
        if (world == null)
        {
            return List.of();
        }

        final BlockPos center = context.snapshot().getColonyCenter();
        final int radius = PlanningConfig.searchRadius();
        final List<Candidate> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx += ANCHOR_STEP)
        {
            for (int dz = -radius; dz <= radius; dz += ANCHOR_STEP)
            {
                if (candidates.size() >= PlanningConfig.maxPlacementCandidates())
                {
                    return candidates;
                }
                if (dx * dx + dz * dz > radius * radius)
                {
                    continue;
                }

                final BlockPos anchor = surfaceAnchor(context, blueprint, center.getX() + dx, center.getZ() + dz);
                if (anchor == null)
                {
                    continue;
                }

                candidates.add(new Candidate(anchor, baseScore(context, type, blueprint, anchor)));
            }
        }

        return candidates;
    }

    @Nullable
    private static BlockPos surfaceAnchor(
      @NotNull final PlanningContext context,
      @NotNull final Blueprint blueprint,
      final int x,
      final int z)
    {
        final Level world = context.colony().getWorld();
        if (world == null)
        {
            return null;
        }

        final int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        for (int y = surfaceY + 2; y >= surfaceY - 6; y--)
        {
            final BlockPos groundColumn = new BlockPos(x, y, z);
            final BlockPos anchor = BlueprintAnchorOffsets.anchorFromGroundColumn(groundColumn, blueprint);
            if (!context.colony().isCoordInColony(world, anchor))
            {
                continue;
            }
            if (OccupancyMap.isLooseAnchorCandidate(world, anchor, blueprint, context.occupiedColumns()))
            {
                return anchor;
            }
        }
        return null;
    }

    private static boolean isValidForEveryFacing(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor)
    {
        for (final Direction facing : Direction.Plane.HORIZONTAL)
        {
            if (StructureOverlapGuard.wouldOverlap(context.colony(), type, context.footprints(), blueprint, anchor, facing))
            {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static Direction bestFacing(
      @NotNull final PlanningContext context,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor)
    {
        Direction best = Direction.SOUTH;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (final Direction facing : Direction.Plane.HORIZONTAL)
        {
            final double score = facingScore(context, blueprint, anchor, facing);
            if (score > bestScore)
            {
                bestScore = score;
                best = facing;
            }
        }
        return best;
    }

    private static double baseScore(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor)
    {
        final BlockPos center = context.snapshot().getColonyCenter();
        final BlockPos groundColumn = BlueprintAnchorOffsets.groundColumnFromAnchor(anchor, blueprint);
        final double centerDistance = Math.sqrt(anchor.distSqr(center));
        final double nearest = nearestBuildingDistance(context, anchor);

        double score = 30.0 / (1.0 + centerDistance / 48.0);
        score += nearest < Double.MAX_VALUE ? Math.max(0.0, 22.0 - Math.abs(nearest - 20.0)) : 0.0;
        score += flatnessScore(context, groundColumn);
        score += RoadPlanner.roadScoreBonus(context.colony(), anchor, context.roadNodes());

        score += switch (type.getCategory())
        {
            case LOGISTICS -> 10.0 / (1.0 + centerDistance / 24.0);
            case DEFENSE -> Math.min(10.0, centerDistance / 8.0);
            case FOOD -> groundColumn.getY() <= center.getY() + 4 ? 4.0 : 0.0;
            case RESOURCE -> type == PlannedBuildingType.MINER ? Math.max(0, center.getY() - groundColumn.getY()) * 0.25 : 3.0;
            case CRAFTING -> nearest < 34 ? 5.0 : 0.0;
            case CIVIC, INFRASTRUCTURE -> 2.0;
        };
        return score;
    }

    private static double facingScore(
      @NotNull final PlanningContext context,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final Level world = context.colony().getWorld();
        if (world == null)
        {
            return 0;
        }

        double score = 0;
        for (int step = 1; step <= 4; step++)
        {
            final BlockPos path = anchor.relative(facing, step);
            if (isPassable(world.getBlockState(path)) && isPassable(world.getBlockState(path.above())))
            {
                score += 1.5;
            }
        }

        score += 6.0 / (1.0 + Math.sqrt(anchor.relative(facing, 4).distSqr(context.snapshot().getColonyCenter())) / 24.0);
        score -= obstructionPenalty(world, blueprint, anchor, facing);
        return score;
    }

    private static double obstructionPenalty(
      @NotNull final Level world,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final BlockPos zero = anchor.subtract(BuildingFootprint.rotateOffset(blueprint.getPrimaryBlockOffset(), facing));
        int solid = 0;
        int total = 0;
        for (final BlockInfo info : blueprint.getBlockInfoAsMap().values())
        {
            if (info == null || info.getState().isAir())
            {
                continue;
            }

            total++;
            final BlockPos worldPos = zero.offset(BuildingFootprint.rotateOffset(info.getPos(), facing));
            if (!OccupancyMap.isClearable(world.getBlockState(worldPos), true))
            {
                solid++;
            }
        }

        return total == 0 ? 100.0 : solid / (double) total * 5.0;
    }

    private static double nearestBuildingDistance(@NotNull final PlanningContext context, @NotNull final BlockPos anchor)
    {
        double nearest = Double.MAX_VALUE;
        for (final IBuilding building : context.buildings())
        {
            if (building.getPosition() != null)
            {
                nearest = Math.min(nearest, Math.sqrt(anchor.distSqr(building.getPosition())));
            }
        }
        return nearest;
    }

    private static double flatnessScore(@NotNull final PlanningContext context, @NotNull final BlockPos groundColumn)
    {
        final Level world = context.colony().getWorld();
        if (world == null)
        {
            return 0;
        }

        int flat = 0;
        for (final Direction direction : Direction.Plane.HORIZONTAL)
        {
            final BlockPos neighbor = groundColumn.relative(direction, ANCHOR_STEP);
            final int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, neighbor.getX(), neighbor.getZ());
            if (Math.abs(y - groundColumn.getY()) <= 2)
            {
                flat++;
            }
        }
        return flat * 1.25;
    }

    private static boolean isPassable(@NotNull final BlockState state)
    {
        return state.isAir() || state.canBeReplaced() || OccupancyMap.isClearable(state, true);
    }

    private record Candidate(@NotNull BlockPos anchor, double baseScore)
    {
    }

    public record Placement(@NotNull BlockPos anchor, @NotNull Direction facing, double score)
    {
    }
}
