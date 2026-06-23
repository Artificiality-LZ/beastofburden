package org.Artificial.beastofburden.colony.planning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Network serialization for {@link FixedPlanScript}.
 */
public final class PlanScriptIO
{
    private PlanScriptIO()
    {
    }

    public static void write(@NotNull final FriendlyByteBuf buf, @NotNull final FixedPlanScript script)
    {
        buf.writeNbt(script.writeToNbt());
    }

    @NotNull
    public static FixedPlanScript read(@NotNull final FriendlyByteBuf buf)
    {
        final CompoundTag tag = buf.readNbt();
        return tag == null ? FixedPlanScript.createDefault() : FixedPlanScript.readFromNbt(tag);
    }
}
