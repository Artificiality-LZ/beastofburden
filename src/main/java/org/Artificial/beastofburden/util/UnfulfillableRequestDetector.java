package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.constant.TypeConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects colony requests that the colony cannot fulfill on its own.
 * <p>
 * Uses MineColonies stuck-request semantics (player / retrying resolver), and also treats
 * open citizen deliverable requests as candidates — important in early game when there is
 * no warehouse or deliveryman yet.
 */
public final class UnfulfillableRequestDetector
{
    private UnfulfillableRequestDetector()
    {
    }

    /**
     * Collects candidate requests using only the public MineColonies API.
     */
    @NotNull
    public static Collection<IRequest<?>> getAllRequests(@NotNull final IColony colony)
    {
        final IRequestManager manager = colony.getRequestManager();
        final Set<IToken<?>> seen = new HashSet<>();
        final List<IRequest<?>> result = new ArrayList<>();

        collectFromTokens(manager, manager.getPlayerResolver().getAllAssignedRequests(), seen, result);
        collectFromTokens(manager, manager.getRetryingRequestResolver().getAllAssignedRequests(), seen, result);

        final List<IRequest<?>> snapshot = new ArrayList<>(result);
        for (final IRequest<?> request : snapshot)
        {
            collectChildren(manager, request, seen, result);
        }

        ColonyLogistics.collectOpenRequests(colony, seen, result);

        return result;
    }

    /**
     * @return {@code true} when the request is an active item request that the beast of burden should handle.
     */
    public static boolean isUnfulfillable(@NotNull final IColony colony, @Nullable final IRequest<?> request)
    {
        if (request == null || colony.getWorld().isClientSide)
        {
            return false;
        }

        if (!isActiveItemRequest(request))
        {
            return false;
        }

        final IRequestManager manager = colony.getRequestManager();
        final List<IToken<?>> playerRequests = manager.getPlayerResolver().getAllAssignedRequests();
        final List<IToken<?>> retryingRequests = manager.getRetryingRequestResolver().getAllAssignedRequests();

        if (isRequestStuck(request, playerRequests, retryingRequests, manager))
        {
            return true;
        }

        if (isResolvedByStuckResolver(manager, request))
        {
            return true;
        }

        if (!ColonyLogistics.isOpenOnAnyBuilding(colony, request))
        {
            return false;
        }

        return ColonyLogistics.isEarlyLogistics(colony);
    }

    /**
     * Human-readable explanation for debug logging.
     */
    @NotNull
    public static String explain(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        final IRequestManager manager = colony.getRequestManager();
        final List<IToken<?>> playerRequests = manager.getPlayerResolver().getAllAssignedRequests();
        final List<IToken<?>> retryingRequests = manager.getRetryingRequestResolver().getAllAssignedRequests();

        final StringBuilder sb = new StringBuilder();
        sb.append("state=").append(request.getState());
        sb.append(" type=").append(request.getType());
        sb.append(" requestable=").append(request.getRequest() == null ? "null" : request.getRequest().getClass().getSimpleName());
        sb.append(" displayStacks=").append(request.getDisplayStacks().size());
        sb.append(" deliverable=").append(isDeliverableRequest(request));
        sb.append(" active=").append(isActiveItemRequest(request));
        sb.append(" stuck=").append(isRequestStuck(request, playerRequests, retryingRequests, manager));
        sb.append(" stuckResolver=").append(isResolvedByStuckResolver(manager, request));
        sb.append(" openBuilding=").append(ColonyLogistics.isOpenOnAnyBuilding(colony, request));
        sb.append(" earlyLogistics=").append(ColonyLogistics.isEarlyLogistics(colony));
        sb.append(" warehouse=").append(ColonyLogistics.hasWarehouse(colony));
        sb.append(" deliveryman=").append(ColonyLogistics.hasActiveDeliveryman(colony));
        sb.append(" inPlayer=").append(playerRequests.contains(request.getId()));
        sb.append(" inRetrying=").append(retryingRequests.contains(request.getId()));
        sb.append(" unfulfillable=").append(isUnfulfillable(colony, request));
        return sb.toString();
    }

    /**
     * Priority for queue ordering. Higher values are handled first (matches deliveryman semantics).
     */
    public static int getRequestPriority(@NotNull final IRequest<?> request)
    {
        final Object requestable = request.getRequest();
        if (requestable instanceof IDeliverymanRequestable deliverymanRequestable)
        {
            return deliverymanRequestable.getPriority();
        }

        if (requestable instanceof IDeliverable)
        {
            return 0;
        }

        return Integer.MIN_VALUE;
    }

    public static boolean isOpenOnAnyCitizen(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        return ColonyLogistics.isOpenOnAnyBuilding(colony, request);
    }

    private static void collectFromTokens(
      @NotNull final IRequestManager manager,
      @NotNull final Collection<IToken<?>> tokens,
      @NotNull final Set<IToken<?>> seen,
      @NotNull final List<IRequest<?>> result)
    {
        for (final IToken<?> token : tokens)
        {
            if (!seen.add(token))
            {
                continue;
            }

            final IRequest<?> request = manager.getRequestForToken(token);
            if (request != null)
            {
                result.add(request);
            }
        }
    }

    private static void collectChildren(
      @NotNull final IRequestManager manager,
      @NotNull final IRequest<?> request,
      @NotNull final Set<IToken<?>> seen,
      @NotNull final List<IRequest<?>> result)
    {
        for (final IToken<?> childToken : request.getChildren())
        {
            if (!seen.add(childToken))
            {
                continue;
            }

            final IRequest<?> childRequest = manager.getRequestForToken(childToken);
            if (childRequest != null)
            {
                result.add(childRequest);
                collectChildren(manager, childRequest, seen, result);
            }
        }
    }

    private static boolean isDeliverableRequest(@NotNull final IRequest<?> request)
    {
        return request.getType().isSubtypeOf(TypeConstants.DELIVERABLE)
                 || request.getType().isSubtypeOf(TypeConstants.TOOL)
                 || request.getRequest() instanceof IDeliverable;
    }

    private static boolean isActiveItemRequest(@NotNull final IRequest<?> request)
    {
        if (!isDeliverableRequest(request))
        {
            return false;
        }

        final RequestState state = request.getState();
        return state != RequestState.COMPLETED
                 && state != RequestState.CANCELLED
                 && state != RequestState.FAILED
                 && state != RequestState.OVERRULED
                 && state != RequestState.RECEIVED
                 && state != RequestState.FINALIZING;
    }

    private static boolean isResolvedByStuckResolver(
      @NotNull final IRequestManager manager,
      @NotNull final IRequest<?> request)
    {
        final IRequestResolver<?> resolver;
        try
        {
            resolver = manager.getResolverForRequest(request.getId());
        }
        catch (final IllegalArgumentException ex)
        {
            return false;
        }

        if (resolver == null)
        {
            return false;
        }

        return resolver instanceof IPlayerRequestResolver || resolver instanceof IRetryingRequestResolver;
    }

    private static boolean isRequestStuck(
      @NotNull final IRequest<?> target,
      @NotNull final List<IToken<?>> playerRequests,
      @NotNull final List<IToken<?>> retryingRequests,
      @NotNull final IRequestManager manager)
    {
        if (playerRequests.contains(target.getId()) || retryingRequests.contains(target.getId()))
        {
            return true;
        }

        for (final IToken<?> child : target.getChildren())
        {
            final IRequest<?> childRequest = manager.getRequestForToken(child);
            if (childRequest != null && isRequestStuck(childRequest, playerRequests, retryingRequests, manager))
            {
                return true;
            }
        }

        return false;
    }
}
