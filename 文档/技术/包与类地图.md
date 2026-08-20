# 包与类地图

根包：`org.Artificial.beastofburden`（`Artificial` 的 A 必须大写）。

| 包 | 职责 | 入口类 |
|----|------|--------|
| ` `（根） | 模组入口、Forge 配置 | `Beastofburden`、`Config` |
| `client` | 配置屏、客户端 setup | `ClientSetup`、`BeastofburdenConfigScreen` |
| `client.gui` | 市政厅页、计划编辑器、建筑选择 | `BeastofburdenModuleWindow` |
| `colony.buildings` | 把模块挂到 townhall | `BeastofburdenBuildingModules` |
| `colony.buildings.modules` | 模块本体与客户端 View | `TownHallBeastofburdenModule` |
| `colony.jobs` | Job 注册、声音 | `BeastofburdenJobs`、`JobBeastofburden` |
| `colony.planning` | 自治规划全套 | `ColonyPlanner`、`PlanningService` |
| `colony.work` | 工作状态 / 日志 / 快照 | `BeastWorkSnapshot` |
| `config` | 配置运行时应用 | `ConfigPersistence`、`ConfigSnapshot` |
| `entity.ai` | AI 骨架 | `EntityAIBeastofburden` |
| `entity.ai.states` | 自定义 AI 状态 | `BeastofBurdenState` |
| `entity.ai.tasks` | 生成任务 | `ItemGenerationTask` |
| `event` | 服务端 tick、请求扫描、价值 bootstrap | `BeastofBurdenWorkDriver` |
| `command` | 调试命令 | `BeastofburdenCommands` |
| `network` | SimpleChannel | `ModNetwork` |
| `util` | 物流、价值、队列、MC API 封装 | 见下 |
| `mixin` | 空包；json 未注册任何 mixin | — |

## util 要点

| 类 | 用途 |
|----|------|
| `BeastofBurdenAiDriver` | 保证 AI 实例并在有活时 tick |
| `BeastWorkSync` | job → 模块状态/日志 |
| `BeastofBurdenRequestQueue` | 每殖民地卡住请求队列 |
| `UnfulfillableRequestDetector` | 卡住判定 |
| `ColonySupplyChecker` | 仓库/建筑能否供应 |
| `ColonyLogistics` | 履约、早期物流 |
| `RequestItemUtils` | 从请求抽 ItemStack、配送点 |
| `ItemValueRegistry` | 物品价值 |
| `ColonyBuildings` | 建筑聚合 |
| `ColonyWorkforce` | 床、空岗、无业 |
| `ColonyFieldSupport` | 农田扩展（反射） |
| `ColonyWarehouseResearch` | 仓库扣研究材料 — **无调用点** |
| `ConstructionTapeSupport` | 施工胶带（反射） |
| `BeastofBurdenLog` | debug 日志门闩 |

## 资源

| 路径 | 用途 |
|------|------|
| `assets/beastofburden/gui/layouthuts/layoutbeastofburden.xml` | 市政厅页 |
| `assets/beastofburden/lang/en_us.json` | 英文 |
| `assets/beastofburden/lang/zh_cn.json` | 中文 |
| `META-INF/mods.toml` | 依赖范围 |
| `beastofburden.mixins.json` | 空 mixin 列表 |
