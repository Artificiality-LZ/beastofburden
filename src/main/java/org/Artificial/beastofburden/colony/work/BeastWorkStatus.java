package org.Artificial.beastofburden.colony.work;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Live work status for one assigned beast-of-burden citizen.
 */
public final class BeastWorkStatus
{
    private final int citizenId;
    private final String citizenName;
    private final BeastWorkPhase phase;
    private final ResourceLocation itemId;
    private final int count;
    private final int progressTicks;
    private final int requiredTicks;
    private final String detail;

    public BeastWorkStatus(
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final BeastWorkPhase phase,
      @NotNull final ResourceLocation itemId,
      final int count,
      final int progressTicks,
      final int requiredTicks)
    {
        this(citizenId, citizenName, phase, itemId, count, progressTicks, requiredTicks, "");
    }

    public BeastWorkStatus(
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final BeastWorkPhase phase,
      @NotNull final ResourceLocation itemId,
      final int count,
      final int progressTicks,
      final int requiredTicks,
      @NotNull final String detail)
    {
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.phase = phase;
        this.itemId = itemId;
        this.count = count;
        this.progressTicks = progressTicks;
        this.requiredTicks = requiredTicks;
        this.detail = detail;
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
    public BeastWorkPhase getPhase()
    {
        return phase;
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

    public int getProgressTicks()
    {
        return progressTicks;
    }

    public int getRequiredTicks()
    {
        return requiredTicks;
    }

    public float getProgressPercent()
    {
        return requiredTicks <= 0 ? 0f : Math.min(1f, (float) progressTicks / requiredTicks);
    }

    @NotNull
    public String getDetail()
    {
        return detail;
    }

    @NotNull
    public static BeastWorkStatus idle(final int citizenId, @NotNull final String citizenName)
    {
        return new BeastWorkStatus(citizenId, citizenName, BeastWorkPhase.IDLE, ResourceLocation.fromNamespaceAndPath("minecraft", "air"), 0, 0, 0);
    }

    @NotNull
    public static BeastWorkStatus generating(
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final ItemStack stack,
      final int progressTicks,
      final int requiredTicks)
    {
        return new BeastWorkStatus(
          citizenId,
          citizenName,
          BeastWorkPhase.GENERATING,
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
          stack.getCount(),
          progressTicks,
          requiredTicks
        );
    }

    @NotNull
    public static BeastWorkStatus delivering(final int citizenId, @NotNull final String citizenName, @NotNull final ItemStack stack)
    {
        return new BeastWorkStatus(
          citizenId,
          citizenName,
          BeastWorkPhase.DELIVERING,
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
          stack.getCount(),
          0,
          0
        );
    }

    @NotNull
    public static BeastWorkStatus planning(
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final ResourceLocation hutItem,
      @NotNull final String detail)
    {
        return new BeastWorkStatus(
          citizenId,
          citizenName,
          BeastWorkPhase.PLANNING,
          hutItem,
          1,
          0,
          0,
          detail
        );
    }

    @NotNull
    public static BeastWorkStatus planningIdle(
      final int citizenId,
      @NotNull final String citizenName,
      @NotNull final String phase,
      @Nullable final String lastDecision)
    {
        return new BeastWorkStatus(
          citizenId,
          citizenName,
          BeastWorkPhase.PLANNING,
          ResourceLocation.fromNamespaceAndPath("minecraft", "air"),
          0,
          0,
          0,
          phase + (lastDecision == null || lastDecision.isEmpty() ? "" : ": " + lastDecision)
        );
    }

    @NotNull
    public static BeastWorkStatus read(@NotNull final FriendlyByteBuf buf)
    {
        return new BeastWorkStatus(
          buf.readVarInt(),
          buf.readUtf(),
          BeastWorkPhase.fromId(buf.readByte()),
          buf.readResourceLocation(),
          buf.readVarInt(),
          buf.readVarInt(),
          buf.readVarInt(),
          buf.readUtf()
        );
    }

    public void write(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeVarInt(citizenId);
        buf.writeUtf(citizenName);
        buf.writeByte(phase.ordinal());
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(count);
        buf.writeVarInt(progressTicks);
        buf.writeVarInt(requiredTicks);
        buf.writeUtf(detail);
    }
}
