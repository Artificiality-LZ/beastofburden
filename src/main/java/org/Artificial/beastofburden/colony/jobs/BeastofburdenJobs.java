package org.Artificial.beastofburden.colony.jobs;

import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.Artificial.beastofburden.Beastofburden;

/**
 * Holds the DeferredRegister and {@link RegistryObject}s for all custom MineColonies jobs added by this mod.
 */
public final class BeastofburdenJobs
{
    /**
     * The MineColonies custom Forge registry for jobs is named {@code minecolonies:jobs}.
     */
    public static final DeferredRegister<JobEntry> JOBS =
      DeferredRegister.create(ResourceLocation.fromNamespaceAndPath("minecolonies", "jobs"), Beastofburden.MODID);

    public static final ResourceLocation BEASTOFBURDEN_ID =
      ResourceLocation.fromNamespaceAndPath(Beastofburden.MODID, "beastofburden");

    public static final RegistryObject<JobEntry> BEASTOFBURDEN =
      JOBS.register(BEASTOFBURDEN_ID.getPath(), () -> new JobEntry.Builder()
        .setJobProducer(JobBeastofburden::new)
        .setJobViewProducer(() -> DefaultJobView::new)
        .setRegistryName(BEASTOFBURDEN_ID)
        .createJobEntry());

    private BeastofburdenJobs()
    {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Attaches the job deferred register to the mod event bus.
     *
     * @param modEventBus the mod event bus.
     */
    public static void register(final IEventBus modEventBus)
    {
        JOBS.register(modEventBus);
    }
}
