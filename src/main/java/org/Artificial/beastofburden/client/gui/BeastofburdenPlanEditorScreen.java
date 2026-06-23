package org.Artificial.beastofburden.client.gui;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.Artificial.beastofburden.colony.planning.FixedPlanRequirement;
import org.Artificial.beastofburden.colony.planning.FixedPlanScript;
import org.Artificial.beastofburden.colony.planning.FixedPlanStep;
import org.Artificial.beastofburden.colony.planning.PlanScriptValidator;
import org.Artificial.beastofburden.colony.planning.PlanStepFormatter;
import org.Artificial.beastofburden.colony.planning.PlannedBuildingIcons;
import org.Artificial.beastofburden.colony.planning.PlannedBuildingType;
import org.Artificial.beastofburden.network.ModNetwork;
import org.Artificial.beastofburden.network.SaveColonyPlanMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-styled scripted plan editor. Each row is one build objective.
 */
public final class BeastofburdenPlanEditorScreen extends Screen
{
    private static final int MARGIN = 12;
    private static final int FOOTER = 30;
    private static final int HEADER = 36;
    private static final int ROW_HEIGHT = 22;
    private static final int ICON_SIZE = 16;
    private static final int ACTION_SIZE = 16;
    private static final int SCROLLBAR = 6;

    private static final int COL_STEP_NUM = 16;
    private static final int COL_ICON = 20;
    private static final int COL_ACTIONS = ACTION_SIZE * 3 + 4;

    private static final int SPIN_BUTTON = 14;
    private static final int SPIN_VALUE = 18;
    private static final int SPIN_GAP = 2;
    private static final int LABEL_GAP = 4;
    private static final int GROUP_GAP = 16;
    private static final int TYPE_GAP = 8;
    private static final int ACTIONS_GAP = 6;

    private final BlockPos buildingPos;
    @Nullable
    private final Screen parent;
    @NotNull
    private final List<DraftStep> draftSteps;
    private boolean customDraft;
    private int scrollOffset;

    public BeastofburdenPlanEditorScreen(
      @Nullable final Screen parent,
      @NotNull final IBuildingView buildingView,
      @NotNull final FixedPlanScript script)
    {
        super(Component.translatable("com.beastofburden.gui.plan_editor.title"));
        this.parent = parent;
        this.buildingPos = buildingView.getPosition();
        this.draftSteps = new ArrayList<>();
        this.customDraft = script.isCustom();
        for (int i = 0; i < script.stepCount(); i++)
        {
            draftSteps.addAll(DraftStep.expandFrom(script.getStep(i)));
        }
    }

    @Override
    protected void init()
    {
        clearWidgets();
        scrollOffset = Math.min(scrollOffset, maxScroll());

        final int listTop = listTop();
        final int listBottom = listBottom();
        final int visibleRows = visibleRowCount();
        final int contentLeft = contentLeft();
        final int contentWidth = contentWidth();

        for (int visible = 0; visible < visibleRows; visible++)
        {
            final int index = scrollOffset + visible;
            if (index > draftSteps.size())
            {
                break;
            }

            final int rowY = listTop + visible * ROW_HEIGHT;
            if (index < draftSteps.size())
            {
                bindStepRow(index, contentLeft, rowY, contentWidth);
            }
            else if (index == draftSteps.size())
            {
                bindAddRow(contentLeft, rowY, contentWidth);
            }
        }

        final int buttonWidth = 96;
        final int footerY = height - 24;
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.gui.plan_editor.reset_default"), button -> resetToDefault())
          .bounds(MARGIN, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.gui.plan_editor.save"), button -> savePlan())
          .bounds(width / 2 - buttonWidth / 2, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("com.beastofburden.gui.plan_editor.cancel"), button -> onClose())
          .bounds(width - MARGIN - buttonWidth, footerY, buttonWidth, 20).build());
    }

    @Override
    public void render(@NotNull final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);

        final Component planType = Component.translatable(
          customDraft
            ? "com.beastofburden.gui.plan_editor.custom"
            : "com.beastofburden.gui.plan_editor.default_plan"
        );
        graphics.drawCenteredString(font, planType, width / 2, 22, 0xA0A0A0);

        final int listTop = listTop();
        final int listBottom = listBottom();
        final int contentLeft = contentLeft();
        final int contentWidth = contentWidth();

        graphics.fill(contentLeft, listTop, contentLeft + contentWidth, listBottom, 0xC0101010);
        graphics.renderOutline(contentLeft, listTop, contentWidth, listBottom - listTop, 0xFF505050);
        graphics.enableScissor(contentLeft, listTop, contentLeft + contentWidth, listBottom);

