package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IConcreteDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Helpers for extracting item stacks and delivery locations from MineColonies requests.
 */
public final class RequestItemUtils
{
    private RequestItemUtils()
    {
    }

    /**
     * Build the item stack that fulfills the given deliverable request.
     */
    @NotNull
    public static ItemStack extractItemStack(@NotNull final IRequest<?> request)
    {
        if (!(request.getRequest() instanceof IDeliverable deliverable))
        {
            return ItemStack.EMPTY;
        }

        if (deliverable instanceof Delivery delivery)
        {
            final ItemStack stack = delivery.getStack().copy();
            stack.setCount(Math.max(1, deliverable.getCount()));
            return stack;
        }

        if (deliverable instanceof Tool tool)
        {
            final ItemStack toolStack = extractToolStack(request, tool);
            if (!ItemStackUtils.isEmpty(toolStack))
            {
                return toolStack;
            }
        }

        if (deliverable instanceof IConcreteDeliverable concrete)
        {
            final List<ItemStack> requestedItems = concrete.getRequestedItems();
            if (!requestedItems.isEmpty())
            {
                final ItemStack stack = requestedItems.get(0).copy();
                stack.setCount(Math.max(1, deliverable.getCount()));
                return stack;
            }
        }

        final ItemStack result = deliverable.getResult();
        if (!ItemStackUtils.isEmpty(result))
        {
            final ItemStack stack = result.copy();
            stack.setCount(Math.max(1, deliverable.getCount()));
            return stack;
        }

        if (deliverable instanceof Stack stackDeliverable)
        {
            final ItemStack stack = stackDeliverable.getStack().copy();
            stack.setCount(Math.max(1, deliverable.getCount()));
            return stack;
        }

        final List<ItemStack> displayStacks = request.getDisplayStacks();
        if (!displayStacks.isEmpty())
        {
            final ItemStack stack = displayStacks.get(0).copy();
            stack.setCount(Math.max(1, deliverable.getCount()));
            return stack;
        }

        return ItemStack.EMPTY;
    }

    @NotNull
    private static ItemStack extractToolStack(@NotNull final IRequest<?> request, @NotNull final Tool tool)
    {
        if (!ItemStackUtils.isEmpty(tool.getResult()))
        {
            final ItemStack stack = tool.getResult().copy();
            stack.setCount(1);
            return stack;
        }

        for (final ItemStack candidate : request.getDisplayStacks())
        {
            if (tool.matches(candidate))
            {
                final ItemStack stack = candidate.copy();
                stack.setCount(1);
                return stack;
            }
        }

        for (final ItemStack candidate : IColonyManager.getInstance().getCompatibilityManager().getListOfAllItems())
        {
            if (tool.matches(candidate))
            {
                final ItemStack stack = candidate.copy();
                stack.setCount(1);
                return stack;
            }
        }

        BeastofBurdenLog.warn(
          "Could not resolve tool stack (displayStacks={})",
          request.getDisplayStacks().size()
        );
        return ItemStack.EMPTY;
    }

    /**
     * Resolve where the beast of burden should walk to deliver the generated items.
     */
    @NotNull
    public static BlockPos getDeliveryPosition(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        final Optional<ColonyLogistics.RequestTarget> target = ColonyLogistics.findRequestTargetForDelivery(colony, request);
        if (target.isPresent())
        {
            final Optional<ICitizenData> requester = target.get().citizen();
            if (requester.isPresent() && requester.get().getEntity().isPresent())
            {
                return requester.get().getEntity().get().blockPosition();
            }

            return target.get().building().getPosition();
        }

        final Object requestable = request.getRequest();
        if (requestable instanceof Delivery delivery)
        {
            return toBlockPos(delivery.getTarget(), colony);
        }

        final IRequester requesterLocation = request.getRequester();
        return toBlockPos(requesterLocation.getLocation(), colony);
    }

    @NotNull
    private static BlockPos toBlockPos(@NotNull final ILocation location, @NotNull final IColony colony)
    {
        final BlockPos pos = location.getInDimensionLocation();
        return pos != null ? pos : colony.getCenter();
    }

    /**
     * @return {@code true} when the request is still valid for beast-of-burden fulfillment.
     */
    public static boolean isStillFulfillable(@NotNull final IColony colony, @Nullable final IRequest<?> request)
    {
        return request != null
                 && UnfulfillableRequestDetector.isUnfulfillable(colony, request)
                 && !ItemStackUtils.isEmpty(extractItemStack(request));
    }
}
