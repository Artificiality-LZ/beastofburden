package org.Artificial.beastofburden.util;

import com.mojang.logging.LogUtils;
import org.Artificial.beastofburden.Config;
import org.slf4j.Logger;

/**
 * Debug logging for beast-of-burden request detection and AI.
 */
public final class BeastofBurdenLog
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private BeastofBurdenLog()
    {
    }

    public static void info(final String message, final Object... arguments)
    {
        if (Config.debugLogging)
        {
            LOGGER.info("[beastofburden] " + message, arguments);
        }
    }

    public static void warn(final String message, final Object... arguments)
    {
        LOGGER.warn("[beastofburden] " + message, arguments);
    }

    public static void debug(final String message, final Object... arguments)
    {
        if (Config.debugLogging)
        {
            LOGGER.info("[beastofburden] " + message, arguments);
        }
    }
}
