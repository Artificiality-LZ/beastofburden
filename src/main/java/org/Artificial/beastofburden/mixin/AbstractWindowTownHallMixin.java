package org.Artificial.beastofburden.mixin;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Image;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a Town Hall bookmark (same left wax-seal column as Info / Permissions / Citizens)
 * that opens the Beast of Burden module window.
 * <p>
 * This is the correct entry on 1.1.1214+ where {@code shouldRenderDefaultSidebar()} is false
 * so module side-tabs never appear on Town Hall pages. The same constructor exists on 1.1.873.
 */
@Mixin(value = AbstractWindowTownHall.class, remap = false)
public abstract class AbstractWindowTownHallMixin
{
    private static final String BUTTON_ID = "beastofburden";
    private static final String BUTTON_EXT_ID = "beastofburdenExt";
    private static final String RIBBON_ID = "beastofburden0";
    private static final String GUI_DESC_KEY = "com.beastofburden.gui.townhall.beastofburden";

    /** Y of the settings wax seal in {@code windowtownhall.xml}; next slot is +24. */
    private static final int BOOKMARK_Y = 239;

    @Inject(
      method = "<init>(Lcom/minecolonies/core/colony/buildings/workerbuildings/BuildingTownHall$View;Ljava/lang/String;)V",
      at = @At("RETURN")
    )
    private void beastofburden$addBookmark(
      final BuildingTownHall.View townHall,
      final String page,
      final CallbackInfo ci)
    {
        final AbstractWindowTownHall self = (AbstractWindowTownHall) (Object) this;
        final AbstractWindowSkeleton skeleton = (AbstractWindowSkeleton) (Object) this;

        final Image ribbon = new Image();
        ribbon.setID(RIBBON_ID);
        ribbon.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/bookmark_short_ribbon_06.png"), false);
        ribbon.setPosition(56, BOOKMARK_Y + 2);
        ribbon.setSize(31, 14);
        self.addChild(ribbon);

        final ButtonImage hover = new ButtonImage();
        hover.setID(BUTTON_EXT_ID);
        hover.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/bookmark_medium_ribbon_06.png"), false);
        hover.setPosition(-17, BOOKMARK_Y);
        hover.setSize(104, 14);
        hover.setVisible(false);
        hover.setTextAlignment(Alignment.MIDDLE_LEFT);
        hover.setTextOffset(8, 1);
        hover.setColors(0xFFFFFF);
        hover.setText(Component.translatable(GUI_DESC_KEY));
        self.addChild(hover);

        final ButtonImage button = new ButtonImage();
        button.setID(BUTTON_ID);
        button.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/modules/entity.png"), false);
        button.setPosition(62, BOOKMARK_Y - 2);
        button.setSize(17, 17);
        button.setHoverPane(hover);
        self.addChild(button);

        skeleton.registerButton(BUTTON_ID, () -> openBeastWindow(townHall));
    }

    private static void openBeastWindow(final BuildingTownHall.View townHall)
    {
        final IBuildingView building = townHall;
        final TownHallBeastofburdenModuleView module =
          building.getModuleViewMatching(TownHallBeastofburdenModuleView.class, view -> true);
        if (module != null)
        {
            module.getWindow().open();
        }
    }
}
