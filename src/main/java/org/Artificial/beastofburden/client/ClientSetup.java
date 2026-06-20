package org.Artificial.beastofburden.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.Artificial.beastofburden.Beastofburden;

@Mod.EventBusSubscriber(modid = Beastofburden.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup
{
    private ClientSetup()
    {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
        ModLoadingContext.get().registerExtensionPoint(
          ConfigScreenHandler.ConfigScreenFactory.class,
          () -> new ConfigScreenHandler.ConfigScreenFactory(BeastofburdenConfigScreen::new)
        );
    }
}
