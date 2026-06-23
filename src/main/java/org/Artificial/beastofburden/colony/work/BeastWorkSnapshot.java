package org.Artificial.beastofburden.colony.work;

import net.minecraft.network.FriendlyByteBuf;
import org.Artificial.beastofburden.colony.planning.ColonyPhase;
import org.Artificial.beastofburden.colony.planning.PlanningMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-bound snapshot of active work and recent history.
 */
public final class BeastWorkSnapshot
{
    public static final BeastWorkSnapshot EMPTY = new BeastWorkSnapshot(
      0, 0, List.of(), List.of(), false, PlanningMode.HEURISTIC, 0, 0, ColonyPhase.P0_FOUNDATION, "", ""
    );

    private final int colonyDay;
    private final int historyDays;
    private final List<BeastWorkStatus> activeWork;
    private final List<BeastWorkLogEntry> history;
    private final boolean autonomousPlanningEnabled;
    private final PlanningMode planningMode;
    private final int scriptedStepIndex;
    private final int scriptedStepCount;
    private final ColonyPhase planningPhase;
    private final String planningLastDecision;
    private final String planningDetail;

    public BeastWorkSnapshot(
      final int colonyDay,
      final int historyDays,
      @NotNull final List<BeastWorkStatus> activeWork,
      @NotNull final List<BeastWorkLogEntry> history,
      final boolean autonomousPlanningEnabled,
      @NotNull final PlanningMode planningMode,
      final int scriptedStepIndex,
      final int scriptedStepCount,
      @NotNull final ColonyPhase planningPhase,
      @NotNull final String planningLastDecision,
      @NotNull final String planningDetail)
    {
        this.colonyDay = colonyDay;
        this.historyDays = historyDays;
        this.activeWork = List.copyOf(activeWork);
        this.history = List.copyOf(history);
        this.autonomousPlanningEnabled = autonomousPlanningEnabled;
        this.planningMode = planningMode;
        this.scriptedStepIndex = scriptedStepIndex;
        this.scriptedStepCount = scriptedStepCount;
        this.planningPhase = planningPhase;
        this.planningLastDecision = planningLastDecision;
        this.planningDetail = planningDetail;
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

    public boolean isAutonomousPlanningEnabled()
    {
        return autonomousPlanningEnabled;
    }

    @NotNull
    public PlanningMode getPlanningMode()
    {
        return planningMode;
    }

    public int getScriptedStepIndex()
    {
        return scriptedStepIndex;
    }

    public int getScriptedStepCount()
    {
        return scriptedStepCount;
    }

    @NotNull
    public ColonyPhase getPlanningPhase()
    {
        return planningPhase;
    }

    @NotNull
    public String getPlanningLastDecision()
    {
        return planningLastDecision;
    }

    @NotNull
    public String getPlanningDetail()
    {
        return planningDetail;
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

        final boolean planningEnabled = buf.readBoolean();
        final PlanningMode mode = PlanningMode.fromId(buf.readByte());
        final int scriptedStepIndex = buf.readVarInt();
        final int scriptedStepCount = buf.readVarInt();
        final ColonyPhase phase = ColonyPhase.fromId(buf.readByte());
        final String lastDecision = buf.readUtf();
        final String planningDetail = buf.readableBytes() > 0 ? buf.readUtf() : "";

        return new BeastWorkSnapshot(
          colonyDay,
          historyDays,
          active,
          history,
          planningEnabled,
          mode,
          scriptedStepIndex,
          scriptedStepCount,
          phase,
          lastDecision,
          planningDetail
        );
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
        buf.writeBoolean(autonomousPlanningEnabled);
        buf.writeByte(planningMode.ordinal());
        buf.writeVarInt(scriptedStepIndex);
        buf.writeVarInt(scriptedStepCount);
        buf.writeByte(planningPhase.ordinal());
        buf.writeUtf(planningLastDecision);
        buf.writeUtf(planningDetail);
    }
}
