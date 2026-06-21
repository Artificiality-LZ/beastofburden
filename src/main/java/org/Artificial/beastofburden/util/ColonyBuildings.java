package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IBuildingDeliveryman;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

        try
        {
            final Object manager = invoke(colony, "getBuildingManager");
            if (manager != null)
            {
                final Object map = invoke(manager, "getBuildings");
                if (map instanceof java.util.Map<?, ?> buildingMap)
                {
                    for (final Object value : buildingMap.values())
                    {
                        if (value instanceof IBuilding building)
                        {
                            buildings.add(building);
                        }
                    }
                }
            }
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Failed to enumerate buildings via building manager for colony {}: {}", colony.getID(), ex.toString());
        }

        if (buildings.isEmpty())
        {
            for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
            {
                addBuilding(buildings, citizen.getWorkBuilding());
                addBuilding(buildings, citizen.getHomeBuilding());
            }
        }

        return buildings;
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

    private static void addBuilding(@NotNull final Set<IBuilding> buildings, @org.jetbrains.annotations.Nullable final IBuilding building)
    {
        if (building != null)
        {
            buildings.add(building);
        }
    }

    @org.jetbrains.annotations.Nullable
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
}
