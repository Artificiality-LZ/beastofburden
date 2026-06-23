package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hard overlap checks against existing colony structures (footprints + registered huts).
 */
public final class StructureOverlapGuard
{
    private StructureOverlapGuard()
    {
    }

    public static boolean wouldOverlap(
      @NotNull final IColony colony,
      @NotNull final PlannedBuildingType type,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        return wouldOverlap(colony, type, OccupancyMap.collectFootprints(colony), blueprint, anchor, facing);
    }

    public static boolean wouldOverlap(
      @NotNull final IColony colony,
      @NotNull final PlannedBuildingType type,
      @NotNull final List<BuildingFootprint> footprints,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final Level world = colony.getWorld();
        if (world == null)
        {
            return true;
        }

        if (OccupancyMap.overlapsFootprints(world, footprints, blueprint, anchor, facing))
        {
            return true;
        }

        return intersectsRegisteredStructure(world, blueprint, anchor, facing, null);
    }

    public static boolean wouldOverlap(
      @NotNull final IColony colony,
      @NotNull final PlannedBuildingType type,
      final int level,
      @Nullable final String structurePack,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final String pack = BlueprintPaths.defaultPack(structurePack);
        final Blueprint blueprint = BlueprintPaths.loadBlueprint(pack, type, level);
        if (blueprint == null)
        {
            return true;
        }

        return wouldOverlap(colony, type, blueprint, anchor, facing);
    }

    private static boolean intersectsRegisteredStructure(
      @NotNull final Level world,
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing,
      @Nullable final BlockPos ignoreAnchor)
    {
        final BlockPos rotatedPrimary = BuildingFootprint.rotateOffset(blueprint.getPrimaryBlockOffset(), facing);
        final BlockPos zero = anchor.subtract(rotatedPrimary);

        for (final BlockInfo info : blueprint.getBlockInfoAsMap().values())
        {
            if (info == null || info.getState().isAir())
            {
                continue;
            }

            final BlockPos worldPos = zero.offset(BuildingFootprint.rotateOffset(info.getPos(), facing));
            if (ignoreAnchor != null && worldPos.equals(ignoreAnchor))
            {
                continue;
            }

            final BlockState state = world.getBlockState(worldPos);
            if (state.getBlock() instanceof AbstractBlockHut)
            {
                return true;
            }

            final IBuilding building = IColonyManager.getInstance().getBuilding(world, worldPos);
            if (building != null && (ignoreAnchor == null || !building.getPosition().equals(ignoreAnchor)))
            {
                return true;
            }
        }

        return false;
    }
}
