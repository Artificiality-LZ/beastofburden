package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.work.BeastWorkLogAction;
import org.Artificial.beastofburden.colony.work.BeastWorkLogEntry;
import org.Artificial.beastofburden.colony.work.BeastWorkStatus;
import org.Artificial.beastofburden.event.ColonyRequestEventHandler;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server tick adapter between the Town Hall module and the autonomous planner.
 */
public final class ColonyPlannerDriver
{
    private ColonyPlannerDriver()
    {
    }

    public static void tick(@NotNull final TownHallBeastofburdenModule module, @NotNull final IColony colony)
    {
        if (!module.isAutonomousPlanningEnabled())
        {
            return;
        }

        ColonyRequestEventHandler.purgeQueue(colony);
        final ColonyPlanner planner = module.getColonyPlanner();
        final String coldStartNote = ColdStartManager.tick(colony);

        if (module.getAssignedCitizen().isEmpty())
        {
            syncWaiting(module, planner, "no_assigned_beast", "");
            return;
        }

        if (!PlanningWorkload.hasIdleBeast(module))
        {
            syncWaiting(module, planner, "beast_busy", PlanningWorkload.describeBeastBlockers(module));
            return;
        }

        if (ColdStartManager.isBootstrapping(colony))
        {
            syncWaiting(module, planner, "builder_construction_pending", coldStartNote);
            return;
        }

        if (!PlanningWorkload.canAttemptPlanning(colony)
              && !PlanningConfig.instantBuildDebug()
              && FieldPlanner.findFarmerNeedingField(colony) == null)
        {
            final int builders = PlanningWorkload.countBuilderHuts(colony);
            final int active = PlanningWorkload.countActiveConstructionOrders(colony);
            final String decision = builders <= 0 ? "cold_start_busy" : "builders_busy:" + active + "/" + builders;
            syncWaiting(module, planner, decision, coldStartNote);
            return;
        }

        final ColonyPlanner.PlanningResult result = planner.tick(colony);
        module.syncPlanningState(planner);

        if (result != null)
        {
            appendPlanningWork(module, colony, result);
        }
    }

    /**
     * Clears planning retry cooldown for one or all colonies with a Beast Town Hall module.
     *
     * @param colonyId when non-null, only that colony id is updated
     * @return number of colonies whose cooldown was cleared
     */
    public static int refreshPlanningCooldown(@Nullable final Integer colonyId)
    {
        int updated = 0;
        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            if (colony == null)
            {
                continue;
            }
            if (colonyId != null && colony.getID() != colonyId)
            {
                continue;
            }

            final TownHallBeastofburdenModule module = findBeastModule(colony);
            if (module == null)
            {
                continue;
            }

            final ColonyPlanner planner = module.getColonyPlanner();
            planner.clearRetryCooldown();
            module.syncPlanningState(planner);
            updated++;
        }
        return updated;
    }

    @Nullable
    private static TownHallBeastofburdenModule findBeastModule(@NotNull final IColony colony)
    {
        final IBuilding townHall = ColonyBuildings.getTownHall(colony);
        if (townHall == null)
        {
            return null;
        }
        return townHall.getFirstModuleOccurance(TownHallBeastofburdenModule.class);
    }

    private static void syncWaiting(
      @NotNull final TownHallBeastofburdenModule module,
      @NotNull final ColonyPlanner planner,
      @NotNull final String decision,
      @NotNull final String detail)
    {
        planner.setLastDecision(decision);
        planner.getReport().waiting(decision, detail);
        module.syncPlanningState(planner);
    }

    private static void appendPlanningWork(
      @NotNull final TownHallBeastofburdenModule module,
      @NotNull final IColony colony,
      @NotNull final ColonyPlanner.PlanningResult result)
    {
        final ICitizenData plannerCitizen = module.getAssignedCitizen().isEmpty() ? null : module.getAssignedCitizen().get(0);
        if (plannerCitizen == null)
        {
            return;
        }

        final ResourceLocation hutItem = BuiltInRegistries.ITEM.getKey(result.task().getType().getEntry().getBuildingBlock().asItem());
        final String detail = formatDetail(result);
        module.setActiveWork(BeastWorkStatus.planning(plannerCitizen.getId(), plannerCitizen.getName(), hutItem, detail));
        module.appendLog(new BeastWorkLogEntry(
          colony.getDay(),
          plannerCitizen.getId(),
          plannerCitizen.getName(),
          BeastWorkLogAction.PLANNED,
          hutItem,
          1,
          0,
          detail
        ));
    }

    @NotNull
    private static String formatDetail(@NotNull final ColonyPlanner.PlanningResult result)
    {
        return PlanningDisplayFormatter.formatPlannedToken(result.task());
    }
}
