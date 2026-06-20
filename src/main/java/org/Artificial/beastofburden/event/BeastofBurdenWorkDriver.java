package org.Artificial.beastofburden.event;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.colony.jobs.JobBeastofburden;
import org.Artificial.beastofburden.util.BeastofBurdenAiDriver;

/**
 * Ticks beast-of-burden job AI every server tick.
 * <p>
 * TownHall {@link com.minecolonies.api.colony.buildings.modules.ITickingModule} only runs on the
 * colony slow tick ({@code MAX_TICKRATE} = 500), which is far too slow for generation and delivery.
 */
@Mod.EventBusSubscriber(modid = Beastofburden.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BeastofBurdenWorkDriver
{
    private BeastofBurdenWorkDriver()
    {
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.side.isClient())
        {
            return;
        }

        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            if (colony == null || colony.getWorld() == null || colony.getWorld().isClientSide)
            {
                continue;
            }

            for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
            {
                if (citizen.getJob() instanceof JobBeastofburden)
                {
                    BeastofBurdenAiDriver.tickCitizen(citizen);
                }
            }
        }
    }
}
