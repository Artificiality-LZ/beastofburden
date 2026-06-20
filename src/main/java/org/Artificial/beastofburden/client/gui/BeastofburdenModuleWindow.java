package org.Artificial.beastofburden.client.gui;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Gradient;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.modules.building.SpecialAssignmentModuleWindow;
import com.minecolonies.core.network.messages.server.colony.building.MarkBuildingDirtyMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.colony.buildings.modules.TownHallBeastofburdenModuleView;
import org.Artificial.beastofburden.colony.work.BeastWorkLogAction;
import org.Artificial.beastofburden.colony.work.BeastWorkLogEntry;
import org.Artificial.beastofburden.colony.work.BeastWorkPhase;
import org.Artificial.beastofburden.colony.work.BeastWorkSnapshot;
import org.Artificial.beastofburden.colony.work.BeastWorkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Town Hall beast-of-burden tab: hiring, live work status, and work history.
 */
public class BeastofburdenModuleWindow extends SpecialAssignmentModuleWindow
{
    private static final ResourceLocation WINDOW_LAYOUT =
      ResourceLocation.fromNamespaceAndPath(Beastofburden.MODID, "gui/layouthuts/layoutbeastofburden.xml");

    private static final String LIST_ACTIVE = "activeWork";
    private static final String LIST_HISTORY = "history";
    private static final String LABEL_ACTIVE = "activeLine";
    private static final String LABEL_ACTIVE_TIME = "activeTime";
    private static final String GRADIENT_ACTIVE = "activeProgress";
    private static final String LABEL_HISTORY = "historyLine";
    private static final String LABEL_HISTORY_NOTE = "historyNote";

    private final TownHallBeastofburdenModuleView moduleView;
    private int refreshCooldown = 20;

    public BeastofburdenModuleWindow(@NotNull final TownHallBeastofburdenModuleView moduleView)
    {
        super(moduleView, WINDOW_LAYOUT);
        this.moduleView = moduleView;
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        refreshWorkPanels();
    }

    @Override
    public void onUpdate()
    {
        if (refreshCooldown > 0 && --refreshCooldown == 0)
        {
            Network.getNetwork().sendToServer(new MarkBuildingDirtyMessage(buildingView));
        }

        if (moduleView.checkAndResetWorkUpdated())
        {
            refreshCooldown = 20;
            refreshWorkPanels();
        }

        super.onUpdate();
    }

    private void refreshWorkPanels()
    {
        final BeastWorkSnapshot snapshot = moduleView.getWorkSnapshot();
        final Text historyNote = findPaneOfTypeByID(LABEL_HISTORY_NOTE, Text.class);
        if (historyNote != null)
        {
            if (snapshot.getHistoryDays() <= 0)
            {
                historyNote.setText(Component.translatable("com.beastofburden.gui.townhall.history.all"));
            }
            else
            {
                historyNote.setText(Component.translatable(
                  "com.beastofburden.gui.townhall.history.days",
                  snapshot.getHistoryDays(),
                  snapshot.getColonyDay()
                ));
            }
        }

        refreshActiveWork(snapshot.getActiveWork());
        refreshHistory(snapshot.getHistory());
    }

    private void refreshActiveWork(@NotNull final List<BeastWorkStatus> activeWork)
    {
        final ScrollingList list = findPaneOfTypeByID(LIST_ACTIVE, ScrollingList.class);
        if (list == null)
        {
            return;
        }

        final List<BeastWorkStatus> rows = new ArrayList<>(activeWork);
        list.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return Math.max(1, rows.size());
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final Text line = rowPane.findPaneOfTypeByID(LABEL_ACTIVE, Text.class);
                final Text time = rowPane.findPaneOfTypeByID(LABEL_ACTIVE_TIME, Text.class);
                final Gradient progress = rowPane.findPaneOfTypeByID(GRADIENT_ACTIVE, Gradient.class);
                if (line == null || time == null || progress == null)
                {
                    return;
                }

                if (rows.isEmpty())
                {
                    line.setText(Component.translatable("com.beastofburden.gui.townhall.no_workers"));
                    time.setText(Component.empty());
                    progress.setSize(0, progress.getHeight());
                    return;
                }

                final BeastWorkStatus status = rows.get(index);
                line.setText(formatActiveLine(status));

                if (status.getPhase() == BeastWorkPhase.GENERATING && status.getRequiredTicks() > 0)
                {
                    final float percent = status.getProgressPercent();
                    progress.setSize((int) (rowPane.getWidth() * percent), progress.getHeight());
                    time.setText(Component.translatable(
                      "com.beastofburden.gui.townhall.progress_time",
                      formatSeconds(status.getProgressTicks()),
                      formatSeconds(status.getRequiredTicks())
                    ));
                }
                else if (status.getPhase() == BeastWorkPhase.DELIVERING)
                {
                    progress.setSize(rowPane.getWidth(), progress.getHeight());
                    time.setText(Component.translatable("com.beastofburden.gui.townhall.delivering"));
                }
                else
                {
                    progress.setSize(0, progress.getHeight());
                    time.setText(Component.translatable("com.beastofburden.gui.townhall.idle"));
                }
            }
        });
    }

    private void refreshHistory(@NotNull final List<BeastWorkLogEntry> history)
    {
        final ScrollingList list = findPaneOfTypeByID(LIST_HISTORY, ScrollingList.class);
        if (list == null)
        {
            return;
        }

        list.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return Math.max(1, history.size());
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final Text line = rowPane.findPaneOfTypeByID(LABEL_HISTORY, Text.class);
                if (line == null)
                {
                    return;
                }

                if (history.isEmpty())
                {
                    line.setText(Component.translatable("com.beastofburden.gui.townhall.no_history"));
                    return;
                }

                line.setText(formatHistoryLine(history.get(index)));
            }
        });
    }

    @NotNull
    private Component formatActiveLine(@NotNull final BeastWorkStatus status)
    {
        final ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(status.getItemId()), status.getCount());
        final String itemName = status.getPhase() == BeastWorkPhase.IDLE
          ? "-"
          : stack.getHoverName().getString() + " x" + status.getCount();

        return Component.translatable(
          "com.beastofburden.gui.townhall.active_line",
          status.getCitizenName(),
          Component.translatable("com.beastofburden.gui.townhall.phase." + status.getPhase().name().toLowerCase(Locale.ROOT)),
          itemName
        );
    }

    @NotNull
    private Component formatHistoryLine(@NotNull final BeastWorkLogEntry entry)
    {
        final ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.getItemId()), entry.getCount());
        final String actionKey = "com.beastofburden.gui.townhall.action." + entry.getAction().name().toLowerCase(Locale.ROOT);
        final String duration = entry.getAction() == BeastWorkLogAction.DELIVERED && entry.getDurationTicks() > 0
          ? formatSeconds(entry.getDurationTicks())
          : "-";

        return Component.translatable(
          "com.beastofburden.gui.townhall.history_line",
          entry.getColonyDay(),
          entry.getCitizenName(),
          Component.translatable(actionKey),
          stack.getHoverName().getString(),
          entry.getCount(),
          duration
        );
    }

    @NotNull
    private static String formatSeconds(final int ticks)
    {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0D);
    }
}