        final int visibleRows = visibleRowCount();
        for (int visible = 0; visible < visibleRows; visible++)
        {
            final int index = scrollOffset + visible;
            if (index > draftSteps.size())
            {
                break;
            }

            final int rowY = listTop + visible * ROW_HEIGHT;
            graphics.fill(contentLeft + 1, rowY, contentLeft + contentWidth - 1, rowY + ROW_HEIGHT, 0xC0101010);

            if (index < draftSteps.size())
            {
                renderStepRow(graphics, index, contentLeft, rowY, contentWidth);
            }
            else if (index == draftSteps.size())
            {
                renderAddRowHint(graphics, contentLeft, rowY, contentWidth);
            }
        }

        graphics.disableScissor();

        if (maxScroll() > 0)
        {
            renderScrollbar(graphics, listTop, listBottom);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta)
    {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll(), scrollOffset - delta));
        init();
        return true;
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    private void bindStepRow(final int stepIndex, final int left, final int rowY, final int width)
    {
        final DraftStep step = draftSteps.get(stepIndex);
        final StepRowLayout layout = computeRowLayout(left, width);

        addRenderableWidget(Button.builder(step.typeLabel(), button -> openTypePicker(stepIndex))
          .bounds(layout.typeX, rowY + 2, layout.typeW, 18).build());

        bindSpinner(layout.countSpinnerX, rowY, () -> adjustCount(stepIndex, -1), () -> adjustCount(stepIndex, 1));

        if (!step.isField())
        {
            bindSpinner(layout.levelSpinnerX, rowY, () -> adjustLevel(stepIndex, -1), () -> adjustLevel(stepIndex, 1));
        }

        int actionX = layout.actionsX;
        addRenderableWidget(Button.builder(Component.literal("▲"), button -> moveStep(stepIndex, -1))
          .bounds(actionX, rowY + 3, ACTION_SIZE, ACTION_SIZE).build());
        actionX += ACTION_SIZE + 2;
        addRenderableWidget(Button.builder(Component.literal("▼"), button -> moveStep(stepIndex, 1))
          .bounds(actionX, rowY + 3, ACTION_SIZE, ACTION_SIZE).build());
        actionX += ACTION_SIZE + 2;
        addRenderableWidget(Button.builder(Component.literal("✕"), button -> removeStep(stepIndex))
          .bounds(actionX, rowY + 3, ACTION_SIZE, ACTION_SIZE).build());
    }

    private void bindAddRow(final int left, final int rowY, final int width)
    {
        final int startX = addRowStartX(left, width);
        addRenderableWidget(Button.builder(Component.literal("+"), button -> addStep())
          .bounds(startX, rowY + 2, addRowPlusWidth(), 18).build());
    }

    private void bindSpinner(final int spinnerX, final int rowY, @NotNull final Runnable onMinus, @NotNull final Runnable onPlus)
    {
        addRenderableWidget(Button.builder(Component.literal("-"), button -> onMinus.run())
          .bounds(spinnerX, rowY + 3, SPIN_BUTTON, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> onPlus.run())
          .bounds(spinnerX + SPIN_BUTTON + SPIN_GAP + SPIN_VALUE + SPIN_GAP, rowY + 3, SPIN_BUTTON, 16).build());
    }

    private void renderStepRow(
      @NotNull final GuiGraphics graphics,
      final int stepIndex,
      final int left,
      final int rowY,
      final int width)
    {
        final DraftStep step = draftSteps.get(stepIndex);
        final StepRowLayout layout = computeRowLayout(left, width);

        graphics.drawCenteredString(font, String.valueOf(stepIndex + 1), left + COL_STEP_NUM / 2, rowY + 7, 0xE0E0E0);
        graphics.renderItem(step.icon(), layout.iconX, rowY + 3);

        graphics.drawString(font, countLabel(), layout.countLabelX, rowY + 7, 0xA0A0A0);
        renderSpinnerValue(graphics, layout.countSpinnerX, rowY, step.count());

        if (!step.isField())
        {
            graphics.drawString(font, levelLabel(), layout.levelLabelX, rowY + 7, 0xA0A0A0);
            renderSpinnerValue(graphics, layout.levelSpinnerX, rowY, step.minLevel());
        }
    }

    @NotNull
    private StepRowLayout computeRowLayout(final int left, final int width)
    {
        final int actionsX = left + width - COL_ACTIONS;
        final int countSpinnerX = actionsX - ACTIONS_GAP - spinnerWidth();
        final int countLabelX = countSpinnerX - LABEL_GAP - labelWidth(countLabel());

        // Always reserve the level column so the type button stays the same width for fields and huts.
        final int levelSpinnerX = countLabelX - GROUP_GAP - spinnerWidth();
        final int levelLabelX = levelSpinnerX - LABEL_GAP - labelWidth(levelLabel());
        final int typeRight = levelLabelX - TYPE_GAP;

        final int typeX = left + COL_STEP_NUM + COL_ICON;
        final int typeW = Math.max(48, typeRight - typeX);
        final int iconX = left + COL_STEP_NUM + 2;

        return new StepRowLayout(typeX, typeW, levelLabelX, levelSpinnerX, countLabelX, countSpinnerX, actionsX, iconX);
    }

    private record StepRowLayout(
      int typeX,
      int typeW,
      int levelLabelX,
      int levelSpinnerX,
      int countLabelX,
      int countSpinnerX,
      int actionsX,
      int iconX)
    {
    }

    private void renderAddRowHint(
      @NotNull final GuiGraphics graphics,
      final int left,
      final int rowY,
      final int width)
    {
        final Component hint = Component.translatable("com.beastofburden.gui.plan_editor.add_step_hint");
        graphics.drawString(font, hint, addRowStartX(left, width) + addRowPlusWidth() + addRowGap(), rowY + 7, 0x808080);
    }

    private void renderSpinnerValue(
      @NotNull final GuiGraphics graphics,
      final int spinnerX,
      final int rowY,
      final int value)
    {
        final int centerX = spinnerX + SPIN_BUTTON + SPIN_GAP + SPIN_VALUE / 2;
        graphics.drawCenteredString(font, String.valueOf(value), centerX, rowY + 7, 0xFFFFFF);
    }

    private int spinnerWidth()
    {
        return SPIN_BUTTON + SPIN_GAP + SPIN_VALUE + SPIN_GAP + SPIN_BUTTON;
    }

    private int addRowPlusWidth()
    {
        return 20;
    }

    private int addRowGap()
    {
        return 6;
    }

    private int addRowStartX(final int left, final int width)
    {
        final Component hint = Component.translatable("com.beastofburden.gui.plan_editor.add_step_hint");
        final int totalW = addRowPlusWidth() + addRowGap() + font.width(hint);
        return left + (width - totalW) / 2;
    }

    @NotNull
    private Component levelLabel()
    {
        return Component.translatable("com.beastofburden.gui.plan_editor.level_label");
    }

    @NotNull
    private Component countLabel()
    {
        return Component.translatable("com.beastofburden.gui.plan_editor.count_label");
    }

    private int labelWidth(@NotNull final Component label)
    {
        return font.width(label);
    }

    private void renderScrollbar(@NotNull final GuiGraphics graphics, final int top, final int bottom)
    {
        final int barLeft = width - MARGIN - SCROLLBAR;
        final int barHeight = bottom - top;
        graphics.fill(barLeft, top, barLeft + SCROLLBAR, bottom, 0x40FFFFFF);

        final int totalRows = draftSteps.size() + 1;
        final int thumbHeight = Math.max(16, barHeight * visibleRowCount() / Math.max(1, totalRows));
        final int travel = barHeight - thumbHeight;
        final int thumbTop = top + (totalRows == 0 ? 0 : scrollOffset * travel / Math.max(1, maxScroll()));
        graphics.fill(barLeft, thumbTop, barLeft + SCROLLBAR, thumbTop + thumbHeight, 0xB0FFFFFF);
    }

    private int listTop()
    {
        return HEADER;
    }

    private int listBottom()
    {
        return height - FOOTER;
    }

    private int contentLeft()
    {
        return MARGIN;
    }

    private int contentWidth()
    {
        return width - MARGIN * 2 - (maxScroll() > 0 ? SCROLLBAR + 4 : 0);
    }

    private int visibleRowCount()
    {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    private int maxScroll()
    {
        final int totalRows = draftSteps.size() + 1;
        return Math.max(0, totalRows - visibleRowCount());
    }

    private void openTypePicker(final int stepIndex)
    {
        Minecraft.getInstance().setScreen(new BuildingPickerScreen(this, new BuildingPickerScreen.SelectionHandler()
        {
            @Override
            public void onBuilding(@NotNull final PlannedBuildingType type)
            {
                final DraftStep step = draftSteps.get(stepIndex);
                step.setBuilding(type);
                markCustom();
                init();
            }

            @Override
            public void onFieldStep()
            {
                final DraftStep step = draftSteps.get(stepIndex);
                step.setField();
                markCustom();
                init();
            }
        }));
    }

    private void addStep()
    {
        if (draftSteps.size() >= PlanScriptValidator.MAX_STEPS)
        {
            return;
        }

        draftSteps.add(DraftStep.building(PlannedBuildingType.BUILDER));
        markCustom();
        scrollOffset = maxScroll();
        init();
    }

    private void moveStep(final int stepIndex, final int direction)
    {
        final int target = stepIndex + direction;
        if (target < 0 || target >= draftSteps.size())
        {
            return;
        }

        final DraftStep step = draftSteps.remove(stepIndex);
        draftSteps.add(target, step);
        markCustom();
        init();
    }

    private void removeStep(final int stepIndex)
    {
        if (stepIndex < 0 || stepIndex >= draftSteps.size())
        {
            return;
        }

        draftSteps.remove(stepIndex);
        markCustom();
        init();
    }

    private void adjustLevel(final int stepIndex, final int delta)
    {
        final DraftStep step = draftSteps.get(stepIndex);
        if (step.isField())
        {
            return;
        }

        step.setMinLevel(step.minLevel() + delta);
        markCustom();
        init();
    }

    private void adjustCount(final int stepIndex, final int delta)
    {
        draftSteps.get(stepIndex).setCount(draftSteps.get(stepIndex).count() + delta);
        markCustom();
        init();
    }

    private void resetToDefault()
    {
        draftSteps.clear();
        final FixedPlanScript defaults = FixedPlanScript.createDefault();
        for (int i = 0; i < defaults.stepCount(); i++)
        {
            draftSteps.addAll(DraftStep.expandFrom(defaults.getStep(i)));
        }
        customDraft = false;
        scrollOffset = 0;
        init();
    }

    private void savePlan()
    {
        if (draftSteps.isEmpty())
        {
            return;
        }

        final List<FixedPlanStep> steps = new ArrayList<>(draftSteps.size());
        for (final DraftStep draft : draftSteps)
        {
            steps.add(draft.toStep());
        }

        final FixedPlanScript script = new FixedPlanScript(FixedPlanScript.FORMAT_VERSION, customDraft, steps);
        ModNetwork.CHANNEL.sendToServer(new SaveColonyPlanMessage(buildingPos, script.writeToNbt()));
        onClose();
    }

    private void markCustom()
    {
        customDraft = true;
    }

    private static final class DraftStep
    {
        private FixedPlanRequirement.Kind kind = FixedPlanRequirement.Kind.BUILDING;
        private PlannedBuildingType buildingType = PlannedBuildingType.BUILDER;
        private int minLevel = 1;
        private int count = 1;

        @NotNull
        private static DraftStep building(@NotNull final PlannedBuildingType type)
        {
            final DraftStep step = new DraftStep();
            step.buildingType = type;
            return step;
        }

        @NotNull
        private static DraftStep fromRequirement(@NotNull final FixedPlanRequirement requirement)
        {
            final DraftStep step = new DraftStep();
            step.kind = requirement.getKind();
            step.buildingType = requirement.getBuildingType();
            step.minLevel = requirement.getMinLevel();
            step.count = requirement.getCount();
            return step;
        }

        @NotNull
        private static List<DraftStep> expandFrom(@NotNull final FixedPlanStep step)
        {
            final List<DraftStep> expanded = new ArrayList<>();
            for (final FixedPlanRequirement requirement : step.getRequirements())
            {
                expanded.add(fromRequirement(requirement));
            }
            if (expanded.isEmpty())
            {
                expanded.add(building(PlannedBuildingType.BUILDER));
            }
            return expanded;
        }

        @NotNull
        private FixedPlanStep toStep()
        {
            return new FixedPlanStep(List.of(toRequirement()));
        }

        @NotNull
        private FixedPlanRequirement toRequirement()
        {
            if (isField())
            {
                return FixedPlanRequirement.fields(count);
            }
            return FixedPlanRequirement.building(buildingType, minLevel, count);
        }

        private boolean isField()
        {
            return kind == FixedPlanRequirement.Kind.FIELD;
        }

        @NotNull
        private Component typeLabel()
        {
            if (isField())
            {
                return Component.translatable("com.beastofburden.gui.plan_editor.field_step");
            }
            return PlanStepFormatter.buildingName(buildingType);
        }

        @NotNull
        private net.minecraft.world.item.ItemStack icon()
        {
            return isField() ? PlannedBuildingIcons.fieldStack() : PlannedBuildingIcons.stackFor(buildingType);
        }

        private void setBuilding(@NotNull final PlannedBuildingType type)
        {
            kind = FixedPlanRequirement.Kind.BUILDING;
            buildingType = type;
        }

        private void setField()
        {
            kind = FixedPlanRequirement.Kind.FIELD;
            count = Math.max(1, count);
        }

        private int minLevel()
        {
            return minLevel;
        }

        private void setMinLevel(final int value)
        {
            minLevel = Math.max(PlanScriptValidator.MIN_LEVEL, Math.min(PlanScriptValidator.MAX_LEVEL, value));
        }

        private int count()
        {
            return count;
        }

        private void setCount(final int value)
        {
            count = Math.max(PlanScriptValidator.MIN_COUNT, Math.min(PlanScriptValidator.MAX_COUNT, value));
        }
    }
}
