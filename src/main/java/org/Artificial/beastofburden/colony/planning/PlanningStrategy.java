package org.Artificial.beastofburden.colony.planning;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Chooses the next build intent for one planning mode.
 */
public interface PlanningStrategy
{
    @Nullable
    BuildTask selectNextTask(@NotNull PlanningContext context);

    @NotNull
    String describeState(@NotNull PlanningContext context);
}
