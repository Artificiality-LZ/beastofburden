package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-colony priority queue of stuck item requests for beast-of-burden workers.
 */
public class BeastofBurdenRequestQueue
{
    private final PriorityQueue<QueuedRequest> queue = new PriorityQueue<>(
      Comparator.comparingInt(QueuedRequest::priority).reversed()
        .thenComparingInt(QueuedRequest::order)
    );

    private final Set<IToken<?>> trackedTokens = ConcurrentHashMap.newKeySet();
    private final Set<IToken<?>> inFlightTokens = ConcurrentHashMap.newKeySet();
    private final AtomicInteger orderCounter = new AtomicInteger();

    public synchronized void addRequest(@NotNull final IRequest<?> request)
    {
        if (inFlightTokens.contains(request.getId()) || !trackedTokens.add(request.getId()))
        {
            return;
        }

        queue.offer(new QueuedRequest(
          request.getId(),
          UnfulfillableRequestDetector.getRequestPriority(request),
          orderCounter.getAndIncrement()
        ));
    }

    @Nullable
    public synchronized IRequest<?> pollNext(@NotNull final IColony colony)
    {
        while (!queue.isEmpty())
        {
            final QueuedRequest queued = queue.poll();
            trackedTokens.remove(queued.token());

            final IRequest<?> request = colony.getRequestManager().getRequestForToken(queued.token());
            if (request == null)
            {
                BeastofBurdenLog.info("Queue poll skipped: request token no longer exists.");
                continue;
            }

            if (!RequestItemUtils.isStillFulfillable(colony, request))
            {
                BeastofBurdenLog.info("Queue poll skipped: {}", UnfulfillableRequestDetector.explain(colony, request));
                continue;
            }

            return request;
        }

        return null;
    }

    public synchronized void removeRequest(@NotNull final IToken<?> token)
    {
        trackedTokens.remove(token);
        inFlightTokens.remove(token);
        queue.removeIf(entry -> entry.token().equals(token));
    }

    public synchronized void markInFlight(@NotNull final IToken<?> token)
    {
        trackedTokens.remove(token);
        inFlightTokens.add(token);
    }

    public synchronized void clearInFlight(@NotNull final IToken<?> token)
    {
        inFlightTokens.remove(token);
    }

    public synchronized boolean isEmpty()
    {
        return queue.isEmpty();
    }

    public synchronized boolean hasInFlight()
    {
        return !inFlightTokens.isEmpty();
    }

    public synchronized int size()
    {
        return queue.size();
    }

    public synchronized void clear()
    {
        queue.clear();
        trackedTokens.clear();
        inFlightTokens.clear();
    }

    /**
     * Drop entries that were resolved, cancelled, or are no longer stuck.
     */
    public synchronized void purgeInvalid(@NotNull final IColony colony)
    {
        final IRequestManager manager = colony.getRequestManager();
        queue.removeIf(entry -> {
            final IRequest<?> request = manager.getRequestForToken(entry.token());
            if (request == null)
            {
                trackedTokens.remove(entry.token());
                return true;
            }

            final RequestState state = request.getState();
            if (state == RequestState.COMPLETED
                  || state == RequestState.CANCELLED
                  || state == RequestState.FAILED
                  || state == RequestState.OVERRULED
                  || state == RequestState.RECEIVED)
            {
                trackedTokens.remove(entry.token());
                return true;
            }

            if (!UnfulfillableRequestDetector.isUnfulfillable(colony, request))
            {
                trackedTokens.remove(entry.token());
                return true;
            }

            return false;
        });

        inFlightTokens.removeIf(token -> {
            final IRequest<?> request = manager.getRequestForToken(token);
            if (request == null)
            {
                return true;
            }

            final RequestState state = request.getState();
            if (state == RequestState.COMPLETED
                  || state == RequestState.CANCELLED
                  || state == RequestState.FAILED
                  || state == RequestState.OVERRULED
                  || state == RequestState.RECEIVED)
            {
                return true;
            }

            return !UnfulfillableRequestDetector.isUnfulfillable(colony, request);
        });
    }

    private record QueuedRequest(@NotNull IToken<?> token, int priority, int order)
    {
    }
}
