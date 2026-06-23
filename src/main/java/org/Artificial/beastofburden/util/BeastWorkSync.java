package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.world.item.ItemStack;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.colony.work.BeastWorkLogAction;
import org.Artificial.beastofburden.colony.work.BeastWorkLogEntry;
import org.Artificial.beastofburden.colony.work.BeastWorkStatus;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.Artificial.beastofburden.entity.ai.tasks.ItemGenerationTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Updates TownHall work status and history from beast AI.
 */
public final class BeastWorkSync
{
    private BeastWorkSync()
    {
    }

    public static void syncFromJob(@NotNull final JobBeastofburden job)
    {
        final TownHallBeastofburdenModule module = getModule(job);
        if (module == null)
        {
            return;
        }

        final ICitizenData citizen = job.getCitizen();
        final EntityAIBeastofburden ai = job.getWorkerAI();
        if (ai == null)
        {
            module.setActiveWork(BeastWorkStatus.idle(citizen.getId(), citizen.getName()));
            return;
        }

        final ItemGenerationTask task = ai.getGenerationTask();
        if (task.isWorking())
        {
            final BeastWorkStatus status = BeastWorkStatus.generating(
              citizen.getId(),
              citizen.getName(),
              task.getGeneratedStack(),
              task.getProgressTicks(),
              task.getRequiredTicks()
            );
            module.setActiveWork(status);
            return;
        }

        if (task.hasPendingDelivery())
        {
            final BeastWorkStatus status = BeastWorkStatus.delivering(citizen.getId(), citizen.getName(), task.getPendingDeliveryStack());
            module.setActiveWork(status);
            return;
        }

        final BeastWorkStatus idle = BeastWorkStatus.idle(citizen.getId(), citizen.getName());
        module.setActiveWork(idle);
    }

    public static void onGenerationStarted(@NotNull final JobBeastofburden job, @NotNull final ItemStack stack)
    {
        syncFromJob(job);
    }

    public static void onGenerationProgress(@NotNull final JobBeastofburden job)
    {
        syncFromJob(job);
    }

    public static void onGenerationComplete(@NotNull final JobBeastofburden job, @NotNull final ItemStack stack, final int durationTicks)
    {
        final TownHallBeastofburdenModule module = getModule(job);
        if (module != null)
        {
            module.appendLog(new BeastWorkLogEntry(
              job.getColony().getDay(),
              job.getCitizen().getId(),
              job.getCitizen().getName(),
              BeastWorkLogAction.GENERATED,
              net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
              stack.getCount(),
              durationTicks
            ));
            syncFromJob(job);
        }
    }

    public static void onDeliveryComplete(@NotNull final JobBeastofburden job, @NotNull final ItemStack stack, final int durationTicks)
    {
        final TownHallBeastofburdenModule module = getModule(job);
        if (module != null)
        {
            module.appendLog(BeastWorkLogEntry.delivered(
              job.getColony().getDay(),
              job.getCitizen().getId(),
              job.getCitizen().getName(),
              stack,
              durationTicks
            ));
            syncFromJob(job);
        }
    }

    public static void onCancelled(@NotNull final JobBeastofburden job, @NotNull final ItemStack stack)
    {
        final TownHallBeastofburdenModule module = getModule(job);
        if (module == null)
        {
            return;
        }

        if (!stack.isEmpty())
        {
            module.appendLog(new BeastWorkLogEntry(
              job.getColony().getDay(),
              job.getCitizen().getId(),
              job.getCitizen().getName(),
              BeastWorkLogAction.CANCELLED,
              net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
              stack.getCount(),
              0
            ));
        }

        syncFromJob(job);
    }

    @Nullable
    private static TownHallBeastofburdenModule getModule(@NotNull final JobBeastofburden job)
    {
        final IBuilding building = job.getWorkBuilding();
        if (building == null)
        {
            return null;
        }

        return building.getFirstModuleOccurance(TownHallBeastofburdenModule.class);
    }
}
