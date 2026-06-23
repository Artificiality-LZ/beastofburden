package org.Artificial.beastofburden.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import org.Artificial.beastofburden.config.ConfigPersistence;
import org.Artificial.beastofburden.config.ConfigSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Saves edited mod config from the client config screen.
 */
public final class SaveBeastConfigMessage
{
    private final ConfigSnapshot snapshot;

    public SaveBeastConfigMessage(final ConfigSnapshot snapshot)
    {
        this.snapshot = snapshot;
    }

    public static void encode(final SaveBeastConfigMessage message, final net.minecraft.network.FriendlyByteBuf buf)
    {
        buf.writeVarInt(message.snapshot.baseGenerationTicks());
        buf.writeVarInt(message.snapshot.minGenerationTicks());
        buf.writeDouble(message.snapshot.ticksPerItemValue());
        buf.writeDouble(message.snapshot.strengthSpeedBonus());
        buf.writeVarInt(message.snapshot.defaultItemValue());
        buf.writeBoolean(message.snapshot.deriveFromRecipes());
        buf.writeVarInt(message.snapshot.workLogMaxEntries());
        buf.writeVarInt(message.snapshot.workLogHistoryDays());
        buf.writeBoolean(message.snapshot.planningInstantBuildDebug());
        buf.writeVarInt(message.snapshot.itemValues().size());
        for (final Map.Entry<Item, Integer> entry : message.snapshot.itemValues().entrySet())
        {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
            buf.writeVarInt(entry.getValue());
        }
    }

    public static SaveBeastConfigMessage decode(final net.minecraft.network.FriendlyByteBuf buf)
    {
        final int base = buf.readVarInt();
        final int min = buf.readVarInt();
        final double ticksPerValue = buf.readDouble();
        final double strengthBonus = buf.readDouble();
        final int defaultValue = buf.readVarInt();
        final boolean derive = buf.readBoolean();
        final int logMax = buf.readVarInt();
        final int logDays = buf.readVarInt();
        final boolean instantBuildDebug = buf.readBoolean();
        final int count = buf.readVarInt();
        final Map<Item, Integer> itemValues = new HashMap<>();
        for (int i = 0; i < count; i++)
        {
            final ResourceLocation itemId = buf.readResourceLocation();
            final Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null)
            {
                itemValues.put(item, buf.readVarInt());
            }
            else
            {
                buf.readVarInt();
            }
        }
        return new SaveBeastConfigMessage(new ConfigSnapshot(base, min, ticksPerValue, strengthBonus, defaultValue, derive, logMax, logDays, instantBuildDebug, itemValues));
    }

    public static void handle(final SaveBeastConfigMessage message, final Supplier<NetworkEvent.Context> contextSupplier)
    {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }

            if (!player.server.isSingleplayer() && !player.hasPermissions(2))
            {
                return;
            }

            ConfigPersistence.applyAndSave(message.snapshot);
        });
        context.setPacketHandled(true);
    }
}
