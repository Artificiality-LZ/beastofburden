package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import com.minecolonies.api.util.constant.Constants;
import org.Artificial.beastofburden.util.MineColoniesCompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves structure pack and blueprint paths for planned buildings.
 */
public final class BlueprintPaths
{
    private BlueprintPaths()
    {
    }

    @NotNull
    public static String defaultPack(@Nullable final String colonyPack)
    {
        if (colonyPack != null && !colonyPack.isEmpty() && StructurePacks.hasPack(colonyPack))
        {
            return colonyPack;
        }
        return Constants.DEFAULT_STYLE;
    }

    @NotNull
    public static String pathFor(@NotNull final PlannedBuildingType type, final int level)
    {
        final int clamped = Math.max(1, level);
        return BlueprintMapping.getPathMapping("", type.getBlueprintName()) + clamped + ".blueprint";
    }

    @Nullable
    public static Blueprint loadBlueprint(@NotNull final String pack, @NotNull final PlannedBuildingType type, final int level)
    {
        if (!StructurePacks.hasPack(pack))
        {
            return null;
        }

        try
        {
            return StructurePacks.getBlueprint(pack, pathFor(type, level));
        }
        catch (final Exception ex)
        {
            return null;
        }
    }

    public static boolean isHutType(@NotNull final PlannedBuildingType type)
    {
        return MineColoniesCompat.getBuildingBlock(type.getEntry()) instanceof AbstractBlockHut<?>;
    }
}
