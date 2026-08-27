package org.Artificial.beastofburden.client;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Injects the Beast of Burden button on the Town Hall Actions page.
 * <p>
 * Primary path is {@link ScreenEvent.Init.Post}: Forge userdev does not load this mod's
 * {@code [[mixins]]} config, so {@code WindowMainPageMixin} never runs there. The mixin
 * calls the same helper so a production jar that does apply mixins stays idempotent.
 */
@Mod.EventBusSubscriber(modid = Beastofburden.MODID, value = Dist.CLIENT)
public final class TownHallActionsBeastButton
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BUTTON_ID = "beastofburden";
    private static final String LABEL_KEY = "com.beastofburden.gui.townhall.beastofburden";
    private static final int BUTTON_X = 110;
    private static final int BUTTON_Y = 156;
    private static final int BUTTON_WIDTH = 129;
    private static final int BUTTON_HEIGHT = 17;
    private static final int TEXT_COLOR_BLACK = 0x000000;
    private static final ResourceLocation BUTTON_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/builderhut/builder_button_medium_large.png");

    @Nullable
    private static final MethodHandle BUILDING_VIEW_GETTER;

    @Nullable
    private static final Field BUILDING_VIEW_FIELD;

    static
    {
        final Field field = resolveBuildingViewField();
        BUILDING_VIEW_FIELD = field;
        BUILDING_VIEW_GETTER = field == null ? null : unreflectGetter(field);
    }

    private TownHallActionsBeastButton()
    {
        throw new IllegalStateException("Utility class");
    }

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof BOScreen screen))
        {
            return;
        }

        ensureBeastButton(screen.getWindow());
    }

    /**
     * Adds the Actions-page button if this window is {@link WindowMainPage} and the button is missing.
     */
    public static void ensureBeastButton(@Nullable final BOWindow window)
    {
        if (!(window instanceof WindowMainPage page))
        {
            return;
        }

        if (page.findPaneByID(BUTTON_ID) != null)
        {
            LOGGER.info("[{}] Town Hall Actions beast button already present.", Beastofburden.MODID);
            return;
        }

        final IBuildingView building = resolveBuildingView(page);
        if (building == null)
        {
            LOGGER.error("[{}] Cannot add Town Hall Actions beast button: building view is missing.", Beastofburden.MODID);
            return;
        }

        final ButtonImage button = new ButtonImage();
        button.setID(BUTTON_ID);
        button.setImage(BUTTON_TEXTURE, false);
        button.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setPosition(BUTTON_X, BUTTON_Y);
        button.setColors(TEXT_COLOR_BLACK);
        button.setText(Component.translatable(LABEL_KEY));
        button.setHandler(clicked -> openBeastWindow(building));
        page.addChild(button);
        LOGGER.info("[{}] Injected Town Hall Actions beast button at {},{}.", Beastofburden.MODID, BUTTON_X, BUTTON_Y);
    }

    private static void openBeastWindow(@NotNull final IBuildingView building)
    {
        final TownHallBeastofburdenModuleView module = building.getModuleViewByType(TownHallBeastofburdenModuleView.class);
        if (module == null)
        {
            LOGGER.error("[{}] Town Hall has no Beast of Burden module view; cannot open window.", Beastofburden.MODID);
            return;
        }

        module.getWindow().open();
    }

    @Nullable
    private static IBuildingView resolveBuildingView(@NotNull final AbstractModuleWindow window)
    {
        try
        {
            final Object value;
            if (BUILDING_VIEW_GETTER != null)
            {
                value = BUILDING_VIEW_GETTER.invoke(window);
            }
            else if (BUILDING_VIEW_FIELD != null)
            {
                value = BUILDING_VIEW_FIELD.get(window);
            }
            else
            {
                return null;
            }

            return value instanceof IBuildingView building ? building : null;
        }
        catch (final Throwable ex)
        {
            LOGGER.error("[{}] Failed to read Town Hall window building view: {}", Beastofburden.MODID, ex.toString());
            return null;
        }
    }

    @Nullable
    private static Field resolveBuildingViewField()
    {
        try
        {
            final Field field = AbstractModuleWindow.class.getDeclaredField("buildingView");
            field.setAccessible(true);
            return field;
        }
        catch (final NoSuchFieldException ex)
        {
            LOGGER.error(
              "[{}] Cannot access AbstractModuleWindow.buildingView; Town Hall Actions beast button disabled. {}",
              Beastofburden.MODID,
              ex.toString()
            );
            return null;
        }
    }

    @Nullable
    private static MethodHandle unreflectGetter(@NotNull final Field field)
    {
        try
        {
            return MethodHandles.lookup().unreflectGetter(field);
        }
        catch (final IllegalAccessException ignored)
        {
            return null;
        }
    }
}
