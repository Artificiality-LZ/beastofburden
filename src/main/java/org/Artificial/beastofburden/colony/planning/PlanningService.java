package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single-pass autonomous planning pipeline.
 */
public final class PlanningService
{
    private static final int MIN_SUCCESS_COOLDOWN_PASSES = 2;

    @Nullable
    public ColonyPlanner.PlanningResult tick(@NotNull final ColonyPlanner planner, @NotNull final IColony colony)
    {
        if (planner.consumeRetryCooldown())
        {
            return null;
        }

        final PlanningContext context = PlanningContext.collect(colony);
        planner.refreshScriptedProgress(context);

        final BuildTask task = planner.selectTask(context);
        if (task == null)
        {
            final String decision = planner.getPlanningMode() == PlanningMode.SCRIPTED && planner.getScriptedStrategy().isComplete()
              ? "scripted_complete"
              : "idle";
            planner.setLastDecision(decision);
            planner.getReport().waiting(decision, planner.describeState(context));
            planner.startRetryCooldown(ColonyPlanner.planRetryCooldown());
            return null;
        }

        planner.getReport().selected(task, planner.describeState(context));

        final Target target = resolveTarget(context, task);
        if (target == null)
        {
            return fail(planner, colony, task, "no_location");
        }

        final boolean coldStartBuilder = !context.snapshot().hasBuilderHut() && task.getType() == PlannedBuildingType.BUILDER;
        final BlockPos targetLocation = target.location();
        final BlockPos builder = task.getAction() == BuildTaskAction.UPGRADE
          ? BuilderAssigner.assignForUpgrade(colony, task, targetLocation)
          : BuilderAssigner.assignOrDefault(
            colony,
            targetLocation,
            task.getTargetLevel(),
            coldStartBuilder || task.getAction() == BuildTaskAction.PLACE_FIELD
          );
        if (builder == null)
        {
            return fail(planner, colony, task, "no_builder");
        }

        if (!coldStartBuilder
              && task.getAction() != BuildTaskAction.UPGRADE
              && !BuilderAssigner.isWithinBuilderRange(colony, targetLocation, task.getTargetLevel()))
        {
            return fail(planner, colony, task, "no_builder_range");
        }

        final ColonyBuildingExecutor.ExecutionResult result = ColonyBuildingExecutor.execute(
          colony,
          task,
          target.location(),
          builder,
          context.snapshot().getStructurePack(),
          target.facing()
        );

        if (!result.success())
        {
            return fail(planner, colony, task, result.note() == null ? "execute_failed" : result.note());
        }

        if (task.getAction() != BuildTaskAction.PLACE_FIELD && !coldStartBuilder)
        {
            RoadPlanner.paveEntrance(colony, target.location(), target.facing());
        }

        if (coldStartBuilder)
        {
            final var hut = ColdStartManager.getBuildingAt(colony, target.location());
            if (hut != null)
            {
                ColdStartManager.tryHireBuilder(colony, hut);
            }
            planner.startRetryCooldown(Math.max(MIN_SUCCESS_COOLDOWN_PASSES, ColonyPlanner.coldStartPlanCooldown()));
        }
        else
        {
            planner.startRetryCooldown(Math.max(MIN_SUCCESS_COOLDOWN_PASSES, ColonyPlanner.planRetryCooldown()));
        }

        planner.getScriptedStrategy().noteSuccessfulPlacement();
        planner.getReport().placed(task, target.location(), builder, result.note());
        planner.setLastDecision(planner.getReport().getDecision());
        BeastofBurdenLog.info("Colony {} planned {} at {}", colony.getID(), task.getType(), target.location());

        return new ColonyPlanner.PlanningResult(
          planner.getPhaseId(),
          task,
          target.location(),
          builder,
          result.note()
        );
    }

    @Nullable
    private static Target resolveTarget(@NotNull final PlanningContext context, @NotNull final BuildTask task)
    {
        if (task.getAction() == BuildTaskAction.UPGRADE)
        {
            final BlockPos existing = task.getExistingBuilding();
            return existing == null ? null : new Target(existing, Direction.NORTH);
        }

        if (task.getAction() == BuildTaskAction.PLACE_FIELD)
        {
            final BlockPos farmer = task.getExistingBuilding();
            if (farmer == null)
            {
                return null;
            }
            final BlockPos field = FieldPlanner.findFieldLocation(context.colony(), farmer);
            return field == null ? null : new Target(field, Direction.NORTH);
        }

        final PlacementSearch.Placement placement = PlacementSearch.findBestPlacement(context, task.getType(), task.getTargetLevel());
        return placement == null ? null : new Target(placement.anchor(), placement.facing());
    }

    @Nullable
    private static ColonyPlanner.PlanningResult fail(
      @NotNull final ColonyPlanner planner,
      @NotNull final IColony colony,
      @NotNull final BuildTask task,
      @NotNull final String decision)
    {
        planner.setLastDecision(decision);
        planner.getReport().failed(decision, task, decision);
        planner.startRetryCooldown(ColonyPlanner.planRetryCooldown());
        BeastofBurdenLog.info("Colony {} could not plan {}: {}", colony.getID(), task.getType(), decision);
        return null;
    }

    private record Target(@NotNull BlockPos location, @NotNull Direction facing)
    {
    }
}
