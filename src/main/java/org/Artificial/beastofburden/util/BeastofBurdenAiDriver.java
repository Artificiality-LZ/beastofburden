package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.jetbrains.annotations.NotNull;

/**
 * Drives beast-of-burden job AI when there is queued or in-progress colony work.
 * <p>
 * Called every server tick from {@link org.Artificial.beastofburden.event.BeastofBurdenWorkDriver}.
 * MineColonies {@code CitizenAI} may also tick job AI while {@code WORKING}.
 * {@link EntityAIBeastofburden#tick()} ignores a second call in the same game tick.
 */
public final class BeastofBurdenAiDriver
{
    private BeastofBurdenAiDriver()
    {
    }

    public static void tickCitizen(@NotNull final ICitizenData citizen)
    {
        if (!(citizen.getJob() instanceof JobBeastofburden job))
        {
            return;
        }

        ensureWorkAi(job);

        final EntityAIBeastofburden workAi = job.getWorkerAI();
        if (workAi == null)
        {
            BeastofBurdenLog.warn("Beast citizen {} has no work AI after createAI.", citizen.getId());
            return;
        }

        if (!workAi.hasActiveWork())
        {
            return;
        }

        workAi.tick();
    }

    private static void ensureWorkAi(@NotNull final JobBeastofburden job)
    {
        if (job.getWorkerAI() != null)
        {
            return;
        }

        if (!job.getCitizen().getEntity().isPresent())
        {
            return;
        }

        try
        {
            job.createAI();
            BeastofBurdenLog.info("Created work AI for beast citizen {}.", job.getCitizen().getId());
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn(
              "Failed to create work AI for beast citizen {}: {}",
              job.getCitizen().getId(),
              ex.toString()
            );
        }
    }
}
