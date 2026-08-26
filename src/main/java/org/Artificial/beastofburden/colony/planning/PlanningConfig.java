package org.Artificial.beastofburden.colony.planning;

import org.Artificial.beastofburden.Config;

/**
 * Runtime planning tuning values.
 */
public final class PlanningConfig
{
    private static final int MAX_COOLDOWN_PASSES = 20;

    private PlanningConfig()
    {
    }

    public static int planRetryCooldown()
    {
        return cooldownPasses(Config.planningRetryCooldown);
    }

    public static int coldStartPlanCooldown()
    {
        return cooldownPasses(Config.planningColdStartCooldown);
    }

    public static int searchRadius()
    {
        return Config.planningSearchRadius;
    }

    public static int maxPlacementCandidates()
    {
        return Config.planningMaxCandidates;
    }

    public static int builderRadius()
    {
        return Config.planningBuilderRadius;
    }

    public static int maxBuilderQueueSize()
    {
        return Config.planningMaxBuilderQueue;
    }

    public static int minBlueprintSeparation()
    {
        return Config.planningMinBlueprintSeparation;
    }

    public static boolean instantBuildDebug()
    {
        return PlanningInstantBuildState.isEnabled();
    }

    private static int cooldownPasses(final int configured)
    {
        return Math.max(0, Math.min(MAX_COOLDOWN_PASSES, configured));
    }
}
