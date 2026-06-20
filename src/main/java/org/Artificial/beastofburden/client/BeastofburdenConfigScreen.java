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
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private ConfigSnapshot draft;
    private boolean generalPage = true;

    private EditBox baseTicksBox;
    private EditBox minTicksBox;
    private EditBox ticksPerValueBox;
    private EditBox strengthBonusBox;
    private EditBox defaultValueBox;
    private EditBox logMaxBox;
    private EditBox logDaysBox;
    private CycleButton<Boolean> deriveRecipesButton;

    private EditBox itemSearchBox;
    private EditBox itemIdBox;
    private EditBox itemValueBox;
    private EditBox selectedValueBox;
    @Nullable
    private ResourceLocation selectedItemId;
    private int itemScrollOffset;
    private String searchQuery = "";
    private final List<ResourceLocation> filteredItems = new ArrayList<>();

    public BeastofburdenConfigScreen(final Screen parent)
    {
        super(Component.translatable("com.beastofburden.config.title"));
        this.parent = parent;
        this.draft = ConfigSnapshot.fromCurrent();
    }

    @Override
    protected void init()
    {
        clearWidgets();
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.tab.general"), button -> switchPage(true))
          .bounds(20, 24, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.tab.items"), button -> switchPage(false))
          .bounds(130, 24, 100, 20).build());

        if (generalPage)
        {
            initGeneralPage();
        }
        else
        {
            initItemPage();
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.save"), button -> save())
          .bounds(width / 2 - 155, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
          .bounds(width / 2 + 55, height - 28, 100, 20).build());
    }

    private void switchPage(final boolean general)
    {
        generalPage = general;
        init();
    }

    private void initGeneralPage()
    {
        int y = 55;
        baseTicksBox = addBox(20, y, 220, draft.baseGenerationTicks()); y += ROW_HEIGHT;
        minTicksBox = addBox(20, y, 220, draft.minGenerationTicks()); y += ROW_HEIGHT;
        ticksPerValueBox = addBox(20, y, 220, draft.ticksPerItemValue()); y += ROW_HEIGHT;
        strengthBonusBox = addBox(20, y, 220, draft.strengthSpeedBonus()); y += ROW_HEIGHT;
        defaultValueBox = addBox(20, y, 220, draft.defaultItemValue()); y += ROW_HEIGHT;
        logMaxBox = addBox(20, y, 220, draft.workLogMaxEntries()); y += ROW_HEIGHT;
        logDaysBox = addBox(20, y, 220, draft.workLogHistoryDays()); y += ROW_HEIGHT;
        deriveRecipesButton = addRenderableWidget(CycleButton.onOffBuilder(draft.deriveFromRecipes())
          .create(250, 55, 120, 20, Component.translatable("com.beastofburden.config.derive_recipes"), (button, value) -> draft = replaceDraft(value)));
    }

    private void initItemPage()
    {
        rebuildFilteredItems(searchQuery);
        itemSearchBox = new EditBox(font, 20, 55, width - 40, 20, Component.literal("search"));
        itemSearchBox.setValue(searchQuery);
        itemSearchBox.setResponder(query -> {
            searchQuery = query;
            rebuildFilteredItems(query);
        });
        addRenderableWidget(itemSearchBox);

        itemIdBox = addBox(20, height - 96, 220, selectedItemId == null ? "" : selectedItemId.toString());
        itemValueBox = addBox(250, height - 96, 80, "");
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.add_item"), button -> addItem())
          .bounds(340, height - 96, 80, 20).build());

        selectedValueBox = addBox(20, height - 68, 80, selectedItemId == null ? "" : String.valueOf(
          draft.itemValues().getOrDefault(BuiltInRegistries.ITEM.get(selectedItemId), 0)
        ));
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.update_item"), button -> updateSelectedItem())
          .bounds(110, height - 68, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.config.remove_item"), button -> removeSelectedItem())
          .bounds(200, height - 68, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▲"), button -> scrollItems(-1))
          .bounds(width - 36, 80, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), button -> scrollItems(1))
          .bounds(width - 36, height - 120, 16, 16).build());
    }

    private EditBox addBox(final int x, final int y, final int w, final Object value)
    {
        final EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setValue(String.valueOf(value));
        addRenderableWidget(box);
        return box;
    }

    private void rebuildFilteredItems(final String query)
    {
        filteredItems.clear();
        final String lower = query.toLowerCase(Locale.ROOT);
        for (final Map.Entry<Item, Integer> entry : draft.itemValues().entrySet().stream()
          .sorted(Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()))
          .toList())
        {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            final String name = new ItemStack(entry.getKey()).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (lower.isEmpty() || id.toString().contains(lower) || name.contains(lower))
            {
                filteredItems.add(id);
            }
        }
        itemScrollOffset = Math.min(itemScrollOffset, Math.max(0, filteredItems.size() - 1));
    }

    private void scrollItems(final int delta)
    {
        itemScrollOffset = Math.max(0, Math.min(Math.max(0, filteredItems.size() - 1), itemScrollOffset + delta));
    }

    private void addItem()
    {
        if (!ResourceLocation.isValidResourceLocation(itemIdBox.getValue()))
        {
            return;
        }

        final ResourceLocation id = ResourceLocation.parse(itemIdBox.getValue());
        final Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null)
        {
            return;
        }

        final int value = parseInt(itemValueBox.getValue(), ItemValueRegistry.getPerItemValue(item));
        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.put(item, value);
        draft = replaceDraft(values);
        selectedItemId = id;
        init();
    }

    private void updateSelectedItem()
    {
        if (selectedItemId == null)
        {
            return;
        }

        final Item item = BuiltInRegistries.ITEM.get(selectedItemId);
        if (item == null)
        {
            return;
        }

        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.put(item, parseInt(selectedValueBox.getValue(), values.getOrDefault(item, 1)));
        draft = replaceDraft(values);
        init();
    }

    private void removeSelectedItem()
    {
        if (selectedItemId == null)
        {
            return;
        }

        final Item item = BuiltInRegistries.ITEM.get(selectedItemId);
        if (item == null)
        {
            return;
        }

        final Map<Item, Integer> values = new HashMap<>(draft.itemValues());
        values.remove(item);
        draft = replaceDraft(values);
        selectedItemId = null;
        init();
    }

    private void save()
    {
        if (generalPage)
        {
            draft = new ConfigSnapshot(
              parseInt(baseTicksBox.getValue(), draft.baseGenerationTicks()),
              parseInt(minTicksBox.getValue(), draft.minGenerationTicks()),
              parseDouble(ticksPerValueBox.getValue(), draft.ticksPerItemValue()),
              parseDouble(strengthBonusBox.getValue(), draft.strengthSpeedBonus()),
              parseInt(defaultValueBox.getValue(), draft.defaultItemValue()),
              deriveRecipesButton.getValue(),
              parseInt(logMaxBox.getValue(), draft.workLogMaxEntries()),
              parseInt(logDaysBox.getValue(), draft.workLogHistoryDays()),
              draft.itemValues()
            );
        }

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
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        if (generalPage)
        {
            int y = 55;
            y = label(graphics, y, "com.beastofburden.config.base_ticks");
            y = label(graphics, y, "com.beastofburden.config.min_ticks");
            y = label(graphics, y, "com.beastofburden.config.ticks_per_value");
            y = label(graphics, y, "com.beastofburden.config.strength_bonus");
            y = label(graphics, y, "com.beastofburden.config.default_value");
            y = label(graphics, y, "com.beastofburden.config.log_max");
            y = label(graphics, y, "com.beastofburden.config.log_days");
        }
        else
        {
            graphics.drawString(font, Component.translatable("com.beastofburden.config.item_list"), 20, 80, 0xA0A0A0);
            final int listTop = 92;
            final int listBottom = height - 124;
            final int visibleRows = Math.max(1, (listBottom - listTop) / 14);
            for (int i = 0; i < visibleRows; i++)
            {
                final int index = itemScrollOffset + i;
                if (index >= filteredItems.size())
                {
                    break;
                }

                final ResourceLocation id = filteredItems.get(index);
                final Item item = BuiltInRegistries.ITEM.get(id);
                final int value = draft.itemValues().getOrDefault(item, 0);
                final int rowY = listTop + i * 14;
                final boolean selected = id.equals(selectedItemId);
                if (selected)
                {
                    graphics.fill(18, rowY - 1, width - 42, rowY + 12, 0x804488FF);
                }

                graphics.drawString(
                  font,
                  new ItemStack(item).getHoverName().getString() + " (" + id + ") = " + value,
                  20,
                  rowY,
                  selected ? 0xFFFFFF : 0xC0C0C0
                );
            }

            graphics.drawString(font, Component.translatable("com.beastofburden.config.new_item"), 20, height - 108, 0xA0A0A0);
            graphics.drawString(font, Component.translatable("com.beastofburden.config.selected_item"), 20, height - 80, 0xA0A0A0);
        }
    }

    private int label(@NotNull final GuiGraphics graphics, final int y, @NotNull final String key)
    {
        graphics.drawString(font, Component.translatable(key), 250, y + 4, 0xA0A0A0);
        return y + ROW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button)
    {
        if (!generalPage && button == 0)
        {
            final int listTop = 92;
            final int listBottom = height - 124;
            final int visibleRows = Math.max(1, (listBottom - listTop) / 14);
            for (int i = 0; i < visibleRows; i++)
            {
                final int index = itemScrollOffset + i;
                if (index >= filteredItems.size())
                {
                    break;
                }

                final int rowY = listTop + i * 14;
                if (mouseX >= 18 && mouseX <= width - 42 && mouseY >= rowY - 1 && mouseY <= rowY + 12)
                {
                    selectedItemId = filteredItems.get(index);
                    init();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }
}
