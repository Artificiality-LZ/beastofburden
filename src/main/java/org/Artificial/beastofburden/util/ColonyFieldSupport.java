package org.Artificial.beastofburden.util;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.Artificial.beastofburden.colony.planning.BuildingFootprint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers and counts farmer fields via MineColonies building-extension APIs.
 */
public final class ColonyFieldSupport
{
    private static final int DEFAULT_FIELD_RANGE = 5;

    private ColonyFieldSupport()
    {
        throw new IllegalStateException("Utility class");
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
            if (isFarmField(extension))
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
        for (final var extension : manager.getBuildingExtensions(ColonyFieldSupport::isFarmField))
        {
            if (extension != null)
            {
                count++;
            }
        }
        return count;
    }

    @NotNull
    public static List<BlockPos> listFieldAnchors(@NotNull final IColony colony)
    {
        final IRegisteredStructureManager manager = getStructureManager(colony);
        final List<BlockPos> anchors = new ArrayList<>();
        if (manager == null)
        {
            return anchors;
        }

        for (final var extension : manager.getBuildingExtensions(ColonyFieldSupport::isFarmField))
        {
            if (extension != null)
            {
                anchors.add(extension.getPosition());
            }
        }
        return anchors;
    }

    public static boolean hasFieldAt(@NotNull final IColony colony, @NotNull final BlockPos anchor)
    {
        for (final BlockPos existing : listFieldAnchors(colony))
        {
            if (existing.equals(anchor))
            {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static List<BuildingFootprint> collectFieldFootprints(@NotNull final IColony colony)
    {
        final IRegisteredStructureManager manager = getStructureManager(colony);
        final List<BuildingFootprint> footprints = new ArrayList<>();
        if (manager == null)
        {
            return footprints;
        }

        for (final var extension : manager.getBuildingExtensions(ColonyFieldSupport::isFarmField))
        {
            if (extension != null)
            {
                footprints.add(footprintForField(extension));
            }
        }
        return footprints;
    }

    public static boolean register(@NotNull final IColony colony, @NotNull final BlockPos fieldPos, @NotNull final BlockPos farmerPos)
    {
        final var world = colony.getWorld();
        if (world == null || world.isClientSide)
        {
            return false;
        }

        if (hasFieldAt(colony, fieldPos) || hasFieldAt(colony, snapToScarecrow(world, fieldPos)))
        {
            BeastofBurdenLog.warn("Cannot register duplicate field at {}", fieldPos);
            return false;
        }

        final IBuilding farmer = IColonyManager.getInstance().getBuilding(world, farmerPos);
        if (farmer == null)
        {
            BeastofBurdenLog.warn("Cannot register field at {}: farmer hut missing at {}", fieldPos, farmerPos);
            return false;
        }

        final BlockPos scarecrowPos = snapToScarecrow(world, fieldPos);
        final IBuildingExtension field = createFarmField(scarecrowPos);
        if (field == null)
        {
            BeastofBurdenLog.warn("Failed to construct FarmField at {}", scarecrowPos);
            return false;
        }

        if (field instanceof FarmField farmField)
        {
            farmField.setSeed(new ItemStack(Items.WHEAT_SEEDS));
        }

        final IRegisteredStructureManager manager = getStructureManager(colony);
        if (manager == null || !manager.addBuildingExtension(field))
        {
            BeastofBurdenLog.warn("Failed to add building extension for field at {}", scarecrowPos);
            return false;
        }

        if (!assignToFarmer(farmer, field))
        {
            field.setBuilding(farmerPos);
        }

        if (!field.isTaken())
        {
            BeastofBurdenLog.warn("Field at {} registered but not assigned to farmer {}; rolling back.", scarecrowPos, farmerPos);
            manager.removeBuildingExtension(ext -> scarecrowPos.equals(ext.getPosition()));
            return false;
        }

        colony.markDirty();
        return true;
    }

    @Nullable
    private static IBuildingExtension createFarmField(@NotNull final BlockPos fieldPos)
    {
        final var entry = BuildingExtensionRegistries.farmField.get();
        if (entry == null)
        {
            return null;
        }
        return entry.produceExtension(fieldPos);
    }

    @NotNull
    private static BlockPos snapToScarecrow(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        if (isScarecrow(world, pos))
        {
            return pos;
        }
        if (isScarecrow(world, pos.above()))
        {
            return pos.above();
        }
        if (isScarecrow(world, pos.below()))
        {
            return pos.below();
        }
        return pos;
    }

    public static boolean hasScarecrowAt(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        return isScarecrow(world, pos)
          || isScarecrow(world, pos.above())
          || isScarecrow(world, pos.below());
    }

    private static boolean isScarecrow(@NotNull final Level world, @NotNull final BlockPos pos)
    {
        return ModBlocks.blockScarecrow != null && world.getBlockState(pos).is(ModBlocks.blockScarecrow);
    }

    public static boolean isFarmerHut(@NotNull final IBuilding building)
    {
        final BuildingEntry farmer = ModBuildings.farmer.get();
        return farmer.getRegistryName().equals(building.getBuildingType().getRegistryName());
    }

    public static boolean isFarmField(@Nullable final IBuildingExtension extension)
    {
        if (extension == null)
        {
            return false;
        }

        final var farmFieldType = BuildingExtensionRegistries.farmField.get();
        return farmFieldType != null && farmFieldType.equals(extension.getBuildingExtensionType());
    }

    @NotNull
    private static BuildingFootprint footprintForField(@NotNull final IBuildingExtension extension)
    {
        final BlockPos anchor = extension.getPosition();
        int south = DEFAULT_FIELD_RANGE;
        int west = DEFAULT_FIELD_RANGE;
        int north = DEFAULT_FIELD_RANGE;
        int east = DEFAULT_FIELD_RANGE;

        if (extension instanceof FarmField farmField)
        {
            south = farmField.getRadius(Direction.SOUTH);
            west = farmField.getRadius(Direction.WEST);
            north = farmField.getRadius(Direction.NORTH);
            east = farmField.getRadius(Direction.EAST);
        }

        return BuildingFootprint.fromCorners(
          new BlockPos(anchor.getX() - west, anchor.getY(), anchor.getZ() - north),
          new BlockPos(anchor.getX() + east, anchor.getY(), anchor.getZ() + south)
        );
    }

    private static boolean assignToFarmer(@NotNull final IBuilding farmer, @NotNull final IBuildingExtension field)
    {
        for (final IBuildingModule module : farmer.getModulesByType(IBuildingModule.class))
        {
            if (module instanceof BuildingFarmer.FarmerFieldsModule fieldsModule)
            {
                fieldsModule.assignExtension(field);
                return field.isTaken();
            }
        }
        return false;
    }

    /**
     * MineColonies 1.1.873 caps a farmer at {@code buildingLevel} fields.
     */
    public static int maxAssignableFields(@NotNull final IBuilding farmer)
    {
        return Math.max(1, farmer.getBuildingLevel());
    }

    @Nullable
    private static IRegisteredStructureManager getStructureManager(@NotNull final IColony colony)
    {
        return ColonyBuildings.getStructureManager(colony);
    }
}
