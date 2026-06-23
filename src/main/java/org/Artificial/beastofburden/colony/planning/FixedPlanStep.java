package org.Artificial.beastofburden.colony.planning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A single scripted planning step (all requirements must be satisfied to advance).
 */
public final class FixedPlanStep
{
    public static final String TAG_REQUIREMENTS = "requirements";

    private final List<FixedPlanRequirement> requirements;

    public FixedPlanStep(@NotNull final List<FixedPlanRequirement> requirements)
    {
        this.requirements = List.copyOf(requirements);
    }

    @NotNull
    public List<FixedPlanRequirement> getRequirements()
    {
        return requirements;
    }

    @NotNull
    public CompoundTag writeToNbt()
    {
        final CompoundTag tag = new CompoundTag();
        final ListTag list = new ListTag();
        for (final FixedPlanRequirement requirement : requirements)
        {
            list.add(requirement.writeToNbt());
        }
        tag.put(TAG_REQUIREMENTS, list);
        return tag;
    }

    @NotNull
    public static FixedPlanStep readFromNbt(@NotNull final CompoundTag tag)
    {
        final List<FixedPlanRequirement> requirements = new ArrayList<>();
        if (tag.contains(TAG_REQUIREMENTS, Tag.TAG_LIST))
        {
            final ListTag list = tag.getList(TAG_REQUIREMENTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                requirements.add(FixedPlanRequirement.readFromNbt(list.getCompound(i)));
            }
        }
        return new FixedPlanStep(requirements);
    }
}
