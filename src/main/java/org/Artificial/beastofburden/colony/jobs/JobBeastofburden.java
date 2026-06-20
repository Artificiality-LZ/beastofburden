package org.Artificial.beastofburden.colony.jobs;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import net.minecraft.resources.ResourceLocation;
import org.Artificial.beastofburden.entity.ai.EntityAIBeastofburden;
import org.jetbrains.annotations.NotNull;

/**
 * The Beast of Burden job.
 * <p>
 * Citizens assigned to this job act as a general labourer / "beast of burden" for the colony.
 */
public class JobBeastofburden extends AbstractJob<EntityAIBeastofburden, JobBeastofburden>
{
    public JobBeastofburden(@NotNull final ICitizenData citizen)
    {
        super(citizen);
    }

    @NotNull
    @Override
    public EntityAIBeastofburden generateAI()
    {
        return new EntityAIBeastofburden(this);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.CITIZEN_ID;
    }
}
