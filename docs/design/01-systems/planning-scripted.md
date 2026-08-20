# 固定式规划（SCRIPTED）

> 状态：**已实现**（默认模式）  
> 代码：`ScriptedPlanningStrategy`、`FixedPlanScript`、`PlanScriptValidator`  
> 默认内容：[../04-content/default-plan.md](../04-content/default-plan.md)  
> 编辑器：[../03-ui/ui.md](../03-ui/ui.md)

## 玩家心智

一份有序列表。当前步骤的所有需求都 **建成且达标** 后，才进入下一步。编辑器可增删改排序，可恢复默认 12 步。

## 步骤与需求

一步可含多条需求（例：默认第 5 步 = 农夫×1 **且** 农田×2）。

| 字段 | 规则 |
|------|------|
| `kind` | `BUILDING` 或 `FIELD` |
| `type` | 建筑 schematic id；农田仍带 `FARMER` 类型作占位 |
| `level` | 最低等级 1–5 |
| `count` | 1–10 |

校验（`PlanScriptValidator`）：最多 **32** 步；每步需求非空；**禁止 TOWN_HALL**；非法则拒绝保存。

`FORMAT_VERSION = 1`。与默认比有差异则 `custom = true`。

## 推进规则（重要）

两套计数不要混：

| 用途 | 口径 |
|------|------|
| 要不要再下一座 / 升级 | **已承诺** = 已建 + 在建工单 |
| 步骤能不能 +1 | **运营完成** = 已建且等级 ≥ minLevel（农田看已注册田数） |

因此：步骤会停在「工单施工中」，直到建造者盖完才翻页。这是故意的，避免计划跑在施工前面。

对每条 BUILDING 需求：

- 已承诺数量 < 需要数量 → `BUILD_NEW`
- 否则找最低等级、未达 minLevel、且该坐标没有在建工单的小屋 → `UPGRADE`

对 FIELD：已有田 < 需要数量 → 对「缺田的农夫」`PLACE_FIELD`。

所有固定式任务优先级常量 `900f`。

## 完成态

全部步骤运营完成后：`scripted_complete`，不再选任务。UI 用 `com.beastofburden.gui.townhall.scripted.complete`。

状态描述：`step_{i+1}/{total} - {需求进度}`。

## 保存

客户端 `SaveColonyPlanMessage` → 服务端校验 → 写入该市政厅模块的 planner NBT。计划内容变化时重置 `stepIndex`。
