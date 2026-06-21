package org.Artificial.beastofburden.entity.ai.tasks;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.event.ColonyRequestEventHandler;
import org.Artificial.beastofburden.Config;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.BeastofBurdenRequestQueue;
import org.Artificial.beastofburden.util.ItemValueRegistry;
import org.Artificial.beastofburden.util.RequestItemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles timed item generation for a single stuck request.
 */
public class ItemGenerationTask
{
    private final JobBeastofburden job;

    @Nullable
    private IRequest<?> currentRequest;
    private int progressTicks;
    private int requiredTicks = Config.baseGenerationTicks;
    private int lastGenerationDuration;

    @Nullable
    private ItemStack generatedStack = ItemStack.EMPTY;

    @Nullable
    private IRequest<?> pendingDeliveryRequest;

    @NotNull
    private ItemStack pendingDeliveryStack = ItemStack.EMPTY;

    public ItemGenerationTask(@NotNull final JobBeastofburden job)
    {
        this.job = job;
    }

    /**
     * @return {@code true} when a new request was started.
     */
    public boolean startNextRequest(@NotNull final IColony colony)
    {
        final BeastofBurdenRequestQueue queue = ColonyRequestEventHandler.getQueue(colony);
        if (queue == null || queue.isEmpty())
        {
            return false;
        }

        this.currentRequest = queue.pollNext(colony);
        if (currentRequest == null)
        {
            return false;
        }

        this.generatedStack = RequestItemUtils.extractItemStack(currentRequest);
        if (ItemStackUtils.isEmpty(generatedStack))
        {
            BeastofBurdenLog.warn("Generation aborted: could not extract item stack for request.");
            cancel();
            return false;
        }

        this.requiredTicks = calculateDuration(colony, generatedStack);
        this.progressTicks = 0;
        this.lastGenerationDuration = 0;
        job.getAsyncRequests().add(currentRequest.getId());
        ColonyRequestEventHandler.getQueue(colony).markInFlight(currentRequest.getId());
        return true;
    }

    /**
     * @return {@code true} when generation finished this tick.
     */
    public boolean tick()
    {
        if (currentRequest == null)
        {
            return false;
        }

        if (!RequestItemUtils.isStillFulfillable(job.getColony(), currentRequest))
        {
            BeastofBurdenLog.info("Generation cancelled: request is no longer needed.");
            cancel();
            return false;
        }

        progressTicks++;
        if (progressTicks % 20 == 0)
        {
            spawnWorkParticles();
        }

        if (progressTicks < requiredTicks)
        {
            return false;
        }

        completeGeneration();
        return true;
    }

    private void completeGeneration()
    {
        if (currentRequest == null || ItemStackUtils.isEmpty(generatedStack))
        {
            cancel();
            return;
        }

        if (!RequestItemUtils.isStillFulfillable(job.getColony(), currentRequest))
        {
            BeastofBurdenLog.info("Generation finished but request was already fulfilled; discarding items.");
            cancel();
            return;
        }

        if (job.getCitizen().getEntity().isPresent())
        {
            final ItemStack remaining = InventoryUtils.addItemStackToItemHandlerWithResult(
              job.getCitizen().getEntity().get().getInventoryCitizen(),
              generatedStack.copy()
            );

            if (!ItemStackUtils.isEmpty(remaining))
            {
                job.getCitizen().getEntity().get().spawnAtLocation(remaining);
            }
        }

        final IToken<?> token = currentRequest.getId();
        pendingDeliveryRequest = currentRequest;
        pendingDeliveryStack = generatedStack.copy();
        lastGenerationDuration = requiredTicks;
        job.getAsyncRequests().remove(token);
        currentRequest = null;
        progressTicks = 0;
    }

    public boolean hasPendingDelivery()
    {
        return pendingDeliveryRequest != null && !ItemStackUtils.isEmpty(pendingDeliveryStack);
    }

    @Nullable
    public IRequest<?> getPendingDeliveryRequest()
    {
        return pendingDeliveryRequest;
    }

    @NotNull
    public ItemStack getPendingDeliveryStack()
    {
        return pendingDeliveryStack;
    }

    public void completeDelivery()
    {
        pendingDeliveryRequest = null;
        pendingDeliveryStack = ItemStack.EMPTY;
        generatedStack = ItemStack.EMPTY;
    }

    public void cancel()
    {
        if (currentRequest != null)
        {
            job.getAsyncRequests().remove(currentRequest.getId());
            ColonyRequestEventHandler.getQueue(job.getColony()).clearInFlight(currentRequest.getId());
            currentRequest = null;
        }

        if (pendingDeliveryRequest != null)
        {
            ColonyRequestEventHandler.getQueue(job.getColony()).clearInFlight(pendingDeliveryRequest.getId());
        }

        pendingDeliveryRequest = null;
        pendingDeliveryStack = ItemStack.EMPTY;
        generatedStack = ItemStack.EMPTY;
        progressTicks = 0;
    }

    public boolean isWorking()
    {
        return currentRequest != null;
    }

    @Nullable
    public IRequest<?> getCurrentRequest()
    {
        return currentRequest;
    }

    @NotNull
    public ItemStack getGeneratedStack()
    {
        return generatedStack;
    }

    public int getProgressTicks()
    {
        return progressTicks;
    }

    public int getRequiredTicks()
    {
        return requiredTicks;
    }

    public int getLastGenerationDuration()
    {
        return lastGenerationDuration;
    }

    public float getProgressPercent()
    {
        if (currentRequest == null || requiredTicks == 0)
        {
            return 0f;
        }

        return (float) progressTicks / requiredTicks;
    }

    private int calculateDuration(@NotNull final IColony colony, @NotNull final ItemStack stack)
    {
        final int strengthLevel = job.getCitizen().getCitizenSkillHandler().getLevel(Skill.Strength);
        final int totalValue = ItemValueRegistry.getStackValue(stack);
        final double capability = 1.0D + strengthLevel * Config.strengthSpeedBonus;
        final int rawTicks = Config.baseGenerationTicks + (int) Math.round(totalValue * Config.ticksPerItemValue);
        return Math.max(Config.minGenerationTicks, (int) Math.ceil(rawTicks / capability));
    }

    private void spawnWorkParticles()
    {
        if (!job.getCitizen().getEntity().isPresent())
        {
            return;
        }

        final var entity = job.getCitizen().getEntity().get();
        entity.getCommandSenderWorld().addParticle(
          ParticleTypes.ENCHANT,
          entity.getX(),
          entity.getY() + 1.0D,
          entity.getZ(),
          0.0D,
          0.1D,
          0.0D
        );
    }
}
