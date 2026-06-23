package org.Artificial.beastofburden.mixin.client;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineColonies disables the module sidebar on all TownHall pages (MC 1.1.1214+).
 * Re-enable it when the BeastOfBurden hiring module is present so players can open the hire UI.
 * <p>
 * {@code require = 0}: skip injection on older MineColonies where {@code shouldRenderDefaultSidebar} does not exist.
 */
@Mixin(value = AbstractWindowTownHall.class, remap = false)
public abstract class AbstractWindowTownHallMixin
{
    @Inject(method = "shouldRenderDefaultSidebar", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void beastofburden$enableBeastOfBurdenSidebar(final CallbackInfoReturnable<Boolean> cir)
    {
        final IBuildingView buildingView = ((AbstractBuildingWindowAccessor) this).beastofburden$getBuildingView();
        if (buildingView != null && buildingView.getModuleViewMatching(TownHallBeastofburdenModuleView.class, view -> true) != null)
        {
            cir.setReturnValue(true);
        }
    }
}
