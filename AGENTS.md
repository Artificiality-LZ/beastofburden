# BeastOfBurden — Agent Guide

> No previous `AGENTS.md` existed in this repository. This file is the initial agent-oriented summary based on the actual project contents.

## Project Overview

BeastOfBurden is a **Minecraft Forge 1.20.1 mod** that acts as an **add-on for MineColonies**. It adds a new Town-Hall-based citizen job, the *Beast of Burden*, and an optional autonomous colony-planning system.

- **Mod ID:** `beastofburden`
- **Maven group:** `org.Artificial`
- **Base package:** `org.Artificial.beastofburden` (note the capital `A` in `Artificial`)
- **Display name:** `BeastOfBurden`
- **Version:** `1.0-SNAPSHOT`
- **License:** `GPL-3.0-only` (as declared in `gradle.properties`)

The mod is developed from the Forge MDK. The codebase is in active development, especially in the `colony.planning` package.

## Technology Stack

| Component | Version / Source |
|-----------|------------------|
| Minecraft | `1.20.1` |
| Forge | `47.4.20` |
| Java | `17` |
| Gradle | `8.8` (wrapper) |
| ForgeGradle | `[6.0.16,6.2)` |
| Mixin | `0.8.5` (`mixingradle:0.7-SNAPSHOT`) |
| Mappings | Parchment `2023.06.26-1.20.1` |

### Required Mod Dependencies

Declared in `build.gradle` / `gradle.properties`:

- **MineColonies** — `curse.maven:minecolonies-245506:6444411` (target MineColonies `1.20.1-1.1.873-snapshot`)
- **Structurize** — `com.ldtteam:structurize:1.20.1-1.0.768-snapshot`
- **BlockUI** — `com.ldtteam:blockui:1.20.1-1.0.190-snapshot`
- **Domum Ornamentum** — `com.ldtteam:domum_ornamentum:1.20.1-1.0.285-snapshot:universal`
- **Multi-Piston** — `com.ldtteam:multipiston:1.20-1.2.43-RELEASE`

### Maven Repositories Used

- Forge Maven (plugins)
- MinecraftForge / ParchmentMC plugin portals
- LDTTeam Modding Maven (`https://ldtteam.jfrog.io/ldtteam/modding/`)
- CurseMaven (`https://www.cursemaven.com`) for the MineColonies runtime artifact

## Project Layout

```text
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew / gradlew.bat
├── changelog.txt        (Forge MDK changelog, not a project changelog)
├── README.txt           (Forge MDK setup instructions)
├── src/
│   ├── main/java/org/Artificial/beastofburden/   <- all source code
│   ├── main/resources/                           <- mods.toml, mixins, lang, GUI XML
│   ├── generated/resources/                      <- data-gen output (currently empty)
│   └── test/                                     <- empty
├── run/               <- client/server working directory (gitignored)
├── run-data/          <- data-gen working directory (gitignored)
├── minecolonies-release-1.20/  <- local MineColonies source reference (gitignored, not built)
└── com/minecolonies/  <- empty directory tree, gitignored historical artifact
```

### Source Packages

- `org.Artificial.beastofburden` — mod entry class `Beastofburden`, `Config`
- `client` — config screen (`BeastofburdenConfigScreen`) and client setup
- `client.gui` — BlockUI-based Town Hall tab (`BeastofburdenModuleWindow`)
- `colony.buildings` — module producer registration for MineColonies buildings
- `colony.buildings.modules` — `TownHallBeastofburdenModule` and its client view
- `colony.jobs` — custom job registry, `JobBeastofburden`, citizen sounds
- `colony.planning` — autonomous colony planner (heuristic + scripted modes)
- `colony.work` — work status, work log, snapshots
- `config` — config persistence / snapshot helpers
- `entity.ai` — AI skeleton, states, tasks
- `event` — server tick driver, request handler, item-value bootstrap
- `mixin` — Mixin package (Gradle plugin remains wired; no mixins are currently registered)
- `network` — `SimpleChannel` messages
- `util` — request queue, logistics, item value registry, planning helpers

## Build, Run & IDE Commands

All Gradle commands are run from the repository root.

```bash
# Build the reobfuscated mod jar
./gradlew build

# Run a client test instance (working dir: ./run)
./gradlew runClient

# Run a dedicated server test instance (working dir: ./run, --nogui)
./gradlew runServer

# Run the Forge GameTest server (exits after tests; currently no tests are registered)
./gradlew runGameTestServer

# Run data generation (output: src/generated/resources/)
./gradlew runData

# Generate IntelliJ IDEA run configurations
./gradlew genIntellijRuns

# Generate Eclipse run configurations
./gradlew genEclipseRuns

# Refresh cached dependencies
./gradlew --refresh-dependencies

# Clean build outputs (does not touch source)
./gradlew clean
```

