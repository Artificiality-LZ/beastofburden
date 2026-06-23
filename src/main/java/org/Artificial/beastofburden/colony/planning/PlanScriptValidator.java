package org.Artificial.beastofburden.colony.planning;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates player-submitted colony build scripts.
 */
public final class PlanScriptValidator
{
    public static final int MAX_STEPS = 32;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;
    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 10;

    private PlanScriptValidator()
    {
    }

    @Nullable
    public static FixedPlanScript validate(@NotNull final FixedPlanScript script)
    {
        if (script.stepCount() < 1 || script.stepCount() > MAX_STEPS)
        {
            return null;
        }

        final List<FixedPlanStep> sanitized = new ArrayList<>(script.stepCount());
        for (int i = 0; i < script.stepCount(); i++)
        {
            final FixedPlanStep step = sanitizeStep(script.getStep(i));
            if (step == null)
            {
                return null;
            }
            sanitized.add(step);
        }

        final boolean custom = !contentEquals(new FixedPlanScript(FixedPlanScript.FORMAT_VERSION, true, sanitized), FixedPlanScript.createDefault());
        return new FixedPlanScript(FixedPlanScript.FORMAT_VERSION, custom, sanitized);
    }

    @Nullable
    private static FixedPlanStep sanitizeStep(@NotNull final FixedPlanStep step)
    {
        if (step.getRequirements().isEmpty())
        {
            return null;
        }

        final List<FixedPlanRequirement> requirements = new ArrayList<>();
        for (final FixedPlanRequirement requirement : step.getRequirements())
        {
            final FixedPlanRequirement sanitized = sanitizeRequirement(requirement);
            if (sanitized == null)
            {
                return null;
            }
            requirements.add(sanitized);
        }

        return new FixedPlanStep(requirements);
    }

    @Nullable
    private static FixedPlanRequirement sanitizeRequirement(@NotNull final FixedPlanRequirement requirement)
    {
        final int count = Math.max(MIN_COUNT, Math.min(MAX_COUNT, requirement.getCount()));
        if (requirement.getKind() == FixedPlanRequirement.Kind.FIELD)
        {
            return FixedPlanRequirement.fields(count);
        }

        if (requirement.getBuildingType() == PlannedBuildingType.TOWN_HALL)
        {
            return null;
        }

        final int level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, requirement.getMinLevel()));
        return FixedPlanRequirement.building(requirement.getBuildingType(), level, count);
    }

    public static boolean contentEquals(@NotNull final FixedPlanScript left, @NotNull final FixedPlanScript right)
    {
        if (left.stepCount() != right.stepCount())
        {
            return false;
        }

        for (int i = 0; i < left.stepCount(); i++)
        {
            if (!stepEquals(left.getStep(i), right.getStep(i)))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean stepEquals(@NotNull final FixedPlanStep left, @NotNull final FixedPlanStep right)
    {
        final List<FixedPlanRequirement> leftReqs = left.getRequirements();
        final List<FixedPlanRequirement> rightReqs = right.getRequirements();
        if (leftReqs.size() != rightReqs.size())
        {
            return false;
        }

        for (int i = 0; i < leftReqs.size(); i++)
        {
            if (!requirementEquals(leftReqs.get(i), rightReqs.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean requirementEquals(
      @NotNull final FixedPlanRequirement left,
      @NotNull final FixedPlanRequirement right)
    {
        return left.getKind() == right.getKind()
                 && left.getBuildingType() == right.getBuildingType()
                 && left.getMinLevel() == right.getMinLevel()
                 && left.getCount() == right.getCount();
    }
}
