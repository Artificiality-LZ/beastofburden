package org.Artificial.beastofburden.colony.buildings;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.registry.IBuildingRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModule;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.slf4j.Logger;

/**
 * Registers BeastOfBurden building modules with MineColonies {@link BuildingEntry} instances.
 */
public final class BeastofburdenBuildingModules
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TOWN_HALL_ID = ResourceLocation.fromNamespaceAndPath("minecolonies", "townhall");

    /**
     * Module producer attached to every TownHall building entry.
     */
    public static final BuildingEntry.ModuleProducer<TownHallBeastofburdenModule, TownHallBeastofburdenModuleView> TOWN_HALL_BEASTOFBURDEN =
      new BuildingEntry.ModuleProducer<>(
        TownHallBeastofburdenModule.MODULE_KEY,
        TownHallBeastofburdenModule::new,
        () -> TownHallBeastofburdenModuleView::new);

    private BeastofburdenBuildingModules()
    {
    }

    /**
     * Appends the BeastOfBurden worker module to the MineColonies TownHall {@link BuildingEntry}.
     * <p>
     * MineColonies instantiates buildings through {@link BuildingEntry#produceBuilding} and
     * {@link BuildingEntry#produceBuildingView}, which iterate the module producer list. Adding our
     * producer here lets the TownHall hire BeastOfBurden citizens without patching MineColonies.
     */
    public static void register()
    {
        final IForgeRegistry<BuildingEntry> registry = IBuildingRegistry.getInstance();
        final BuildingEntry townHall = registry.getValue(TOWN_HALL_ID);
        if (townHall == null)
        {
            LOGGER.error("[{}] TownHall BuildingEntry was not found; BeastOfBurden hiring module was not registered.", Beastofburden.MODID);
            return;
        }

        townHall.getModuleProducers().add(TOWN_HALL_BEASTOFBURDEN);
        LOGGER.info("[{}] Registered BeastOfBurden hiring module on TownHall.", Beastofburden.MODID);
    }
}
