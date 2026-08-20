package org.Artificial.beastofburden.colony.planning;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Buildings the autonomous planner can request, mapped to MineColonies entries.
 */
public enum PlannedBuildingType
{
    BUILDER(ModBuildings.builder, ModBuildings.BUILDER_ID, BuildingCategory.INFRASTRUCTURE, false),
    TOWN_HALL(ModBuildings.townHall, ModBuildings.TOWNHALL_ID, BuildingCategory.INFRASTRUCTURE, false),
    RESIDENCE(ModBuildings.home, ModBuildings.HOME_ID, BuildingCategory.INFRASTRUCTURE, false),
    FISHER(ModBuildings.fisherman, ModBuildings.FISHERMAN_ID, BuildingCategory.FOOD, false),
    FARMER(ModBuildings.farmer, ModBuildings.FARMER_ID, BuildingCategory.FOOD, true),
    FORESTER(ModBuildings.lumberjack, ModBuildings.LUMBERJACK_ID, BuildingCategory.RESOURCE, true),
    MINER(ModBuildings.miner, ModBuildings.MINER_ID, BuildingCategory.RESOURCE, true),
    GUARD_TOWER(ModBuildings.guardTower, ModBuildings.GUARD_TOWER_ID, BuildingCategory.DEFENSE, true),
    WAREHOUSE(ModBuildings.wareHouse, ModBuildings.WAREHOUSE_ID, BuildingCategory.LOGISTICS, false),
    COURIER(ModBuildings.deliveryman, ModBuildings.DELIVERYMAN_ID, BuildingCategory.LOGISTICS, false),
    TAVERN(ModBuildings.tavern, ModBuildings.TAVERN_ID, BuildingCategory.INFRASTRUCTURE, false),
    SAWMILL(ModBuildings.sawmill, ModBuildings.SAWMILL_ID, BuildingCategory.CRAFTING, false),
    STONEMASON(ModBuildings.stoneMason, ModBuildings.STONE_MASON_ID, BuildingCategory.CRAFTING, false),
    BLACKSMITH(ModBuildings.blacksmith, ModBuildings.BLACKSMITH_ID, BuildingCategory.CRAFTING, false),
    SMELTERY(ModBuildings.smeltery, ModBuildings.SMELTERY_ID, BuildingCategory.CRAFTING, false),
    UNIVERSITY(ModBuildings.university, ModBuildings.UNIVERSITY_ID, BuildingCategory.CIVIC, false),
    LIBRARY(ModBuildings.library, ModBuildings.LIBRARY_ID, BuildingCategory.CIVIC, false),
    SCHOOL(ModBuildings.school, ModBuildings.SCHOOL_ID, BuildingCategory.CIVIC, false),
    HOSPITAL(ModBuildings.hospital, ModBuildings.HOSPITAL_ID, BuildingCategory.CIVIC, false),
    BARRACKS(ModBuildings.barracks, ModBuildings.BARRACKS_ID, BuildingCategory.DEFENSE, false),
    MYSTICAL_SITE(ModBuildings.mysticalSite, ModBuildings.MYSTICAL_SITE_ID, BuildingCategory.CIVIC, false),
    COOK(ModBuildings.cook, ModBuildings.COOK_ID, BuildingCategory.FOOD, false),
    BAKERY(ModBuildings.bakery, ModBuildings.BAKERY_ID, BuildingCategory.FOOD, false),
    COMPOSTER(ModBuildings.composter, ModBuildings.COMPOSTER_ID, BuildingCategory.RESOURCE, false),
    SHEPHERD(ModBuildings.shepherd, ModBuildings.SHEPHERD_ID, BuildingCategory.FOOD, true),
    COWBOY(ModBuildings.cowboy, ModBuildings.COWBOY_ID, BuildingCategory.FOOD, true),
    CHICKEN_HERDER(ModBuildings.chickenHerder, ModBuildings.CHICKENHERDER_ID, BuildingCategory.FOOD, true),
    SWINE_HERDER(ModBuildings.swineHerder, ModBuildings.SWINE_HERDER_ID, BuildingCategory.FOOD, true),
    GLASSBLOWER(ModBuildings.glassblower, ModBuildings.GLASSBLOWER_ID, BuildingCategory.CRAFTING, false),
    FLETCHER(ModBuildings.fletcher, ModBuildings.FLETCHER_ID, BuildingCategory.CRAFTING, false),
    ENCHANTER(ModBuildings.enchanter, ModBuildings.ENCHANTER_ID, BuildingCategory.CRAFTING, false),
    STONE_SMELTERY(ModBuildings.stoneSmelter, ModBuildings.STONE_SMELTERY_ID, BuildingCategory.CRAFTING, false),
    CRUSHER(ModBuildings.crusher, ModBuildings.CRUSHER_ID, BuildingCategory.CRAFTING, false),
    SIFTER(ModBuildings.sifter, ModBuildings.SIFTER_ID, BuildingCategory.CRAFTING, false),
    FLORIST(ModBuildings.florist, ModBuildings.FLORIST_ID, BuildingCategory.RESOURCE, false),
    ARCHERY(ModBuildings.archery, ModBuildings.ARCHERY_ID, BuildingCategory.DEFENSE, false),
    COMBAT_ACADEMY(ModBuildings.combatAcademy, ModBuildings.COMBAT_ACADEMY_ID, BuildingCategory.DEFENSE, false);

    private final RegistryObject<BuildingEntry> entry;
    private final String schematicId;
    private final BuildingCategory category;
    private final boolean capacityBuilding;

    PlannedBuildingType(
      final RegistryObject<BuildingEntry> entry,
      @NotNull final String schematicId,
      @NotNull final BuildingCategory category,
      final boolean capacityBuilding)
    {
        this.entry = entry;
        this.schematicId = schematicId;
        this.category = category;
        this.capacityBuilding = capacityBuilding;
    }

    @NotNull
    public BuildingEntry getEntry()
    {
        return entry.get();
    }

    @NotNull
    public String getSchematicId()
    {
        return schematicId;
    }

    @NotNull
    public BuildingCategory getCategory()
    {
        return category;
    }

    public boolean isCapacityBuilding()
    {
        return capacityBuilding;
    }

    @NotNull
    public String getBlueprintName()
    {
        final var block = getEntry().getBuildingBlock();
        if (block instanceof AbstractBlockHut<?>)
        {
            return ((AbstractBlockHut<?>) block).getBlueprintName();
        }
        return schematicId;
    }

    @Nullable
    public static PlannedBuildingType fromEntry(@NotNull final BuildingEntry entry)
    {
        for (final PlannedBuildingType type : values())
        {
            if (type.matches(entry))
            {
                return type;
            }
        }
        return null;
    }

    public boolean matches(@NotNull final BuildingEntry entry)
    {
        return entry.getTranslationKey().equals(getEntry().getTranslationKey());
    }

    @NotNull
    public static PlannedBuildingType fromSchematicId(@NotNull final String schematicId)
    {
        for (final PlannedBuildingType type : values())
        {
            if (type.schematicId.equals(schematicId))
            {
                return type;
            }
        }
        return BUILDER;
    }

    public enum BuildingCategory
    {
        INFRASTRUCTURE,
        FOOD,
        RESOURCE,
        CRAFTING,
        LOGISTICS,
        DEFENSE,
        CIVIC
    }
}
