package org.Artificial.beastofburden.entity.ai;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.entity.ai.states.BeastofBurdenState;
import org.Artificial.beastofburden.entity.ai.tasks.ItemGenerationTask;
import org.Artificial.beastofburden.event.ColonyRequestEventHandler;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.BeastofBurdenRequestQueue;
import org.Artificial.beastofburden.util.BeastWorkSync;
import org.Artificial.beastofburden.util.ColonyLogistics;
import org.Artificial.beastofburden.util.RequestItemUtils;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.INIT;

/**
 * Beast of Burden AI: detects stuck colony item requests, generates the items, and delivers them.
 */
public class EntityAIBeastofburden extends AbstractAISkeleton<JobBeastofburden>
{
    private static final int IDLE_WANDER_RADIUS = 15;
    private static final int WANDER_INTERVAL_TICKS = 200;

    private final ItemGenerationTask generationTask;

    private int wanderCooldown;
    private BlockPos wanderTarget = BlockPos.ZERO;

    private int debugLogCooldown;

    private int progressSyncCooldown;

    public EntityAIBeastofburden(@NotNull final JobBeastofburden job)
    {
        super(job);
        this.generationTask = new ItemGenerationTask(job);

        super.registerTargets(
          new AITarget(INIT, IDLE, 1),
          new AITarget(IDLE, this::hasWorkAvailable, () -> BeastofBurdenState.GENERATE_ITEM, 1),
          new AITarget(IDLE, this::idle, 20),
          new AITarget(BeastofBurdenState.GENERATE_ITEM, this::tickGenerateItem, 1),
          new AITarget(BeastofBurdenState.DELIVER_ITEM, this::tickDeliverItem, 1)
        );
    }

    @NotNull
    public ItemGenerationTask getGenerationTask()
    {
        return generationTask;
    }

    /**
     * @return true while generation, delivery, or queued colony work is active.
     */
    public boolean hasActiveWork()
    {
        if (generationTask.isWorking() || generationTask.hasPendingDelivery())
        {
            return true;
        }

        return !ColonyRequestEventHandler.getQueue(job.getColony()).isEmpty();
    }

    @Override
    public void resetAI()
    {
        if (generationTask.isWorking() || generationTask.hasPendingDelivery())
        {
            return;
        }

        generationTask.cancel();
        super.resetAI();
    }

    @Override
    public boolean canBeInterrupted()
    {
        return !generationTask.isWorking() && !generationTask.hasPendingDelivery();
    }

    @Override
    public void onRemoval()
    {
        final ItemStack stack = generationTask.getGeneratedStack().isEmpty()
          ? generationTask.getPendingDeliveryStack()
          : generationTask.getGeneratedStack();
        BeastWorkSync.onCancelled(job, stack);
        generationTask.cancel();
        super.onRemoval();
    }

    private void syncProgressIfDue()
    {
        if (--progressSyncCooldown <= 0)
        {
            progressSyncCooldown = 10;
            BeastWorkSync.onGenerationProgress(job);
        }
    }

    private boolean hasWorkAvailable()
    {
        if (generationTask.isWorking() || generationTask.hasPendingDelivery())
        {
            return true;
        }

        final IColony colony = job.getColony();
        ColonyRequestEventHandler.scanColonyIfDue(colony);
        final boolean hasQueueWork = !ColonyRequestEventHandler.getQueue(colony).isEmpty();
        logAiStatus(colony, hasQueueWork);
        return hasQueueWork;
    }

    private void logAiStatus(@NotNull final IColony colony, final boolean hasQueueWork)
    {
        if (--debugLogCooldown > 0)
        {
            return;
        }

        debugLogCooldown = 100;
        BeastofBurdenLog.info(
          "Citizen {} colony={} aiState={} queueSize={} hasWork={} working={} pendingDelivery={}",
          job.getCitizen().getId(),
          colony.getID(),
          getState(),
          ColonyRequestEventHandler.getQueue(colony).size(),
          hasQueueWork,
          generationTask.isWorking(),
          generationTask.hasPendingDelivery()
        );
    }

    @NotNull
    private IAIState idle()
    {
        if (generationTask.hasPendingDelivery())
        {
            return BeastofBurdenState.DELIVER_ITEM;
        }

        if (hasActiveWork())
        {
            return BeastofBurdenState.GENERATE_ITEM;
        }

        if (--wanderCooldown <= 0)
        {
            wanderCooldown = WANDER_INTERVAL_TICKS;
            wanderTarget = pickWanderTarget();
        }

        if (!wanderTarget.equals(BlockPos.ZERO))
        {
            walkTowards(wanderTarget, CitizenConstants.DEFAULT_RANGE_FOR_DELAY);
        }

        return IDLE;
    }

