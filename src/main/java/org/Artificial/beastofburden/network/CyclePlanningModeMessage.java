package org.Artificial.beastofburden.network;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;

import java.util.function.Supplier;

/**
 * Cycles heuristic / scripted planning mode for the Town Hall beast module.
 */
public final class CyclePlanningModeMessage
{
    private final BlockPos buildingPos;

    public CyclePlanningModeMessage(final BlockPos buildingPos)
    {
        this.buildingPos = buildingPos;
    }

    public static void encode(final CyclePlanningModeMessage message, final FriendlyByteBuf buf)
    {
        buf.writeBlockPos(message.buildingPos);
    }

    public static CyclePlanningModeMessage decode(final FriendlyByteBuf buf)
    {
        return new CyclePlanningModeMessage(buf.readBlockPos());
    }

    public static void handle(final CyclePlanningModeMessage message, final Supplier<NetworkEvent.Context> ctx)
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
                module.cyclePlanningMode();
            }
        });
        context.setPacketHandled(true);
    }
}
