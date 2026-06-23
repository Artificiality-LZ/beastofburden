package org.Artificial.beastofburden.network;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.planning.FixedPlanScript;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Saves a player-edited scripted colony plan to the Town Hall beast module.
 */
public final class SaveColonyPlanMessage
{
    private final BlockPos buildingPos;
    @NotNull
    private final CompoundTag scriptNbt;

    public SaveColonyPlanMessage(final BlockPos buildingPos, @NotNull final CompoundTag scriptNbt)
    {
        this.buildingPos = buildingPos;
        this.scriptNbt = scriptNbt;
    }

    public static void encode(final SaveColonyPlanMessage message, final FriendlyByteBuf buf)
    {
        buf.writeBlockPos(message.buildingPos);
        buf.writeNbt(message.scriptNbt);
    }

    public static SaveColonyPlanMessage decode(final FriendlyByteBuf buf)
    {
        final BlockPos pos = buf.readBlockPos();
        final CompoundTag tag = buf.readNbt();
        return new SaveColonyPlanMessage(pos, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(final SaveColonyPlanMessage message, final Supplier<NetworkEvent.Context> ctx)
    {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null)
            {
                return;
            }

            final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(player.serverLevel(), message.buildingPos);
            if (colony == null || !colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
            {
                return;
            }

            final IBuilding building = IColonyManager.getInstance().getBuilding(player.serverLevel(), message.buildingPos);
            if (building == null)
            {
                return;
            }

            final TownHallBeastofburdenModule module = building.getFirstModuleOccurance(TownHallBeastofburdenModule.class);
            if (module != null)
            {
                module.applyPlanScript(FixedPlanScript.readFromNbt(message.scriptNbt));
            }
        });
        context.setPacketHandled(true);
    }
}
