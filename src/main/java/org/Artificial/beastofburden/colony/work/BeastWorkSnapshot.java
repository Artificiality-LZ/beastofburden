package org.Artificial.beastofburden.colony.work;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-bound snapshot of active work and recent history.
 */
public final class BeastWorkSnapshot
{
    public static final BeastWorkSnapshot EMPTY = new BeastWorkSnapshot(0, 0, List.of(), List.of());

    private final int colonyDay;
    private final int historyDays;
    private final List<BeastWorkStatus> activeWork;
    private final List<BeastWorkLogEntry> history;

    public BeastWorkSnapshot(
      final int colonyDay,
      final int historyDays,
      @NotNull final List<BeastWorkStatus> activeWork,
      @NotNull final List<BeastWorkLogEntry> history)
    {
        this.colonyDay = colonyDay;
        this.historyDays = historyDays;
        this.activeWork = List.copyOf(activeWork);
        this.history = List.copyOf(history);
    }

    public int getColonyDay()
    {
        return colonyDay;
    }

    public int getHistoryDays()
    {
        return historyDays;
    }

    @NotNull
    public List<BeastWorkStatus> getActiveWork()
    {
        return activeWork;
    }

    @NotNull
    public List<BeastWorkLogEntry> getHistory()
    {
        return history;
    }

    @NotNull
    public static BeastWorkSnapshot read(@NotNull final FriendlyByteBuf buf)
    {
        final int colonyDay = buf.readVarInt();
        final int historyDays = buf.readVarInt();

        final int activeCount = buf.readVarInt();
        final List<BeastWorkStatus> active = new ArrayList<>(activeCount);
        for (int i = 0; i < activeCount; i++)
        {
            active.add(BeastWorkStatus.read(buf));
        }

        final int historyCount = buf.readVarInt();
        final List<BeastWorkLogEntry> history = new ArrayList<>(historyCount);
        for (int i = 0; i < historyCount; i++)
        {
            history.add(BeastWorkLogEntry.read(buf));
        }

        return new BeastWorkSnapshot(colonyDay, historyDays, active, history);
    }

    public void write(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeVarInt(colonyDay);
        buf.writeVarInt(historyDays);
        buf.writeVarInt(activeWork.size());
        for (final BeastWorkStatus status : activeWork)
        {
            status.write(buf);
        }
        buf.writeVarInt(history.size());
        for (final BeastWorkLogEntry entry : history)
        {
            entry.write(buf);
        }
    }
}
