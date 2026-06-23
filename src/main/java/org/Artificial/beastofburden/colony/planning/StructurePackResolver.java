package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import org.Artificial.beastofburden.util.ColonyBuildings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the structure style pack for autonomous planning.
 * <p>
 * Prefers the colony default pack (set by the Town Hall), then the Town Hall building itself.
 */
public final class StructurePackResolver
{
    private StructurePackResolver()
    {
    }

    /**
     * @return the style pack to use for new planned buildings, or {@code null} to fall back to MineColonies default.
     */
    @Nullable
    public static String resolveColonyPack(@NotNull final IColony colony)
    {
        final String colonyPack = colony.getStructurePack();
        if (isUsablePack(colonyPack))
        {
            return colonyPack;
        }

        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (!isTownHall(building))
            {
                continue;
            }

            final String townHallPack = building.getStructurePack();
            if (isUsablePack(townHallPack))
            {
                return townHallPack;
            }
        }

        return findPackFromAnyBuilding(colony);
    }

    /**
     * @return resolved pack name, never empty (falls back to {@link BlueprintPaths#defaultPack}).
     */
    @NotNull
    public static String resolveColonyPackOrDefault(@NotNull final IColony colony)
    {
        return BlueprintPaths.defaultPack(resolveColonyPack(colony));
    }

    @Nullable
    private static String findPackFromAnyBuilding(@NotNull final IColony colony)
    {
        for (final IBuilding building : ColonyBuildings.getAllBuildings(colony))
        {
            if (building.getBuildingLevel() <= 0)
            {
                continue;
            }

            final String pack = building.getStructurePack();
            if (isUsablePack(pack))
            {
                return pack;
            }
        }

        return null;
    }

    private static boolean isTownHall(@NotNull final IBuilding building)
    {
        return ModBuildings.townHall.get().getRegistryName().equals(building.getBuildingType().getRegistryName());
    }

    private static boolean isUsablePack(@Nullable final String pack)
    {
        return pack != null && !pack.isEmpty() && StructurePacks.hasPack(pack);
    }
}
