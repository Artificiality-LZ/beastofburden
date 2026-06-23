package org.Artificial.beastofburden.colony.planning;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single build or upgrade task produced by the tactical planner.
 */
public final class BuildTask
{
    private final PlannedBuildingType type;
    private final BuildTaskAction action;
    private final int targetLevel;
    private final float priority;
    @Nullable
    private final BlockPos existingBuilding;
    @Nullable
    private final String reason;

    public BuildTask(
      @NotNull final PlannedBuildingType type,
      @NotNull final BuildTaskAction action,
      final int targetLevel,
      final float priority,
      @Nullable final BlockPos existingBuilding,
      @Nullable final String reason)
    {
        this.type = type;
        this.action = action;
        this.targetLevel = targetLevel;
        this.priority = priority;
        this.existingBuilding = existingBuilding;
        this.reason = reason;
    }

    @NotNull
    public PlannedBuildingType getType()
    {
        return type;
    }

    @NotNull
    public BuildTaskAction getAction()
    {
        return action;
    }

    public int getTargetLevel()
    {
        return targetLevel;
    }

    public float getPriority()
    {
        return priority;
    }

    @Nullable
    public BlockPos getExistingBuilding()
    {
        return existingBuilding;
    }

    @Nullable
    public String getReason()
    {
        return reason;
    }
}
