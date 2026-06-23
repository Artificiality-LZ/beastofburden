package org.Artificial.beastofburden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.Artificial.beastofburden.config.ConfigPersistence;
import org.Artificial.beastofburden.config.ConfigSnapshot;
import org.Artificial.beastofburden.network.ModNetwork;
import org.Artificial.beastofburden.network.SaveBeastConfigMessage;
import org.Artificial.beastofburden.util.ItemValueRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editable BeastOfBurden config screen with item-value management.
 */
public class BeastofburdenConfigScreen extends Screen
{
    private enum ConfigPage
    {
        GENERAL,
        CONFIGURED,
        ADD_ITEM
    }

    private static final int MARGIN = 20;
    private static final int FOOTER_RESERVE = 36;
    private static final int TAB_Y = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_BOTTOM = TAB_Y + TAB_HEIGHT;
    private static final int CONTENT_TOP = 50;
    private static final int ADD_PAGE_TOP = TAB_BOTTOM + 10;
    /** Pixels from row top to the input field (label uses the first ~9px). */
    private static final int GENERAL_LABEL_TO_FIELD = 10;
    private static final int GENERAL_FIELD_HEIGHT = 18;
    /** Gap between the bottom of a field and the next row's label. */
    private static final int GENERAL_ROW_SPACING = 10;
    private static final int GENERAL_ROW_HEIGHT = GENERAL_LABEL_TO_FIELD + GENERAL_FIELD_HEIGHT + GENERAL_ROW_SPACING;
    private static final int GENERAL_FIELD_ROWS = 4;
    private static final int GENERAL_DERIVE_ROW_HEIGHT = 24;
    private static final int GENERAL_TOGGLE_ROWS = 2;
    private static final int GENERAL_CONTENT_HEIGHT = GENERAL_ROW_HEIGHT * GENERAL_FIELD_ROWS + GENERAL_DERIVE_ROW_HEIGHT * GENERAL_TOGGLE_ROWS;
    private static final int CONFIGURED_ROW_HEIGHT = 22;
    private static final int ICON_SIZE = 16;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int VALUE_BOX_WIDTH = 52;
    private static final int DELETE_BUTTON_WIDTH = 20;
    private static final int ADD_SELECTION_HEIGHT = 36;
    private static final int ADD_SUGGESTION_ROW = 20;
    private static final int ADD_FOOTER_HEIGHT = 28;

    private final Screen parent;
    private ConfigSnapshot draft;
    private ConfigPage currentPage = ConfigPage.GENERAL;

    private int generalScrollOffset;
    private int configuredScrollOffset;
    private int addSuggestionScrollOffset;

    private EditBox baseTicksBox;
    private EditBox minTicksBox;
    private EditBox ticksPerValueBox;
    private EditBox strengthBonusBox;
    private EditBox defaultValueBox;
    private EditBox logMaxBox;
    private EditBox logDaysBox;
    private CycleButton<Boolean> deriveRecipesButton;
    private CycleButton<Boolean> instantBuildDebugButton;

    private EditBox configuredSearchBox;
    private EditBox addItemSearchBox;
    private EditBox addItemValueBox;

    @Nullable
    private ResourceLocation addItemCandidate;

    private String configuredSearchQuery = "";
    private String addItemQuery = "";
    private final List<ResourceLocation> filteredConfigured = new ArrayList<>();
    private final List<ResourceLocation> addItemSuggestions = new ArrayList<>();

    public BeastofburdenConfigScreen(final Screen parent)
    {
        super(Component.translatable("com.beastofburden.config.title"));
        this.parent = parent;
        this.draft = ConfigSnapshot.fromCurrent();
    }

    @Override
    protected void init()
    {
        captureGeneralDraft();
        clearWidgets();
        addTabButtons();

        switch (currentPage)
        {
            case GENERAL -> initGeneralPage();
            case CONFIGURED -> initConfiguredPage();
            case ADD_ITEM -> initAddItemPage();
        }

        final int buttonWidth = 110;
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.save"), button -> save())
          .bounds(width / 2 - buttonWidth - 6, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.back"), button -> onClose())
          .bounds(width / 2 + 6, height - 28, buttonWidth, 20).build());
    }

