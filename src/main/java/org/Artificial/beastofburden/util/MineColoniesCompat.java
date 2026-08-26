package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Runtime bridges for MineColonies API renames across 1.1.873 and newer builds.
 * <p>
 * Compile target remains 1.1.873; callers must not bytecode-link renamed methods
 * (e.g. {@code IColony.getBuildingManager()}) or the JVM throws {@link NoSuchMethodError}
 * on 1.1.1214+.
 */
public final class MineColoniesCompat
{
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    @Nullable
    private static final MethodHandle STRUCTURE_MANAGER_GETTER = resolveStructureManagerGetter();

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
