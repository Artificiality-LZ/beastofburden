package org.Artificial.beastofburden.util;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Registers and counts farmer fields (MineColonies 1.20+ building extensions API).
 */
public final class ColonyFieldSupport
{
    private static final String FARM_FIELD_CLASS = "com.minecolonies.core.colony.buildingextensions.FarmField";
    private static final String EXTENSION_INTERFACE = "com.minecolonies.api.colony.buildingextensions.IBuildingExtension";
    private static final String FARMER_FIELDS_MODULE = "com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer$FarmerFieldsModule";

    private ColonyFieldSupport()
    {
    }

    public static int countForFarmer(@NotNull final IColony colony, @NotNull final BlockPos farmerPos)
    {
        final IRegisteredStructureManager manager = getStructureManager(colony);
        if (manager == null)
        {
            return 0;
        }

        int count = 0;
        for (final var extension : manager.getBuildingExtensions(ext -> farmerPos.equals(ext.getBuildingId())))
        {
            if (extension != null)
            {
                count++;
            }
        }
        return count;
    }

    public static int countFields(@NotNull final IColony colony)
    {
        final IRegisteredStructureManager manager = getStructureManager(colony);
        if (manager == null)
        {
            return 0;
        }

        int count = 0;
        for (final var extension : manager.getBuildingExtensions(ext -> true))
        {
            if (extension != null)
            {
                count++;
            }
        }
        return count;
    }

    public static boolean register(@NotNull final IColony colony, @NotNull final BlockPos fieldPos, @NotNull final BlockPos farmerPos)
    {
        final var world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return false;
        }

        final IBuilding farmer = com.minecolonies.api.colony.IColonyManager.getInstance().getBuilding(world, farmerPos);
        if (farmer == null)
        {
            BeastofBurdenLog.warn("Cannot register field at {}: farmer hut missing at {}", fieldPos, farmerPos);
            return false;
        }

        try
        {
            final Object field = Class.forName(FARM_FIELD_CLASS)
              .getMethod("create", BlockPos.class, net.minecraft.world.level.Level.class)
              .invoke(null, fieldPos, world);
            field.getClass().getMethod("setBuilding", BlockPos.class).invoke(field, farmerPos);

            final IRegisteredStructureManager manager = getStructureManager(colony);
            if (manager == null || !manager.addBuildingExtension(castExtension(field)))
            {
                BeastofBurdenLog.warn("Failed to add building extension for field at {}", fieldPos);
                return false;
            }

            if (!assignToFarmer(farmer, field))
            {
                BeastofBurdenLog.warn("Field at {} registered but not assigned to farmer {}", fieldPos, farmerPos);
                return false;
            }

            colony.markDirty();
            return true;
        }
        catch (final ReflectiveOperationException ex)
        {
            BeastofBurdenLog.warn("Failed to register field at {}: {}", fieldPos, ex.toString());
            return false;
        }
    }

    public static boolean isFarmerHut(@NotNull final IBuilding building)
    {
        final BuildingEntry farmer = ModBuildings.farmer.get();
        return farmer.getRegistryName().equals(building.getBuildingType().getRegistryName());
    }

    private static boolean assignToFarmer(@NotNull final IBuilding farmer, @NotNull final Object field)
    {
        for (final IBuildingModule module : farmer.getModules())
        {
            if (!module.getClass().getName().equals(FARMER_FIELDS_MODULE))
            {
                continue;
            }

            try
            {
                final Method assign = module.getClass().getMethod(
                  "assignExtension",
                  Class.forName(EXTENSION_INTERFACE)
                );
                return (boolean) assign.invoke(module, field);
            }
            catch (final ReflectiveOperationException ex)
            {
                BeastofBurdenLog.warn("Failed to assign field to farmer module: {}", ex.toString());
                return false;
            }
        }

        return false;
    }

    @Nullable
    private static IRegisteredStructureManager getStructureManager(@NotNull final IColony colony)
    {
        try
        {
            final Method method = colony.getClass().getMethod("getServerBuildingManager");
            final Object manager = method.invoke(colony);
            return manager instanceof IRegisteredStructureManager registered ? registered : null;
        }
        catch (final ReflectiveOperationException ex)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static com.minecolonies.api.colony.buildingextensions.IBuildingExtension castExtension(@NotNull final Object field)
    {
        return (com.minecolonies.api.colony.buildingextensions.IBuildingExtension) field;
    }
}
