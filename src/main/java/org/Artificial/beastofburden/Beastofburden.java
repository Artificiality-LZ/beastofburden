package org.Artificial.beastofburden;

import com.minecolonies.api.colony.jobs.registry.IJobRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.Artificial.beastofburden.colony.buildings.BeastofburdenBuildingModules;
import org.Artificial.beastofburden.colony.jobs.BeastofburdenCitizenSounds;
import org.Artificial.beastofburden.colony.jobs.BeastofburdenJobs;
import org.slf4j.Logger;

@Mod(Beastofburden.MODID)
public class Beastofburden
{
    public static final String MODID = "beastofburden";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Beastofburden()
    {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BeastofburdenJobs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[{}] Mod loading.", MODID);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            if (IJobRegistry.getInstance().containsKey(BeastofburdenJobs.BEASTOFBURDEN_ID))
            {
                LOGGER.info("[{}] Job '{}' registered.", MODID, BeastofburdenJobs.BEASTOFBURDEN_ID);
            }
            else
            {
                LOGGER.error("[{}] Job '{}' was not registered.", MODID, BeastofburdenJobs.BEASTOFBURDEN_ID);
            }

            BeastofburdenBuildingModules.register();
            BeastofburdenCitizenSounds.register();
            org.Artificial.beastofburden.network.ModNetwork.register();
        });
    }

    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event)
    {
        LOGGER.info("[{}] Server starting.", MODID);
    }
}