### IDE Setup

- Java 17 SDK is required.
- `.idea/runConfigurations/` already contains generated `runClient`, `runServer`, `runGameTestServer`, and `runData` configurations.
- If the run configurations are missing or stale, run `./gradlew genIntellijRuns` and refresh the Gradle project.

### Build Output

- Reobfuscated artifact: `build/libs/beastofburden-<version>.jar`
- `build.gradle` configures `jar { finalizedBy 'reobfJar' }`, so a normal `build` produces the production jar.
- The current workspace already contains `build/libs/beastofburden-1.0-SNAPSHOT.jar`.

## Runtime Architecture

### Mod Entry

`Beastofburden` is annotated with `@Mod("beastofburden")`. In its constructor it:

1. Registers the `BeastofburdenJobs` `DeferredRegister<JobEntry>` on the mod event bus.
2. Registers a common `ForgeConfigSpec` via `Config.SPEC`.
3. Schedules `commonSetup`, which:
   - Verifies the job is registered.
   - Registers the Town Hall building module (`BeastofburdenBuildingModules`).
   - Registers citizen sounds.
   - Registers the network channel (`ModNetwork`).

### Job System

- `BeastofburdenJobs` registers one job: `beastofburden:beastofburden`.
- `JobBeastofburden` extends MineColonies `AbstractJob`, produces `EntityAIBeastofburden`, and uses the plain settler model (`ModModelTypes.SETTLER_ID`).

### Town Hall Module

- `TownHallBeastofburdenModule` is a MineColonies building module attached to the MineColonies `townhall` `BuildingEntry` at runtime.
- Implements `IAssignsJob`, `IPersistentModule`, `IBuildingEventsModule`, `IBuildingWorkerModule`, and `ITickingModule`.
- Capacity scales with Town Hall level: 1 at levels 1-2, 2 at 3-4, 3 at level 5.
- Auto-hires jobless citizens when not full.
- Persists assigned citizens, work log, autonomous-planning state, and the colony planner to NBT.
- Sends a `BeastWorkSnapshot` to the client view (`TownHallBeastofburdenModuleView`) for the GUI.

### AI and Work Driver

- `EntityAIBeastofburden` extends MineColonies `AbstractAISkeleton` and uses `BeastofBurdenState` states.
- It detects stuck colony item requests, generates items after a configurable tick delay, and delivers them.
- Because the Town Hall module only ticks on the colony slow tick, a dedicated `BeastofBurdenWorkDriver` subscribes to `TickEvent.ServerTickEvent` and drives Beast citizens every server tick.
- `ItemValueRegistry` provides item "values" used to compute generation time. Values come from explicit config entries, recipe derivation (if enabled), or a default fallback.

### Autonomous Planning

- `ColonyPlanner` (driven by `ColonyPlannerDriver`) can place new huts, fields, roads, and research autonomously.
- Two planning modes: `HEURISTIC` (`HeuristicPlanningStrategy`) and `SCRIPTED` (`ScriptedPlanningStrategy`).
- Config options (`Config.java`) control intervals, search radius, builder range, max queue, separation, and an `planningInstantBuildDebug` flag that pastes blueprints instantly for testing.
- Planned placements are validated against terrain, existing structures, anchor block rules, builder range, and an internal occupancy map.

### Network

`ModNetwork` uses Forge `SimpleChannel` with protocol version `"1"`. Registered messages:

- `SaveBeastConfigMessage` — client config screen saves config to the server.
- `ToggleAutonomousPlanningMessage` — toggles autonomous planning for a Town Hall.
- `CyclePlanningModeMessage` — cycles heuristic/scripted planning mode.

### GUI

- `BeastofburdenModuleWindow` extends MineColonies `SpecialAssignmentModuleWindow` and uses `assets/beastofburden/gui/layouthuts/layoutbeastofburden.xml`.
- Displays hired beasts, active work with progress bars, work history, and planning status.
- `BeastofburdenConfigScreen` is registered as Forge’s config screen extension point (`ConfigScreenHandler.ConfigScreenFactory`).

### Mixins

- Mixin Gradle (`mixingradle`) remains configured, but `beastofburden.mixins.json` currently has empty `mixins` and `client` lists.
- MineColonies 1.1.873 still shows the Town Hall module sidebar natively, so the 1214-era `shouldRenderDefaultSidebar` injection is not used.

