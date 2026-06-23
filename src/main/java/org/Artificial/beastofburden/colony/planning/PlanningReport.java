package org.Artificial.beastofburden.colony.planning;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Structured result of the latest autonomous planning pass.
 */
public final class PlanningReport
{
    private static final String TAG_DECISION = "decision";
    private static final String TAG_DETAIL = "detail";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_ACTION = "action";
    private static final String TAG_REASON = "reason";
    private static final String TAG_LOCATION = "location";
    private static final String TAG_BUILDER = "builder";
    private static final String TAG_NOTE = "note";

    private String decision = "";
    private String detail = "";
    private String intent = "";
    private String action = "";
    private String reason = "";
    private String location = "";
    private String builder = "";
    private String note = "";

    public void clear()
    {
        decision = "";
        detail = "";
        intent = "";
        action = "";
        reason = "";
        location = "";
        builder = "";
        note = "";
    }

    public void waiting(@NotNull final String decision, @NotNull final String detail)
    {
        clear();
        this.decision = decision;
        this.detail = detail;
    }

    public void selected(@NotNull final BuildTask task, @NotNull final String detail)
    {
        fillTask(task);
        decision = "planning";
        this.detail = detail;
    }

    public void placed(
      @NotNull final BuildTask task,
      @NotNull final BlockPos location,
      @NotNull final BlockPos builder,
      @Nullable final String note)
    {
        fillTask(task);
        decision = task.getType().getSchematicId() + "@" + location.toShortString();
        this.location = location.toShortString();
        this.builder = builder.equals(BlockPos.ZERO) ? "auto" : builder.toShortString();
        this.note = note == null ? "" : note;
        detail = formatSummary();
    }

    public void failed(@NotNull final String decision, @NotNull final BuildTask task, @NotNull final String note)
    {
        fillTask(task);
        this.decision = decision;
        this.note = note;
        detail = formatSummary();
    }

    private void fillTask(@NotNull final BuildTask task)
    {
        intent = task.getType().getSchematicId();
        action = task.getAction().name();
        reason = task.getReason() == null ? "" : task.getReason();
    }

    @NotNull
    public String getDecision()
    {
        return decision;
    }

    @NotNull
    public String getDetail()
    {
        return detail.isEmpty() ? formatSummary() : detail;
    }

    @NotNull
    public String formatSummary()
    {
        final StringBuilder builder = new StringBuilder();
        if (!intent.isEmpty())
        {
            builder.append(intent);
            if (!action.isEmpty())
            {
                builder.append(" (").append(action).append(')');
            }
            if (!reason.isEmpty())
            {
                builder.append(" - ").append(reason);
            }
            if (!location.isEmpty())
            {
                builder.append(" @ ").append(location);
            }
            if (!this.builder.isEmpty())
            {
                builder.append(" via ").append(this.builder);
            }
            if (!note.isEmpty())
            {
                builder.append(" [").append(note).append(']');
            }
            return builder.toString();
        }

        if (!decision.isEmpty())
        {
            builder.append(decision);
            if (!note.isEmpty())
            {
                builder.append(": ").append(note);
            }
        }
        return builder.toString();
    }

    public void readFromNbt(@Nullable final CompoundTag tag)
    {
        if (tag == null)
        {
            clear();
            return;
        }
        decision = tag.getString(TAG_DECISION);
        detail = tag.getString(TAG_DETAIL);
        intent = tag.getString(TAG_INTENT);
        action = tag.getString(TAG_ACTION);
        reason = tag.getString(TAG_REASON);
        location = tag.getString(TAG_LOCATION);
        builder = tag.getString(TAG_BUILDER);
        note = tag.getString(TAG_NOTE);
    }

    @NotNull
    public CompoundTag writeToNbt()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DECISION, decision);
        tag.putString(TAG_DETAIL, detail);
        tag.putString(TAG_INTENT, intent);
        tag.putString(TAG_ACTION, action);
        tag.putString(TAG_REASON, reason);
        tag.putString(TAG_LOCATION, location);
        tag.putString(TAG_BUILDER, builder);
        tag.putString(TAG_NOTE, note);
        return tag;
    }
}
