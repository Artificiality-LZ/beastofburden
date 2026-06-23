package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Places MineColonies construction tape via the core helper (runtime dependency).
 */
public final class ConstructionTapeSupport
{
    private static final String HELPER_CLASS = "com.minecolonies.core.entity.ai.workers.util.ConstructionTapeHelper";

    private ConstructionTapeSupport()
    {
    }

    public static void place(
      @NotNull final Tuple<BlockPos, BlockPos> corners,
      @NotNull final Level world,
      @Nullable final IColony colony)
    {
        try
        {
            final Class<?> helper = Class.forName(HELPER_CLASS);
            if (colony != null)
            {
                try
                {
                    helper.getMethod("placeConstructionTape", Tuple.class, IColony.class).invoke(null, corners, colony);
                    return;
                }
                catch (final NoSuchMethodException ignored)
                {
                }
            }

            helper.getMethod("placeConstructionTape", Tuple.class, Level.class).invoke(null, corners, world);
        }
        catch (final ReflectiveOperationException ex)
        {
            BeastofBurdenLog.warn("Construction tape helper unavailable: {}", ex.toString());
        }
    }
}