## Code Style Guidelines

The codebase follows the style visible in the existing files. When editing, prefer:

- **Indentation:** 4 spaces.
- **Braces:** Opening brace on the same line (K&R style).
- **Language level:** Java 17 features such as `var`, `switch` expressions, `instanceof` patterns, records, and `Map.of`/`List.of` are used.
- **Nullability:** Use JetBrains `@NotNull` annotations where existing code does.
- **Finality:** Declare local variables and parameters `final` unless mutation is required.
- **Naming:**
  - Classes use `Beastofburden` concatenation (e.g., `BeastofburdenModuleWindow`), not `BeastOfBurden`.
  - Constants are `UPPER_SNAKE_CASE`.
  - Mixin methods (if added later) use the `beastofburden$` prefix.
- **Logging:** Use `com.mojang.logging.LogUtils.getLogger()` and `org.slf4j.Logger`. Prefer parameterized log messages.
- **Resource locations:** Use `ResourceLocation.fromNamespaceAndPath(MODID, path)`.
- **Imports:** Avoid wildcard imports; static imports are used sparingly for MineColonies constants.
- **Javadoc:** Public classes and non-trivial methods should have Javadoc comments.
- **Utility classes:** Keep a private constructor and throw `IllegalStateException("Utility class")`.
- **NBT serialization keys:** Prefer stable, namespaced string constants (`MODULE_KEY = "beastofburden:townhall_beastofburden"`).

## Data Generation & Resources

- `src/generated/resources` is added to the main source set in `build.gradle`.
- There are **currently no data generator classes** and the generated resources folder is empty.
- If data generators are added later, run `./gradlew runData` to populate `src/generated/resources/`.
- Language files exist for `en_us` and `zh_cn`; when adding new UI strings, add entries to both files.
- GUI layouts are BlockUI XML files under `assets/beastofburden/gui/layouthuts/`.

## Testing

- **No unit tests** exist under `src/test/`.
- **No GameTests** are registered yet, although the `runGameTestServer` configuration is set up.
- Testing is currently manual:
  - `./gradlew runClient` for single-player / integrated server behavior.
  - `./gradlew runServer` for dedicated-server behavior.
  - The `run/mods/` folder can be used to drop additional runtime dependency jars when testing outside Gradle.

## Deployment / Distribution

- There is **no CI pipeline** and **no Gradle publishing block** in the current build file.
- To distribute: run `./gradlew build`, then ship `build/libs/beastofburden-1.0-SNAPSHOT.jar` (or the versioned jar).
- End users must install the required MineColonies dependency chain in their modpack.
- The mod declares dependency version ranges in `mods.toml`:
  - Forge: `[47,)`
  - Minecraft: `[1.20.1,1.21)`
  - MineColonies: `[1.1.873,1.1.1214)`

## Security Considerations

- **Network permission checks:** `SaveBeastConfigMessage.handle` verifies `player.hasPermissions(2)` (or single-player) before applying config changes.
- **Input validation:** Item-value config entries are parsed with `ResourceLocation.isValidResourceLocation` and checked against the item registry.
- **Config ranges:** All numeric config values use `defineInRange` to keep values within safe bounds.
- **Debug flags:** `planningInstantBuildDebug` pastes structures instantly and is intended for testing only; keep it disabled for normal survival play.
- **Compatibility risk:** The mod relies on MineColonies internals. It targets 1.1.873 and will not load correctly on 1.1.1214+. Updates to MineColonies can break internal API calls; always verify against the declared version range.
- **Sensitive paths:** The project is on Windows; IDE run configurations contain absolute paths to the local Gradle cache. These are machine-specific and should not be committed if they change.

## Notes for AI Agents

- Design and technical specs for later sessions live under `docs/` (index: `docs/README.md`). `CONTEXT.md` only lists that tree.
- Do **not** modify `minecolonies-release-1.20/`. It is a local reference copy, is gitignored, and is not part of the Gradle project.
- Preserve the `org.Artificial` package name exactly (capital `A`).
- When adding a new network message, register it in `ModNetwork.register()` and bump the protocol string if the wire format changes incompatibly.
- When adding new translation keys, mirror them in both `en_us.json` and `zh_cn.json`.
- If you add data generators, place the output under `src/generated/resources/` so `sourceSets.main.resources` picks it up automatically.
- After structural changes (new mixins, resource files, run configs), run `./gradlew build` or `./gradlew runClient` to verify the dev environment still loads.
