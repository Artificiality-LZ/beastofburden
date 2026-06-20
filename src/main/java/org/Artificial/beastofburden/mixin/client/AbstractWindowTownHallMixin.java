package org.Artificial.beastofburden.mixin.client;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineColonies disables the module sidebar on all TownHall pages.
 * Re-enable it when the BeastOfBurden hiring module is present so players can open the hire UI.
 */
@Mixin(value = AbstractWindowTownHall.class, remap = false)
public abstract class AbstractWindowTownHallMixin
{
    @Inject(method = "shouldRenderDefaultSidebar", at = @At("HEAD"), cancellable = true, remap = false)
    private void beastofburden$enableBeastOfBurdenSidebar(final CallbackInfoReturnable<Boolean> cir)
    {
        final IBuildingView buildingView = ((AbstractBuildingWindowAccessor) this).beastofburden$getBuildingView();
        if (buildingView != null && buildingView.getModuleViewMatching(TownHallBeastofburdenModuleView.class, view -> true) != null)
        {
            cir.setReturnValue(true);
        }
    }
}
