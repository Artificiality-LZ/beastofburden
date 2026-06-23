package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.work.BeastWorkLogAction;
import org.Artificial.beastofburden.colony.work.BeastWorkLogEntry;
import org.Artificial.beastofburden.colony.work.BeastWorkStatus;
import org.Artificial.beastofburden.event.ColonyRequestEventHandler;
import org.jetbrains.annotations.NotNull;

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
        return result.task().getAction().name()
          + " "
          + result.task().getType().getSchematicId()
          + " @ "
          + result.location().getX()
          + ","
          + result.location().getY()
          + ","
          + result.location().getZ()
          + " ("
          + (result.task().getReason() == null ? "" : result.task().getReason())
          + ")";
    }
}
