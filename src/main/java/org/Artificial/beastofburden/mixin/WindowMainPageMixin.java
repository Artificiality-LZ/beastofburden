package org.Artificial.beastofburden.mixin;

import com.ldtteam.blockui.controls.ButtonImage;
import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a Beast of Burden button on the Town Hall Actions page.
 * <p>
 * MineColonies 1.1.1214+ hides the module sidebar on Town Hall windows, so this is the
 * version-stable entry point (same {@code WindowMainPage} / layout on 873 and 1214).
 */
@Mixin(value = WindowMainPage.class, remap = false)
public abstract class WindowMainPageMixin
{
    private static final String BUTTON_ID = "beastofburden";
    private static final String LABEL_KEY = "com.beastofburden.gui.townhall.beastofburden";
    private static final ResourceLocation BUTTON_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/builderhut/builder_button_medium_large.png");

    @Inject(method = "<init>", at = @At("RETURN"))
    private void beastofburden$addBeastButton(final BuildingTownHall.View building, final CallbackInfo ci)
    {
        final WindowMainPage self = (WindowMainPage) (Object) this;
        if (self.findPaneByID(BUTTON_ID) != null)
        {
            return;
        }

        final ButtonImage button = new ButtonImage();
        button.setID(BUTTON_ID);
        button.setImage(BUTTON_TEXTURE, false);
        button.setSize(129, 17);
        button.setPosition(110, 156);
        button.setText(Component.translatable(LABEL_KEY));
        button.setHandler(clicked -> {
            final TownHallBeastofburdenModuleView module =
              building.getModuleViewByType(TownHallBeastofburdenModuleView.class);
            if (module != null)
            {
                module.getWindow().open();
            }
        });
        self.addChild(button);
    }
}
