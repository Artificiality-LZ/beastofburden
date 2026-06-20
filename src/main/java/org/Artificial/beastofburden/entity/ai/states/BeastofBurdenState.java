package org.Artificial.beastofburden.entity.ai.states;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;

/**
 * AI states for the Beast of Burden worker.
 */
public enum BeastofBurdenState implements IAIState
{
    /**
     * Generating the requested item (timed work).
     */
    GENERATE_ITEM(false),

    /**
     * Walking to the requester to deliver the generated item.
     */
    DELIVER_ITEM(false);

    private final boolean okayToEat;

    BeastofBurdenState(final boolean okayToEat)
    {
        this.okayToEat = okayToEat;
    }

    @Override
    public boolean isOkayToEat()
    {
        return okayToEat;
    }
}
