package org.Artificial.beastofburden.config;

import org.Artificial.beastofburden.Config;
import org.jetbrains.annotations.NotNull;

/**
 * Persists in-memory config changes to {@code beastofburden-common.toml}.
 */
public final class ConfigPersistence
{
    private ConfigPersistence()
    {
    }

    public static void saveCommonConfig()
    {
        Config.saveCommonConfig();
    }

    public static void applyAndSave(@NotNull final ConfigSnapshot snapshot)
    {
        Config.applyRuntimeValues(
          snapshot.baseGenerationTicks(),
          snapshot.minGenerationTicks(),
          snapshot.ticksPerItemValue(),
          snapshot.strengthSpeedBonus(),
          snapshot.defaultItemValue(),
          snapshot.deriveFromRecipes(),
          snapshot.workLogMaxEntries(),
          snapshot.workLogHistoryDays(),
          snapshot.planningInstantBuildDebug(),
          snapshot.itemValues()
        );
        saveCommonConfig();
    }
}
