package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.Artificial.beastofburden.util.ConstructionTapeSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Places hut anchors and requests builder work orders.
 */
public final class ColonyBuildingExecutor
{
    private ColonyBuildingExecutor()
    {
    }

    @NotNull
    public static ExecutionResult execute(
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final BlockPos location,
      @NotNull final BlockPos builderPos,
      @Nullable final String structurePack,
      @NotNull final Direction facing)
    {
        if (task.getAction() == BuildTaskAction.UPGRADE)
        {
            return executeUpgrade(colony, task, builderPos);
        }

        if (task.getAction() == BuildTaskAction.PLACE_FIELD)
        {
            final BlockPos farmerPos = task.getExistingBuilding();
            if (farmerPos == null)
            {
                return ExecutionResult.failed("missing_farmer");
            }
            return FieldPlanner.placeField(colony, location, farmerPos);
        }

        return executeNewBuild(colony, task, location, builderPos, structurePack, facing);
    }

    @NotNull
    private static ExecutionResult executeUpgrade(
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final BlockPos builderPos)
    {
        final BlockPos buildingPos = task.getExistingBuilding();
        if (buildingPos == null)
        {
            return ExecutionResult.failed("missing_building");
        }

        final Level world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return ExecutionResult.failed("no_world");
        }

        final IBuilding building = IColonyManager.getInstance().getBuilding(world, buildingPos);
        if (building == null)
        {
            return ExecutionResult.failed("building_not_found");
        }

