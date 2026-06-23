package org.Artificial.beastofburden.config;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Editable BeastOfBurden common-config payload.
 */
public record ConfigSnapshot(
  int baseGenerationTicks,
  int minGenerationTicks,
  double ticksPerItemValue,
  double strengthSpeedBonus,
  int defaultItemValue,
  boolean deriveFromRecipes,
  int workLogMaxEntries,
  int workLogHistoryDays,
  boolean planningInstantBuildDebug,
  @NotNull Map<Item, Integer> itemValues)
{
    @NotNull
    public static ConfigSnapshot fromCurrent()
    {
        return new ConfigSnapshot(
          org.Artificial.beastofburden.Config.baseGenerationTicks,
          org.Artificial.beastofburden.Config.minGenerationTicks,
          org.Artificial.beastofburden.Config.ticksPerItemValue,
          org.Artificial.beastofburden.Config.strengthSpeedBonus,
          org.Artificial.beastofburden.Config.defaultItemValue,
          org.Artificial.beastofburden.Config.deriveFromRecipes,
          org.Artificial.beastofburden.Config.workLogMaxEntries,
          org.Artificial.beastofburden.Config.workLogHistoryDays,
          org.Artificial.beastofburden.Config.planningInstantBuildDebug,
          org.Artificial.beastofburden.Config.copyExplicitItemValues()
        );
    }
}
