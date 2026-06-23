package org.Artificial.beastofburden.colony.planning;

import org.jetbrains.annotations.NotNull;

/**
 * How the colony planner chooses the next build task.
 */
public enum PlanningMode
{
    /** Weighted scoring over colony phase (CDAI tactical layer). */
    HEURISTIC,
    /** Fixed step sequence from {@link FixedPlanScript}. */
    SCRIPTED;

    public static PlanningMode fromId(final int id)
    {
        final PlanningMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : HEURISTIC;
    }

    @NotNull
    public PlanningMode next()
    {
        final PlanningMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