    private void addTabButtons()
    {
        final int gap = 4;
        final int tabWidth = Math.max(72, (width - MARGIN * 2 - gap * 2) / 3);
        addRenderableWidget(tabButton(MARGIN, tabWidth, "com.beastofburden.config.tab.general", ConfigPage.GENERAL));
        addRenderableWidget(tabButton(MARGIN + tabWidth + gap, tabWidth, "com.beastofburden.config.tab.configured", ConfigPage.CONFIGURED));
        addRenderableWidget(tabButton(MARGIN + (tabWidth + gap) * 2, tabWidth, "com.beastofburden.config.tab.add_item", ConfigPage.ADD_ITEM));
    }

    @NotNull
    private Button tabButton(final int x, final int w, @NotNull final String key, @NotNull final ConfigPage page)
    {
        return Button.builder(Component.translatable(key), button -> switchPage(page))
          .bounds(x, TAB_Y, w, TAB_HEIGHT)
          .build();
    }

    private void switchPage(@NotNull final ConfigPage page)
    {
        captureGeneralDraft();
        currentPage = page;
        if (page != ConfigPage.GENERAL)
        {
            generalScrollOffset = 0;
        }
        if (page != ConfigPage.CONFIGURED)
        {
            configuredScrollOffset = 0;
        }
        if (page != ConfigPage.ADD_ITEM)
        {
            addSuggestionScrollOffset = 0;
        }
        init();
    }

    private int generalContentTop()
    {
        return CONTENT_TOP - generalScrollOffset;
    }

    private int contentBottom()
    {
        return height - FOOTER_RESERVE;
    }

    private int generalViewportHeight()
    {
        return contentBottom() - CONTENT_TOP;
    }

    private int maxGeneralScroll()
    {
        return Math.max(0, GENERAL_CONTENT_HEIGHT - generalViewportHeight());
    }

    private void initGeneralPage()
    {
        generalScrollOffset = Math.min(generalScrollOffset, maxGeneralScroll());
        final int top = generalContentTop();
        final int colGap = 12;
        final int colWidth = (width - MARGIN * 2 - colGap) / 2;
        final int leftX = MARGIN;
        final int rightX = MARGIN + colWidth + colGap;
        int row = top;

        baseTicksBox = addBox(leftX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.baseGenerationTicks());
        minTicksBox = addBox(rightX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.minGenerationTicks());
        row += GENERAL_ROW_HEIGHT;

        ticksPerValueBox = addBox(leftX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.ticksPerItemValue());
        strengthBonusBox = addBox(rightX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.strengthSpeedBonus());
        row += GENERAL_ROW_HEIGHT;

        defaultValueBox = addBox(leftX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.defaultItemValue());
        logMaxBox = addBox(rightX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.workLogMaxEntries());
        row += GENERAL_ROW_HEIGHT;

        logDaysBox = addBox(leftX, row + GENERAL_LABEL_TO_FIELD, colWidth, draft.workLogHistoryDays());
        row += GENERAL_ROW_HEIGHT;

        deriveRecipesButton = addRenderableWidget(CycleButton.onOffBuilder(draft.deriveFromRecipes())
          .create(MARGIN, row, width - MARGIN * 2, 20,
            Component.translatable("com.beastofburden.config.derive_recipes"),
            (button, value) -> draft = replaceDraft(value)));
        row += GENERAL_DERIVE_ROW_HEIGHT;

        instantBuildDebugButton = addRenderableWidget(CycleButton.onOffBuilder(draft.planningInstantBuildDebug())
          .create(MARGIN, row, width - MARGIN * 2, 20,
            Component.translatable("com.beastofburden.config.instant_build_debug"),
            (button, value) -> draft = replaceDraftInstantBuild(value)));
    }

