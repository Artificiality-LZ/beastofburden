package org.Artificial.beastofburden.colony.buildings.modules;

import com.google.common.collect.Lists;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.IBuildingWorkerModule;
import com.minecolonies.api.colony.buildings.modules.*;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.Artificial.beastofburden.Config;
import org.Artificial.beastofburden.colony.jobs.BeastofburdenJobs;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.colony.work.BeastWorkLogEntry;
import org.Artificial.beastofburden.colony.work.BeastWorkSnapshot;
import org.Artificial.beastofburden.colony.work.BeastWorkStatus;
import com.minecolonies.core.util.BuildingUtils;
import org.Artificial.beastofburden.util.BeastofBurdenAiDriver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_ASSIGNED;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_HIRING_MODE;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_WORKING_RESIDENTS;

/**
 * {@link IAssignsJob} module for the MineColonies TownHall.
 * <p>
 * Lets the TownHall hire citizens into {@link JobBeastofburden} without a dedicated hut.
 * Capacity scales with TownHall level: 1 at levels 1-2, 2 at 3-4, 3 at level 5.
 */
public class TownHallBeastofburdenModule extends AbstractBuildingModule
  implements IAssignsJob, IPersistentModule, IBuildingEventsModule, IBuildingWorkerModule, ITickingModule
{
    /**
     * Stable serialization key for NBT and network sync.
     */
    public static final String MODULE_KEY = "beastofburden:townhall_beastofburden";

    private static final Skill PRIMARY_SKILL = Skill.Strength;
    private static final Skill SECONDARY_SKILL = Skill.Adaptability;
    private static final String TAG_WORK_LOG = "workLog";

    private final List<ICitizenData> assignedCitizens = Lists.newArrayList();
    private final Deque<BeastWorkLogEntry> workLog = new ArrayDeque<>();
    private final Map<Integer, BeastWorkStatus> activeWork = new HashMap<>();
    private HiringMode hiringMode = HiringMode.DEFAULT;

    @NotNull
    @Override
    public JobEntry getJobEntry()
    {
        return BeastofburdenJobs.BEASTOFBURDEN.get();
    }

    @Override
    public boolean canWorkDuringTheRain()
    {
        return true;
    }

    @NotNull
    @Override
    public IJob<?> createJob(final ICitizenData citizen)
    {
        return getJobEntry().produceJob(citizen);
    }

    @NotNull
    @Override
    public Skill getPrimarySkill()
    {
        return PRIMARY_SKILL;
    }

    @NotNull
    @Override
    public Skill getSecondarySkill()
    {
        return SECONDARY_SKILL;
    }

    @Override
    public void onColonyTick(@NotNull final IColony colony)
    {
        for (final ICitizenData citizen : getAssignedCitizen())
        {
            if (citizen.getEntity().isPresent() && citizen.getJob() instanceof JobBeastofburden job && job.getWorkerAI() == null)
            {
                BeastofBurdenAiDriver.tickCitizen(citizen);
            }
        }

        // Like the builder hut: hire and work while the TownHall is still under construction.
        if (!isFull()
              && (!building.isBuilt() || building.getBuildingLevel() > 0)
              && BuildingUtils.canAutoHire(building, getHiringMode(), getJobEntry()))
        {
            final ICitizenData joblessCitizen = colony.getCitizenManager().getJoblessCitizen();
            if (joblessCitizen != null)
            {
                assignCitizen(joblessCitizen);
            }
        }
    }

    @Override
    public boolean assignCitizen(final ICitizenData citizen)
    {
        if (citizen == null || isFull() || assignedCitizens.contains(citizen))
        {
            return false;
        }

        IJob<?> job = citizen.getJob();
        if (job == null)
        {
            job = getJobEntry().produceJob(citizen);
        }

        if (!job.assignTo(this))
        {
            return false;
        }

        assignedCitizens.add(citizen);
        onAssignment(citizen);
        BeastofBurdenAiDriver.tickCitizen(citizen);
        markDirty();
        return true;
    }

    @Override
    public boolean removeCitizen(@NotNull final ICitizenData citizen)
    {
        if (!assignedCitizens.contains(citizen))
        {
            return false;
        }

        assignedCitizens.remove(citizen);
        markDirty();
        onRemoval(citizen);
        return true;
    }

    @Override
    public void onDestroyed()
    {
        for (final ICitizenData citizen : new ArrayList<>(assignedCitizens))
        {
            removeCitizen(citizen);
        }
    }

    @Override
    public List<ICitizenData> getAssignedCitizen()
    {
        return new ArrayList<>(assignedCitizens);
    }

    @Override
    public boolean isFull()
    {
        return assignedCitizens.size() >= getModuleMax();
    }

    @Override
    public int getModuleMax()
    {
        final int level = building.getBuildingLevel();
        return Math.max(1, Math.min(3, (level + 1) / 2));
    }

    @Override
    public boolean hasAssignedCitizen(final ICitizenData citizen)
    {
        return assignedCitizens.contains(citizen);
    }

    @Override
    public boolean hasAssignedCitizen()
    {
        return !assignedCitizens.isEmpty();
    }

    @Override
    public List<Optional<AbstractEntityCitizen>> getAssignedEntities()
    {
        return assignedCitizens.stream()
          .filter(Objects::nonNull)
          .map(ICitizenData::getEntity)
          .collect(Collectors.toList());
    }

    @Override
    public void setHiringMode(final HiringMode hiringMode)
    {
        this.hiringMode = hiringMode;
        markDirty();
    }

    @Override
    public HiringMode getHiringMode()
    {
        return hiringMode;
    }

    public void setActiveWork(@NotNull final BeastWorkStatus status)
    {
        activeWork.put(status.getCitizenId(), status);
        markDirty();
    }

    public void appendLog(@NotNull final BeastWorkLogEntry entry)
    {
        workLog.addFirst(entry);
        while (workLog.size() > Config.workLogMaxEntries)
        {
            workLog.removeLast();
        }
        markDirty();
    }

    @NotNull
    private BeastWorkSnapshot createSnapshot()
    {
        final int currentDay = building.getColony().getDay();
        final int historyDays = Config.workLogHistoryDays;
        final List<BeastWorkLogEntry> visibleHistory = new ArrayList<>();

        for (final BeastWorkLogEntry entry : workLog)
        {
            if (historyDays <= 0 || currentDay - entry.getColonyDay() <= historyDays)
            {
                visibleHistory.add(entry);
            }
        }

        final List<BeastWorkStatus> statuses = new ArrayList<>();
        for (final ICitizenData citizen : assignedCitizens)
        {
            statuses.add(activeWork.getOrDefault(citizen.getId(), BeastWorkStatus.idle(citizen.getId(), citizen.getName())));
        }

        return new BeastWorkSnapshot(currentDay, historyDays, statuses, visibleHistory);
    }

    @Override
    public void serializeNBT(final CompoundTag compound)
    {
        compound.putInt(TAG_HIRING_MODE, hiringMode.ordinal());
        if (!assignedCitizens.isEmpty())
        {
            final int[] residentIds = assignedCitizens.stream().mapToInt(ICitizenData::getId).toArray();
            compound.putIntArray(TAG_WORKING_RESIDENTS, residentIds);
        }

        final ListTag logTag = new ListTag();
        for (final BeastWorkLogEntry entry : workLog)
        {
            logTag.add(entry.save());
        }
        compound.put(TAG_WORK_LOG, logTag);
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        if (compound.contains(TAG_ASSIGNED))
        {
            hiringMode = HiringMode.values()[compound.getCompound(TAG_ASSIGNED).getInt(TAG_HIRING_MODE)];
        }
        else
        {
            hiringMode = HiringMode.values()[compound.getInt(TAG_HIRING_MODE)];
        }

        if (!compound.contains(TAG_WORKING_RESIDENTS))
        {
            return;
        }

        for (final int citizenId : compound.getIntArray(TAG_WORKING_RESIDENTS))
        {
            final ICitizenData citizen = building.getColony().getCitizenManager().getCivilian(citizenId);
            if (citizen != null)
            {
                assignCitizen(citizen);
            }
        }

        workLog.clear();
        if (compound.contains(TAG_WORK_LOG, Tag.TAG_LIST))
        {
            final ListTag logTag = compound.getList(TAG_WORK_LOG, Tag.TAG_COMPOUND);
            for (int i = 0; i < logTag.size(); i++)
            {
                workLog.add(BeastWorkLogEntry.load(logTag.getCompound(i)));
            }
        }
    }

    @Override
    public void serializeToView(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(assignedCitizens.size());
        for (final ICitizenData citizen : assignedCitizens)
        {
            buf.writeInt(citizen.getId());
        }
        buf.writeInt(hiringMode.ordinal());
        buf.writeInt(getModuleMax());
        buf.writeRegistryId(IMinecoloniesAPI.getInstance().getJobRegistry(), getJobEntry());
        buf.writeInt(PRIMARY_SKILL.ordinal());
        buf.writeInt(SECONDARY_SKILL.ordinal());
        createSnapshot().write(buf);
    }

    private void onAssignment(final ICitizenData citizen)
    {
        if (citizen.getJob() instanceof JobBeastofburden job)
        {
            BeastofBurdenAiDriver.tickCitizen(citizen);
            job.onLevelUp();
        }
    }

    private void onRemoval(final ICitizenData citizen)
    {
        if (citizen.getJob() != null)
        {
            citizen.getJob().onRemoval();
        }
        citizen.setVisibleStatus(null);
    }
}
