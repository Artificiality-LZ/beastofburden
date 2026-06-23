package org.Artificial.beastofburden.colony.planning;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats scripted plan steps for GUI display.
 */
public final class PlanStepFormatter
{
    private PlanStepFormatter()
    {
    }

    @NotNull
    public static Component formatStep(@NotNull final FixedPlanStep step)
    {
        final List<Component> parts = new ArrayList<>();
        for (final FixedPlanRequirement requirement : step.getRequirements())
        {
            parts.add(formatRequirement(requirement));
        }

        if (parts.isEmpty())
        {
            return Component.translatable("com.beastofburden.gui.plan_editor.empty_step");
        }

        MutableComponent result = parts.get(0).copy();
        for (int i = 1; i < parts.size(); i++)
        {
            result.append(Component.literal(", ")).append(parts.get(i));
        }
        return result;
    }

    @NotNull
    public static Component formatRequirement(@NotNull final FixedPlanRequirement requirement)
    {
        if (requirement.getKind() == FixedPlanRequirement.Kind.FIELD)
        {
            return Component.translatable(
              "com.beastofburden.gui.plan_editor.field_count",
              requirement.getCount()
            );
        }

        return Component.translatable(
          "com.beastofburden.gui.plan_editor.building_requirement",
          requirement.getMinLevel(),
          buildingName(requirement.getBuildingType()),
          requirement.getCount()
        );
    }

    @NotNull
    public static Component buildingName(@NotNull final PlannedBuildingType type)
    {
        return Component.translatable(type.getEntry().getTranslationKey());
    }

    @NotNull
    public static String formatStepPlain(@NotNull final FixedPlanStep step)
    {
        return formatStep(step).getString();
    }
}
