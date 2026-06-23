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
 * Toggles autonomous colony planning for the Town Hall beast module.
 */
public final class ToggleAutonomousPlanningMessage
{
    private final BlockPos buildingPos;
    private final boolean enabled;

    public ToggleAutonomousPlanningMessage(final BlockPos buildingPos, final boolean enabled)
    {
        this.buildingPos = buildingPos;
        this.enabled = enabled;
    }

    public static void encode(final ToggleAutonomousPlanningMessage message, final FriendlyByteBuf buf)
    {
        buf.writeBlockPos(message.buildingPos);
        buf.writeBoolean(message.enabled);
    }

    public static ToggleAutonomousPlanningMessage decode(final FriendlyByteBuf buf)
    {
        return new ToggleAutonomousPlanningMessage(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(final ToggleAutonomousPlanningMessage message, final Supplier<NetworkEvent.Context> ctx)
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
                module.setAutonomousPlanningEnabled(message.enabled);
            }
        });
        context.setPacketHandled(true);
    }
}
