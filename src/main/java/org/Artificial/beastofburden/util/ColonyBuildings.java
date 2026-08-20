package org.Artificial.beastofburden.util;



import com.minecolonies.api.colony.ICitizenData;

import com.minecolonies.api.colony.IColony;

import com.minecolonies.api.colony.IColonyManager;

import com.minecolonies.api.colony.buildings.IBuilding;

import com.minecolonies.api.colony.buildings.workerbuildings.IBuildingDeliveryman;

import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;

import com.minecolonies.api.colony.workorders.IWorkOrder;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;



import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.Collection;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;



/**

 * Enumerates colony buildings across MineColonies API versions.

 */

public final class ColonyBuildings

{

    private ColonyBuildings()

    {

    }



    @NotNull

    public static List<IWareHouse> getWarehouses(@NotNull final IColony colony)

    {

        final List<IWareHouse> fromManager = getWarehousesFromManager(colony);

        if (!fromManager.isEmpty())

        {

            return fromManager;

        }



        final List<IWareHouse> warehouses = new ArrayList<>();

        for (final IBuilding building : getAllBuildings(colony))

        {

            if (building instanceof IWareHouse warehouse)

            {

                warehouses.add(warehouse);

            }

        }

        return warehouses;

    }



    @NotNull

    public static Collection<IBuilding> getAllBuildings(@NotNull final IColony colony)

    {

        final Set<IBuilding> buildings = new HashSet<>();



        collectFromBuildingManager(colony, buildings);

        collectFromCitizens(colony, buildings);

        collectFromWorkOrders(colony, buildings);

        addBuilding(buildings, getTownHall(colony));



        return buildings;

    }



    @Nullable

    public static IBuilding getTownHall(@NotNull final IColony colony)

    {

        try

        {

            final Object manager = getBuildingManager(colony);

            if (manager == null)

            {

                return null;

            }



            final Object hall = invoke(manager, "getTownHall");

            return hall instanceof IBuilding building ? building : null;

        }

        catch (final Exception ex)

        {

            return null;

        }

    }



    public static boolean hasActiveDeliveryman(@NotNull final IColony colony)

    {

        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())

        {

            final IBuilding workBuilding = citizen.getWorkBuilding();

            if (workBuilding instanceof IBuildingDeliveryman)

            {

                return true;

            }

        }



        for (final IBuilding building : getAllBuildings(colony))

        {

            if (building instanceof IBuildingDeliveryman && !building.getAllAssignedCitizen().isEmpty())

            {

                return true;

            }

        }



        return false;

    }



    @Nullable

    public static Object getBuildingManager(@NotNull final IColony colony)

    {
        return colony.getBuildingManager();
    }



    private static void collectFromBuildingManager(@NotNull final IColony colony, @NotNull final Set<IBuilding> buildings)

    {

        try

        {

            final Object manager = getBuildingManager(colony);

            if (manager == null)

            {

                return;

            }



            final Object map = invoke(manager, "getBuildings");

            if (map instanceof Map<?, ?> buildingMap)

            {

                for (final Object value : buildingMap.values())

                {

                    if (value instanceof IBuilding building)

                    {

                        buildings.add(building);

                    }

                }

            }



            final Object warehouses = invoke(manager, "getWareHouses");

            if (warehouses instanceof List<?> list)

            {

                for (final Object entry : list)

                {

                    if (entry instanceof IBuilding building)

                    {

                        buildings.add(building);

                    }

                }

            }

        }

        catch (final Exception ex)

        {

            BeastofBurdenLog.warn("Failed to enumerate buildings for colony {}: {}", colony.getID(), ex.toString());

        }

    }



    private static void collectFromCitizens(@NotNull final IColony colony, @NotNull final Set<IBuilding> buildings)

    {

        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())

        {

            addBuilding(buildings, citizen.getWorkBuilding());

            addBuilding(buildings, citizen.getHomeBuilding());

        }

    }



    private static void collectFromWorkOrders(@NotNull final IColony colony, @NotNull final Set<IBuilding> buildings)

    {

        final var world = colony.getWorld();

        if (world == null)

        {

            return;

        }



        try

        {

            for (final IWorkOrder order : colony.getWorkManager().getWorkOrders().values())

            {

                if (order == null)

                {

                    continue;

                }



                final BlockPos location = order.getLocation();

                if (location == null)

                {

                    continue;

                }



                addBuilding(buildings, IColonyManager.getInstance().getBuilding(world, location));

            }

        }

        catch (final Exception ignored)

        {

        }

    }



    @NotNull

    private static List<IWareHouse> getWarehousesFromManager(@NotNull final IColony colony)

    {

        final List<IWareHouse> warehouses = new ArrayList<>();

        try

        {

            final Object manager = getBuildingManager(colony);

            if (manager == null)

            {

                return warehouses;

            }



            final Object result = invoke(manager, "getWareHouses");

            if (result instanceof List<?> list)

            {

                for (final Object entry : list)

                {

                    if (entry instanceof IWareHouse warehouse)

                    {

                        warehouses.add(warehouse);

                    }

                }

            }

        }

        catch (final Exception ignored)

        {

        }

        return warehouses;

    }



    private static void addBuilding(@NotNull final Set<IBuilding> buildings, @Nullable final IBuilding building)

    {

        if (building != null)

        {

            buildings.add(building);

        }

    }



    @Nullable

    private static Object invoke(@NotNull final Object target, @NotNull final String methodName)

    {

        try

        {

            final Method method = target.getClass().getMethod(methodName);

            return method.invoke(target);

        }

        catch (final ReflectiveOperationException ex)

        {

            return null;

        }

    }



    /**
     * @return a hut only when the colony building manager already lists it at {@code position}.
     */
    @Nullable

    public static IBuilding findTrackedBuildingAt(@NotNull final IColony colony, @NotNull final BlockPos position)

    {

        for (final IBuilding building : getAllBuildings(colony))

        {

            final BlockPos buildingPos = building.getPosition();

            if (buildingPos != null && buildingPos.equals(position))

            {

                return building;

            }

        }

        return null;

    }



    /**
     * Buildings registered on the colony manager but missing from {@link #getAllBuildings}.
     */
    @NotNull

    public static List<IBuilding> findManagerBuildingsMissingFromAggregate(@NotNull final IColony colony)

    {

        final Set<IBuilding> tracked = new HashSet<>(getAllBuildings(colony));
        return findManagerBuildingsMissingFromAggregate(colony, tracked);

    }



    /**
     * Buildings registered on the colony manager but missing from a caller-supplied aggregate.
     */
    @NotNull

    public static List<IBuilding> findManagerBuildingsMissingFromAggregate(
      @NotNull final IColony colony,
      @NotNull final Collection<IBuilding> trackedBuildings)

    {

        final Set<IBuilding> tracked = new HashSet<>(trackedBuildings);

        final List<IBuilding> missing = new ArrayList<>();

        try

        {

            final Object manager = getBuildingManager(colony);

            if (manager == null)

            {

                return missing;

            }



            final Object map = invoke(manager, "getBuildings");

            if (map instanceof Map<?, ?> buildingMap)

            {

                for (final Object value : buildingMap.values())

                {

                    if (value instanceof IBuilding building && !tracked.contains(building))

                    {

                        missing.add(building);

                    }

                }

            }

        }

        catch (final Exception ignored)

        {

        }

        return missing;

    }

}
