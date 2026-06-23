package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fixed step strategy. Each step is satisfied only when all requirements are operational.
 */
public final class ScriptedPlanningStrategy implements PlanningStrategy
{
    private static final float TASK_PRIORITY = 900f;
    private static final String TAG_STEP_INDEX = "stepIndex";

    private FixedPlanScript script = FixedPlanScript.createDefault();
    private int currentStepIndex;

    public void readFromNbt(@Nullable final CompoundTag tag)
    {
        if (tag == null)
        {
            script = FixedPlanScript.createDefault();
            currentStepIndex = 0;
            return;
        }

        script = tag.contains(FixedPlanScript.TAG_STEPS)
          ? FixedPlanScript.readFromNbt(tag)
          : FixedPlanScript.createDefault();
        currentStepIndex = Math.max(0, tag.getInt(TAG_STEP_INDEX));
        clampStepIndex();
    }

    @NotNull
    public CompoundTag writeToNbt()
    {
        final CompoundTag tag = script.writeToNbt();
        tag.putInt(TAG_STEP_INDEX, currentStepIndex);
        return tag;
    }

    public void setScript(@NotNull final FixedPlanScript script)
    {
        this.script = script;
        clampStepIndex();
    }

    @NotNull
    public FixedPlanScript getScript()
    {
        return script;
    }

    public int getCurrentStepIndex()
    {
        return currentStepIndex;
    }

    public int getStepCount()
    {
        return script.stepCount();
    }

    public boolean isComplete()
    {
        return currentStepIndex >= script.stepCount();
    }

    public void noteSuccessfulPlacement()
    {
        // Progress is derived from live colony/work-order state on the next pass.
    }

    @Override
    @Nullable
    public BuildTask selectNextTask(@NotNull final PlanningContext context)
    {
        advanceCompletedSteps(context);
        if (isComplete())
        {
            return null;
        }

        final FixedPlanStep step = script.getStep(currentStepIndex);
        for (final FixedPlanRequirement requirement : step.getRequirements())
        {
            if (ScriptedPlanProgress.isRequirementMet(context, requirement))
            {
                continue;
            }

            final BuildTask task = taskForRequirement(context, requirement);
            if (task != null)
            {
                return task;
            }
        }
        return null;
    }

    @Override
    @NotNull
    public String describeState(@NotNull final PlanningContext context)
    {
        if (isComplete())
        {
            return "scripted_complete";
        }

        final FixedPlanStep step = script.getStep(currentStepIndex);
        return "step_" + (currentStepIndex + 1) + "/" + script.stepCount() + " - " + describeStep(context, step);
    }

    public boolean refreshProgress(@NotNull final PlanningContext context)
    {
        final int before = currentStepIndex;
        advanceCompletedSteps(context);
        return currentStepIndex != before;
    }

    public boolean isCurrentStepSatisfied(@NotNull final PlanningContext context)
    {
        if (isComplete())
        {
            return true;
        }
        return ScriptedPlanProgress.isStepOperationalComplete(context, script.getStep(currentStepIndex));
    }

    private void advanceCompletedSteps(@NotNull final PlanningContext context)
    {
        while (currentStepIndex < script.stepCount()
                 && ScriptedPlanProgress.isStepOperationalComplete(context, script.getStep(currentStepIndex)))
        {
            currentStepIndex++;
        }
    }

    @Nullable
    private static BuildTask taskForRequirement(@NotNull final PlanningContext context, @NotNull final FixedPlanRequirement requirement)
    {
        return switch (requirement.getKind())
        {
            case BUILDING -> taskForBuilding(context, requirement);
            case FIELD -> taskForFields(context, requirement);
        };
    }

    @Nullable
    private static BuildTask taskForBuilding(@NotNull final PlanningContext context, @NotNull final FixedPlanRequirement requirement)
    {
        final PlannedBuildingType type = requirement.getBuildingType();
        final int countAtAnyLevel = ScriptedPlanProgress.countCommittedBuildingSlots(context, type, 1);
        if (countAtAnyLevel < requirement.getCount())
        {
            return new BuildTask(type, BuildTaskAction.BUILD_NEW, 1, TASK_PRIORITY, null, "scripted_build");
        }

        final IBuilding upgradeTarget = findUpgradeTarget(context, type, requirement.getMinLevel());
        if (upgradeTarget != null)
        {
            return new BuildTask(
              type,
              BuildTaskAction.UPGRADE,
              Math.min(requirement.getMinLevel(), upgradeTarget.getBuildingLevel() + 1),
              TASK_PRIORITY,
              upgradeTarget.getID(),
              "scripted_upgrade"
            );
        }

        return null;
    }

    @Nullable
    private static BuildTask taskForFields(@NotNull final PlanningContext context, @NotNull final FixedPlanRequirement requirement)
    {
        if (ScriptedPlanProgress.countFields(context.colony()) >= requirement.getCount())
        {
            return null;
        }

        final BlockPos farmerPos = FieldPlanner.findFarmerNeedingField(context.colony());
        if (farmerPos == null)
        {
            return null;
        }

        return new BuildTask(
          PlannedBuildingType.FARMER,
          BuildTaskAction.PLACE_FIELD,
          1,
          TASK_PRIORITY,
          farmerPos,
          "scripted_field"
        );
    }

    @Nullable
    private static IBuilding findUpgradeTarget(
      @NotNull final PlanningContext context,
      @NotNull final PlannedBuildingType type,
      final int minLevel)
    {
        IBuilding lowest = null;
        for (final IBuilding building : context.buildings())
        {
            if (!type.matches(building.getBuildingType())
                  || !building.isBuilt()
                  || building.getBuildingLevel() <= 0
                  || building.getBuildingLevel() >= minLevel
                  || building.getBuildingLevel() >= building.getMaxBuildingLevel()
                  || hasPendingOrderAt(context, building.getPosition()))
            {
                continue;
            }

            if (lowest == null || building.getBuildingLevel() < lowest.getBuildingLevel())
            {
                lowest = building;
            }
        }
        return lowest;
    }

    private static boolean hasPendingOrderAt(@NotNull final PlanningContext context, @NotNull final BlockPos position)
    {
        try
        {
            for (final var order : context.colony().getWorkManager().getWorkOrders().values())
            {
                if (order != null && position.equals(order.getLocation()))
                {
                    return true;
                }
            }
        }
        catch (final Exception ignored)
        {
        }
        return false;
    }

    @NotNull
    private static String describeStep(
      @NotNull final PlanningContext context,
      @NotNull final FixedPlanStep step)
    {
        final StringBuilder builder = new StringBuilder();
        for (final FixedPlanRequirement requirement : step.getRequirements())
        {
            if (!builder.isEmpty())
            {
                builder.append(", ");
            }
            builder.append(requirement.getKind().name().toLowerCase(java.util.Locale.ROOT));
            if (requirement.getKind() == FixedPlanRequirement.Kind.BUILDING)
            {
                builder.append(":").append(requirement.getBuildingType().getSchematicId());
                builder.append(" lvl>=").append(requirement.getMinLevel());
                builder.append(" ")
                  .append(ScriptedPlanProgress.countBuiltBuildingSlots(context, requirement.getBuildingType(), requirement.getMinLevel()))
                  .append("/")
                  .append(requirement.getCount());
            }
            else
            {
                builder.append(" ")
                  .append(ScriptedPlanProgress.countFields(context.colony()))
                  .append("/")
                  .append(requirement.getCount());
            }
        }
        return builder.toString();
    }

    private void clampStepIndex()
    {
        currentStepIndex = Math.max(0, Math.min(currentStepIndex, script.stepCount()));
    }
}
