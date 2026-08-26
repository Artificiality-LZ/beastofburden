package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.ColonyUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Axis-aligned bounds of a hut blueprint or built structure in world space.
 * Uses MineColonies {@link ColonyUtils#calculateCorners} (same as construction tape).
 */
public final class BuildingFootprint
{
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    private BuildingFootprint(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @NotNull
    public static BuildingFootprint fromCorners(@NotNull final BlockPos a, @NotNull final BlockPos b)
    {
        return new BuildingFootprint(
          Math.min(a.getX(), b.getX()),
          Math.min(a.getY(), b.getY()),
          Math.min(a.getZ(), b.getZ()),
          Math.max(a.getX(), b.getX()),
          Math.max(a.getY(), b.getY()),
          Math.max(a.getZ(), b.getZ())
        );
    }

    @NotNull
    public static BuildingFootprint fromAabb(@NotNull final AABB box)
    {
        final Tuple<BlockPos, BlockPos> corners = ColonyUtils.calculateCorners(box);
        if (corners != null && corners.getA() != null && corners.getB() != null)
        {
            return fromCorners(corners.getA(), corners.getB());
        }

        return new BuildingFootprint(
          (int) Math.floor(box.minX),
          (int) Math.floor(box.minY),
          (int) Math.floor(box.minZ),
          (int) Math.ceil(box.maxX) - 1,
          (int) Math.ceil(box.maxY) - 1,
          (int) Math.ceil(box.maxZ) - 1
        );
    }

    @NotNull
    public static BuildingFootprint fromAnchor(@NotNull final BlockPos anchor)
    {
        return new BuildingFootprint(
          anchor.getX(),
          anchor.getY(),
          anchor.getZ(),
          anchor.getX(),
          anchor.getY() + 1,
          anchor.getZ()
        );
    }

    /**
     * Bounds for a planned or in-progress hut (matches construction-tape corners).
     */
    @NotNull
    public static BuildingFootprint fromSchematic(
      @NotNull final BlockPos anchor,
      @NotNull final Level world,
      @NotNull final Blueprint blueprint,
      @NotNull final Direction facing,
      final boolean mirrored)
    {
        final Tuple<BlockPos, BlockPos> corners = ColonyUtils.calculateCorners(
          anchor,
          world,
          blueprint,
          facingToRotation(facing),
          mirrored
        );

        if (isValidCorners(corners))
        {
            return fromCorners(corners.getA(), corners.getB());
        }

        return fromBlueprintFallback(blueprint, anchor, facing);
    }

    @NotNull
    public static BuildingFootprint fromBlueprint(
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        return fromBlueprintFallback(blueprint, anchor, facing);
    }

    @NotNull
    private static BuildingFootprint fromBlueprintFallback(
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos anchor,
      @NotNull final Direction facing)
    {
        final BlockPos rotatedPrimary = rotateOffset(blueprint.getPrimaryBlockOffset(), facing);
        final BlockPos zero = anchor.subtract(rotatedPrimary);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean any = false;

        for (final BlockInfo info : blueprint.getBlockInfoAsMap().values())
        {
            if (info == null || info.getState().isAir())
            {
                continue;
            }

            final BlockPos worldPos = zero.offset(rotateOffset(info.getPos(), facing));
            minX = Math.min(minX, worldPos.getX());
            minY = Math.min(minY, worldPos.getY());
            minZ = Math.min(minZ, worldPos.getZ());
            maxX = Math.max(maxX, worldPos.getX());
            maxY = Math.max(maxY, worldPos.getY());
            maxZ = Math.max(maxZ, worldPos.getZ());
            any = true;
        }

        if (!any)
        {
            return fromAnchor(anchor);
        }

        return new BuildingFootprint(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @NotNull
    public static BuildingFootprint fromBuilding(@NotNull final IBuilding building)
    {
        return fromBuilding(building, null);
    }

    @NotNull
    public static BuildingFootprint fromBuilding(@NotNull final IBuilding building, @Nullable final Level world)
    {
        if (world != null && (!building.isBuilt() || building.getBuildingLevel() <= 0))
        {
            final BuildingFootprint planned = fromBuildingSchematic(building, world);
            if (planned != null)
            {
                return planned;
            }
        }

        Tuple<BlockPos, BlockPos> corners = building.getCorners();
        if (!isValidCorners(corners))
        {
            building.calculateCorners();
            corners = building.getCorners();
        }

        if (isValidCorners(corners))
        {
            return fromCorners(corners.getA(), corners.getB());
        }

        if (world != null)
        {
            final BuildingFootprint planned = fromBuildingSchematic(building, world);
            if (planned != null)
            {
                return planned;
            }
        }

        BeastofBurdenLog.warn(
          "Falling back to anchor-only footprint for {} at {}",
          building.getBuildingType().getTranslationKey(),
          building.getPosition()
        );
        return fromAnchor(building.getPosition());
    }

    @Nullable
    private static BuildingFootprint fromBuildingSchematic(@NotNull final IBuilding building, @NotNull final Level world)
    {
        final PlannedBuildingType type = PlannedBuildingType.fromEntry(building.getBuildingType());
        if (type == null)
        {
            return null;
        }

        final String pack = BlueprintPaths.defaultPack(building.getStructurePack());
        final int level = Math.max(1, building.getBuildingLevel());
        final Blueprint blueprint = BlueprintPaths.loadBlueprint(pack, type, level);
        if (blueprint == null)
        {
            return null;
        }

        return fromSchematic(
          building.getPosition(),
          world,
          blueprint,
          resolveFacing(building, world),
          building.isMirrored()
        );
    }

    /**
     * @return true when fewer than {@code minGap} empty horizontal blocks separate the two footprints.
     */
    public boolean conflictsWith(@NotNull final BuildingFootprint other, final int minGap)
    {
        return !(minX > other.maxX + minGap
          || other.minX > maxX + minGap
          || minZ > other.maxZ + minGap
          || other.minZ > maxZ + minGap);
    }

    public boolean contains(@NotNull final BlockPos pos)
    {
        return pos.getX() >= minX && pos.getX() <= maxX
          && pos.getY() >= minY && pos.getY() <= maxY
          && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static boolean isValidCorners(@Nullable final Tuple<BlockPos, BlockPos> corners)
    {
        if (corners == null || corners.getA() == null || corners.getB() == null)
        {
            return false;
        }

        final BlockPos a = corners.getA();
        final BlockPos b = corners.getB();
        return a.getX() != b.getX() || a.getY() != b.getY() || a.getZ() != b.getZ();
    }

    @NotNull
    private static Direction resolveFacing(@NotNull final IBuilding building, @Nullable final Level world)
    {
        if (world != null)
        {
            final BlockState state = world.getBlockState(building.getPosition());
            if (state.getBlock() instanceof AbstractBlockHut<?> && state.hasProperty(AbstractBlockHut.FACING))
            {
                return state.getValue(AbstractBlockHut.FACING);
            }
        }

        return rotationToFacing(building.getRotation());
    }

    static int facingToRotation(@NotNull final Direction facing)
    {
        return switch (facing)
        {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    int minX()
    {
        return minX;
    }

    int minY()
    {
        return minY;
    }

    int minZ()
    {
        return minZ;
    }

    int maxX()
    {
        return maxX;
    }

    int maxY()
    {
        return maxY;
    }

    int maxZ()
    {
        return maxZ;
    }

    @NotNull
    static BlockPos rotateOffset(@NotNull final BlockPos offset, @NotNull final Direction facing)
    {
        final int x = offset.getX();
        final int z = offset.getZ();
        return switch (facing)
        {
            case SOUTH -> offset;
            case NORTH -> new BlockPos(-x, offset.getY(), -z);
            case EAST -> new BlockPos(-z, offset.getY(), x);
            case WEST -> new BlockPos(z, offset.getY(), -x);
            default -> offset;
        };
    }

    @NotNull
    static Direction rotationToFacing(final int rotation)
    {
        return switch (rotation & 3)
        {
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }
}
