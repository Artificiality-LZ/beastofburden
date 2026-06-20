package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.jetbrains.annotations.NotNull;

/**
 * Drives beast-of-burden job AI when there is queued or in-progress colony work.
 * <p>
 * Called every server tick from {@link org.Artificial.beastofburden.event.BeastofBurdenWorkDriver}.
 * MineColonies {@code CitizenAI} also ticks job AI in {@code WORKING}, but TownHall beasts may
 * stay in {@code IDLE} while still showing work from {@code setVisibleStatus}; this driver ensures
 * generation and delivery still advance.
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

        job.createAI();
        BeastofBurdenLog.info("Created work AI for beast citizen {}.", job.getCitizen().getId());
    }
}
