package org.Artificial.beastofburden.colony.planning;

/**
 * Server-side runtime toggle for instant hut pasting during autonomous planning (debug).
 */
public final class PlanningInstantBuildState
{
    private static volatile boolean enabled;

    private PlanningInstantBuildState()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(final boolean value)
    {
        enabled = value;
    }
}
