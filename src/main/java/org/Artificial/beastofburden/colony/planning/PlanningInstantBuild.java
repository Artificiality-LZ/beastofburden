package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.ChangeStorage;
import com.ldtteam.structurize.util.PlacementSettings;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.CreativeBuildingStructureHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;

/**
 * Instantly completes hut blueprints for planning debug mode (no builder work orders).
 * Uses creative "pretty" placement ({@code fancyPlacement = true}) so substitution blocks resolve
 * to real materials instead of structural placeholders.
 */
public final class PlanningInstantBuild
{
    private static final int MAX_PASTE_STEPS = 2_000_000;

    private PlanningInstantBuild()
    {
    }

    public static boolean completeBuilding(
      @NotNull final IColony colony,
      @NotNull final IBuilding building,
      @NotNull final BlockPos location,
      @NotNull final Blueprint blueprint,
      @NotNull final Direction facing,
      final int targetLevel,
      @NotNull final String structurePack,
      @NotNull final String blueprintPath)
    {
        final Level world = colony.getWorld();
        if (!(world instanceof ServerLevel server))
        {
            return false;
        }

        if (!pasteBlueprint(server, location, blueprint, facing))
        {
            return false;
        }

        building.setStructurePack(structurePack);
        building.setBlueprintPath(blueprintPath);
        building.setBuildingLevel(targetLevel);
        building.setIsMirrored(false);

        if (building.getTileEntity() != null)
        {
            building.getTileEntity().setColony(colony);
        }

        building.onUpgradeComplete(targetLevel);
        building.calculateCorners();
        colony.markDirty();
        return true;
    }

    public static boolean pasteBlueprint(
      @NotNull final ServerLevel world,
      @NotNull final BlockPos location,
      @NotNull final Blueprint blueprint,
      @NotNull final Direction facing)
    {
        try
        {
            final Rotation rotation = BlockPosUtil.getRotationFromRotations(facingToRotations(facing));
            final PlacementSettings settings = new PlacementSettings(Mirror.NONE, rotation);
            final IStructureHandler handler = new CreativeBuildingStructureHandler(world, location, blueprint, settings, true);
            if (!handler.hasBluePrint())
            {
                return false;
            }

            final StructurePlacer placer = new StructurePlacer(handler);
            return pasteSynchronously(world, placer);
        }
        catch (final Exception ex)
        {
            BeastofBurdenLog.warn("Instant build paste failed at {}: {}", location, ex.toString());
            return false;
        }
    }

    private static boolean pasteSynchronously(@NotNull final ServerLevel world, @NotNull final StructurePlacer placer)
    {
        final ChangeStorage storage = new ChangeStorage(Component.literal("BeastOfBurden"), UUID.randomUUID());
        int structurePhase = 0;
        BlockPos currentPos = NULL_POS;

        for (int step = 0; step < MAX_PASTE_STEPS; step++)
        {
            if (!placer.isReady())
            {
                return false;
            }

            final StructurePhasePlacementResult result;
            switch (structurePhase)
            {
                case 0 -> result = placer.executeStructureStep(
                  world,
                  storage,
                  currentPos,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment((info, pos, handler) -> !BlockUtils.canBlockFloatInAir(info.getBlockInfo().getState())),
                  false
                );
                case 1 -> result = placer.executeStructureStep(
                  world,
                  storage,
                  currentPos,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment((info, pos, handler) -> !BlockUtils.isWeakSolidBlock(info.getBlockInfo().getState())),
                  false
                );
                case 2 ->
                {
                    final StructurePhasePlacementResult water = placer.clearWaterStep(world, currentPos);
                    currentPos = water.getIteratorPos();
                    if (water.getBlockResult().getResult() == BlockPlacementResult.Result.FINISHED)
                    {
                        currentPos = placer.getIterator().getProgressPos();
                    }
                    result = water;
                }
                case 3 -> result = placer.executeStructureStep(
                  world,
                  storage,
                  currentPos,
                  StructurePlacer.Operation.BLOCK_PLACEMENT,
                  () -> placer.getIterator().increment((info, pos, handler) -> BlockUtils.isAnySolid(info.getBlockInfo().getState())),
                  false
                );
                default -> result = placer.executeStructureStep(
                  world,
                  storage,
                  currentPos,
                  StructurePlacer.Operation.SPAWN_ENTITY,
                  () -> placer.getIterator().increment((info, pos, handler) -> info.getEntities().length == 0),
                  true
                );
            }

            if (structurePhase != 2)
            {
                currentPos = result.getIteratorPos();
            }

            if (result.getBlockResult().getResult() == BlockPlacementResult.Result.FINISHED)
            {
                structurePhase++;
                if (structurePhase > 4)
                {
                    placer.getHandler().onCompletion();
                    return true;
                }
            }
        }

        BeastofBurdenLog.warn("Instant build paste exceeded step limit");
        return false;
    }

    private static int facingToRotations(@NotNull final Direction facing)
    {
        return switch (facing)
        {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }
}