        try
        {
            if (PlanningConfig.instantBuildDebug())
            {
                return executeInstantUpgrade(colony, task, building);
            }

            building.requestUpgrade(null, builderPos);
            if (!PlanningWorkOrders.hasConstructionOrderAt(colony, buildingPos))
            {
                BeastofBurdenLog.warn(
                  "Upgrade requested at {} to level {} but no work order was created (builder {}).",
                  buildingPos,
                  task.getTargetLevel(),
                  builderPos
                );
                return ExecutionResult.failed("upgrade_failed");
            }

            return ExecutionResult.success(buildingPos, task.getType(), "upgrade");
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to request upgrade at {}: {}", buildingPos, ex.toString());
            return ExecutionResult.failed("upgrade_failed");
        }
    }

    @NotNull
    private static ExecutionResult executeNewBuild(
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final BlockPos location,
      @NotNull final BlockPos builderPos,
      @Nullable final String structurePack,
      @NotNull final Direction facing)
    {
        final Level world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return ExecutionResult.failed("no_world");
        }

        if (!colony.isCoordInColony(world, location))
        {
            return ExecutionResult.failed("outside_colony");
        }

        if (IColonyManager.getInstance().getBuilding(world, location) != null)
        {
            return ExecutionResult.failed("occupied");
        }

        if (StructureOverlapGuard.wouldOverlap(colony, task.getType(), task.getTargetLevel(), structurePack, location, facing))
        {
            BeastofBurdenLog.warn("Rejected overlapping planned build {} at {}", task.getType(), location);
            return ExecutionResult.failed("overlap");
        }

        final Block hutBlock = task.getType().getEntry().getBuildingBlock();
        if (!(hutBlock instanceof AbstractBlockHut<?> abstractHut))
        {
            return ExecutionResult.failed("invalid_hut");
        }

        final String pack = BlueprintPaths.defaultPack(structurePack);
        final String blueprintPath = BlueprintMapping.getPathMapping("", abstractHut.getBlueprintName()) + task.getTargetLevel() + ".blueprint";

        if (!StructurePacks.hasPack(pack))
        {
            BeastofBurdenLog.warn("Structure pack {} missing for planned build {}", pack, task.getType());
            return ExecutionResult.failed("missing_pack");
        }

        final Blueprint blueprint = BlueprintPaths.loadBlueprint(pack, task.getType(), task.getTargetLevel());
        if (blueprint == null)
        {
            BeastofBurdenLog.warn("Blueprint missing for planned build {} pack={} level={}", task.getType(), pack, task.getTargetLevel());
            return ExecutionResult.failed("missing_blueprint");
        }

        if (!OccupancyMap.prepareAnchorSite(world, location, blueprint))
        {
            return ExecutionResult.failed("blocked_anchor");
        }

        final BlockState existing = world.getBlockState(location);
        if (!existing.isAir() && !existing.canBeReplaced())
        {
            return ExecutionResult.failed("blocked_anchor");
        }

        final BlockState state = hutBlock.defaultBlockState().setValue(AbstractBlockHut.FACING, facing);
        world.setBlockAndUpdate(location, state);

        IBuilding building = null;
        try
        {
            abstractHut.onBlockPlacedByBuildTool(world, location, state, null, null, false, pack, blueprintPath);

            building = IColonyManager.getInstance().getBuilding(world, location);
            if (building == null)
            {
                BeastofBurdenLog.warn("Planned hut placed but building not registered at {}", location);
                rollbackNewBuild(colony, world, location, null);
                return ExecutionResult.failed("registration_failed");
            }

            building.setStructurePack(pack);
            building.setBlueprintPath(blueprintPath);
            building.setBuildingLevel(0);
            building.setIsMirrored(false);

            if (building.getTileEntity() != null)
            {
                building.getTileEntity().setColony(colony);
            }

            if (PlanningConfig.instantBuildDebug())
            {
                final ExecutionResult instant = executeInstantNewBuild(
                  colony, task, location, structurePack, facing, building, pack, blueprintPath);
                if (!instant.success())
                {
                    rollbackNewBuild(colony, world, location, building);
                }
                return instant;
            }

            placeConstructionTape(building.getCorners(), world, colony);

            if (ColdStartManager.shouldDeferWorkOrder(colony, task))
            {
                BeastofBurdenLog.info(
                  "Placed first builder hut at {} without a work order; hiring a builder comes next.",
                  location
                );
                return ExecutionResult.success(location, task.getType(), task.getReason());
            }

            building.requestUpgrade(null, builderPos);
            if (!PlanningWorkOrders.hasConstructionOrderAt(colony, location))
            {
                BeastofBurdenLog.warn(
                  "Build requested at {} but no work order was created (builder {}).",
                  location,
                  builderPos
                );
                rollbackNewBuild(colony, world, location, building);
                return ExecutionResult.failed("work_order_failed");
            }

            return ExecutionResult.success(location, task.getType(), task.getReason());
        }
        catch (final RuntimeException ex)
        {
            BeastofBurdenLog.warn("Failed to request build at {}: {}", location, ex.toString());
            rollbackNewBuild(colony, world, location, building);
            return ExecutionResult.failed("work_order_failed");
        }
    }

    private static void rollbackNewBuild(
      @NotNull final IColony colony,
      @NotNull final Level world,
      @NotNull final BlockPos location,
      @Nullable final IBuilding building)
    {
        try
        {
            if (building != null)
            {
                final IRegisteredStructureManager manager = ColonyBuildings.getStructureManager(colony);
                if (manager != null)
                {
                    final Set<ServerPlayer> subscribers = Collections.emptySet();
                    manager.removeBuilding(building, subscribers);
                }
            }
        }
        catch (final RuntimeException ex)
        {
            BeastofBurdenLog.warn("Failed to unregister orphaned hut at {}: {}", location, ex.toString());
        }

        world.setBlock(location, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @NotNull
    private static ExecutionResult executeInstantNewBuild(
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final BlockPos location,
      @Nullable final String structurePack,
      @NotNull final Direction facing,
      @NotNull final IBuilding building,
      @NotNull final String pack,
      @NotNull final String blueprintPath)
    {
        final Blueprint blueprint = BlueprintPaths.loadBlueprint(pack, task.getType(), task.getTargetLevel());
        if (blueprint == null)
        {
            return ExecutionResult.failed("missing_blueprint");
        }

        if (!PlanningInstantBuild.completeBuilding(
          colony,
          building,
          location,
          blueprint,
          facing,
          task.getTargetLevel(),
          pack,
          blueprintPath
        ))
        {
            return ExecutionResult.failed("instant_build_failed");
        }

        return ExecutionResult.success(location, task.getType(), "instant_debug");
    }

    @NotNull
    private static ExecutionResult executeInstantUpgrade(
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final IBuilding building)
    {
        final BlockPos buildingPos = building.getPosition();
        final Level world = colony.getWorld();
        if (world == null)
        {
            return ExecutionResult.failed("no_world");
        }

        final String pack = BlueprintPaths.defaultPack(building.getStructurePack());
        final Blueprint blueprint = BlueprintPaths.loadBlueprint(pack, task.getType(), task.getTargetLevel());
        if (blueprint == null)
        {
            return ExecutionResult.failed("missing_blueprint");
        }

        final Direction facing = world.getBlockState(buildingPos).hasProperty(AbstractBlockHut.FACING)
          ? world.getBlockState(buildingPos).getValue(AbstractBlockHut.FACING)
          : Direction.SOUTH;
        final String blueprintPath = BlueprintPaths.pathFor(task.getType(), task.getTargetLevel());

        if (!PlanningInstantBuild.completeBuilding(
          colony,
          building,
          buildingPos,
          blueprint,
          facing,
          task.getTargetLevel(),
          pack,
          blueprintPath
        ))
        {
            return ExecutionResult.failed("instant_upgrade_failed");
        }

        return ExecutionResult.success(buildingPos, task.getType(), "instant_debug_upgrade");
    }

    private static void placeConstructionTape(
      @NotNull final Tuple<BlockPos, BlockPos> corners,
      @NotNull final Level world,
      @NotNull final IColony colony)
    {
        try
        {
            ConstructionTapeSupport.place(corners, world, colony);
        }
        catch (final RuntimeException ex)
        {
            BeastofBurdenLog.warn("Could not place construction tape: {}", ex.toString());
        }
    }

    public record ExecutionResult(boolean success, @Nullable BlockPos location, @Nullable PlannedBuildingType type, @Nullable String note)
    {
        @NotNull
        public static ExecutionResult success(@NotNull final BlockPos location, @NotNull final PlannedBuildingType type, @Nullable final String note)
        {
            return new ExecutionResult(true, location, type, note);
        }

        @NotNull
        public static ExecutionResult failed(@NotNull final String note)
        {
            return new ExecutionResult(false, null, null, note);
        }
    }
}