    @NotNull
    private BlockPos pickWanderTarget()
    {
        final IBuilding workBuilding = job.getWorkBuilding();
        final BlockPos center = workBuilding != null ? workBuilding.getPosition() : job.getColony().getCenter();
        final int offsetX = worker.getRandom().nextInt(IDLE_WANDER_RADIUS * 2 + 1) - IDLE_WANDER_RADIUS;
        final int offsetZ = worker.getRandom().nextInt(IDLE_WANDER_RADIUS * 2 + 1) - IDLE_WANDER_RADIUS;
        return center.offset(offsetX, 0, offsetZ);
    }

    @NotNull
    private IAIState tickGenerateItem()
    {
        updateWorkStatus(true);

        if (!generationTask.isWorking())
        {
            ColonyRequestEventHandler.scanColonyIfDue(job.getColony());
            if (!generationTask.startNextRequest(job.getColony()))
            {
                BeastofBurdenLog.info("Citizen {} found no queue work after scan.", job.getCitizen().getId());
                return IDLE;
            }

            BeastofBurdenLog.info("Citizen {} started generating for request.", job.getCitizen().getId());
            BeastWorkSync.onGenerationStarted(job, generationTask.getGeneratedStack());
        }

        syncProgressIfDue();

        final BlockPos workPos = RequestItemUtils.getDeliveryPosition(job.getColony(), generationTask.getCurrentRequest());
        if (workPos != null && !workPos.equals(BlockPos.ZERO))
        {
            walkTowards(workPos, CitizenConstants.DEFAULT_RANGE_FOR_DELAY * 2);
        }

        if (!generationTask.tick())
        {
            worker.setRenderMetadata("working");
            syncProgressIfDue();
            return BeastofBurdenState.GENERATE_ITEM;
        }

        BeastWorkSync.onGenerationComplete(job, generationTask.getPendingDeliveryStack(), generationTask.getLastGenerationDuration());
        worker.setRenderMetadata("");
        if (generationTask.hasPendingDelivery())
        {
            BeastofBurdenLog.info("Citizen {} finished generating, delivering.", job.getCitizen().getId());
            return BeastofBurdenState.DELIVER_ITEM;
        }

        return IDLE;
    }

    @NotNull
    private IAIState tickDeliverItem()
    {
        updateWorkStatus(true);

        if (!generationTask.hasPendingDelivery())
        {
            updateWorkStatus(false);
            return IDLE;
        }

        BeastWorkSync.syncFromJob(job);

        final IRequest<?> request = generationTask.getPendingDeliveryRequest();
        final ItemStack stack = generationTask.getPendingDeliveryStack();
        if (request == null || ItemStackUtils.isEmpty(stack))
        {
            generationTask.completeDelivery();
            updateWorkStatus(false);
            return IDLE;
        }

        final IColony colony = job.getColony();
        if (!RequestItemUtils.isStillFulfillable(colony, request))
        {
            generationTask.completeDelivery();
            updateWorkStatus(false);
            return IDLE;
        }

        final BlockPos deliveryPos = RequestItemUtils.getDeliveryPosition(colony, request);
        if (!walkTowards(deliveryPos, CitizenConstants.DEFAULT_RANGE_FOR_DELAY))
        {
            return BeastofBurdenState.DELIVER_ITEM;
        }

        if (!deliverRequest(colony, request, stack))
        {
            BeastofBurdenLog.warn("Citizen {} failed to deliver {}.", job.getCitizen().getId(), stack.getHoverName().getString());
            return BeastofBurdenState.DELIVER_ITEM;
        }

        generationTask.completeDelivery();

        final BeastofBurdenRequestQueue queue = ColonyRequestEventHandler.getQueue(colony);
        queue.removeRequest(request.getId());

        BeastWorkSync.onDeliveryComplete(job, stack, generationTask.getLastGenerationDuration());

        BeastofBurdenLog.info("Citizen {} delivered {} to request.", job.getCitizen().getId(), stack.getHoverName().getString());
        updateWorkStatus(false);
        return IDLE;
    }

    private boolean deliverRequest(@NotNull final IColony colony, @NotNull final IRequest<?> request, @NotNull final ItemStack stack)
    {
        final ItemStack delivery = stack.copy();
        InventoryUtils.removeStackFromItemHandler(worker.getInventoryCitizen(), delivery, delivery.getCount());
        return ColonyLogistics.fulfillRequest(colony, request, delivery);
    }

    private void updateWorkStatus(final boolean working)
    {
        job.getCitizen().setVisibleStatus(working ? VisibleCitizenStatus.WORKING : null);
    }

    /**
     * @return {@code true} when the worker has arrived within range of the target.
     */
    private boolean walkTowards(@NotNull final BlockPos target, final int range)
    {
        if (worker.blockPosition().distSqr(target) <= (long) range * range)
        {
            worker.getNavigation().stop();
            return true;
        }

        worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
        return false;
    }
}
