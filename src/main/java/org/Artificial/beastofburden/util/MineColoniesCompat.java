package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Runtime bridges for MineColonies API renames across 1.1.873 and newer builds.
 * <p>
 * Compile target remains 1.1.873; callers must not bytecode-link renamed methods
 * (e.g. {@code IColony.getBuildingManager()} or {@code IRegisteredStructureManager.getTownHall()}
 * with an {@code ITownHall} return) or the JVM throws {@link NoSuchMethodError} on 1.1.1214+.
 */
public final class MineColoniesCompat
{
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    @Nullable
    private static final MethodHandle STRUCTURE_MANAGER_GETTER = resolveStructureManagerGetter();

    @Nullable
    private static final MethodHandle TOWN_HALL_GETTER = resolveTownHallGetter();

    private MineColoniesCompat()
    {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves the colony structure/building manager.
     * Prefers {@code getServerBuildingManager()} (1214+), then {@code getBuildingManager()} (873).
     */
    @Nullable
    public static IRegisteredStructureManager getStructureManager(@NotNull final IColony colony)
    {
        if (STRUCTURE_MANAGER_GETTER == null)
        {
            return null;
        }

        try
        {
            final Object manager = STRUCTURE_MANAGER_GETTER.invoke(colony);
            return manager instanceof IRegisteredStructureManager registered ? registered : null;
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn("Failed to resolve colony structure manager for colony {}: {}", colony.getID(), ex.toString());
            return null;
        }
    }

    /**
     * Resolves the town hall from a structure manager.
     * <p>
     * On 873 the interface method returns {@link ITownHall}; on 1214+ it lives on a parent
     * interface as {@code T getTownHall()} and erases to {@code Object}. Direct invoke of the
     * 873 descriptor crashes with {@link NoSuchMethodError}.
     */
    @Nullable
    public static IBuilding getTownHall(@Nullable final IRegisteredStructureManager manager)
    {
        if (manager == null || TOWN_HALL_GETTER == null)
        {
            return null;
        }

        try
        {
            final Object townHall = TOWN_HALL_GETTER.invoke(manager);
            return townHall instanceof IBuilding building ? building : null;
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn("Failed to resolve town hall from structure manager: {}", ex.toString());
            return null;
        }
    }

    @Nullable
    private static MethodHandle resolveStructureManagerGetter()
    {
        final MethodType type = MethodType.methodType(IRegisteredStructureManager.class);
        final MethodHandle newer = findVirtual(IColony.class, "getServerBuildingManager", type);
        if (newer != null)
        {
            return newer;
        }

        final MethodHandle legacy = findVirtual(IColony.class, "getBuildingManager", type);
        if (legacy == null)
        {
            BeastofBurdenLog.warn(
              "Neither IColony.getServerBuildingManager nor IColony.getBuildingManager is present; building enumeration disabled."
            );
        }
        return legacy;
    }

    @Nullable
    private static MethodHandle resolveTownHallGetter()
    {
        final MethodHandle asTownHall = findVirtual(
          IRegisteredStructureManager.class,
          "getTownHall",
          MethodType.methodType(ITownHall.class)
        );
        if (asTownHall != null)
        {
            return asTownHall;
        }

        final MethodHandle asObject = findVirtual(
          IRegisteredStructureManager.class,
          "getTownHall",
          MethodType.methodType(Object.class)
        );
        if (asObject != null)
        {
            return asObject;
        }

        // 1214+: method may only be declared on ICommonRegisteredStructureManager with Object erasure.
        final MethodHandle fromHierarchy = findTownHallOnHierarchy(IRegisteredStructureManager.class);
        if (fromHierarchy == null)
        {
            BeastofBurdenLog.warn("IRegisteredStructureManager.getTownHall is not resolvable; town-hall lookup disabled.");
        }
        return fromHierarchy;
    }

    @Nullable
    private static MethodHandle findTownHallOnHierarchy(@NotNull final Class<?> type)
    {
        for (final Method method : type.getMethods())
        {
            if (!"getTownHall".equals(method.getName()) || method.getParameterCount() != 0)
            {
                continue;
            }

            try
            {
                return LOOKUP.unreflect(method);
            }
            catch (final IllegalAccessException ignored)
            {
                // try next
            }
        }
        return null;
    }

    @Nullable
    private static MethodHandle findVirtual(
      @NotNull final Class<?> owner,
      @NotNull final String name,
      @NotNull final MethodType type)
    {
        try
        {
            return LOOKUP.findVirtual(owner, name, type);
        }
        catch (final NoSuchMethodException | IllegalAccessException ignored)
        {
            return null;
        }
    }
}
