package org.Artificial.beastofburden.colony.work;

/**
 * Current phase of a beast-of-burden citizen.
 */
public enum BeastWorkPhase
{
    IDLE,
    GENERATING,
    DELIVERING;

    public static BeastWorkPhase fromId(final int id)
    {
        final BeastWorkPhase[] values = values();
        return id >= 0 && id < values.length ? values[id] : IDLE;
    }
}
