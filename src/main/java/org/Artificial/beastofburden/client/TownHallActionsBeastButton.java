package org.Artificial.beastofburden.client;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
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

import java.lang.reflect.Field;

/**
 * Injects the Beast of Burden button on the Town Hall Actions page.
 * <p>
 * Primary path is {@link ScreenEvent.Init.Post} because Forge userdev often does not apply
 * this mod's {@code [[mixins]]} config. Optional {@code WindowMainPageMixin} calls the same
 * helper with the constructor's building view.
 * <p>
 * Do not mention {@code AbstractModuleWindow} in this class: on MineColonies 1.1.1214+
 * {@code WindowMainPage} no longer extends it, and a bytecode link causes {@link VerifyError}
 * at mod construct time.
 */
@Mod.EventBusSubscriber(modid = Beastofburden.MODID, value = Dist.CLIENT)
public final class TownHallActionsBeastButton
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WINDOW_MAIN_PAGE = "com.minecolonies.core.client.gui.townhall.WindowMainPage";
    private static final String BUILDING_VIEW_FIELD = "buildingView";
    private static final String BUTTON_ID = "beastofburden";
    private static final String LABEL_KEY = "com.beastofburden.gui.townhall.beastofburden";
    private static final int BUTTON_X = 110;
    private static final int BUTTON_Y = 156;
    private static final int BUTTON_WIDTH = 129;
    private static final int BUTTON_HEIGHT = 17;
    private static final int TEXT_COLOR_BLACK = 0x000000;
    private static final ResourceLocation BUTTON_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/builderhut/builder_button_medium_large.png");

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

        ensureBeastButton(screen.getWindow(), null);
    }

    /**
     * Adds the Actions-page button if this window is Town Hall Actions and the button is missing.
     *
     * @param window   candidate BlockUI window
     * @param building Town Hall view when already known (mixin constructor); otherwise resolved by field walk
     */
    public static void ensureBeastButton(@Nullable final BOWindow window, @Nullable final IBuildingView building)
    {
        if (window == null || !WINDOW_MAIN_PAGE.equals(window.getClass().getName()))
        {
            return;
        }

        if (window.findPaneByID(BUTTON_ID) != null)
        {
            LOGGER.info("[{}] Town Hall Actions beast button already present.", Beastofburden.MODID);
            return;
        }

        final IBuildingView resolved = building != null ? building : resolveBuildingView(window);
        if (resolved == null)
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
        button.setHandler(clicked -> openBeastWindow(resolved));
        window.addChild(button);
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
    private static IBuildingView resolveBuildingView(@NotNull final Object window)
    {
        Class<?> type = window.getClass();
        while (type != null && type != Object.class)
        {
            try
            {
                final Field field = type.getDeclaredField(BUILDING_VIEW_FIELD);
                field.setAccessible(true);
                final Object value = field.get(window);
                return value instanceof IBuildingView view ? view : null;
            }
            catch (final NoSuchFieldException ignored)
            {
                type = type.getSuperclass();
            }
            catch (final Throwable ex)
            {
                LOGGER.error("[{}] Failed to read Town Hall window building view: {}", Beastofburden.MODID, ex.toString());
                return null;
            }
        }

        return null;
    }
}
