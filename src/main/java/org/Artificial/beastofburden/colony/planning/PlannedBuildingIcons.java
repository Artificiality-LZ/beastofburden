package org.Artificial.beastofburden.colony.planning;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Item stacks for plan editor icons.
 */
public final class PlannedBuildingIcons
{
    private static final ItemStack FIELD_ICON = new ItemStack(Items.WHEAT);

    private PlannedBuildingIcons()
    {
    }

    @NotNull
    public static ItemStack stackFor(@NotNull final PlannedBuildingType type)
    {
        final Block block = type.getEntry().getBuildingBlock();
        return new ItemStack(block);
    }

    @NotNull
    public static ItemStack fieldStack()
    {
        return FIELD_ICON.copy();
    }

    @NotNull
    public static ItemStack stackForRequirement(@NotNull final FixedPlanRequirement requirement)
    {
        if (requirement.getKind() == FixedPlanRequirement.Kind.FIELD)
        {
            return fieldStack();
        }
        return stackFor(requirement.getBuildingType());
    }
}
