package org.Artificial.beastofburden.colony.work;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * One persisted work-history row for a beast-of-burden citizen.
 */
public final class BeastWorkLogEntry
{
    private static final String TAG_DAY = "day";
    private static final String TAG_CITIZEN = "citizen";
    private static final String TAG_NAME = "name";
    private static final String TAG_ACTION = "action";
    private static final String TAG_ITEM = "item";
    private static final String TAG_COUNT = "count";
    private static final String TAG_DURATION = "duration";
    private static final String TAG_DETAIL = "detail";

    private final int colonyDay;
    private final int citizenId;
    private final String citizenName;
    private final BeastWorkLogAction action;
    private final ResourceLocation itemId;
    private final int count;
    private final int durationTicks;
    private final String detail;

    public BeastWorkLogEntry(
      final int colonyDay,
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final BeastWorkLogAction action,
      @NotNull final ResourceLocation itemId,
      final int count,
      final int durationTicks)
    {
        this(colonyDay, citizenId, citizenName, action, itemId, count, durationTicks, "");
    }

    public BeastWorkLogEntry(
      final int colonyDay,
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final BeastWorkLogAction action,
      @NotNull final ResourceLocation itemId,
      final int count,
      final int durationTicks,
      @NotNull final String detail)
    {
        this.colonyDay = colonyDay;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.action = action;
        this.itemId = itemId;
        this.count = count;
        this.durationTicks = durationTicks;
        this.detail = detail;
    }

    public int getColonyDay()
    {
        return colonyDay;
    }

    public int getCitizenId()
    {
        return citizenId;
    }

    @NotNull
    public String getCitizenName()
    {
        return citizenName;
    }

    @NotNull
    public BeastWorkLogAction getAction()
    {
        return action;
    }

    @NotNull
    public ResourceLocation getItemId()
    {
        return itemId;
    }

    public int getCount()
    {
        return count;
    }

    public int getDurationTicks()
    {
        return durationTicks;
    }

    @NotNull
    public String getDetail()
    {
        return detail;
    }

    @NotNull
    public static BeastWorkLogEntry read(@NotNull final FriendlyByteBuf buf)
    {
        return new BeastWorkLogEntry(
          buf.readVarInt(),
          buf.readVarInt(),
          buf.readUtf(),
          BeastWorkLogAction.fromId(buf.readByte()),
          buf.readResourceLocation(),
          buf.readVarInt(),
          buf.readVarInt(),
          buf.readUtf()
        );
    }

    public void write(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeVarInt(colonyDay);
        buf.writeVarInt(citizenId);
        buf.writeUtf(citizenName);
        buf.writeByte(action.ordinal());
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(count);
        buf.writeVarInt(durationTicks);
        buf.writeUtf(detail);
    }

    @NotNull
    public CompoundTag save()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_DAY, colonyDay);
        tag.putInt(TAG_CITIZEN, citizenId);
        tag.putString(TAG_NAME, citizenName);
        tag.putInt(TAG_ACTION, action.ordinal());
        tag.putString(TAG_ITEM, itemId.toString());
        tag.putInt(TAG_COUNT, count);
        tag.putInt(TAG_DURATION, durationTicks);
        if (!detail.isEmpty())
        {
            tag.putString(TAG_DETAIL, detail);
        }
        return tag;
    }

    @NotNull
    public static BeastWorkLogEntry load(@NotNull final CompoundTag tag)
    {
        return new BeastWorkLogEntry(
          tag.getInt(TAG_DAY),
          tag.getInt(TAG_CITIZEN),
          tag.getString(TAG_NAME),
          BeastWorkLogAction.fromId(tag.getInt(TAG_ACTION)),
          ResourceLocation.parse(tag.getString(TAG_ITEM)),
          tag.getInt(TAG_COUNT),
          tag.getInt(TAG_DURATION),
          tag.getString(TAG_DETAIL)
        );
    }

    @NotNull
    public static BeastWorkLogEntry delivered(
      final int colonyDay,
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final ItemStack stack,
      final int durationTicks)
    {
        return new BeastWorkLogEntry(
          colonyDay,
          citizenId,
          citizenName,
          BeastWorkLogAction.DELIVERED,
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
          stack.getCount(),
          durationTicks
        );
    }
}
