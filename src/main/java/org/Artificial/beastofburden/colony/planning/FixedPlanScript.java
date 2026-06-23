package org.Artificial.beastofburden.colony.planning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered scripted build plan. Stored per-colony so players can customize steps later.
 */
public final class FixedPlanScript
{
    public static final int FORMAT_VERSION = 1;

    public static final String TAG_VERSION = "version";
    public static final String TAG_CUSTOM = "custom";
    public static final String TAG_STEPS = "steps";

    private final int version;
    private final boolean custom;
    @NotNull
    private final List<FixedPlanStep> steps;

    public FixedPlanScript(final int version, final boolean custom, @NotNull final List<FixedPlanStep> steps)
    {
        this.version = version;
        this.custom = custom;
        this.steps = List.copyOf(steps);
    }

    @NotNull
    public static FixedPlanScript createDefault()
    {
        return new FixedPlanScript(FORMAT_VERSION, false, defaultSteps());
    }

    @NotNull
    private static List<FixedPlanStep> defaultSteps()
    {
        final List<FixedPlanStep> steps = new ArrayList<>();
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.BUILDER, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.TAVERN, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.WAREHOUSE, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.COURIER, 1, 1)));
        steps.add(step(
          FixedPlanRequirement.building(PlannedBuildingType.FARMER, 1, 1),
          FixedPlanRequirement.fields(2)
        ));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.COOK, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.RESIDENCE, 1, 2)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.FORESTER, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.MINER, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.GUARD_TOWER, 1, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.BUILDER, 2, 1)));
        steps.add(step(FixedPlanRequirement.building(PlannedBuildingType.UNIVERSITY, 1, 1)));
        return steps;
    }

    @NotNull
    private static FixedPlanStep step(@NotNull final FixedPlanRequirement... requirements)
    {
        return new FixedPlanStep(List.of(requirements));
    }

    public int getVersion()
    {
        return version;
    }

    public boolean isCustom()
    {
        return custom;
    }

    @NotNull
    public List<FixedPlanStep> getSteps()
    {
        return steps;
    }

    public int stepCount()
    {
        return steps.size();
    }

    @NotNull
    public FixedPlanStep getStep(final int index)
    {
        return steps.get(Math.max(0, Math.min(index, steps.size() - 1)));
    }

    @NotNull
    public CompoundTag writeToNbt()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_VERSION, version);
        tag.putBoolean(TAG_CUSTOM, custom);
        final ListTag list = new ListTag();
        for (final FixedPlanStep step : steps)
        {
            list.add(step.writeToNbt());
        }
        tag.put(TAG_STEPS, list);
        return tag;
    }

    @NotNull
    public static FixedPlanScript readFromNbt(@NotNull final CompoundTag tag)
    {
        if (!tag.contains(TAG_STEPS, Tag.TAG_LIST))
        {
            return createDefault();
        }

        final int version = tag.contains(TAG_VERSION) ? tag.getInt(TAG_VERSION) : FORMAT_VERSION;
        final boolean custom = tag.getBoolean(TAG_CUSTOM);
        final ListTag list = tag.getList(TAG_STEPS, Tag.TAG_COMPOUND);
        final List<FixedPlanStep> steps = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            steps.add(FixedPlanStep.readFromNbt(list.getCompound(i)));
        }

        if (steps.isEmpty())
        {
            return createDefault();
        }

        if (!custom && version != FORMAT_VERSION)
        {
            return createDefault();
        }

        return new FixedPlanScript(version, custom, steps);
    }
}
