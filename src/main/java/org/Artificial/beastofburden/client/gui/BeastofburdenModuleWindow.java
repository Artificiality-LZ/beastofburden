package org.Artificial.beastofburden.client.gui;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Gradient;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.WindowHireWorker;
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
    private static final String LABEL_ACTIVE_ITEM = "activeItem";
    private static final String ICON_ACTIVE = "activeIcon";
    private static final String GRADIENT_ACTIVE = "activeProgress";
    private static final String LABEL_HISTORY_META = "historyMeta";
    private static final String LABEL_HISTORY_ITEM = "historyItem";
    private static final String ICON_HISTORY = "historyIcon";
    private static final String LABEL_HISTORY_NOTE = "historyNote";

    private static final String BUTTON_HIRE = "hire";

    private final TownHallBeastofburdenModuleView moduleView;
    private int refreshCooldown = 20;

    public BeastofburdenModuleWindow(@NotNull final TownHallBeastofburdenModuleView moduleView)
    {
        super(moduleView, WINDOW_LAYOUT);
        this.moduleView = moduleView;
        registerButton(BUTTON_HIRE, this::hireClicked);
    }

    /**
     * Allow hiring beasts while the Town Hall is still under construction (level 0).
     */
    @Override
    protected void hireClicked(@NotNull final Button button)
    {
        new WindowHireWorker(buildingView.getColony(), buildingView.getPosition()).open();
    }

    @Override
    public void onOpened()
    {
        registerButton(BUTTON_HIRE, this::hireClicked);
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
                final Text itemLine = rowPane.findPaneOfTypeByID(LABEL_ACTIVE_ITEM, Text.class);
                final ItemIcon icon = rowPane.findPaneOfTypeByID(ICON_ACTIVE, ItemIcon.class);
                final Gradient progress = rowPane.findPaneOfTypeByID(GRADIENT_ACTIVE, Gradient.class);
                if (line == null || itemLine == null || progress == null)
                {
                    return;
                }

                if (rows.isEmpty())
                {
                    if (icon != null)
                    {
                        icon.setVisible(false);
                    }
                    line.setText(Component.translatable("com.beastofburden.gui.townhall.no_workers"));
                    itemLine.setText(Component.empty());
                    progress.setSize(0, progress.getHeight());
                    return;
                }

                final BeastWorkStatus status = rows.get(index);
                line.setText(formatActiveMeta(status));

                if (status.getPhase() == BeastWorkPhase.IDLE)
                {
                    if (icon != null)
                    {
                        icon.setVisible(false);
                    }
                    itemLine.setText(Component.translatable("com.beastofburden.gui.townhall.idle"));
                    progress.setSize(0, progress.getHeight());
                    return;
                }

                final ItemStack stack = itemStack(status.getItemId(), status.getCount());
                if (icon != null)
                {
                    icon.setVisible(true);
                    icon.setItem(stack);
                }
                itemLine.setText(formatItemLine(stack, status.getCount()));

                if (status.getPhase() == BeastWorkPhase.GENERATING && status.getRequiredTicks() > 0)
                {
                    final float percent = status.getProgressPercent();
                    progress.setSize((int) (142 * percent), progress.getHeight());
                    line.setText(Component.translatable(
                      "com.beastofburden.gui.townhall.active_meta_progress",
                      status.getCitizenName(),
                      Component.translatable("com.beastofburden.gui.townhall.phase." + status.getPhase().name().toLowerCase(Locale.ROOT)),
                      formatSeconds(status.getProgressTicks()),
                      formatSeconds(status.getRequiredTicks())
                    ));
                }
                else if (status.getPhase() == BeastWorkPhase.DELIVERING)
                {
                    progress.setSize(142, progress.getHeight());
                    itemLine.setText(Component.translatable(
                      "com.beastofburden.gui.townhall.delivering_item",
                      formatItemLine(stack, status.getCount())
                    ));
                }
                else
                {
                    progress.setSize(0, progress.getHeight());
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
                final Text meta = rowPane.findPaneOfTypeByID(LABEL_HISTORY_META, Text.class);
                final Text itemLine = rowPane.findPaneOfTypeByID(LABEL_HISTORY_ITEM, Text.class);
                final ItemIcon icon = rowPane.findPaneOfTypeByID(ICON_HISTORY, ItemIcon.class);
                if (meta == null || itemLine == null)
                {
                    return;
                }

                if (history.isEmpty())
                {
                    if (icon != null)
                    {
                        icon.setVisible(false);
                    }
                    meta.setText(Component.translatable("com.beastofburden.gui.townhall.no_history"));
                    itemLine.setText(Component.empty());
                    return;
                }

                final BeastWorkLogEntry entry = history.get(index);
                final ItemStack stack = itemStack(entry.getItemId(), entry.getCount());
                if (icon != null)
                {
                    icon.setVisible(true);
                    icon.setItem(stack);
                }

                meta.setText(formatHistoryMeta(entry));
                itemLine.setText(formatHistoryItem(entry, stack));
            }
        });
    }

    @NotNull
    private Component formatActiveMeta(@NotNull final BeastWorkStatus status)
    {
        return Component.translatable(
          "com.beastofburden.gui.townhall.active_meta",
          status.getCitizenName(),
          Component.translatable("com.beastofburden.gui.townhall.phase." + status.getPhase().name().toLowerCase(Locale.ROOT))
        );
    }

    @NotNull
    private Component formatHistoryMeta(@NotNull final BeastWorkLogEntry entry)
    {
        final String actionKey = "com.beastofburden.gui.townhall.action." + entry.getAction().name().toLowerCase(Locale.ROOT);
        return Component.translatable(
          "com.beastofburden.gui.townhall.history_meta",
          entry.getColonyDay(),
          entry.getCitizenName(),
          Component.translatable(actionKey)
        );
    }

    @NotNull
    private Component formatHistoryItem(@NotNull final BeastWorkLogEntry entry, @NotNull final ItemStack stack)
    {
        if (entry.getAction() == BeastWorkLogAction.DELIVERED && entry.getDurationTicks() > 0)
        {
            return Component.translatable(
              "com.beastofburden.gui.townhall.history_item_timed",
              stack.getHoverName(),
              entry.getCount(),
              formatSeconds(entry.getDurationTicks())
            );
        }

        return Component.translatable(
          "com.beastofburden.gui.townhall.history_item",
          stack.getHoverName(),
          entry.getCount()
        );
    }

    @NotNull
    private static Component formatItemLine(@NotNull final ItemStack stack, final int count)
    {
        return Component.translatable(
          "com.beastofburden.gui.townhall.item_count",
          stack.getHoverName(),
          count
        );
    }

    @NotNull
    private static ItemStack itemStack(@NotNull final ResourceLocation itemId, final int count)
    {
        return new ItemStack(BuiltInRegistries.ITEM.get(itemId), Math.max(1, count));
    }

    @NotNull
    private static String formatSeconds(final int ticks)
    {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0D);
    }
}
