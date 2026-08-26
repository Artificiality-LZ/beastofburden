package org.Artificial.beastofburden.event;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.Artificial.beastofburden.util.BeastofBurdenRequestQueue;
import org.Artificial.beastofburden.util.RequestItemUtils;
import org.Artificial.beastofburden.util.UnfulfillableRequestDetector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans colony request systems and enqueues stuck item requests for beast-of-burden workers.
 */
@Mod.EventBusSubscriber(modid = Beastofburden.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ColonyRequestEventHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_INTERVAL_TICKS = 40;

    private static final Map<Integer, BeastofBurdenRequestQueue> COLONY_QUEUES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> LAST_SCAN_TICK = new ConcurrentHashMap<>();
    private static int tickCounter;

    private ColonyRequestEventHandler()
    {
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.side.isClient())
        {
            return;
        }

        tickCounter++;
        if (tickCounter % SCAN_INTERVAL_TICKS != 0)
        {
            return;
        }

        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            if (colony == null || colony.getWorld() == null || colony.getWorld().isClientSide)
            {
                continue;
            }

            try
            {
                scanColony(colony, tickCounter);
            }
            catch (final Throwable ex)
            {
                LOGGER.error(
                  "[{}] Failed scanning colony {} for stuck requests; continuing. {}",
                  Beastofburden.MODID,
                  colony.getID(),
                  ex.toString(),
                  ex
                );
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel unloading))
        {
            return;
        }

        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            if (colony == null)
            {
                continue;
            }

            if (colony.getWorld() == unloading)
            {
                clearColonyQueue(colony.getID());
                continue;
            }

            // Colony world pointer already cleared but dimension still matches the unloading level.
            if (colony.getWorld() == null && colony.getDimension().equals(unloading.dimension()))
            {
                clearColonyQueue(colony.getID());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(final ServerStoppingEvent event)
    {
        COLONY_QUEUES.clear();
        LAST_SCAN_TICK.clear();
    }

    /**
     * Scan a colony when a beast of burden is looking for work (throttled).
     */
    public static void scanColonyIfDue(@NotNull final IColony colony)
    {
        final int lastScan = LAST_SCAN_TICK.getOrDefault(colony.getID(), -SCAN_INTERVAL_TICKS);
        if (tickCounter - lastScan < SCAN_INTERVAL_TICKS)
        {
            return;
        }

        scanColony(colony, tickCounter);
    }

    private static void scanColony(@NotNull final IColony colony, final int currentTick)
    {
        LAST_SCAN_TICK.put(colony.getID(), currentTick);
        final BeastofBurdenRequestQueue queue = COLONY_QUEUES.computeIfAbsent(colony.getID(), id -> new BeastofBurdenRequestQueue());
        queue.purgeInvalid(colony);

        final Collection<IRequest<?>> candidates = UnfulfillableRequestDetector.getAllRequests(colony);

        for (final IRequest<?> request : candidates)
        {
            if (!UnfulfillableRequestDetector.isUnfulfillable(colony, request))
            {
                BeastofBurdenLog.info("Colony {} skip (not unfulfillable): {}", colony.getID(), UnfulfillableRequestDetector.explain(colony, request));
                continue;
            }

            final ItemStack stack = RequestItemUtils.extractItemStack(request);
            if (stack.isEmpty())
            {
                BeastofBurdenLog.info(
                  "Colony {} skip (no item stack): {} requestable={}",
                  colony.getID(),
                  UnfulfillableRequestDetector.explain(colony, request),
                  request.getRequest() == null ? "null" : request.getRequest().getClass().getSimpleName()
                );
                continue;
            }

            final int sizeBefore = queue.size();
            queue.addRequest(request);
            if (queue.size() > sizeBefore)
            {
                LOGGER.info(
                  "[{}] Colony {}: queued request for {} x{}.",
                  Beastofburden.MODID,
                  colony.getID(),
                  stack.getHoverName().getString(),
                  stack.getCount()
                );
                BeastofBurdenLog.info("Colony {} queued: {}", colony.getID(), UnfulfillableRequestDetector.explain(colony, request));
            }
        }
    }

    @NotNull
    public static BeastofBurdenRequestQueue getQueue(@NotNull final IColony colony)
    {
        return COLONY_QUEUES.computeIfAbsent(colony.getID(), id -> new BeastofBurdenRequestQueue());
    }

    public static void purgeQueue(@NotNull final IColony colony)
    {
        getQueue(colony).purgeInvalid(colony);
    }

    public static void clearColonyQueue(final int colonyId)
    {
        COLONY_QUEUES.remove(colonyId);
        LAST_SCAN_TICK.remove(colonyId);
    }
}
