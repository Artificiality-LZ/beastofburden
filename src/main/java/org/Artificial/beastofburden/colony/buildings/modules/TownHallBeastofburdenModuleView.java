package org.Artificial.beastofburden.colony.buildings.modules;

import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.client.gui.BeastofburdenModuleWindow;
import org.Artificial.beastofburden.colony.jobs.BeastofburdenJobs;
import org.Artificial.beastofburden.colony.planning.ColonyPhase;
import org.Artificial.beastofburden.colony.planning.PlanningMode;
import org.Artificial.beastofburden.colony.work.BeastWorkSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * Client view for {@link TownHallBeastofburdenModule}.
 */
public class TownHallBeastofburdenModuleView extends WorkerBuildingModuleView
{
    private static final ResourceLocation TAB_ICON =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/modules/entity.png");

    private static final String GUI_DESC_KEY = "com.beastofburden.gui.townhall.beastofburden";

    private BeastWorkSnapshot workSnapshot = BeastWorkSnapshot.EMPTY;
    private boolean workUpdated = true;
    private boolean autonomousPlanningEnabled;
    private PlanningMode planningMode = PlanningMode.HEURISTIC;
    private int scriptedStepIndex;
    private int scriptedStepCount;
    private ColonyPhase planningPhase = ColonyPhase.P0_FOUNDATION;
    private String planningLastDecision = "";

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return TAB_ICON;
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable(GUI_DESC_KEY);
    }

    @Override
    public boolean isPageVisible()
    {
        return true;
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
