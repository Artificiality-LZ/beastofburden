package org.Artificial.beastofburden.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.Artificial.beastofburden.colony.planning.PlanStepFormatter;
import org.Artificial.beastofburden.colony.planning.PlannedBuildingIcons;
import org.Artificial.beastofburden.colony.planning.PlannedBuildingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Searchable building picker overlay for the plan editor.
 */
public final class BuildingPickerScreen extends Screen
{
    public interface SelectionHandler
    {
        void onBuilding(@NotNull PlannedBuildingType type);

        void onFieldStep();
    }

    private static final int ROW_HEIGHT = 22;
    private static final int MARGIN = 16;
    private static final PlannedBuildingType[] TYPES = Arrays.stream(PlannedBuildingType.values())
      .filter(type -> type != PlannedBuildingType.TOWN_HALL)
      .toArray(PlannedBuildingType[]::new);

    private final Screen parent;
    private final SelectionHandler handler;
    private final List<PickerEntry> filtered = new ArrayList<>();
    private EditBox searchBox;
    private String query = "";
    private int scrollOffset;

    public BuildingPickerScreen(@NotNull final Screen parent, @NotNull final SelectionHandler handler)
    {
        super(Component.translatable("com.beastofburden.gui.plan_editor.pick_building"));
        this.parent = parent;
        this.handler = handler;
        rebuildFiltered("");
    }

    @Override
    protected void init()
    {
        clearWidgets();
        scrollOffset = Math.min(scrollOffset, maxScroll());

        final int panelWidth = panelWidth();
        final int panelLeft = panelLeft(panelWidth);
        final int panelTop = panelTop();
        final int listTop = listTop(panelTop);
        final int listBottom = listBottom(panelTop, panelHeight());

        searchBox = new EditBox(font, panelLeft + 8, panelTop + 8, panelWidth - 16, 18, Component.empty());
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("com.beastofburden.gui.plan_editor.search_building"));
        searchBox.setResponder(text -> {
            query = text;
            scrollOffset = 0;
            rebuildFiltered(text);
            init();
        });
        addRenderableWidget(searchBox);

        final int visibleRows = visibleRowCount(listTop, listBottom);
        for (int i = 0; i < visibleRows; i++)
        {
            final int index = scrollOffset + i;
            if (index >= filtered.size())
            {
                break;
            }

            final PickerEntry entry = filtered.get(index);
            final int rowY = listTop + i * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.empty(), button -> select(entry))
              .bounds(panelLeft + 4, rowY + 1, panelWidth - 8, ROW_HEIGHT - 2)
              .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
          .bounds(width / 2 - 50, height - 28, 100, 20)
          .build());
    }

    private void select(@NotNull final PickerEntry entry)
    {
        if (entry.fieldStep())
        {
            handler.onFieldStep();
        }
        else
        {
            handler.onBuilding(entry.type());
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(@NotNull final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        renderBackground(graphics);

        final int panelWidth = panelWidth();
        final int panelLeft = panelLeft(panelWidth);
        final int panelTop = panelTop();
        final int panelHeight = panelHeight();
        final int listTop = listTop(panelTop);
        final int listBottom = listBottom(panelTop, panelHeight);

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0101010);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFF404040);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        graphics.enableScissor(panelLeft + 1, listTop, panelLeft + panelWidth - 1, listBottom);
        graphics.fill(panelLeft + 1, listTop, panelLeft + panelWidth - 1, listBottom, 0xF0101010);

        final int visibleRows = visibleRowCount(listTop, listBottom);
        for (int i = 0; i < visibleRows; i++)
        {
            final int index = scrollOffset + i;
            if (index >= filtered.size())
            {
                break;
            }

            final PickerEntry entry = filtered.get(index);
            final int rowY = listTop + i * ROW_HEIGHT;
            graphics.fill(panelLeft + 4, rowY + 1, panelLeft + panelWidth - 4, rowY + ROW_HEIGHT - 1, 0xF0101010);
            graphics.renderItem(entry.icon(), panelLeft + 8, rowY + 3);
            graphics.drawString(font, entry.label(), panelLeft + 28, rowY + 7, 0xFFFFFF);
        }

        graphics.disableScissor();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta)
    {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll(), scrollOffset - delta));
        init();
        return true;
    }

    private int panelWidth()
    {
        return Math.min(280, width - MARGIN * 2);
    }

    private int panelLeft(final int panelWidth)
    {
        return (width - panelWidth) / 2;
    }

    private int panelTop()
    {
        return 32;
    }

    private int panelHeight()
    {
        return height - panelTop() - 40;
    }

    private int listTop(final int panelTop)
    {
        return panelTop + 32;
    }

    private int listBottom(final int panelTop, final int panelHeight)
    {
        return panelTop + panelHeight - 8;
    }

    private int visibleRowCount(final int listTop, final int listBottom)
    {
        return Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
    }

    private int maxScroll()
    {
        final int panelTop = panelTop();
        final int listTop = listTop(panelTop);
        final int listBottom = listBottom(panelTop, panelHeight());
        return Math.max(0, filtered.size() - visibleRowCount(listTop, listBottom));
    }

    private void rebuildFiltered(@NotNull final String rawQuery)
    {
        filtered.clear();
        final String q = rawQuery.trim().toLowerCase(Locale.ROOT);
        final Component fieldLabel = Component.translatable("com.beastofburden.gui.plan_editor.field_step");
        if (q.isEmpty() || fieldLabel.getString().toLowerCase(Locale.ROOT).contains(q) || "field".contains(q))
        {
            filtered.add(PickerEntry.field());
        }

        for (final PlannedBuildingType type : TYPES)
        {
            final String name = PlanStepFormatter.buildingName(type).getString().toLowerCase(Locale.ROOT);
            final String id = type.getSchematicId().toLowerCase(Locale.ROOT);
            if (q.isEmpty() || name.contains(q) || id.contains(q))
            {
                filtered.add(PickerEntry.building(type));
            }
        }
    }

    private record PickerEntry(@NotNull Component label, @NotNull net.minecraft.world.item.ItemStack icon, @Nullable PlannedBuildingType type, boolean fieldStep)
    {
        @NotNull
        private static PickerEntry building(@NotNull final PlannedBuildingType type)
        {
            return new PickerEntry(
              PlanStepFormatter.buildingName(type),
              PlannedBuildingIcons.stackFor(type),
              type,
              false
            );
        }

        @NotNull
        private static PickerEntry field()
        {
            return new PickerEntry(
              Component.translatable("com.beastofburden.gui.plan_editor.field_step"),
              PlannedBuildingIcons.fieldStack(),
              null,
              true
            );
        }
    }
}
