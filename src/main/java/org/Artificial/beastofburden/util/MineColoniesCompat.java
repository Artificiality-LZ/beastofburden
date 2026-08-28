package org.Artificial.beastofburden.util;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.managers.interfaces.IRegisteredStructureManager;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Runtime bridges for MineColonies API renames across 1.1.873 and newer builds.
 * <p>
 * Compile target remains 1.1.873; callers must not bytecode-link renamed methods
 * (e.g. {@code IColony.getBuildingManager()} or {@code IRegisteredStructureManager.getTownHall()}
 * with an {@code ITownHall} return) or the JVM throws {@link NoSuchMethodError} on 1.1.1214+.
 * The same applies to {@link AITarget} constructors that swapped JDK functional types for
 * MineColonies SAM interfaces on 1.1.1197+, to {@link IBuilding#onUpgradeComplete} which gained
 * a {@link Blueprint} parameter on 1214+, and to {@link BuildingEntry#getBuildingBlock()} whose
 * return type changed from {@code AbstractBlockHut} to {@code AbstractColonyBlock}.
 */
public final class MineColoniesCompat
{
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final String I_BOOLEAN_CONDITION_SUPPLIER =
      "com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.IBooleanConditionSupplier";
    private static final String I_STATE_SUPPLIER =
      "com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.IStateSupplier";

    @Nullable
    private static final MethodHandle STRUCTURE_MANAGER_GETTER = resolveStructureManagerGetter();

    @Nullable
    private static final MethodHandle TOWN_HALL_GETTER = resolveTownHallGetter();

    @Nullable
    private static final MethodHandle BUILDING_BLOCK_GETTER = resolveBuildingBlockGetter();

    @Nullable
    private static final MethodHandle ON_UPGRADE_COMPLETE_LEGACY = resolveOnUpgradeCompleteLegacy();

    @Nullable
    private static final MethodHandle ON_UPGRADE_COMPLETE_WITH_BLUEPRINT = resolveOnUpgradeCompleteWithBlueprint();

    @Nullable
    private static final MethodHandle AI_TARGET_FOUR_ARG = resolveAiTargetFourArg();

    @Nullable
    private static final MethodHandle AI_TARGET_THREE_ARG = resolveAiTargetThreeArg();

    private MineColoniesCompat()
    {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves the colony structure/building manager.
     * Prefers {@code getServerBuildingManager()} (1214+), then {@code getBuildingManager()} (873).
     */
    @Nullable
    public static IRegisteredStructureManager getStructureManager(@NotNull final IColony colony)
    {
        if (STRUCTURE_MANAGER_GETTER == null)
        {
            return null;
        }

        try
        {
            final Object manager = STRUCTURE_MANAGER_GETTER.invoke(colony);
            return manager instanceof IRegisteredStructureManager registered ? registered : null;
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn("Failed to resolve colony structure manager for colony {}: {}", colony.getID(), ex.toString());
            return null;
        }
    }

    /**
     * Resolves the town hall from a structure manager.
     * <p>
     * On 873 the interface method returns {@link ITownHall}; on 1214+ it lives on a parent
     * interface as {@code T getTownHall()} and erases to {@code Object}. Direct invoke of the
     * 873 descriptor crashes with {@link NoSuchMethodError}.
     */
    @Nullable
    public static IBuilding getTownHall(@Nullable final IRegisteredStructureManager manager)
    {
        if (manager == null || TOWN_HALL_GETTER == null)
        {
            return null;
        }

        try
        {
            final Object townHall = TOWN_HALL_GETTER.invoke(manager);
            return townHall instanceof IBuilding building ? building : null;
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn("Failed to resolve town hall from structure manager: {}", ex.toString());
            return null;
        }
    }

    /**
     * Resolves the hut/colony block for a building entry.
     * <p>
     * On 873 the method returns {@code AbstractBlockHut}; on 1214+ it returns
     * {@code AbstractColonyBlock}. Bytecode linking either descriptor crashes on the other build.
     */
    @Nullable
    public static Block getBuildingBlock(@NotNull final BuildingEntry entry)
    {
        if (BUILDING_BLOCK_GETTER == null)
        {
            return null;
        }

        try
        {
            final Object block = BUILDING_BLOCK_GETTER.invoke(entry);
            return block instanceof Block resolved ? resolved : null;
        }
        catch (final Throwable ex)
        {
            BeastofBurdenLog.warn("Failed to resolve building block for {}: {}", entry.getRegistryName(), ex.toString());
            return null;
        }
    }

    /**
     * Notifies a building that an upgrade has completed.
     * <p>
     * On 873 the API is {@code onUpgradeComplete(int)}; on 1214+ it is
     * {@code onUpgradeComplete(Blueprint, int)} as used by {@code SurvivalHandler}.
     */
    public static void onUpgradeComplete(
      @NotNull final IBuilding building,
      @Nullable final Blueprint blueprint,
      final int newLevel)
    {
        if (ON_UPGRADE_COMPLETE_WITH_BLUEPRINT != null)
        {
            try
            {
                ON_UPGRADE_COMPLETE_WITH_BLUEPRINT.invoke(building, blueprint, newLevel);
                return;
            }
            catch (final Throwable ex)
            {
                throw new IllegalStateException("Failed to invoke IBuilding.onUpgradeComplete(Blueprint, int).", ex);
            }
        }

        if (ON_UPGRADE_COMPLETE_LEGACY != null)
        {
            try
            {
                ON_UPGRADE_COMPLETE_LEGACY.invoke(building, newLevel);
                return;
            }
            catch (final Throwable ex)
            {
                throw new IllegalStateException("Failed to invoke IBuilding.onUpgradeComplete(int).", ex);
            }
        }

        throw new IllegalStateException("No compatible IBuilding.onUpgradeComplete method found.");
    }

    /**
     * Builds an {@link AITarget} with a predicate and next-state supplier.
     * <p>
     * On 873 the constructor takes {@link BooleanSupplier}/{@link Supplier}; on 1197+ it takes
     * MineColonies SAM types with the same shapes. Callers pass method references or lambdas;
     * this method selects the constructor at runtime and passes them through unchanged (same as
     * {@code AbstractEntityAIBasic} on 1214+).
     */
    @NotNull
    public static <S extends IState> AITarget<S> aiTarget(
      @NotNull final S state,
      @NotNull final BooleanSupplier predicate,
      @NotNull final Supplier<S> action,
      final int tickRate)
    {
        if (AI_TARGET_FOUR_ARG == null)
        {
            throw new IllegalStateException("No compatible AITarget(IState, predicate, action, int) constructor found.");
        }

        try
        {
            @SuppressWarnings("unchecked")
            final AITarget<S> target = (AITarget<S>) AI_TARGET_FOUR_ARG.invoke(state, predicate, action, tickRate);
            return target;
        }
        catch (final Throwable ex)
        {
            throw new IllegalStateException("Failed to construct AITarget with predicate/action.", ex);
        }
    }

    /**
     * Builds an {@link AITarget} that always runs when in {@code state} and returns the next state
     * from {@code action}.
     */
    @NotNull
    public static <S extends IState> AITarget<S> aiTarget(
      @NotNull final S state,
      @NotNull final Supplier<S> action,
      final int tickRate)
    {
        if (AI_TARGET_THREE_ARG != null)
        {
            try
            {
                @SuppressWarnings("unchecked")
                final AITarget<S> target = (AITarget<S>) AI_TARGET_THREE_ARG.invoke(state, action, tickRate);
                return target;
            }
            catch (final Throwable ex)
            {
                throw new IllegalStateException("Failed to construct AITarget with action.", ex);
            }
        }

        return aiTarget(state, () -> true, action, tickRate);
    }

    @Nullable
    private static MethodHandle resolveStructureManagerGetter()
    {
        final MethodType type = MethodType.methodType(IRegisteredStructureManager.class);
        final MethodHandle newer = findVirtual(IColony.class, "getServerBuildingManager", type);
        if (newer != null)
        {
            return newer;
        }

        final MethodHandle legacy = findVirtual(IColony.class, "getBuildingManager", type);
        if (legacy == null)
        {
            BeastofBurdenLog.warn(
              "Neither IColony.getServerBuildingManager nor IColony.getBuildingManager is present; building enumeration disabled."
            );
        }
        return legacy;
    }

    @Nullable
    private static MethodHandle resolveTownHallGetter()
    {
        final MethodHandle asTownHall = findVirtual(
          IRegisteredStructureManager.class,
          "getTownHall",
          MethodType.methodType(ITownHall.class)
        );
        if (asTownHall != null)
        {
            return asTownHall;
        }

        final MethodHandle asObject = findVirtual(
          IRegisteredStructureManager.class,
          "getTownHall",
          MethodType.methodType(Object.class)
        );
        if (asObject != null)
        {
            return asObject;
        }

        // 1214+: method may only be declared on ICommonRegisteredStructureManager with Object erasure.
        final MethodHandle fromHierarchy = findZeroArgMethod(IRegisteredStructureManager.class, "getTownHall");
        if (fromHierarchy == null)
        {
            BeastofBurdenLog.warn("IRegisteredStructureManager.getTownHall is not resolvable; town-hall lookup disabled.");
        }
        return fromHierarchy;
    }

    @Nullable
    private static MethodHandle resolveBuildingBlockGetter()
    {
        final MethodHandle handle = findZeroArgMethod(BuildingEntry.class, "getBuildingBlock");
        if (handle == null)
        {
            BeastofBurdenLog.warn("BuildingEntry.getBuildingBlock is not resolvable; hut block lookup disabled.");
        }
        return handle;
    }

    @Nullable
    private static MethodHandle resolveOnUpgradeCompleteLegacy()
    {
        return findVirtual(
          IBuilding.class,
          "onUpgradeComplete",
          MethodType.methodType(void.class, int.class)
        );
    }

    @Nullable
    private static MethodHandle resolveOnUpgradeCompleteWithBlueprint()
    {
        final MethodHandle handle = findVirtual(
          IBuilding.class,
          "onUpgradeComplete",
          MethodType.methodType(void.class, Blueprint.class, int.class)
        );
        if (handle == null)
        {
            return null;
        }

        if (ON_UPGRADE_COMPLETE_LEGACY != null)
        {
            BeastofBurdenLog.warn(
              "Both IBuilding.onUpgradeComplete(int) and onUpgradeComplete(Blueprint, int) are present; using Blueprint variant."
            );
        }
        return handle;
    }

    @Nullable
    private static MethodHandle findZeroArgMethod(@NotNull final Class<?> type, @NotNull final String name)
    {
        for (final Method method : type.getMethods())
        {
            if (!name.equals(method.getName()) || method.getParameterCount() != 0)
            {
                continue;
            }

            try
            {
                return LOOKUP.unreflect(method);
            }
            catch (final IllegalAccessException ignored)
            {
                // try next
            }
        }
        return null;
    }

    @Nullable
    private static MethodHandle findVirtual(
      @NotNull final Class<?> owner,
      @NotNull final String name,
      @NotNull final MethodType type)
    {
        try
        {
            return MethodHandles.publicLookup().findVirtual(owner, name, type);
        }
        catch (final NoSuchMethodException | IllegalAccessException ignored)
        {
            return null;
        }
    }

    @Nullable
    private static MethodHandle resolveAiTargetFourArg()
    {
        final MethodHandle legacy = findConstructor(
          AITarget.class,
          MethodType.methodType(void.class, IState.class, BooleanSupplier.class, Supplier.class, int.class)
        );
        if (legacy != null)
        {
            return legacy;
        }

        final Class<?> predicateType = loadClass(I_BOOLEAN_CONDITION_SUPPLIER);
        final Class<?> actionType = loadClass(I_STATE_SUPPLIER);
        if (predicateType == null || actionType == null)
        {
            BeastofBurdenLog.warn(
              "AITarget four-arg constructor not found (neither BooleanSupplier/Supplier nor custom SAM types)."
            );
            return null;
        }

        final MethodHandle newer = findConstructor(
          AITarget.class,
          MethodType.methodType(void.class, IState.class, predicateType, actionType, int.class)
        );
        if (newer == null)
        {
            BeastofBurdenLog.warn("AITarget four-arg constructor not resolvable on this MineColonies build.");
        }
        return newer;
    }

    @Nullable
    private static MethodHandle resolveAiTargetThreeArg()
    {
        final MethodHandle legacy = findConstructor(
          AITarget.class,
          MethodType.methodType(void.class, IState.class, Supplier.class, int.class)
        );
        if (legacy != null)
        {
            return legacy;
        }

        final Class<?> actionType = loadClass(I_STATE_SUPPLIER);
        if (actionType == null)
        {
            return null;
        }

        return findConstructor(
          AITarget.class,
          MethodType.methodType(void.class, IState.class, actionType, int.class)
        );
    }

    @Nullable
    private static MethodHandle findConstructor(@NotNull final Class<?> owner, @NotNull final MethodType type)
    {
        try
        {
            return MethodHandles.publicLookup().findConstructor(owner, type);
        }
        catch (final NoSuchMethodException | IllegalAccessException ignored)
        {
            return null;
        }
    }

    @Nullable
    private static Class<?> loadClass(@NotNull final String name)
    {
        try
        {
            return Class.forName(name);
        }
        catch (final ClassNotFoundException ignored)
        {
            return null;
        }
    }
}
