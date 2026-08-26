package org.Artificial.beastofburden.colony.planning;

import org.jetbrains.annotations.NotNull;

/**
 * How the colony planner chooses the next build task.
 */
public enum PlanningMode
{
    /** Score-based next building from the current colony snapshot (experimental). */
    HEURISTIC,
    /** Fixed step sequence from {@link FixedPlanScript}. Supported 1.0 path. */
    SCRIPTED;

    public static final PlanningMode DEFAULT = SCRIPTED;

    public static PlanningMode fromId(final int id)
    {
        final PlanningMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : DEFAULT;
    }

    @NotNull
    public PlanningMode next()
    {
        final PlanningMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