    private void initConfiguredPage()
    {
        rebuildConfiguredItems(configuredSearchQuery);
        final int searchWidth = width - MARGIN * 2 - SCROLLBAR_WIDTH - 4;

        configuredSearchBox = new EditBox(font, MARGIN, 58, searchWidth, 18, Component.literal("configured"));
        configuredSearchBox.setValue(configuredSearchQuery);
        configuredSearchBox.setHint(Component.translatable("com.beastofburden.config.search_configured"));
        configuredSearchBox.setResponder(query -> {
            configuredSearchQuery = query;
            configuredScrollOffset = 0;
            rebuildConfiguredItems(query);
            init();
        });
        addRenderableWidget(configuredSearchBox);

        final int listTop = configuredListTop();
        final int visibleRows = configuredVisibleRows();
        final int valueX = valueBoxX();
        final int deleteX = deleteButtonX();

        for (int i = 0; i < visibleRows; i++)
        {
            final int index = configuredScrollOffset + i;
            if (index >= filteredConfigured.size())
            {
                break;
            }

            final ResourceLocation id = filteredConfigured.get(index);
            final Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null)
            {
                continue;
            }

            final int rowY = listTop + i * CONFIGURED_ROW_HEIGHT;
            final int currentValue = draft.itemValues().getOrDefault(item, 0);
            final EditBox valueBox = addBox(valueX, rowY + 2, VALUE_BOX_WIDTH, currentValue);
            valueBox.setResponder(text -> setConfiguredValue(item, parseInt(text, currentValue)));

            addRenderableWidget(Button.builder(Component.literal("✕"), button -> removeConfiguredItem(item))
              .bounds(deleteX, rowY + 2, DELETE_BUTTON_WIDTH, 18)
              .build());
        }
    }

    private void initAddItemPage()
    {
        rebuildAddSuggestions(addItemQuery);
        final int contentWidth = width - MARGIN * 2 - SCROLLBAR_WIDTH - 4;
        final int footerY = contentBottom() - ADD_FOOTER_HEIGHT;

        addItemSearchBox = new EditBox(font, MARGIN, addSearchY(), contentWidth, 18, Component.literal("add"));
        addItemSearchBox.setValue(addItemQuery);
        addItemSearchBox.setHint(Component.translatable("com.beastofburden.config.search_add_item"));
        addItemSearchBox.setResponder(query -> {
            addItemQuery = query;
            addSuggestionScrollOffset = 0;
            rebuildAddSuggestions(query);
        });
        addRenderableWidget(addItemSearchBox);

        addItemValueBox = addBox(MARGIN + contentWidth - 130, footerY + 4, 48, defaultAddValue());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.add_item"), button -> addItem())
          .bounds(MARGIN + contentWidth - 76, footerY + 4, 76, 18).build());
    }

    private int configuredListTop()
    {
        return 82;
    }

    private int configuredListBottom()
    {
        return contentBottom();
    }

    private int configuredVisibleRows()
    {
        return Math.max(1, (configuredListBottom() - configuredListTop()) / CONFIGURED_ROW_HEIGHT);
    }

    private int maxConfiguredScroll()
    {
        return Math.max(0, filteredConfigured.size() - configuredVisibleRows());
    }

    private int valueBoxX()
    {
        return width - MARGIN - SCROLLBAR_WIDTH - DELETE_BUTTON_WIDTH - VALUE_BOX_WIDTH - 8;
    }

    private int deleteButtonX()
    {
        return width - MARGIN - SCROLLBAR_WIDTH - DELETE_BUTTON_WIDTH - 2;
    }

    private int addSelectionTop()
    {
        return ADD_PAGE_TOP + 12;
    }

    private int addSearchY()
    {
        return addSelectionTop() + ADD_SELECTION_HEIGHT + 8;
    }

    private int addSuggestionsTop()
    {
        return addSearchY() + 22;
    }

    private int addSuggestionsBottom()
    {
        return contentBottom() - ADD_FOOTER_HEIGHT - 6;
    }

    private int addSuggestionVisibleRows()
    {
        return Math.max(1, (addSuggestionsBottom() - addSuggestionsTop()) / ADD_SUGGESTION_ROW);
    }

    private int maxAddSuggestionScroll()
    {
        return Math.max(0, addItemSuggestions.size() - addSuggestionVisibleRows());
    }

    private EditBox addBox(final int x, final int y, final int w, final Object value)
    {
        final EditBox box = new EditBox(font, x, y, w, 18, Component.empty());
        box.setValue(String.valueOf(value));
        addRenderableWidget(box);
        return box;
    }

    private void setConfiguredValue(@NotNull final Item item, final int value)
    {
        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.put(item, value);
        draft = replaceDraft(values);
    }

    private void removeConfiguredItem(@NotNull final Item item)
    {
        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.remove(item);
        draft = replaceDraft(values);
        configuredScrollOffset = Math.min(configuredScrollOffset, maxConfiguredScroll());
        init();
    }

    private void rebuildConfiguredItems(final String query)
    {
        filteredConfigured.clear();
        final String lower = query.toLowerCase(Locale.ROOT);
        for (final Map.Entry<Item, Integer> entry : draft.itemValues().entrySet().stream()
          .sorted(Comparator.comparing(e -> new ItemStack(e.getKey()).getHoverName().getString()))
          .toList())
        {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            final String name = new ItemStack(entry.getKey()).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (lower.isEmpty() || id.toString().contains(lower) || name.contains(lower))
            {
                filteredConfigured.add(id);
            }
        }
        configuredScrollOffset = Math.min(configuredScrollOffset, maxConfiguredScroll());
    }

    private void rebuildAddSuggestions(final String query)
    {
        addItemSuggestions.clear();
        if (query.isBlank())
        {
            return;
        }

        final String lower = query.toLowerCase(Locale.ROOT).trim();
        for (final Item item : BuiltInRegistries.ITEM)
        {
            if (item == Items.AIR)
            {
                continue;
            }

            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            final String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (id.toString().contains(lower) || name.contains(lower))
            {
                addItemSuggestions.add(id);
                if (addItemSuggestions.size() >= 400)
                {
                    break;
                }
            }
        }

        addItemSuggestions.sort(Comparator.comparing(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName().getString()));
        addSuggestionScrollOffset = Math.min(addSuggestionScrollOffset, maxAddSuggestionScroll());
    }

    private int defaultAddValue()
    {
        if (addItemCandidate != null)
        {
            final Item item = BuiltInRegistries.ITEM.get(addItemCandidate);
            if (item != null)
            {
                return ItemValueRegistry.getPerItemValue(item);
            }
        }

        return draft.defaultItemValue();
    }

    private void scrollConfigured(final int delta)
    {
        final int next = configuredScrollOffset - delta;
        if (next == configuredScrollOffset)
        {
            return;
        }
        configuredScrollOffset = Math.max(0, Math.min(maxConfiguredScroll(), next));
        init();
    }

    private void scrollAddSuggestions(final int delta)
    {
        addSuggestionScrollOffset = Math.max(0, Math.min(maxAddSuggestionScroll(), addSuggestionScrollOffset - delta));
    }

    private void scrollGeneral(final int delta)
    {
        final int next = generalScrollOffset - delta;
        if (next == generalScrollOffset)
        {
            return;
        }
        generalScrollOffset = Math.max(0, Math.min(maxGeneralScroll(), next));
        init();
    }

    private void addItem()
    {
        final ResourceLocation id = addItemCandidate != null ? addItemCandidate : resolveItemFromQuery(addItemQuery);
        if (id == null)
        {
            return;
        }

        final Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR)
        {
            return;
        }

        final int value = parseInt(addItemValueBox.getValue(), ItemValueRegistry.getPerItemValue(item));
        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.put(item, value);
        draft = replaceDraft(values);
        addItemCandidate = id;
        addItemQuery = new ItemStack(item).getHoverName().getString();
        rebuildAddSuggestions(addItemQuery);
        init();
    }

    @Nullable
    private ResourceLocation resolveItemFromQuery(@NotNull final String query)
    {
        if (ResourceLocation.isValidResourceLocation(query))
        {
            return ResourceLocation.parse(query);
        }

        if (!addItemSuggestions.isEmpty())
        {
            return addItemSuggestions.get(0);
        }

        return null;
    }

    private void captureGeneralDraft()
    {
        if (baseTicksBox == null)
        {
            return;
        }

        draft = new ConfigSnapshot(
          parseInt(baseTicksBox.getValue(), draft.baseGenerationTicks()),
          parseInt(minTicksBox.getValue(), draft.minGenerationTicks()),
          parseDouble(ticksPerValueBox.getValue(), draft.ticksPerItemValue()),
          parseDouble(strengthBonusBox.getValue(), draft.strengthSpeedBonus()),
          parseInt(defaultValueBox.getValue(), draft.defaultItemValue()),
          deriveRecipesButton.getValue(),
          parseInt(logMaxBox.getValue(), draft.workLogMaxEntries()),
          parseInt(logDaysBox.getValue(), draft.workLogHistoryDays()),
          instantBuildDebugButton == null ? draft.planningInstantBuildDebug() : instantBuildDebugButton.getValue(),
          draft.itemValues()
        );
    }

    private void save()
    {
        captureGeneralDraft();

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer())
        {
            ConfigPersistence.applyAndSave(draft);
        }
        else
        {
            ModNetwork.CHANNEL.sendToServer(new SaveBeastConfigMessage(draft));
        }
    }

    private ConfigSnapshot replaceDraft(@NotNull final Map<Item, Integer> itemValues)
    {
        return new ConfigSnapshot(
          draft.baseGenerationTicks(),
          draft.minGenerationTicks(),
          draft.ticksPerItemValue(),
          draft.strengthSpeedBonus(),
          draft.defaultItemValue(),
          draft.deriveFromRecipes(),
          draft.workLogMaxEntries(),
          draft.workLogHistoryDays(),
          draft.planningInstantBuildDebug(),
          itemValues
        );
    }

    private ConfigSnapshot replaceDraft(final boolean deriveRecipes)
    {
        return new ConfigSnapshot(
          draft.baseGenerationTicks(),
          draft.minGenerationTicks(),
          draft.ticksPerItemValue(),
          draft.strengthSpeedBonus(),
          draft.defaultItemValue(),
          deriveRecipes,
          draft.workLogMaxEntries(),
          draft.workLogHistoryDays(),
          draft.planningInstantBuildDebug(),
          draft.itemValues()
        );
    }

    private ConfigSnapshot replaceDraftInstantBuild(final boolean instantBuildDebug)
    {
        return new ConfigSnapshot(
          draft.baseGenerationTicks(),
          draft.minGenerationTicks(),
          draft.ticksPerItemValue(),
          draft.strengthSpeedBonus(),
          draft.defaultItemValue(),
          draft.deriveFromRecipes(),
          draft.workLogMaxEntries(),
          draft.workLogHistoryDays(),
          instantBuildDebug,
          draft.itemValues()
        );
    }

    private static int parseInt(final String text, final int fallback)
    {
        try
        {
            return Integer.parseInt(text.trim());
        }
        catch (final NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static double parseDouble(final String text, final double fallback)
    {
        try
        {
            return Double.parseDouble(text.trim());
        }
        catch (final NumberFormatException ignored)
        {
            return fallback;
        }
    }

    @Override
    public void render(@NotNull final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        if (currentPage == ConfigPage.GENERAL)
        {
            renderGeneralLabels(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        switch (currentPage)
        {
            case GENERAL -> {
                if (maxGeneralScroll() > 0)
                {
                    renderScrollbar(graphics, width - MARGIN - SCROLLBAR_WIDTH, CONTENT_TOP, generalViewportHeight(),
                      generalScrollOffset, maxGeneralScroll() + 1, 1);
                }
            }
            case CONFIGURED -> renderConfiguredPage(graphics);
            case ADD_ITEM -> renderAddItemPage(graphics, mouseX, mouseY);
        }
    }

    private void renderGeneralLabels(@NotNull final GuiGraphics graphics)
    {
        graphics.enableScissor(MARGIN, CONTENT_TOP, width - MARGIN, contentBottom());
        int row = generalContentTop();
        final int colGap = 12;
        final int colWidth = (width - MARGIN * 2 - colGap) / 2;
        final int leftX = MARGIN;
        final int rightX = MARGIN + colWidth + colGap;

        drawFieldLabel(graphics, leftX, row, "com.beastofburden.config.base_ticks");
        drawFieldLabel(graphics, rightX, row, "com.beastofburden.config.min_ticks");
        row += GENERAL_ROW_HEIGHT;

        drawFieldLabel(graphics, leftX, row, "com.beastofburden.config.ticks_per_value");
        drawFieldLabel(graphics, rightX, row, "com.beastofburden.config.strength_bonus");
        row += GENERAL_ROW_HEIGHT;

        drawFieldLabel(graphics, leftX, row, "com.beastofburden.config.default_value");
        drawFieldLabel(graphics, rightX, row, "com.beastofburden.config.log_max");
        row += GENERAL_ROW_HEIGHT;

        drawFieldLabel(graphics, leftX, row, "com.beastofburden.config.log_days");
        graphics.disableScissor();
    }

    private void drawFieldLabel(@NotNull final GuiGraphics graphics, final int x, final int y, @NotNull final String key)
    {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0);
    }

    private void renderConfiguredPage(@NotNull final GuiGraphics graphics)
    {
        final int listTop = configuredListTop();
        final int listBottom = configuredListBottom();
        final int nameRight = valueBoxX() - 6;

        graphics.drawString(font, Component.translatable("com.beastofburden.config.item_list"), MARGIN, 46, 0xA0A0A0);
        graphics.fill(MARGIN, listTop - 2, width - MARGIN, listTop - 1, 0x40FFFFFF);

        graphics.enableScissor(MARGIN, listTop, width - MARGIN - SCROLLBAR_WIDTH - 2, listBottom);
        final int visibleRows = configuredVisibleRows();
        for (int i = 0; i < visibleRows; i++)
        {
            final int index = configuredScrollOffset + i;
            if (index >= filteredConfigured.size())
            {
                break;
            }

            final ResourceLocation id = filteredConfigured.get(index);
            final Item item = BuiltInRegistries.ITEM.get(id);
            final int rowY = listTop + i * CONFIGURED_ROW_HEIGHT;
            final ItemStack stack = new ItemStack(item);

            graphics.fill(MARGIN, rowY, nameRight, rowY + CONFIGURED_ROW_HEIGHT - 1, i % 2 == 0 ? 0x18000000 : 0x0C000000);
            graphics.renderItem(stack, MARGIN + 2, rowY + 3);
            graphics.drawString(font, truncate(stack.getHoverName(), nameRight - MARGIN - ICON_SIZE - 10), MARGIN + ICON_SIZE + 6, rowY + 7, 0xE8E8E8);
        }
        graphics.disableScissor();

        if (filteredConfigured.isEmpty())
        {
            graphics.drawString(font, Component.translatable("com.beastofburden.config.no_configured"), MARGIN, listTop + 8, 0x808080);
        }
        else if (filteredConfigured.size() > visibleRows)
        {
            renderScrollbar(graphics, width - MARGIN - SCROLLBAR_WIDTH, listTop, listBottom - listTop,
              configuredScrollOffset, filteredConfigured.size(), visibleRows);
        }
    }

    private void renderAddItemPage(@NotNull final GuiGraphics graphics, final int mouseX, final int mouseY)
    {
        final int selectionTop = addSelectionTop();
        final int listTop = addSuggestionsTop();
        final int listBottom = addSuggestionsBottom();
        final int footerY = contentBottom() - ADD_FOOTER_HEIGHT;

        graphics.drawString(font, Component.translatable("com.beastofburden.config.selected_for_add"), MARGIN, ADD_PAGE_TOP, 0xA0A0A0);
        renderSelectionPanel(graphics, selectionTop);

        graphics.fill(MARGIN, listTop - 2, width - MARGIN, listTop - 1, 0x40FFFFFF);
        graphics.drawString(font, Component.translatable("com.beastofburden.config.add_value_hint"), MARGIN, footerY - 10, 0xA0A0A0);

        if (!addItemQuery.isBlank())
        {
            graphics.enableScissor(MARGIN, listTop, width - MARGIN - SCROLLBAR_WIDTH - 2, listBottom);
            final int visibleRows = addSuggestionVisibleRows();
            for (int i = 0; i < visibleRows; i++)
            {
                final int index = addSuggestionScrollOffset + i;
                if (index >= addItemSuggestions.size())
                {
                    break;
                }

                final ResourceLocation id = addItemSuggestions.get(index);
                final Item item = BuiltInRegistries.ITEM.get(id);
                final int rowY = listTop + i * ADD_SUGGESTION_ROW;
                final boolean hovered = mouseX >= MARGIN && mouseX <= width - MARGIN
                  && mouseY >= rowY && mouseY < rowY + ADD_SUGGESTION_ROW;
                final boolean picked = id.equals(addItemCandidate);

                if (picked)
                {
                    graphics.fill(MARGIN, rowY, width - MARGIN - SCROLLBAR_WIDTH - 2, rowY + ADD_SUGGESTION_ROW - 1, 0xA05588FF);
                }
                else if (hovered)
                {
                    graphics.fill(MARGIN, rowY, width - MARGIN - SCROLLBAR_WIDTH - 2, rowY + ADD_SUGGESTION_ROW - 1, 0x40303030);
                }

                final ItemStack stack = new ItemStack(item);
                graphics.renderItem(stack, MARGIN + 2, rowY + 2);
                graphics.drawString(font, stack.getHoverName(), MARGIN + ICON_SIZE + 6, rowY + 3, picked ? 0xFFFFFF : 0xE0E0E0);
                graphics.drawString(font, id.toString(), MARGIN + ICON_SIZE + 6, rowY + 12, 0x808080);
            }
            graphics.disableScissor();

            if (addItemSuggestions.size() > visibleRows)
            {
                renderScrollbar(graphics, width - MARGIN - SCROLLBAR_WIDTH, listTop, listBottom - listTop,
                  addSuggestionScrollOffset, addItemSuggestions.size(), visibleRows);
            }
        }
        else
        {
            graphics.drawString(font, Component.translatable("com.beastofburden.config.add_search_hint"), MARGIN, listTop + 6, 0x707070);
        }
    }

    private void renderSelectionPanel(@NotNull final GuiGraphics graphics, final int top)
    {
        final int right = width - MARGIN;
        final int bottom = top + ADD_SELECTION_HEIGHT;
        final boolean hasSelection = addItemCandidate != null;
        graphics.fill(MARGIN, top, right, bottom, hasSelection ? 0x705588FF : 0x30202020);
        graphics.fill(MARGIN, top, right, top + 1, hasSelection ? 0xFF88AAFF : 0x50FFFFFF);
        graphics.fill(MARGIN, bottom - 1, right, bottom, hasSelection ? 0xFF88AAFF : 0x50FFFFFF);

        if (hasSelection)
        {
            final Item item = BuiltInRegistries.ITEM.get(addItemCandidate);
            if (item != null)
            {
                final ItemStack stack = new ItemStack(item);
                graphics.renderItem(stack, MARGIN + 10, top + 10);
                graphics.renderItemDecorations(font, stack, MARGIN + 10, top + 10);
                graphics.drawString(font, stack.getHoverName(), MARGIN + 34, top + 8, 0xFFFFFF);
                graphics.drawString(font, addItemCandidate.toString(), MARGIN + 34, top + 20, 0xC0C0C0);
            }
        }
        else
        {
            graphics.drawString(font, Component.translatable("com.beastofburden.config.no_selection"), MARGIN + 10, top + 13, 0x909090);
        }
    }

    @NotNull
    private String truncate(@NotNull final Component text, final int maxWidth)
    {
        String value = text.getString();
        while (font.width(value) > maxWidth && value.length() > 3)
        {
            value = value.substring(0, value.length() - 4) + "...";
        }
        return value;
    }

    private void renderScrollbar(
      @NotNull final GuiGraphics graphics,
      final int x,
      final int y,
      final int height,
      final int offset,
      final int total,
      final int visible)
    {
        if (total <= visible)
        {
            return;
        }

        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, 0x40000000);
        final int thumbHeight = Math.max(12, (int) ((height * (long) visible) / total));
        final int travel = Math.max(1, height - thumbHeight);
        final int thumbY = y + (int) ((offset * (long) travel) / Math.max(1, total - visible));
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF909090);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button)
    {
        if (button != 0)
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (currentPage == ConfigPage.GENERAL && maxGeneralScroll() > 0)
        {
            final int trackX = width - MARGIN - SCROLLBAR_WIDTH;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH && mouseY >= CONTENT_TOP && mouseY <= contentBottom())
            {
                scrollGeneralFromTrack(mouseY);
                return true;
            }
        }

        if (currentPage == ConfigPage.CONFIGURED && handleConfiguredScrollbarClick(mouseX, mouseY))
        {
            return true;
        }

        if (currentPage == ConfigPage.ADD_ITEM)
        {
            if (handleAddSuggestionClick(mouseX, mouseY))
            {
                return true;
            }

            if (handleAddScrollbarClick(mouseX, mouseY))
            {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void scrollGeneralFromTrack(final double mouseY)
    {
        final int viewport = generalViewportHeight();
        final int thumbHeight = Math.max(12, (int) ((viewport * (long) GENERAL_FIELD_ROWS) / (maxGeneralScroll() / GENERAL_ROW_HEIGHT + GENERAL_FIELD_ROWS)));
        final int travel = Math.max(1, viewport - thumbHeight);
        final double relative = (mouseY - CONTENT_TOP - thumbHeight / 2.0D) / travel;
        generalScrollOffset = (int) Math.round(relative * maxGeneralScroll());
        generalScrollOffset = Math.max(0, Math.min(maxGeneralScroll(), generalScrollOffset));
        init();
    }

    private boolean handleConfiguredScrollbarClick(final double mouseX, final double mouseY)
    {
        final int trackX = width - MARGIN - SCROLLBAR_WIDTH;
        if (mouseX < trackX || mouseX > trackX + SCROLLBAR_WIDTH)
        {
            return false;
        }

        final int listTop = configuredListTop();
        final int listBottom = configuredListBottom();
        if (mouseY >= listTop && mouseY < listBottom && filteredConfigured.size() > configuredVisibleRows())
        {
            scrollConfiguredFromTrack(mouseY, listTop, listBottom, filteredConfigured.size(), configuredVisibleRows());
            return true;
        }

        return false;
    }

    private boolean handleAddScrollbarClick(final double mouseX, final double mouseY)
    {
        final int trackX = width - MARGIN - SCROLLBAR_WIDTH;
        if (mouseX < trackX || mouseX > trackX + SCROLLBAR_WIDTH || addItemQuery.isBlank())
        {
            return false;
        }

        final int top = addSuggestionsTop();
        final int bottom = addSuggestionsBottom();
        if (mouseY >= top && mouseY < bottom && addItemSuggestions.size() > addSuggestionVisibleRows())
        {
            scrollAddSuggestionsFromTrack(mouseY, top, bottom);
            return true;
        }

        return false;
    }

    private boolean handleAddSuggestionClick(final double mouseX, final double mouseY)
    {
        if (addItemQuery.isBlank())
        {
            return false;
        }

        final int top = addSuggestionsTop();
        final int bottom = addSuggestionsBottom();
        if (mouseX < MARGIN || mouseX > width - MARGIN - SCROLLBAR_WIDTH || mouseY < top || mouseY >= bottom)
        {
            return false;
        }

        final int row = (int) ((mouseY - top) / ADD_SUGGESTION_ROW);
        final int index = addSuggestionScrollOffset + row;
        if (index < 0 || index >= addItemSuggestions.size())
        {
            return false;
        }

        addItemCandidate = addItemSuggestions.get(index);
        final Item item = BuiltInRegistries.ITEM.get(addItemCandidate);
        if (item != null && addItemValueBox != null)
        {
            addItemValueBox.setValue(String.valueOf(ItemValueRegistry.getPerItemValue(item)));
        }
        return true;
    }

    private void scrollConfiguredFromTrack(final double mouseY, final int top, final int bottom, final int total, final int visible)
    {
        final int height = bottom - top;
        final int thumbHeight = Math.max(12, (int) ((height * (long) visible) / total));
        final int travel = Math.max(1, height - thumbHeight);
        final double relative = (mouseY - top - thumbHeight / 2.0D) / travel;
        configuredScrollOffset = (int) Math.round(relative * Math.max(0, total - visible));
        configuredScrollOffset = Math.max(0, Math.min(maxConfiguredScroll(), configuredScrollOffset));
        init();
    }

    private void scrollAddSuggestionsFromTrack(final double mouseY, final int top, final int bottom)
    {
        final int height = bottom - top;
        final int visible = addSuggestionVisibleRows();
        final int thumbHeight = Math.max(12, (int) ((height * (long) visible) / addItemSuggestions.size()));
        final int travel = Math.max(1, height - thumbHeight);
        final double relative = (mouseY - top - thumbHeight / 2.0D) / travel;
        addSuggestionScrollOffset = (int) Math.round(relative * maxAddSuggestionScroll());
        addSuggestionScrollOffset = Math.max(0, Math.min(maxAddSuggestionScroll(), addSuggestionScrollOffset));
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta)
    {
        if (currentPage == ConfigPage.GENERAL && mouseY >= CONTENT_TOP && mouseY < contentBottom())
        {
            scrollGeneral((int) delta);
            return true;
        }

        if (currentPage == ConfigPage.CONFIGURED
              && mouseX >= MARGIN
              && mouseY >= configuredListTop()
              && mouseY < configuredListBottom())
        {
            scrollConfigured((int) delta);
            return true;
        }

        if (currentPage == ConfigPage.ADD_ITEM
              && !addItemQuery.isBlank()
              && mouseX >= MARGIN
              && mouseY >= addSuggestionsTop()
              && mouseY < addSuggestionsBottom())
        {
            scrollAddSuggestions((int) delta);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }
}
