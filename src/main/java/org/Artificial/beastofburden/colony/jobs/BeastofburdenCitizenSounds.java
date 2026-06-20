package org.Artificial.beastofburden.colony.jobs;

import com.minecolonies.api.sounds.ModSoundEvents;
import com.mojang.logging.LogUtils;
import org.Artificial.beastofburden.Beastofburden;
import org.slf4j.Logger;

/**
 * Registers citizen voice lines for the BeastOfBurden job.
 * <p>
 * MineColonies builds {@link ModSoundEvents#CITIZEN_SOUND_EVENTS} during static init from jobs that
 * already exist at class-load time. Custom jobs registered later must add their own entry, otherwise
 * bumping into a citizen crashes in {@link com.minecolonies.api.util.SoundUtils}.
 */
public final class BeastofburdenCitizenSounds
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Reuse builder voice lines until dedicated BeastOfBurden audio is added.
     */
    private static final String FALLBACK_JOB_KEY = "builder";

    private BeastofburdenCitizenSounds()
    {
    }

    public static void register()
    {
        final String jobKey = BeastofburdenJobs.BEASTOFBURDEN_ID.getPath();
        if (ModSoundEvents.CITIZEN_SOUND_EVENTS.containsKey(jobKey))
        {
            return;
        }

        final var fallbackSounds = ModSoundEvents.CITIZEN_SOUND_EVENTS.get(FALLBACK_JOB_KEY);
        if (fallbackSounds == null)
        {
            LOGGER.error("[{}] Unable to register citizen sounds for '{}': fallback job '{}' was not found.",
              Beastofburden.MODID, jobKey, FALLBACK_JOB_KEY);
            return;
        }

        ModSoundEvents.CITIZEN_SOUND_EVENTS.put(jobKey, fallbackSounds);
        LOGGER.info("[{}] Registered citizen sounds for job '{}' (using '{}' voice set).",
          Beastofburden.MODID, jobKey, FALLBACK_JOB_KEY);
    }
}
