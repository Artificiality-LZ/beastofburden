package org.Artificial.beastofburden.colony.planning;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Formats autonomous-planning tokens for Town Hall GUI display.
 */
public final class PlanningDisplayFormatter
{
    private static final String PREFIX_PLANNED = "planned:";
    private static final String PREFIX_BUILDERS_BUSY = "builders_busy:";
    private static final String LANG_PREFIX = "com.beastofburden.gui.townhall.planning.";

    private PlanningDisplayFormatter()
    {
    }

    /**
     * Compact display token stored in work logs and planning reports.
     */
    @NotNull
    public static String formatPlannedToken(@NotNull final BuildTask task)
    {
        if (task.getAction() == BuildTaskAction.PLACE_FIELD)
        {
            return PREFIX_PLANNED + "field";
        }

        if (task.getAction() == BuildTaskAction.UPGRADE)
        {
            return PREFIX_PLANNED + "upgrade:" + task.getType().getSchematicId();
        }

        return PREFIX_PLANNED + task.getType().getSchematicId();
    }

    @NotNull
    public static Component formatDecision(@NotNull final String decision)
    {
        if (decision.isEmpty())
        {
            return Component.empty();
        }

        if ("scripted_complete".equals(decision))
        {
            return Component.translatable("com.beastofburden.gui.townhall.scripted.complete");
        }

        if (decision.startsWith(PREFIX_BUILDERS_BUSY))
        {
            return Component.translatable(
              LANG_PREFIX + "builders_busy",
              decision.substring(PREFIX_BUILDERS_BUSY.length())
            );
        }

        if (decision.startsWith(PREFIX_PLANNED))
        {
            return formatPlannedToken(decision.substring(PREFIX_PLANNED.length()));
        }

        final int atIndex = decision.indexOf('@');
        if (atIndex > 0)
        {
            return buildingName(decision.substring(0, atIndex).trim());
        }

        final Component translated = Component.translatable(LANG_PREFIX + decision);
        return translated.getString().equals(LANG_PREFIX + decision) ? Component.literal(decision) : translated;
    }

    @NotNull
    public static Component formatCooldownDetail(final int retryCooldown)
    {
        if (retryCooldown <= 0)
        {
            return Component.empty();
        }

        return Component.translatable(LANG_PREFIX + "cooldown_ticks", retryCooldown);
    }

    @NotNull
    public static Component formatDetail(@NotNull final String detail)
    {
        if (detail.isEmpty() || isDebugDetail(detail))
        {
            return Component.empty();
        }

        if (detail.startsWith(PREFIX_PLANNED))
        {
            return formatPlannedToken(detail.substring(PREFIX_PLANNED.length()));
        }

        if (isKnownPlanningToken(detail))
        {
            return Component.translatable(LANG_PREFIX + detail);
        }

        return Component.empty();
    }

    private static boolean isDebugDetail(@NotNull final String detail)
    {
        if (detail.startsWith("heuristic ")
              || detail.startsWith("step_")
              || detail.startsWith("scripted_complete")
              || detail.startsWith("retry in ")
              || detail.startsWith("cold_start:"))
        {
            return true;
        }

        if (detail.contains(":generating")
              || detail.contains(":delivering")
              || detail.contains(":inFlight=")
              || detail.contains(":wrong_job")
              || detail.contains(":no_ai"))
        {
            return true;
        }

        return detail.contains(" @ ")
              || detail.contains("BUILD_NEW")
              || detail.contains("UPGRADE")
              || detail.contains("PLACE_FIELD");
    }

    private static boolean isKnownPlanningToken(@NotNull final String token)
    {
        final Component translated = Component.translatable(LANG_PREFIX + token);
        return !translated.getString().equals(LANG_PREFIX + token);
    }

    @NotNull
    private static Component formatPlannedToken(@NotNull final String token)
    {
        if ("field".equals(token))
        {
            return Component.translatable(LANG_PREFIX + "planned_field");
        }

        if (token.startsWith("upgrade:"))
        {
            final String schematicId = token.substring("upgrade:".length());
            return Component.translatable(
              LANG_PREFIX + "planned_upgrade",
              buildingName(schematicId)
            );
        }

        return buildingName(token);
    }

    @NotNull
    private static Component buildingName(@NotNull final String schematicId)
    {
        return PlanStepFormatter.buildingName(PlannedBuildingType.fromSchematicId(schematicId));
    }
}
