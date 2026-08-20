# 已知洞（遗留 / 未接线）

改功能前先看此页，避免「接上死代码」或「修一个其实是故意停用的系统」而不更新策划。

| 项 | 现状 | 不要做的事 |
|----|------|------------|
| `ColonyPhase` P0–P4 | 枚举、GUI、NBT、翻译仍在；`ColonyPlanner` 恒 P0 | 不要把 UI 阶段当成进度；要恢复阶段须先写策划再接线 |
| `planningPhaseEvaluationInterval` | 仅 Config | 改它无效 |
| `ColonyWarehouseResearch` | 可从仓库扣研究材料 | **零调用点**；接上需规划 tick + 大学建筑 |
| `planningResearchTickInterval` | 仅 Config | 改它无效 |
| `planningRequireRoadAccess` | `PlanningConfig` 暴露 | 选址只把道路当分数，不是硬门 |
| `planningPlacementFailureCooldown` | 仅 Config | 失败用 `planningRetryCooldown` |
| `PlannedBuildingType.capacityBuilding` | 枚举字段 | 无读取方 |
| `planningTraceLogging` | 会 load 到静态字段 | 不要假设所有门控都打 `[beastofburden/trace]` |
| 瞬间建造 | 命令 + 内存 flag | 不是 `Config` 项，重启失效 |
| Mixin 类 | json 列表为空 | 不要默默加回 1214 侧栏 mixin，除非升版本并改 json |
| 启发式工艺链 | 大学分依赖锯木+石匠+铁匠 | 启发式不建这三座，大学几乎不会被点 |
| 固定式第 11 步 | BUILDER minLevel 2 count 1 | 可能升级现有建造者，不一定新建第二座 |

策划已标明的非目标见 [../design/00-overview/vision.md](../design/00-overview/vision.md)。接上表中任何一项，必须同步策划状态标记（遗留 → 已实现）。
