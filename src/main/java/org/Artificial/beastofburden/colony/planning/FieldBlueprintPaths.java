package org.Artificial.beastofburden.colony.planning;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.Artificial.beastofburden.util.BeastofBurdenLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the MineColonies basicfield decoration blueprint for autonomous field placement.
 */
public final class FieldBlueprintPaths
{
    private static final String[] PATH_CANDIDATES = {
      "infrastructure/fields/basicfield.blueprint",
      "agriculture/fields/basicfield.blueprint",
      "decorations/fields/basicfield.blueprint",
      "decorations/basicfield.blueprint",
      "fields/basicfield.blueprint"
    };

    private FieldBlueprintPaths()
    {
    }

    /**
     * @param pack    structure pack name
     * @param path    blueprint path within the pack
     * @param blueprint loaded blueprint
     */
    public record LoadedFieldBlueprint(@NotNull String pack, @NotNull String path, @NotNull Blueprint blueprint)
    {
    }

    @Nullable
    public static LoadedFieldBlueprint loadBasicField(@NotNull final IColony colony)
    {
        for (final String pack : packCandidates(colony))
        {
            for (final String path : PATH_CANDIDATES)
            {
                if (!StructurePacks.hasPack(pack))
                {
                    continue;
                }

                try
                {
                    final Blueprint blueprint = StructurePacks.getBlueprint(pack, path);
                    if (blueprint != null)
                    {
                        BeastofBurdenLog.info("Resolved basicfield blueprint: {}/{}", pack, path);
                        return new LoadedFieldBlueprint(pack, path, blueprint);
                    }
                }
                catch (final Exception ex)
                {
                    BeastofBurdenLog.warn("Failed to load field blueprint {}/{}: {}", pack, path, ex.toString());
                }
            }
        }

        BeastofBurdenLog.warn("No basicfield blueprint found for colony {}", colony.getID());
        return null;
    }

    @Nullable
    public static BlockPos resolveScarecrowAnchor(
      @NotNull final Blueprint blueprint,
      @NotNull final BlockPos pasteAnchor,
      @NotNull final Direction facing)
    {
        final Block scarecrow = ModBlocks.blockScarecrow;
        if (scarecrow == null)
        {
            return pasteAnchor;
        }

        final BlockPos rotatedPrimary = BuildingFootprint.rotateOffset(blueprint.getPrimaryBlockOffset(), facing);
        final BlockPos zero = pasteAnchor.subtract(rotatedPrimary);
        BlockPos scarecrowPos = null;

        for (final BlockInfo info : blueprint.getBlockInfoAsMap().values())
        {
            if (info == null || !info.getState().is(scarecrow))
            {
                continue;
            }

            final BlockPos worldPos = zero.offset(BuildingFootprint.rotateOffset(info.getPos(), facing));
            if (scarecrowPos == null || info.getPos().equals(blueprint.getPrimaryBlockOffset()))
            {
                scarecrowPos = worldPos;
            }
        }

        return scarecrowPos != null ? scarecrowPos.below() : pasteAnchor.below();
    }

    @NotNull
    private static List<String> packCandidates(@NotNull final IColony colony)
    {
        final Set<String> packs = new LinkedHashSet<>();
        addIfUsable(packs, StructurePackResolver.resolveColonyPack(colony));
        addIfUsable(packs, Constants.STORAGE_STYLE);
        addIfUsable(packs, Constants.DEFAULT_STYLE);
        return new ArrayList<>(packs);
    }

    private static void addIfUsable(@NotNull final Set<String> packs, @Nullable final String pack)
    {
        if (pack != null && !pack.isEmpty() && StructurePacks.hasPack(pack))
        {
            packs.add(pack);
        }
    }
}
