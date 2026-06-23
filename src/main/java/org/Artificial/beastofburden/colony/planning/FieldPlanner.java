package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.Artificial.beastofburden.util.ColonyFieldSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Places scarecrow fields for farmer huts.
 */
public final class FieldPlanner
{
    private static final int FIELD_SEARCH_RADIUS = 24;
    private static final int MIN_FIELD_DISTANCE = 8;

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

        final var reserved = OccupancyMap.collectReservedFootprint(colony);
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int ring = MIN_FIELD_DISTANCE; ring <= FIELD_SEARCH_RADIUS; ring += 4)
        {
            for (int dx = -ring; dx <= ring; dx += 4)
            {
                for (int dz = -ring; dz <= ring; dz += 4)
                {
                    if (Math.abs(dx) < MIN_FIELD_DISTANCE && Math.abs(dz) < MIN_FIELD_DISTANCE)
                    {
                        continue;
                    }

                    final int x = farmerPos.getX() + dx;
                    final int z = farmerPos.getZ() + dz;
                    final int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    final BlockPos candidate = new BlockPos(x, y, z);

                    if (!colony.isCoordInColony(world, candidate) || OccupancyMap.overlapsAnchor(reserved, candidate))
                    {
                        continue;
                    }

                    if (!isFieldSurface(world, candidate))
                    {
                        continue;
                    }

                    final double score = scoreFieldSpot(world, candidate, farmerPos, colony.getCenter());
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        return best;
    }

    @NotNull
    public static ColonyBuildingExecutor.ExecutionResult placeField(@NotNull final IColony colony, @NotNull final BlockPos location, @NotNull final BlockPos farmerPos)
    {
        final Level world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("no_world");
        }

        if (!colony.isCoordInColony(world, location))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("outside_colony");
        }

        final Block scarecrow = ModBlocks.blockScarecrow;
        if (scarecrow == null)
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("no_scarecrow_block");
        }

        final BlockState lower = scarecrow.defaultBlockState();
        if (!world.getBlockState(location).canBeReplaced() || !world.getBlockState(location.above()).canBeReplaced())
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("blocked");
        }

        world.setBlockAndUpdate(location, lower);
        world.setBlockAndUpdate(location.above(), lower);

        if (!ColonyFieldSupport.register(colony, location, farmerPos))
        {
            return ColonyBuildingExecutor.ExecutionResult.failed("field_registration_failed");
        }

        return ColonyBuildingExecutor.ExecutionResult.success(location, PlannedBuildingType.FARMER, "farmer_field");
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
