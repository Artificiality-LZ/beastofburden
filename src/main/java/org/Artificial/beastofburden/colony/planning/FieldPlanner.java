package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.Artificial.beastofburden.util.ColonyFieldSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Places basicfield blueprints for farmer huts.
 */
public final class FieldPlanner
{
    private static final int MIN_FIELD_DISTANCE = 8;
    private static final int FIELD_CANDIDATE_STEP = 4;
    private static final Direction FIELD_FACING = Direction.NORTH;

    private FieldPlanner()
    {
    }

    public static int countFieldsForFarmer(@NotNull final IColony colony, @NotNull final BlockPos farmerPos)
    {
        return ColonyFieldSupport.countForFarmer(colony, farmerPos);
    }

    public static int desiredFieldCount(@NotNull final IBuilding farmer)
    {
        final int level = Math.max(1, farmer.getBuildingLevel());
        return Math.min(3, 1 + level / 2);
    }

    @Nullable
    public static BlockPos findFieldLocation(@NotNull final IColony colony, @NotNull final BlockPos farmerPos)
    {
        final Level world = colony.getWorld();
        if (world == null)
        {
            return null;
        }

        final FieldBlueprintPaths.LoadedFieldBlueprint loaded = FieldBlueprintPaths.loadBasicField(colony);
        if (loaded == null)
        {
            return null;
        }

        final Blueprint blueprint = loaded.blueprint();
        final List<BuildingFootprint> reservedFootprints = OccupancyMap.collectFootprints(colony);
        final Set<BlockPos> existingAnchors = new HashSet<>(ColonyFieldSupport.listFieldAnchors(colony));
        final int minGap = Math.max(1, PlanningConfig.minBlueprintSeparation());

        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        final int searchRadius = PlanningConfig.searchRadius();

        for (int ring = MIN_FIELD_DISTANCE; ring <= searchRadius; ring += FIELD_CANDIDATE_STEP)
        {
            for (int dx = -ring; dx <= ring; dx += FIELD_CANDIDATE_STEP)
            {
                for (int dz = -ring; dz <= ring; dz += FIELD_CANDIDATE_STEP)
                {
                    if (Math.abs(dx) < MIN_FIELD_DISTANCE && Math.abs(dz) < MIN_FIELD_DISTANCE)
                    {
                        continue;
                    }

                    final int x = farmerPos.getX() + dx;
                    final int z = farmerPos.getZ() + dz;
                    final int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    final BlockPos candidate = new BlockPos(x, y, z);

                    if (!colony.isCoordInColony(world, candidate))
                    {
                        continue;
                    }

                    final BlockPos scarecrowAnchor = FieldBlueprintPaths.resolveScarecrowAnchor(blueprint, candidate, FIELD_FACING);
                    if (existingAnchors.contains(scarecrowAnchor))
                    {
                        continue;
                    }

                    final BuildingFootprint candidateFootprint = BuildingFootprint.fromSchematic(
                      candidate,
                      world,
                      blueprint,
                      FIELD_FACING,
                      false
                    );

                    if (overlapsAny(candidateFootprint, reservedFootprints, minGap))
                    {
                        continue;
                    }

                    if (!isFieldSurface(world, scarecrowAnchor))
                    {
                        continue;
                    }

                    final double score = scoreFieldSpot(world, scarecrowAnchor, farmerPos, colony.getCenter());
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        return best!=null?best.below():best;
    }

    @NotNull
    public static ColonyBuildingExecutor.ExecutionResult placeField(@NotNull final IColony colony, @NotNull final BlockPos location, @NotNull final BlockPos farmerPos)
    {
        final Level world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("no_world");
        }

        if (!(world instanceof ServerLevel server))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("no_world");
        }

        if (!colony.isCoordInColony(world, location))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("outside_colony");
        }

        final FieldBlueprintPaths.LoadedFieldBlueprint loaded = FieldBlueprintPaths.loadBasicField(colony);
        if (loaded == null)
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("missing_field_blueprint");
        }

        final Blueprint blueprint = loaded.blueprint();
        final BlockPos scarecrowAnchor = FieldBlueprintPaths.resolveScarecrowAnchor(blueprint, location, FIELD_FACING);
        if (ColonyFieldSupport.hasFieldAt(colony, scarecrowAnchor))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("field_already_exists");
        }

        if (!OccupancyMap.prepareAnchorSite(world, location))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("blocked");
        }

        if (!PlanningInstantBuild.pasteBlueprint(server, location, blueprint, FIELD_FACING))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("field_paste_failed");
        }

        if (!ColonyFieldSupport.register(colony, scarecrowAnchor, farmerPos))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("field_registration_failed");
        }

        return ColonyBuildingExecutor.ExecutionResult.success(scarecrowAnchor, PlannedBuildingType.FARMER, "farmer_field");
    }

    @Nullable
    public static BlockPos findFarmerNeedingField(@NotNull final IColony colony)
    {
        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (!isFarmerHut(building) || !building.isBuilt() || building.getBuildingLevel() <= 0)
            {
                continue;
            }

            if (countFieldsForFarmer(colony, building.getPosition()) < desiredFieldCount(building))
            {
                return building.getPosition();
            }
        }
        return null;
    }

    private static boolean overlapsAny(
      @NotNull final BuildingFootprint candidate,
      @NotNull final List<BuildingFootprint> existing,
      final int minGap)
    {
        for (final BuildingFootprint footprint : existing)
        {
            if (candidate.conflictsWith(footprint, minGap))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isFarmerHut(@NotNull final IBuilding building)
    {
        return ColonyFieldSupport.isFarmerHut(building);
    }

    private static boolean isFieldSurface(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        final BlockState ground = world.getBlockState(pos.below());
        final BlockState at = world.getBlockState(pos);
        final BlockState above = world.getBlockState(pos.above());

        return (at.isAir() || at.canBeReplaced())
          && (above.isAir() || above.canBeReplaced())
          && !ground.isAir()
          && ground.getBlock().defaultBlockState().isSolid();
    }

    private static double scoreFieldSpot(
      @NotNull final Level world,
      @NotNull final BlockPos candidate,
      @NotNull final BlockPos farmerPos,
      @NotNull final BlockPos center)
    {
        final double farmerDist = Math.sqrt(candidate.distSqr(farmerPos));
        if (farmerDist < MIN_FIELD_DISTANCE)
        {
            return Double.NEGATIVE_INFINITY;
        }

        double score = 20.0 / (1.0 + farmerDist / 12.0);
        score += 6.0 / (1.0 + Math.sqrt(candidate.distSqr(center)) / 40.0);

        int flat = 0;
        for (final Direction dir : Direction.Plane.HORIZONTAL)
        {
            final BlockPos neighbor = candidate.offset(dir.getStepX(), 0, dir.getStepZ());
            if (Math.abs(world.getHeight(Heightmap.Types.WORLD_SURFACE, neighbor.getX(), neighbor.getZ()) - candidate.getY()) <= 1)
            {
                flat++;
            }
        }
        score += flat;

        return score;
    }
}
