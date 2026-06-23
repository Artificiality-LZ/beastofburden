package org.Artificial.beastofburden.colony.planning;

/**
 * Colony development phases from the CDAI design document.
 */
public enum ColonyPhase
{
    P0_FOUNDATION,
    P1_SURVIVAL,
    P2_EXPANSION,
    P3_INDUSTRIAL,
    P4_METROPOLIS;

    public static ColonyPhase fromId(final int id)
    {
        final ColonyPhase[] values = values();
        return id >= 0 && id < values.length ? values[id] : P0_FOUNDATION;
    }

    /** Target town hall level for the given development phase. */
    public int townHallTargetLevel()
    {
        return switch (this)
        {
            case P0_FOUNDATION -> 1;
            case P1_SURVIVAL -> 2;
            case P2_EXPANSION -> 3;
            case P3_INDUSTRIAL -> 4;
            case P4_METROPOLIS -> 5;
        };
    }
}
