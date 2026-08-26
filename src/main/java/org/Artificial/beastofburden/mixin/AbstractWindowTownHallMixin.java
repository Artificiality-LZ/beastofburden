package org.Artificial.beastofburden.mixin;

import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On MineColonies 1.1.1214+, the Town Hall main window hides the default module sidebar.
 * Force it back on so the Beast of Burden tab remains reachable.
 * <p>
 * {@code require = 0}: 1.1.873 has no {@code shouldRenderDefaultSidebar}; the inject is skipped there.
 */
@Mixin(value = AbstractWindowTownHall.class, remap = false)
public abstract class AbstractWindowTownHallMixin
{
    @Inject(method = "shouldRenderDefaultSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void beastofburden$showModuleSidebar(final CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(true);
    }
}
