package org.Artificial.beastofburden.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.util.ItemValueRegistry;

/**
 * Reloads item value tables when a server starts (recipes are available).
 */
@Mod.EventBusSubscriber(modid = Beastofburden.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemValueBootstrap
{
    private ItemValueBootstrap()
    {
    }

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event)
    {
        final MinecraftServer server = event.getServer();
        if (server.overworld() != null)
        {
            ItemValueRegistry.reload(server.overworld());
        }
    }
}
