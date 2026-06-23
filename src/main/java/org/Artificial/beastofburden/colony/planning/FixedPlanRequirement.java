package org.Artificial.beastofburden.colony.planning;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * One objective inside a {@link FixedPlanStep}, serializable for future player editing.
 */
public final class FixedPlanRequirement
{
    public static final String TAG_KIND = "kind";
    public static final String TAG_TYPE = "type";
    public static final String TAG_LEVEL = "level";
    public static final String TAG_COUNT = "count";

    public enum Kind
    {
        BUILDING,
        FIELD
    }

    private final Kind kind;
    @NotNull
    private final PlannedBuildingType buildingType;
    private final int minLevel;
    private final int count;

    public FixedPlanRequirement(
      @NotNull final Kind kind,
      @NotNull final PlannedBuildingType buildingType,
      final int minLevel,
      final int count)
    {
        this.kind = kind;
        this.buildingType = buildingType;
        this.minLevel = Math.max(1, minLevel);
        this.count = Math.max(1, count);
    }

    @NotNull
    public static FixedPlanRequirement building(@NotNull final PlannedBuildingType type, final int minLevel, final int count)
    {
        return new FixedPlanRequirement(Kind.BUILDING, type, minLevel, count);
    }

    @NotNull
    public static FixedPlanRequirement fields(final int count)
    {
        return new FixedPlanRequirement(Kind.FIELD, PlannedBuildingType.FARMER, 1, count);
    }

    @NotNull
    public Kind getKind()
    {
        return kind;
    }

    @NotNull
    public PlannedBuildingType getBuildingType()
    {
        return buildingType;
    }

    public int getMinLevel()
    {
        return minLevel;
    }

    public int getCount()
    {
        return count;
    }

    @NotNull
    public CompoundTag writeToNbt()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString(TAG_KIND, kind.name());
        tag.putString(TAG_TYPE, buildingType.getSchematicId());
        tag.putInt(TAG_LEVEL, minLevel);
        tag.putInt(TAG_COUNT, count);
        return tag;
    }

    @NotNull
    public static FixedPlanRequirement readFromNbt(@NotNull final CompoundTag tag)
    {
        final Kind kind = Kind.valueOf(tag.getString(TAG_KIND));
        final PlannedBuildingType type = PlannedBuildingType.fromSchematicId(tag.getString(TAG_TYPE));
        final int level = tag.contains(TAG_LEVEL) ? tag.getInt(TAG_LEVEL) : 1;
        final int count = tag.contains(TAG_COUNT) ? tag.getInt(TAG_COUNT) : 1;
        return new FixedPlanRequirement(kind, type, level, count);
    }
}
