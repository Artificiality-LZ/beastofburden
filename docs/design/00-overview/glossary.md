# 术语表

> 策划与代码共用同一套名字。UI 中文见 `zh_cn.json`，代码标识用英文枚举。

## 产品名

| 中文 | 代码 / 键 | 说明 |
|------|-----------|------|
| 牛马 | `JobBeastofburden`，job id `beastofburden:beastofburden` | 市政厅职业 |
| 本模组 | mod id `beastofburden` | 包名 `org.Artificial.beastofburden`（`Artificial` 的 A 大写） |

## 工作循环

| 术语 | 代码 | 含义 |
|------|------|------|
| 卡住请求 | `UnfulfillableRequestDetector` | 殖民地请求系统里，正常物流短期内无法满足的可配送请求 |
| 早期物流 | `ColonyLogistics.isEarlyLogistics` | 没有仓库 **或** 没有在职工配送员 |
| 生成 | `BeastWorkPhase.GENERATING` | 按物品价值耗时，凭空产出物品 |
| 配送 | `BeastWorkPhase.DELIVERING` | 走到请求目标并 `fulfillRequest` |
| 物品价值 | `ItemValueRegistry` | 用于生成耗时，不是经济货币 |
| 力量 | `Skill.Strength` | 主技能，加快生成 |
| 适应力 | `Skill.Adaptability` | 副技能，当前无额外公式 |

## 规划

| 术语 | 代码 | 含义 |
|------|------|------|
| 自主规划 | `autonomousPlanning` | 市政厅开关；关则完全不跑规划 |
| 固定式 | `PlanningMode.SCRIPTED`（默认） | 按 `FixedPlanScript` 步骤走 |
| 启发式 | `PlanningMode.HEURISTIC`（实验） | 每轮打分选最高优先任务 |
| 冷启动 | `ColdStartManager` | 尚无「可运营建造者小屋」时，先落下第一座建造者 |
| 可运营建造者小屋 | `level > 0` 或 `isBuilt()` 的 builder | 未建成的 builder 不算 |
| 步骤完成 | `ScriptedPlanProgress.isStepOperationalComplete` | **已建成** 且达到最低等级，不是「已下工单」 |
| 已承诺数量 | committed = 已建 + 在建工单 | 固定式用它决定要不要再下一座 |
| 工单 | MineColonies `IWorkOrder` BUILD/UPGRADE/REPAIR | 规划成功后交给建造者 |
| 瞬间建造 | `PlanningInstantBuildState` | 调试：粘贴蓝图并立刻完工；**不写配置文件**，用命令开关 |
| 发展阶段 | `ColonyPhase` P0–P4 | **遗留**。规划逻辑恒返回 P0_FOUNDATION |

## 内容对象

| 术语 | 代码 | 含义 |
|------|------|------|
| 可规划建筑 | `PlannedBuildingType` | 规划器认识的小屋类型，映射 `ModBuildings` |
| 农田步骤 | `FixedPlanRequirement.Kind.FIELD` | 给需要田的农夫贴 `basicfield` |
| 占地 | `BuildingFootprint` / `OccupancyMap` | 蓝图 AABB + 最小间隔 |
| 锚点 | hut 方块位置 | 选址网格步长 3 |

## 容量

市政厅等级 → 可雇牛马数：`max(1, min(3, (level + 1) / 2))` → 1–2 级 1 人，3–4 级 2 人，5 级 3 人。
