package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal per-colony planner state. All decisions are delegated to the active strategy.
 */
public final class ColonyPlanner
{
    private final PlanningService service = new PlanningService();
    private final HeuristicPlanningStrategy heuristicStrategy = new HeuristicPlanningStrategy();
    private final ScriptedPlanningStrategy scriptedStrategy = new ScriptedPlanningStrategy();
    private final PlanningReport report = new PlanningReport();

    private PlanningMode planningMode = PlanningMode.HEURISTIC;
    private int retryCooldown;
    private String lastDecision = "";

    public static int planRetryCooldown()
    {
        return PlanningConfig.planRetryCooldown();
    }

    public static int coldStartPlanCooldown()
    {
        return PlanningConfig.coldStartPlanCooldown();
    }

    @Nullable
    public PlanningResult tick(@NotNull final IColony colony)
    {
        return service.tick(this, colony);
    }

    @Nullable
    BuildTask selectTask(@NotNull final PlanningContext context)
    {
        return activeStrategy().selectNextTask(context);
    }

    @NotNull
    String describeState(@NotNull final PlanningContext context)
    {
        return activeStrategy().describeState(context);
    }

    @NotNull
    private PlanningStrategy activeStrategy()
    {
        return planningMode == PlanningMode.SCRIPTED ? scriptedStrategy : heuristicStrategy;
    }

    boolean consumeRetryCooldown()
    {
        if (retryCooldown <= 0)
        {
            return false;
        }

        retryCooldown--;
        lastDecision = "cooldown";
        report.waiting("cooldown", "retry in " + retryCooldown + " ticks");
        return true;
    }

    void startRetryCooldown(final int ticks)
    {
        retryCooldown = Math.max(0, ticks);
    }

    /**
     * Clears the retry cooldown so the next planning pass can run immediately (debug / admin).
     */
    public void clearRetryCooldown()
    {
        retryCooldown = 0;
        if ("cooldown".equals(lastDecision))
        {
            lastDecision = "";
        }
        if ("cooldown".equals(report.getDecision()))
        {
            report.clear();
        }
    }

    @NotNull
    public PlanningReport getReport()
    {
        return report;
    }

    @NotNull
    public PlanningMode getPlanningMode()
    {
        return planningMode;
    }

    public void setPlanningMode(@NotNull final PlanningMode mode)
    {
        if (planningMode == mode)
        {
            return;
        }
        planningMode = mode;
        retryCooldown = 0;
        lastDecision = "";
        report.clear();
    }

    @NotNull
    public ScriptedPlanningStrategy getScriptedStrategy()
    {
        return scriptedStrategy;
    }

    public boolean refreshScriptedProgress(@NotNull final IColony colony)
    {
        return planningMode == PlanningMode.SCRIPTED && scriptedStrategy.refreshProgress(PlanningContext.collect(colony));
    }

    public boolean refreshScriptedProgress(@NotNull final PlanningContext context)
    {
        return planningMode == PlanningMode.SCRIPTED && scriptedStrategy.refreshProgress(context);
    }

    @NotNull
    public String describePlanningState(@NotNull final IColony colony, @NotNull final ColonySnapshot snapshot)
    {
        return report.getDetail();
    }

    @NotNull
    public String getLastDecision()
    {
        return lastDecision;
    }

    public void setLastDecision(@NotNull final String decision)
    {
        lastDecision = decision;
    }

    /**
     * Kept only for the existing client snapshot field. Planning no longer uses phases.
     */
    @NotNull
    public ColonyPhase getCurrentPhaseOrDefault()
    {
        return ColonyPhase.P0_FOUNDATION;
    }

    @Nullable
    public ColonyPhase getCurrentPhase()
    {
        return ColonyPhase.P0_FOUNDATION;
    }

    public void syncBootstrapState(@NotNull final IColony colony)
    {
    }

    public void updateBootstrapTransition(@NotNull final IColony colony)
    {
    }

    public void readFromNbt(
      final int phaseId,
      final int emergencyDays,
      final int recoveryDays,
      final int phaseCooldown,
      final int retryCooldown,
      final int researchCooldown,
      @Nullable final CompoundTag debugTag,
      @Nullable final CompoundTag blocklistTag,
      final long gameTime,
      final int planningModeId,
      @Nullable final CompoundTag scriptedTag)
    {
        this.retryCooldown = retryCooldown;
        planningMode = PlanningMode.fromId(planningModeId);
        scriptedStrategy.readFromNbt(scriptedTag);
        report.readFromNbt(debugTag);
    }

    public int getResearchCooldown()
    {
        return 0;
    }

    public int getPhaseCooldown()
    {
        return 0;
    }

    public int getRetryCooldown()
    {
        return retryCooldown;
    }

    public int getPhaseId()
    {
        return ColonyPhase.P0_FOUNDATION.ordinal();
    }

    public int getEmergencyDays()
    {
        return 0;
    }

    public int getRecoveryDays()
    {
        return 0;
    }

    @NotNull
    public CompoundTag writeDebugNbt()
    {
        return report.writeToNbt();
    }

    @NotNull
    public CompoundTag writeBlocklistNbt(final long gameTime)
    {
        return new CompoundTag();
    }

    public record PlanningResult(
      int phaseId,
      @NotNull BuildTask task,
      @NotNull BlockPos location,
      @NotNull BlockPos builder,
      @Nullable String note)
    {
    }
}
