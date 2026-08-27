package org.Artificial.beastofburden.mixin;

import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import org.Artificial.beastofburden.client.TownHallActionsBeastButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional early inject of the Beast of Burden button on the Town Hall Actions page.
 * <p>
 * The reliable entry is {@link TownHallActionsBeastButton} via {@code ScreenEvent.Init.Post}.
 * This mixin only calls the same helper with the constructor's {@code BuildingTownHall.View}
 * so a jar that does apply mixins stays idempotent on both 873 and 1214+.
 */
@Mixin(value = WindowMainPage.class, remap = false)
public abstract class WindowMainPageMixin
{
    @Inject(method = "<init>", at = @At("RETURN"))
    private void beastofburden$addBeastButton(final BuildingTownHall.View building, final CallbackInfo ci)
    {
        TownHallActionsBeastButton.ensureBeastButton((WindowMainPage) (Object) this, building);
    }
}
