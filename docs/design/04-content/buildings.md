# 可规划建筑目录

> 状态：**目录已实现**；启发式只用其中一小撮，其余给固定式编辑器  
> 代码：`PlannedBuildingType`  
> 未知 schematic id 回落为 `BUILDER`

`capacityBuilding` 标志位已写入枚举，**当前无读取方**，不要当规则用。

分类用于选址加分（物流靠中心、防御靠外等），见 [placement.md](../01-systems/placement.md)。

| 枚举 | schematic | 分类 | 启发式会点？ |
|------|-----------|------|--------------|
| BUILDER | builder | INFRASTRUCTURE | 是（冷启动 / 覆盖 / 升级） |
| TOWN_HALL | townhall | INFRASTRUCTURE | 仅升级；编辑器禁止新建 |
| RESIDENCE | home | INFRASTRUCTURE | 是（住房压力） |
| TAVERN | tavern | INFRASTRUCTURE | 是（第一座） |
| WAREHOUSE | warehouse | LOGISTICS | 是 |
| COURIER | deliveryman | LOGISTICS | 是 |
| FARMER | farmer | FOOD | 是 + 农田 |
| COOK | cook | FOOD | 是（有食物来源后） |
| FORESTER | lumberjack | RESOURCE | 是（人口≥3 且还没有） |
| MINER | miner | RESOURCE | 同上 |
| GUARD_TOWER | guardtower | DEFENSE | 是（人口≥6 且守卫比低） |
| UNIVERSITY | university | CIVIC | 是（需工艺链， practically 难触发） |
| FISHER | fisherman | FOOD | 仅固定式 |
| BAKERY | bakery | FOOD | 仅固定式 |
| SHEPHERD / COWBOY / CHICKEN_HERDER / SWINE_HERDER | 对应畜牧 | FOOD | 仅固定式 |
| COMPOSTER / FLORIST | | RESOURCE | 仅固定式 |
| SAWMILL / STONEMASON / BLACKSMITH / SMELTERY | | CRAFTING | 仅固定式 |
| GLASSBLOWER / FLETCHER / ENCHANTER / STONE_SMELTERY / CRUSHER / SIFTER | | CRAFTING | 仅固定式 |
| LIBRARY / SCHOOL / HOSPITAL / MYSTICAL_SITE | | CIVIC | 仅固定式 |
| BARRACKS / ARCHERY / COMBAT_ACADEMY | | DEFENSE | 仅固定式 |

增删类型：改枚举 → 编辑器自动出现；启发式要另加候选；翻译若有独立键则双语言补齐。蓝图名来自 hut 方块 `getBlueprintName()`，路径 `BlueprintPaths.pathFor(type, level)`。
