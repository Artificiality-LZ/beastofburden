package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.entity.ai.workers.util.ConstructionTapeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Places MineColonies construction tape via the core helper.
 */
public final class ConstructionTapeSupport
{
    private ConstructionTapeSupport()
    {
        throw new IllegalStateException("Utility class");
    }

    public static void place(
      @NotNull final Tuple<BlockPos, BlockPos> corners,
      @NotNull final Level world,
      @Nullable final IColony colony)
    {
        if (colony == null)
        {
            BeastofBurdenLog.warn("Construction tape skipped: colony is null (world={}).", world.dimension().location());
            return;
        }

        try
        {
            ConstructionTapeHelper.placeConstructionTape(corners, colony);
        }
        catch (final RuntimeException ex)
        {
            BeastofBurdenLog.warn("Construction tape placement failed: {}", ex.toString());
        }
    }
}
