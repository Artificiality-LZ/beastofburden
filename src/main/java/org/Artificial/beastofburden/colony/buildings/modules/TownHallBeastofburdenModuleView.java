package org.Artificial.beastofburden.colony.buildings.modules;

import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import net.minecraft.network.FriendlyByteBuf;
import org.Artificial.beastofburden.client.gui.BeastofburdenModuleWindow;
import org.Artificial.beastofburden.colony.jobs.BeastofburdenJobs;
import org.Artificial.beastofburden.colony.planning.ColonyPhase;
import org.Artificial.beastofburden.colony.planning.FixedPlanScript;
import org.Artificial.beastofburden.colony.planning.PlanningMode;
import org.Artificial.beastofburden.colony.work.BeastWorkSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * Client view for {@link TownHallBeastofburdenModule}.
 * <p>
 * Not shown on the MineColonies module sidebar ({@link #isPageVisible()} is false). Opened from
 * the Town Hall Actions page via {@link org.Artificial.beastofburden.mixin.WindowMainPageMixin}.
 */
public class TownHallBeastofburdenModuleView extends WorkerBuildingModuleView
{
    private BeastWorkSnapshot workSnapshot = BeastWorkSnapshot.EMPTY;
    private boolean workUpdated = true;
    private boolean autonomousPlanningEnabled;
    private PlanningMode planningMode = PlanningMode.DEFAULT;
    private int scriptedStepIndex;
    private int scriptedStepCount;
    private ColonyPhase planningPhase = ColonyPhase.P0_FOUNDATION;
    private String planningLastDecision = "";

    @Override
    public boolean isPageVisible()
    {
        return false;
    }

    @NotNull
    @Override
    public BOWindow getWindow()
    {
        return new BeastofburdenModuleWindow(this);
    }

    @Override
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {
        super.deserialize(buf);
        workSnapshot = BeastWorkSnapshot.read(buf);
        autonomousPlanningEnabled = workSnapshot.isAutonomousPlanningEnabled();
        planningMode = workSnapshot.getPlanningMode();
        scriptedStepIndex = workSnapshot.getScriptedStepIndex();
        scriptedStepCount = workSnapshot.getScriptedStepCount();
        planningPhase = workSnapshot.getPlanningPhase();
        planningLastDecision = workSnapshot.getPlanningLastDecision();
        workUpdated = true;
    }

    public boolean isAutonomousPlanningEnabled()
    {
        return autonomousPlanningEnabled;
    }

    @NotNull
    public PlanningMode getPlanningMode()
    {
        return planningMode;
    }

    public int getScriptedStepIndex()
    {
        return scriptedStepIndex;
    }

    public int getScriptedStepCount()
    {
        return scriptedStepCount;
    }

    @NotNull
    public ColonyPhase getPlanningPhase()
    {
        return planningPhase;
    }

    @NotNull
    public String getPlanningLastDecision()
    {
        return planningLastDecision;
    }

    public boolean checkAndResetWorkUpdated()
    {
        if (workUpdated)
        {
            workUpdated = false;
            return true;
        }

        return false;
    }

    @NotNull
    public BeastWorkSnapshot getWorkSnapshot()
    {
        return workSnapshot;
    }

    @NotNull
    public FixedPlanScript getPlanScript()
    {
        return workSnapshot.getPlanScript();
    }

    @Override
    public boolean isFull()
    {
        return getAssignedCitizens().size() >= getMaxInhabitants();
    }

    @Override
    public boolean canAssign(final ICitizenDataView citizen)
    {
        if (citizen.isChild())
        {
            return false;
        }

        if (getAssignedCitizens().contains(citizen.getId()))
        {
            return true;
        }

        return citizen.getWorkBuilding() == null;
    }

    @Override
    public JobEntry getJobEntry()
    {
        final JobEntry syncedEntry = super.getJobEntry();
        return syncedEntry != null ? syncedEntry : BeastofburdenJobs.BEASTOFBURDEN.get();
    }
}
