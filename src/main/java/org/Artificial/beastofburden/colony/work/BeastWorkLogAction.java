package org.Artificial.beastofburden.colony.work;

/**
 * Recorded beast-of-burden work events.
 */
public enum BeastWorkLogAction
{
    GENERATED,
    DELIVERED,
    CANCELLED,
    PLANNED;

    public static BeastWorkLogAction fromId(final int id)
    {
        final BeastWorkLogAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : CANCELLED;
    }
}
