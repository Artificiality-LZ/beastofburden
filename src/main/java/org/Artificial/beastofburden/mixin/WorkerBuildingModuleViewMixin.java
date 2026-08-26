package org.Artificial.beastofburden.mixin;

import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import net.minecraft.network.chat.Component;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On MineColonies 1.1.1214+, {@code IBuildingModuleView.getDesc()} returns {@link Component}.
 * Our module view still overrides the 873 {@code String} signature, which does not override the
 * newer method. Inject the translated title when the Component overload is present.
 * <p>
 * {@code require = 0}: 1.1.873 uses {@code String getDesc()}; this inject is skipped there.
 */
@Mixin(value = WorkerBuildingModuleView.class, remap = false)
public abstract class WorkerBuildingModuleViewMixin
{
    private static final String GUI_DESC_KEY = "com.beastofburden.gui.townhall.beastofburden";

    @Inject(method = "getDesc()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true, require = 0)
    private void beastofburden$componentDesc(final CallbackInfoReturnable<Component> cir)
    {
        if ((Object) this instanceof TownHallBeastofburdenModuleView)
        {
            cir.setReturnValue(Component.translatable(GUI_DESC_KEY));
        }
    }
}
