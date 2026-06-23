package org.Artificial.beastofburden.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.Artificial.beastofburden.Beastofburden;
import org.Artificial.beastofburden.colony.planning.ColonyPlannerDriver;
import org.Artificial.beastofburden.colony.planning.PlanningInstantBuildState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server commands for BeastOfBurden debug and admin features.
 */
public final class BeastofburdenCommands
{
    private BeastofburdenCommands()
    {
    }

    public static void register(@NotNull final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
          Commands.literal(Beastofburden.MODID)
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("planningInstantBuild")
              .then(Commands.literal("on")
                .executes(context -> setInstantBuild(context.getSource(), true)))
              .then(Commands.literal("off")
                .executes(context -> setInstantBuild(context.getSource(), false)))
              .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> setInstantBuild(
                  context.getSource(),
                  BoolArgumentType.getBool(context, "enabled")
                )))
            )
            .then(Commands.literal("refreshPlanningCooldown")
              .executes(context -> refreshPlanningCooldown(context.getSource(), null))
              .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(context -> refreshPlanningCooldown(
                  context.getSource(),
                  IntegerArgumentType.getInteger(context, "colonyId")
                )))
            )
        );
    }

    private static int refreshPlanningCooldown(@NotNull final CommandSourceStack source, @Nullable final Integer colonyId)
    {
        final int updated = ColonyPlannerDriver.refreshPlanningCooldown(colonyId);
        if (updated <= 0)
        {
            source.sendFailure(Component.translatable("com.beastofburden.command.refresh_planning_cooldown.none"));
            return 0;
        }

        source.sendSuccess(
          () -> colonyId != null
            ? Component.translatable("com.beastofburden.command.refresh_planning_cooldown.colony", colonyId)
            : Component.translatable("com.beastofburden.command.refresh_planning_cooldown.all", updated),
          true
        );
        return updated;
    }

    private static int setInstantBuild(@NotNull final CommandSourceStack source, final boolean enabled)
    {
        PlanningInstantBuildState.setEnabled(enabled);
        source.sendSuccess(
          () -> Component.translatable(
            enabled
              ? "com.beastofburden.command.planning_instant_build.enabled"
              : "com.beastofburden.command.planning_instant_build.disabled"
          ),
          true
        );
        return 1;
    }
}
