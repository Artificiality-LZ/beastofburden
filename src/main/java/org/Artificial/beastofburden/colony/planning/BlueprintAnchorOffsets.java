package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintTagUtils;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves hut-block anchor height from blueprint ground level metadata.
 * <p>
 * Structurize stores {@code groundlevel} tags relative to the hut anchor block, not as absolute
 * blueprint Y coordinates. Use {@link BlueprintTagUtils#getGroundAnchorOffset} so placement matches
 * the build tool.
 */
public final class BlueprintAnchorOffsets
{
    /**
     * Sink placements by this many blocks so schematic floor slabs and entry paths sit flush with terrain.
     */
    private static final int TERRAIN_SINK_BLOCKS = 1;

    private BlueprintAnchorOffsets()
    {
    }

    /**
     * How many blocks the hut block sits above the placement ground column.
     */
    public static int hutVerticalOffset(@NotNull final Blueprint blueprint)
    {
        return BlueprintTagUtils.getGroundAnchorOffset(blueprint, 0);
    }

    /**
     * Blueprint Y coordinate of the building ground floor.
     */
    public static int resolveGroundLevelY(@NotNull final Blueprint blueprint)
    {
        return blueprint.getPrimaryBlockOffset().getY() - hutVerticalOffset(blueprint);
    }

    @NotNull
    public static BlockPos anchorFromGroundColumn(@NotNull final BlockPos groundColumn, @NotNull final Blueprint blueprint)
    {
        return groundColumn.atY(groundColumn.getY() + hutVerticalOffset(blueprint) - TERRAIN_SINK_BLOCKS);
    }

    @NotNull
    public static BlockPos groundColumnFromAnchor(@NotNull final BlockPos anchor, @NotNull final Blueprint blueprint)
    {
        return anchor.atY(anchor.getY() - hutVerticalOffset(blueprint) + TERRAIN_SINK_BLOCKS);
    }
}
