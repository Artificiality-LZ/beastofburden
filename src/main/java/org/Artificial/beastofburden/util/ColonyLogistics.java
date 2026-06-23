package org.Artificial.beastofburden.util;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IBuildingDeliveryman;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
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
        if (!ColonyBuildings.getWarehouses(colony).isEmpty())
        {
            return true;
        }

        return anyKnownBuildingMatches(colony, IWareHouse.class::isInstance);
    }

    public static boolean hasActiveDeliveryman(@NotNull final IColony colony)
    {
        return ColonyBuildings.hasActiveDeliveryman(colony);
    }

    public static int countAllOpenRequests(@NotNull final IColony colony)
    {
        final int[] count = {0};

        try
        {
            for (final IBuilding building : collectKnownBuildings(colony))
            {
                forEachOpenRequestBucket(colony, building, requests -> count[0] += requests.size());
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to count open requests for colony {}: {}", colony.getID(), ex.toString());
        }

        return count[0];
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
            for (final IBuilding building : collectKnownBuildings(colony))
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
        return findRequestTarget(colony, request.getId()).isPresent();
    }

    /**
     * Resolve the building (and optional citizen) that owns an open request.
     */
    @NotNull
    public static Optional<RequestTarget> findRequestTarget(@NotNull final IColony colony, @NotNull final IToken<?> requestId)
    {
        try
        {
            for (final IBuilding building : collectKnownBuildings(colony))
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
     * Deliver generated items to the requester, then complete the request in the colony RS.
     *
     * @return {@code true} when the item reached the requester and the request was overruled.
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

        final Optional<RequestTarget> target = findRequestTarget(colony, request.getId());
        if (target.isEmpty())
        {
            BeastofBurdenLog.warn("No request target found for request {}", request.getId());
            return false;
        }

        final IBuilding building = target.get().building();
        final Optional<ICitizenData> requester = target.get().citizen();

        if (requester.isPresent() && isCitizenOpenRequest(building, requester.get().getId(), request.getId()))
        {
            if (!giveStackToCitizen(requester.get(), delivery))
            {
                BeastofBurdenLog.warn(
                  "Failed to insert {} into citizen {} inventory.",
                  delivery.getHoverName().getString(),
                  requester.get().getId()
                );
                return false;
            }

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
        }
        else
        {
            if (!giveStackToBuilding(colony, building, delivery))
            {
                BeastofBurdenLog.warn(
                  "Failed to insert {} into building {} inventory.",
                  delivery.getHoverName().getString(),
                  building.getID()
                );
                return false;
            }

            if (buildingHasOpenRequest(building, colony, BUILDING_REQUEST_CITIZEN_ID, request.getId()))
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

        request.overrideCurrentDeliveries(ImmutableList.of(delivery));
        colony.getRequestManager().overruleRequest(request.getId(), delivery);
        BeastofBurdenLog.info(
          "Fulfilled request {} via overrule with {} (building={}, citizen={}).",
          request.getId(),
          delivery.getHoverName().getString(),
          building.getID(),
          requester.map(ICitizenData::getId).orElse(null)
        );
        return true;
    }

    /**
     * Buildings reachable through citizen work/home assignments (public API only).
     */
    @NotNull
    private static Set<IBuilding> collectKnownBuildings(@NotNull final IColony colony)
    {
        final Set<IBuilding> buildings = new HashSet<>();

        try
        {
            for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
            {
                addBuilding(buildings, citizen.getWorkBuilding());
                addBuilding(buildings, citizen.getHomeBuilding());
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to enumerate buildings for colony {}: {}", colony.getID(), ex.toString());
        }

        return buildings;
    }

    private static boolean anyKnownBuildingMatches(@NotNull final IColony colony, @NotNull final java.util.function.Predicate<IBuilding> predicate)
    {
        try
        {
            for (final IBuilding building : collectKnownBuildings(colony))
            {
                if (predicate.test(building))
                {
                    return true;
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to inspect buildings for colony {}: {}", colony.getID(), ex.toString());
        }

        return false;
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
        }

        return true;
    }

    private static void addBuilding(@NotNull final Set<IBuilding> buildings, @Nullable final IBuilding building)
    {
        if (building != null)
        {
            buildings.add(building);
        }
    }

    /**
     * Building that owns an open request, plus an optional citizen when the request is citizen-scoped.
     */
    public record RequestTarget(@NotNull IBuilding building, @NotNull Optional<ICitizenData> citizen)
    {
    }
}
