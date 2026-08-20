# MineColonies 挂钩

> 目标版本 **1.1.873**。`mods.toml` 范围 `[1.1.873, 1.1.1214)`。不要用 1.1.1214+ 的 API 想当然。

本地只读参考：`minecolonies-release-1.20/`（gitignored）。禁止改该目录。

## 稳定依赖（Gradle）

MineColonies curse 构件 `6444411`；Structurize / BlockUI / Domum Ornamentum / Multi-Piston 版本见 `AGENTS.md`。加载顺序 `minecolonies` AFTER。

## 使用的内部面

| 能力 | API / 类 | 本模组用法 |
|------|----------|------------|
| Job 注册 | `DeferredRegister<JobEntry>` on `minecolonies:jobs` | `BeastofburdenJobs` |
| 建筑模块 | `BuildingEntry.ModuleProducer`、`IAssignsJob` 等 | 挂 townhall |
| 市民 AI | `AbstractAISkeleton`、`AIWorkerState` | 补给状态机 |
| 请求系统 | `IPlayerRequestResolver`、`IRetryingRequestResolver`、`overruleRequest` | 卡住检测与履约 |
| 仓库 | `hasMatchingItemStackInWarehouse`、rack 提取 | 供应检查；研究支付未接线 |
| 工单 | `IWorkOrder` BUILD/UPGRADE/REPAIR、`requestUpgrade` | 规划产物 |
| 蓝图 | Structurize `Blueprint` / `StructurePacks`、`BlueprintMapping` | 选址与粘贴 |
| 农田 | `FarmField`、`FarmerFieldsModule.assignExtension` | **反射**，升级 MC 时首查 |
| 施工胶带 | `ConstructionTapeHelper.placeConstructionTape` | 反射 |
| GUI | BlockUI + `SpecialAssignmentModuleWindow` | 市政厅页 |
| 权限 | `Action.MANAGE_HUTS` | 规划相关 C2S |

## 已知脆弱点

1. 反射农田 / 胶带：混淆或改名即崩，不要换成「看起来更新」的 1214 API。
2. 模块侧栏：873 原生显示；1214 曾需要 mixin `shouldRenderDefaultSidebar`（已删除）。
3. `AbstractBlockHut.onBlockPlacedByBuildTool`、level 0 + requestUpgrade 是规划落 hut 的契约，改 MC 建造流程时必回归。
4. Job 模型必须 `SETTLER_ID`，`CITIZEN_ID` 会裁切。

升版本 checklist：对照本页 API → 跑 `runClient` 开市政厅页 → 冷启动第一座建造者 → 卡住请求生成配送 → 固定式走完第 1–5 步。
