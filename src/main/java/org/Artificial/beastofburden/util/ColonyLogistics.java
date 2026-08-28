package org.Artificial.beastofburden.util;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Colony logistics helpers for early-game request detection.
 */
public final class ColonyLogistics
{
    /**
     * MineColonies uses {@code -1} for open requests created by the building itself (not a citizen).
     */
    public static final int BUILDING_REQUEST_CITIZEN_ID = -1;

    private ColonyLogistics()
    {
    }

    public static boolean hasWarehouse(@NotNull final IColony colony)
    {
        return !ColonyBuildings.getWarehouses(colony).isEmpty();
    }

    public static boolean hasActiveDeliveryman(@NotNull final IColony colony)
    {
        return ColonyBuildings.hasActiveDeliveryman(colony);
    }

    public static boolean isEarlyLogistics(@NotNull final IColony colony)
    {
        return !hasWarehouse(colony) || !hasActiveDeliveryman(colony);
    }

    public static void collectOpenRequests(
      @NotNull final IColony colony,
      @NotNull final Set<IToken<?>> seen,
      @NotNull final List<IRequest<?>> result)
    {
        try
        {
            for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
            {
                forEachOpenRequestBucket(colony, building, requests ->
                {
                    for (final IRequest<?> request : requests)
                    {
                        if (request != null && seen.add(request.getId()))
                        {
                            result.add(request);
                        }
                    }
                });
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to collect open requests for colony {}: {}", colony.getID(), ex.toString());
        }
    }

    public static boolean isOpenOnAnyBuilding(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        return findRequestTargetForDelivery(colony, request).isPresent();
    }

    /**
     * Resolve the building (and optional citizen) that owns an open request.
     */
    @NotNull
    public static Optional<RequestTarget> findRequestTarget(@NotNull final IColony colony, @NotNull final IToken<?> requestId)
    {
        try
        {
            for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
            {
                final Optional<ICitizenData> citizen = building.getCitizenForRequest(requestId);
                if (citizen.isPresent())
                {
                    return Optional.of(new RequestTarget(building, citizen));
                }

                if (buildingHasOpenRequest(building, colony, requestId))
                {
                    return Optional.of(new RequestTarget(building, findFallbackCitizen(building)));
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to resolve request target for colony {}: {}", colony.getID(), ex.toString());
        }

        return Optional.empty();
    }

    /**
     * Resolve delivery target for a request, including child ingredient requests and resolver-owned requests.
     */
    @NotNull
    public static Optional<RequestTarget> findRequestTargetForDelivery(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        final Optional<RequestTarget> direct = findRequestTarget(colony, request.getId());
        if (direct.isPresent())
        {
            return direct;
        }

        try
        {
            final Optional<IBuilding> requesterBuilding = findBuildingFromRequester(colony, request);
            if (requesterBuilding.isPresent())
            {
                final IBuilding building = requesterBuilding.get();
                final Optional<ICitizenData> citizen = building.getCitizenForRequest(request.getId())
                  .or(() -> findFallbackCitizen(building));
                return Optional.of(new RequestTarget(building, citizen));
            }

            final Optional<RequestTarget> fromParent = findRequestTargetFromParentChain(colony, request);
            if (fromParent.isPresent())
            {
                return fromParent;
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn(
              "Failed to resolve delivery target for request {} in colony {}: {}",
              request.getId(),
              colony.getID(),
              ex.toString()
            );
        }

        return Optional.empty();
    }

    /**
     * Deliver generated items to the requester, then complete the request in the colony RS.
     *
     * @return {@code true} when the request was overruled successfully.
     */
    public static boolean fulfillRequest(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request,
      @NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return false;
        }

        final ItemStack delivery = stack.copy();
        if (request.getRequest() instanceof Tool tool && !tool.matches(delivery))
        {
            BeastofBurdenLog.warn("Delivery stack {} does not match tool request.", delivery.getHoverName().getString());
            return false;
        }

        final Optional<RequestTarget> target = findRequestTargetForDelivery(colony, request);
        if (target.isEmpty())
        {
            BeastofBurdenLog.debug("No delivery target for request {}; attempting direct overrule.", request.getId());
            return overruleRequestDirect(colony, request, delivery);
        }

        final IBuilding building = target.get().building();
        final Optional<ICitizenData> requester = target.get().citizen();
        final boolean citizenOpenRequest = requester.isPresent()
          && isCitizenOpenRequest(building, requester.get().getId(), request.getId());

        if (requester.isPresent() && requester.get().getEntity().isPresent())
        {
            giveStackToCitizen(requester.get(), delivery);

            if (building.overruleNextOpenRequestOfCitizenWithStack(requester.get(), delivery))
            {
                BeastofBurdenLog.info(
                  "Fulfilled citizen request {} via building {} with {}.",
                  requester.get().getId(),
                  building.getBuildingType(),
                  delivery.getHoverName().getString()
                );
                return true;
            }

            if (overruleRequestDirect(colony, request, delivery))
            {
                BeastofBurdenLog.info(
                  "Fulfilled request {} via direct overrule to citizen {} with {}.",
                  request.getId(),
                  requester.get().getId(),
                  delivery.getHoverName().getString()
                );
                return true;
            }
        }

        if (buildingHasOpenRequest(building, colony, BUILDING_REQUEST_CITIZEN_ID, request.getId()))
        {
            if (giveStackToBuilding(colony, building, delivery))
            {
                building.overruleNextOpenRequestWithStack(delivery);
                BeastofBurdenLog.info(
                  "Fulfilled building request at {} with {}.",
                  building.getID(),
                  delivery.getHoverName().getString()
                );
                return true;
            }
        }
        else if (!citizenOpenRequest && requester.isEmpty())
        {
            giveStackToBuilding(colony, building, delivery);
        }

        if (overruleRequestDirect(colony, request, delivery))
        {
            BeastofBurdenLog.info(
              "Fulfilled request {} via overrule with {} (building={}, citizen={}).",
              request.getId(),
              delivery.getHoverName().getString(),
              building.getID(),
              requester.map(ICitizenData::getId).orElse(null)
            );
            return true;
        }

        BeastofBurdenLog.warn(
          "Failed to fulfill request {} (building={}, citizen={}, citizenOpen={}).",
          request.getId(),
          building.getID(),
          requester.map(ICitizenData::getId).orElse(null),
          citizenOpenRequest
        );
        return false;
    }

    /**
     * Force-place items at the delivery location and overrule the request after a delivery timeout.
     *
     * @return {@code true} when the request was overruled.
     */
    public static boolean forceFulfillRequest(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request,
      @NotNull final ItemStack stack,
      @Nullable final BlockPos deliveryPos)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return false;
        }

        final ItemStack delivery = stack.copy();
        boolean placed = false;

        final Optional<RequestTarget> target = findRequestTargetForDelivery(colony, request);
        if (target.isPresent())
        {
            final IBuilding building = target.get().building();
            final Optional<ICitizenData> requester = target.get().citizen();

            if (requester.isPresent() && requester.get().getEntity().isPresent())
            {
                giveStackToCitizen(requester.get(), delivery);
                placed = true;
            }
            else if (giveStackToBuilding(colony, building, delivery))
            {
                placed = true;
            }
        }

        if (!placed && colony.getWorld() != null && deliveryPos != null && !deliveryPos.equals(BlockPos.ZERO))
        {
            Block.popResource(colony.getWorld(), deliveryPos, delivery.copy());
            placed = true;
        }

        if (!placed)
        {
            BeastofBurdenLog.warn(
              "Force delivery could not place {} for request {}; overrule only.",
              delivery.getHoverName().getString(),
              request.getId()
            );
        }

        return overruleRequestDirect(colony, request, delivery);
    }

    @NotNull
    private static Optional<IBuilding> findBuildingFromRequester(@NotNull final IColony colony, @NotNull final IRequest<?> request)
    {
        final BlockPos pos = request.getRequester().getLocation().getInDimensionLocation();
        if (pos == null)
        {
            return Optional.empty();
        }

        final IRegisteredStructureManager manager = MineColoniesCompat.getStructureManager(colony);
        if (manager == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(manager.getBuilding(pos));
    }

    @NotNull
    private static Optional<RequestTarget> findRequestTargetFromParentChain(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request)
    {
        final IRequestManager manager = colony.getRequestManager();
        IToken<?> parentToken = request.getParent();

        while (parentToken != null)
        {
            final IRequest<?> parent = manager.getRequestForToken(parentToken);
            if (parent == null)
            {
                break;
            }

            final Optional<RequestTarget> parentTarget = findRequestTargetForDelivery(colony, parent);
            if (parentTarget.isPresent())
            {
                return parentTarget;
            }

            parentToken = parent.getParent();
        }

        return Optional.empty();
    }

    private static boolean overruleRequestDirect(
      @NotNull final IColony colony,
      @NotNull final IRequest<?> request,
      @NotNull final ItemStack delivery)
    {
        try
        {
            request.overrideCurrentDeliveries(ImmutableList.of(delivery.copy()));
            colony.getRequestManager().overruleRequest(request.getId(), delivery.copy());
            return true;
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Direct overrule failed for request {}: {}", request.getId(), ex.toString());
            return false;
        }
    }

    private static void forEachOpenRequestBucket(
      @NotNull final IColony colony,
      @NotNull final IBuilding building,
      @NotNull final Consumer<List<IRequest<?>>> consumer)
    {
        consumer.accept(building.getOpenRequests(BUILDING_REQUEST_CITIZEN_ID).stream().toList());
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            consumer.accept(building.getOpenRequests(citizen.getId()).stream().toList());
        }
    }

    private static boolean buildingHasOpenRequest(
      @NotNull final IBuilding building,
      @NotNull final IColony colony,
      @NotNull final IToken<?> requestId)
    {
        if (buildingHasOpenRequest(building, colony, BUILDING_REQUEST_CITIZEN_ID, requestId))
        {
            return true;
        }

        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (buildingHasOpenRequest(building, colony, citizen.getId(), requestId))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean buildingHasOpenRequest(
      @NotNull final IBuilding building,
      @NotNull final IColony colony,
      final int citizenId,
      @NotNull final IToken<?> requestId)
    {
        for (final IRequest<?> open : building.getOpenRequests(citizenId))
        {
            if (open != null && open.getId().equals(requestId))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isCitizenOpenRequest(
      @NotNull final IBuilding building,
      final int citizenId,
      @NotNull final IToken<?> requestId)
    {
        return citizenId != BUILDING_REQUEST_CITIZEN_ID && buildingHasOpenRequest(building, building.getColony(), citizenId, requestId);
    }

    @NotNull
    private static Optional<ICitizenData> findFallbackCitizen(@NotNull final IBuilding building)
    {
        final Set<ICitizenData> assigned = building.getAllAssignedCitizen();
        if (assigned.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.of(assigned.iterator().next());
    }

    private static boolean giveStackToCitizen(@NotNull final ICitizenData citizen, @NotNull final ItemStack stack)
    {
        if (!citizen.getEntity().isPresent())
        {
            return false;
        }

        final ItemStack remaining = InventoryUtils.addItemStackToItemHandlerWithResult(
          citizen.getEntity().get().getInventoryCitizen(),
          stack.copy()
        );

        if (!ItemStackUtils.isEmpty(remaining))
        {
            citizen.getEntity().get().spawnAtLocation(remaining);
        }

        return true;
    }

    private static boolean giveStackToBuilding(@NotNull final IColony colony, @NotNull final IBuilding building, @NotNull final ItemStack stack)
    {
        if (colony.getWorld() == null)
        {
            return false;
        }

        final BlockEntity blockEntity = colony.getWorld().getBlockEntity(building.getID());
        if (blockEntity == null)
        {
            return false;
        }

        final ItemStack remaining = InventoryUtils.addItemStackToProviderWithResult(blockEntity, stack.copy());
        if (!ItemStackUtils.isEmpty(remaining))
        {
            BeastofBurdenLog.warn(
              "Building {} inventory full; {} items left over.",
              building.getID(),
              remaining.getCount()
            );
            return false;
        }

        return true;
    }

    /**
     * Building that owns an open request, plus an optional citizen when the request is citizen-scoped.
     */
    public record RequestTarget(@NotNull IBuilding building, @NotNull Optional<ICitizenData> citizen)
    {
    }
}
